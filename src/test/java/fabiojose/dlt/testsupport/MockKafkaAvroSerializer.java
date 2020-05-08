package fabiojose.dlt.testsupport;

import java.util.Map;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;

/**
 * Utiliza schema registry mock.
 * <br>
 * <a href="https://medium.com/@igorvlahek1/no-need-for-schema-registry-in-your-spring-kafka-tests-a5b81468a0e1">Referência</a>
 */
@Slf4j
public class MockKafkaAvroSerializer extends KafkaAvroSerializer {

    public MockKafkaAvroSerializer() {
        super();
        super.schemaRegistry = new MockSchemaRegistryClient();

        log.info("MOCK SCHEMA REGISTRY FOR TESTS");
    }

    public MockKafkaAvroSerializer(SchemaRegistryClient client){
        super(new MockSchemaRegistryClient());
        log.info("MOCK SCHEMA REGISTRY FOR TESTS");
    }

    public MockKafkaAvroSerializer(SchemaRegistryClient client,
            Map<String, ?> props){
        super(new MockSchemaRegistryClient(), props);
        log.info("MOCK SCHEMA REGISTRY FOR TESTS");
    }
}