APP_KAFKA_CONSUMER_TOPICS='some-event-t' \
KAFKA_CLIENT_ID='spring-kafka-dlt-ex' \
KAFKA_CONSUMER_GROUP='spring-kafka-dlt-ex' \
KAFKA_BOOTSTRAP_SERVERS='localhost:9092' \
SCHEMA_REGISTRY_URL='http://localhost:8081' \
KAFKA_FAIL_WHEN_MISSING_TOPICS='false' \
java -Dserver.port='38002' -jar target/app-spring-boot.jar
