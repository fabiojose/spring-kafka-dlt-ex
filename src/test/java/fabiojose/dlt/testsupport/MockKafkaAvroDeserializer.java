package fabiojose.dlt.testsupport;

import java.io.IOException;

import org.apache.avro.Schema;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;

/**
 * Utiliza schema registry mock.
 * <br>
 * <a href="https://medium.com/@igorvlahek1/no-need-for-schema-registry-in-your-spring-kafka-tests-a5b81468a0e1">Referência</a>
 */
public class MockKafkaAvroDeserializer extends KafkaAvroDeserializer {

    @Override
    public Object deserialize(String topic,  byte[] bytes) {

        try{
            if (topic.equals("configures-me-consumer-topics")) {
                this.schemaRegistry = getMockClient(new Schema.Parser().parse(
                    getClass().getResourceAsStream("/avro/NewProductAddedToCatalog.avsc")));
            }

            return super.deserialize(topic, bytes);

        }catch(IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static SchemaRegistryClient getMockClient(final Schema schema$) {
        return new MockSchemaRegistryClient() {
            @Override
            public synchronized Schema getById(int id) {
                return schema$;
            }
        };
    }
}