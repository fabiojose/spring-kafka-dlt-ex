package fabiojose.dlt.kafka;

import static org.springframework.kafka.support.KafkaHeaders.DLT_ORIGINAL_TOPIC;

import java.util.Optional;
import java.util.function.BiFunction;

import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.util.backoff.FixedBackOff;

import lombok.extern.slf4j.Slf4j;

/**
 * @author fabiojose
 */
@EnableKafka
@Configuration
@Slf4j
public class KafkaConfig {

    /**
     * Retentar por 3 vezes, antes de iniciar fluxo DLT.
     */
    static final FixedBackOff RETENTAR_3X = new FixedBackOff(0, 2);
    static final FixedBackOff NENHUM = new FixedBackOff(0, 0);

    private static final String NENHUM_CABECALHO = "__$$none";
    private static final int QUALQUER_PARTICAO = -1;

    @Autowired
    Excecoes excecoes;

    @Value("${spring.kafka.listener.missing-topics-fatal}")
    Boolean missingTopicsFatal;

    @Value("${app.kafka.dlt.retry.topics}")
    int retryTopicsCount;

    @Value("${app.kafka.dlt.retry.topics.pattern}")
    String retryTopicsPattern;

    @Value("${app.kafka.dlt.retry.topic.first}")
    String retryFirstTopic;

    @Value("${app.kafka.consumer.topics}")
    String topicoOriginal;

    @Value("${app.kafka.dlt.topic}")
    String dltTopic;

    @Bean
    public ConsumerFactory<?, ?> consumerFactory(
            KafkaProperties properties) {
        
        return new DefaultKafkaConsumerFactory<>(
                properties.buildConsumerProperties());
    }

    /**
     * É uma exceção classificada como recuperável?
     */
    private boolean isRecuperavel(Exception e) {
        
        // Qualquer erro, será não-recuperável
        boolean result = false;

        // Obter causa raiz
        Throwable causa =
            ExceptionUtils.getRootCause(e);

        log.error(causa.getMessage(), causa);

        result = excecoes.getRecuperaveis()
            .contains(causa.getClass());

        log.info("{} é {}recuperável", causa.getClass(),
            (result ? "" : "não-"));

        return result;
    }

    /**
     * Tópico origem do registro
     */
    private Optional<String> topicoOrigem(Headers headers) {

        return 
        Optional.ofNullable(headers.lastHeader(DLT_ORIGINAL_TOPIC))
            .map(Header::value)
            .map(bytes -> new String(bytes));

    }

    /**
     * Resolve o tópico + partição com base no registro e exceção
     */
    @Bean
    public BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition>
            mainResolver() {

        return new BiFunction<ConsumerRecord<?,?>,Exception,TopicPartition>() {
            @Override
            public TopicPartition apply(ConsumerRecord<?, ?> r, Exception e) {

                // Erro não-recuperável?
                //  segue diretamente para o tópico dead-letter
                TopicPartition result = 
                    new TopicPartition(dltTopic,
                        QUALQUER_PARTICAO);

                final boolean recuperavel = isRecuperavel(e);
                if(recuperavel){
                    // Erro recuperável
                    Optional<String> origem = 
                        topicoOrigem(r.headers())
                            .or(() -> Optional.of(NENHUM_CABECALHO));

                    log.info("Tópico de origem {}", origem);

                    /*
                     * Se origem é outro tópico, então segue para o
                     * primeiro tópico retry
                     */
                    String destino = 
                    origem
                        .filter(topico -> !topico.matches(retryTopicsPattern))
                        .map(t -> retryFirstTopic)
                        .orElse(dltTopic);

                    log.info("Tópico destino do registro {}", destino);

                    result = new TopicPartition(destino, QUALQUER_PARTICAO);
                }

                return result;
            }
        };
    }

    @Bean
    public SeekToCurrentErrorHandler mainErrorHandler(
            @Qualifier("mainResolver")
            BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> resolver,
            KafkaTemplate<?, ?> template) {

        // Recuperação usando dead-letter 
        DeadLetterPublishingRecoverer recoverer = 
            new DeadLetterPublishingRecoverer(template, resolver);

        /* Tentar processar por 3x localmente antes de iniciar o fluxo e
         * talvez chegar no tópico dead-letter
         */
        SeekToCurrentErrorHandler handler = 
            new SeekToCurrentErrorHandler(recoverer, RETENTAR_3X);

        /*
         * Lista com exceções que não devem passar pelo retry local, ou seja,
         * devem seguir direto para processo dead-letter
         */ 
        excecoes.getNaoRecuperavies().forEach(e -> 
            handler.addNotRetryableException(e));

        return handler;
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, GenericRecord>>
        mainKafkaListenerContainerFactory(
                @Qualifier("mainErrorHandler")
                SeekToCurrentErrorHandler errorHandler,
                KafkaProperties properties,
                ConsumerFactory<String, GenericRecord> factory){

        ConcurrentKafkaListenerContainerFactory<String, GenericRecord> listener = 
            new ConcurrentKafkaListenerContainerFactory<>();
      
        listener.setConsumerFactory(factory);
        listener.setErrorHandler(errorHandler);

        // Falhar, caso os tópicos não existam?
        listener.getContainerProperties()
            .setMissingTopicsFatal(missingTopicsFatal);

        // Commit do offset no registro, logo após processá-lo no listener
        listener.getContainerProperties().setAckMode(AckMode.MANUAL);

        // Commits síncronos
        listener.getContainerProperties().setSyncCommits(Boolean.TRUE);

        return listener;
    }

    /**
     * Retry tp resolver
     */
    @Bean
    public BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition>
        retryResolver() {

        return new BiFunction<ConsumerRecord<?,?>,Exception,TopicPartition>() {
            @Override
            public TopicPartition apply(ConsumerRecord<?, ?> r, Exception e) {

                //Verificar tópico de origem
                Optional<String> origem = 
                    topicoOrigem(r.headers())
                        .filter(t -> t.matches(retryTopicsPattern))
                        .or(() -> Optional.of(retryFirstTopic));

                log.info("Tópico origem {}", origem.get());

                String destino = 
                    origem
                        .filter(topico -> topico.matches(retryTopicsPattern))
                        .map(t -> t.substring(t.lastIndexOf("-")))
                        .map(n -> n.split("-"))
                        .map(n -> n[1])
                        .map(Integer::parseInt)
                        .filter(n -> n < retryTopicsCount)
                        .map(n -> 
                            origem.get().substring(0,
                                origem.get().lastIndexOf("-")) + "-" + (n + 1))
                        .orElse(dltTopic);

                log.info("Tópico destino do registro {}", destino);

                return new TopicPartition(destino, QUALQUER_PARTICAO);
            }
        };
    }

    /**
     * Retry tp error handler
     */
    @Bean
    public SeekToCurrentErrorHandler retryErrorHandler(
        @Qualifier("retryResolver")
        BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> resolver,
        KafkaTemplate<?, ?> template) {

        // Recuperação usando dead-letter 
        DeadLetterPublishingRecoverer recoverer = 
            new DeadLetterPublishingRecoverer(template, resolver);

        // Nenhuma retentativa local
        SeekToCurrentErrorHandler handler = 
            new SeekToCurrentErrorHandler(recoverer, NENHUM);

        return handler;
    }

    /**
     * Retry kafka factory
     */
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, GenericRecord>>
        retryKafkaListenerContainerFactory(
            @Qualifier("retryErrorHandler")
            SeekToCurrentErrorHandler errorHandler,
            KafkaProperties properties,
            ConsumerFactory<String, GenericRecord> factory){

        ConcurrentKafkaListenerContainerFactory<String, GenericRecord> listener = 
            new ConcurrentKafkaListenerContainerFactory<>();

        listener.setConsumerFactory(factory);
        listener.setErrorHandler(errorHandler);

        // Falhar, caso os tópicos não existam?
        listener.getContainerProperties()
            .setMissingTopicsFatal(missingTopicsFatal);

        // Commit do offset no registro, logo após processá-lo no listener
        listener.getContainerProperties().setAckMode(AckMode.MANUAL);

        // Commits síncronos
        listener.getContainerProperties().setSyncCommits(Boolean.TRUE);

        return listener;
    }
}