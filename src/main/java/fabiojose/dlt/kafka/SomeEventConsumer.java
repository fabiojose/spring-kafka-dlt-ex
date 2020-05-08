package fabiojose.dlt.kafka;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * @author fabiojose
 */
@Service
@Slf4j
public class SomeEventConsumer {

    /**
     * Lógica de neǵocio
     */
    private void process(ConsumerRecord<String, GenericRecord> r) {

        final GenericRecord value = r.value();

        log.info("Valor do registro {}", value);

        //TODO Sua lógica c/ algo que possa falhar

    }

    @KafkaListener(
        id = "main-kafka-listener",
        topics = "${app.kafka.consumer.topics}",
        containerFactory = "mainKafkaListenerContainerFactory"
    )
    public void consume(@Payload ConsumerRecord<String, GenericRecord> record,
        Acknowledgment ack) throws Exception {

        try{
            log.info("Processar registro {}", record.value());

            // Processar
            process(record);

        }finally {
            // Sempre confirmar o offset
            ack.acknowledge();
        }

    }

    @KafkaListener(
        id = "retry-kafka-listener",
        topicPattern = "${app.kafka.dlt.retry.topics.pattern}",
        containerFactory = "retryKafkaListenerContainerFactory",
        properties = {
            "fetch.min.bytes=${app.kafka.dlt.retry.min.bytes}",
            "fetch.max.wait.ms=${app.kafka.dlt.retry.max.wait.ms}"
        }
    )
    public void retry(@Payload ConsumerRecord<String, GenericRecord> record,
        Acknowledgment ack) throws Exception{

        try{

            log.info("Reprocessar registro {}", record.value());

            // Reprocessar
            process(record);

        }finally {
            // Sempre confirmar o offset
            ack.acknowledge();
        }

    }
}