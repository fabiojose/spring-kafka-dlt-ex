# spring-kafka-dlt-ex

Dead-letter Topico com Spring Kafka e formato de dados Avro.

## Requerimentos

- JDK 1.8
- Apache Maven 3.6+
- Docker 19+
- Acesso ao repositório https://repo.maven.apache.org/maven2/ ou uma 
alternativa com acesso às dependências presentes no `pom.xml`
- Schema Registry
- Kafka
- Testcontainers
- Lombok
- Commons Lang3

## Configurações

Não se preocupe, pois apesar de existirem atalhos pelas variávies
de ambiente, você pode utilizar tranquilamente aquilo que o Spring Boot
oferece. Então veja todos as propriedades no 
[application.properties](./src/main/resources/application.properties)

No caso do Kafka, utilizamos Spring Kafka, então você utilizar 
o modo Spring para configurações.

### Variáveis de Ambiente

- `APP_KAFKA_CONSUMER_TOPICS`: tópicos para consumir, ou expressão.
- `KAFKA_CLIENT_ID`: nome do cliente Kafka, usado pelos brokers para logs e 
métricas. Utilize um [nome clean](https://medium.com/coding-skills/clean-code-101-meaningful-names-and-functions-bf450456d90c), não genérico.
  - `spring.kafka.producer.client-id`, `spring.kafka.consumer.client-id`
- `KAFKA_CONSUMER_GROUP`: nome do grupo de consumo que esta aplicação pertence
  - `spring.kafka.consumer.group-id`
- `KAFKA_BOOTSTRAP_SERVERS`: lista de brokers para o cluster Kafka
  - `spring.kafka.bootstrap-servers`
- `SCHEMA_REGISTRY_URL`: url para o registro de esquemas Avro 
  - `spring.kafka.properties.schema.registry.url`
- `KAFKA_FAIL_WHEN_MISSING_TOPICS` (`true` | `false`): configure `true` em
ambientes restritos, onde a criação automática de tópicos é bloqueada.
Assim a aplicação só iniciará se eles existirem.
  - `spring.kafka.listener.missing-topics-fatal`

- `APP_LOG_LEVEL` _(opcional)_: nível de registro para logs. Padrão é `INFO`

Tabela com os níveis de registro e sua hierarquia:

| `APP_LOG_LEVEL` | TRACE  | DEBUG  | INFO   | WARN   | ERROR  |
| --------------- | :----: | :----: | :----: | :----: | :----: |
| TRACE           |  sim   |  não   |  não   |  não   |  não   |
| DEBUG           |  sim   |  sim   |  não   |  não   |  não   |
| INFO            |  sim   |  sim   |  sim   |  não   |  não   |
| WARN            |  sim   |  sim   |  sim   |  sim   |  não   |
| ERROR           |  sim   |  sim   |  sim   |  sim   |  sim   |

- `APP_LOG_PATTERN` _(opcional)_: padrão do registro de logs. Esse padrão
segue o formato documentado [aqui](https://logback.qos.ch/manual/layouts.html),
porém, veja um exemplo na configuração
[logback.groovy](src/main/resources/logback.groovy)

- `APP_LOG_LOGGERS` _(opcional)_: para configurar níveis de log de loggers
presentes nas dependências da aplicação.
```bash
# logger_1:LEVEL,logger_2:LEVEL,...
APP_LOG_LOGGERS='io.netty:ERROR,org.springframework:ERROR'
```

## Build & Run

Depois de utilizar uma das duas formas para build & run, a documentação swagger
estará disponível em:

- http://localhost:8080/swagger-ui.html

### Maven

Para montar o fatjar, execute o comando:

```bash
mvn clean package
```

Para rodar e iniciar a aplicação na porta `38002`:

> Utilize o [docker-compose](./docker-compose.yaml) e inicie todos os serviços
para testes

O exemplo abaixo também está disponível no script [run.sh](./run.sh).

```bash
# Exemplo de execução
APP_KAFKA_CONSUMER_TOPICS='some-event-t' \
KAFKA_CLIENT_ID='spring-kafka-dlt-ex' \
KAFKA_CONSUMER_GROUP='spring-kafka-dlt-ex' \
KAFKA_BOOTSTRAP_SERVERS='localhost:9092' \
SCHEMA_REGISTRY_URL='http://localhost:8081' \
KAFKA_FAIL_WHEN_MISSING_TOPICS='false' \
java -Dserver.port='38002' -jar target/app-spring-boot.jar
```

### Docker

A definição [Dockerfile](./Dockerfile) desta aplicação emprega 
[multi-stage builds](https://docs.docker.com/develop/develop-images/multistage-build/).
Isso significa que nela acontece o build da aplicação e a criação da imagem.

Se for necessário somente a criar a imagem, pode-se utilizar a definição 
[Dockerfile-image](./Dockerfile-image). Mas antes é necessário montar
o fatjar através do maven.

Para build do fatjar e montar a imagem, execute o comando:

```bash
docker build . -t spring-kafka-dlt-ex:1.0
```

Para montar apenas a imagem (antes é necessário o build do maven):

```bash
docker build -f Dockerfile-image . -t spring-kafka-dlt-ex:1.0
```

Para rodar o container:

> Utilize o [docker-compose](./docker-compose.yaml) e inicie todos os serviços
para testes

```bash
docker run -p 8080:8080 \
       -i --rm \
       -e APP_KAFKA_CONSUMER_TOPICS='some-event-t' \
       -e KAFKA_CLIENT_ID='spring-kafka-dlt-ex' \
       -e KAFKA_CONSUMER_GROUP='spring-kafka-dlt-ex' \
       -e KAFKA_BOOTSTRAP_SERVERS='localhost:9092' \
       -e SCHEMA_REGISTRY_URL='http://localhost:8081' \
       -e KAFKA_FAIL_WHEN_MISSING_TOPICS='false' \
       spring-kafka-dlt-ex:1.0
```

## Cobertura

- Executar os testes

```bash
mvn clean test
```

- Acessar o relatório: `target/site/jacoco/index.html`

## Caminho HTTP p/ Verificações de Saúde

> Health Checks

Essas são configurações comuns quando a implantação do aplicação 
é feita em container, especificamente no Kubernetes.

- [Liveness Probe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/#define-a-liveness-http-request)
  - `/actuator/info`

- [Readiness Probe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/#define-readiness-probes)
  - `/actuator/health`

## Dicas

### Docker Compose

Neste repositório existe o arquivo [docker-compose.yaml](./docker-compose.yaml),
que inicia todas as partes móveis que a aplicação depende.

Iniciar a stack:

```bash
docker-compose up
```

Serviços presentes na stack

- Kafka: `localhost:9092`
- Jaeger UI: `http://localhost:16686`
