package fabiojose.dlt.kafka;

import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import fabiojose.dlt.App;
import fabiojose.dlt.testsupport.MockKafkaAvroDeserializer;
import fabiojose.dlt.testsupport.MockKafkaAvroSerializer;

/**
 * @author fabiojose
 */
@Tag("integration")
@ExtendWith(SpringExtension.class)
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = { App.class }
)
@Testcontainers
public class SomeEventConsumerTest {

    @Container
    private static KafkaContainer KAFKA = new KafkaContainer();

    @Value("${app.kafka.consumer.topics}")
    String topico;

    @Autowired
    KafkaTemplate<String, GenericRecord> producer;

    @BeforeAll
    public static void beforeAll() {

        // Configurar o endereço do kafka iniciado pelo test containers.
        System.setProperty("KAFKA_BOOTSTRAP_SERVERS",
            KAFKA.getBootstrapServers());

        // Configurar serializer com mock schema registry
        System.setProperty("spring.kafka.producer.value-serializer", 
            MockKafkaAvroSerializer.class.getName());

        // Configurar deserializer com mock schema registry
        System.setProperty("spring.kafka.consumer.value-deserializer", 
            MockKafkaAvroDeserializer.class.getName());

    }

    @Test
    public void seu_teste_aqui() throws Exception {

    }
}