# 💳 Messaging — Fluxo de Pagamento com RabbitMQ

Projeto desenvolvido em **Spring Boot 4.1 + Java 26** para praticar **mensageria assíncrona com RabbitMQ**, simulando um fluxo completo de pagamento e geração de código de rastreio logístico.

---

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Fluxo da Aplicação](#fluxo-da-aplicação)
- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Endpoints REST](#endpoints-rest)
- [Filas e Exchanges RabbitMQ](#filas-e-exchanges-rabbitmq)
- [Status do Pedido](#status-do-pedido)
- [Executando com Docker](#executando-com-docker)
- [Executando Localmente](#executando-localmente)
- [Exemplos de Uso](#exemplos-de-uso)
- [Dicas para Testes](#dicas-para-testes)

---

## Visão Geral

Esta aplicação simula um sistema de e-commerce com processamento assíncrono de pagamentos. O cliente envia os dados do cartão e número do pedido via REST, e o restante do processamento acontece de forma **totalmente assíncrona** via mensageria, sem bloquear a resposta ao cliente.

---

## Fluxo da Aplicação

```
Cliente
  │
  │  POST /payments (dados do cartão + nº pedido)
  ▼
┌─────────────────────┐
│   PaymentController │  ── salva pedido (PENDING) no banco
└─────────────────────┘
          │
          │ publica em payment.exchange
          ▼
    ┌──────────────┐
    │ payment.queue │
    └──────────────┘
          │
          │ consome
          ▼
┌──────────────────────┐
│   PaymentConsumer    │  ── chama FakePaymentGateway (80% aprovação)
└──────────────────────┘
     │              │
  APROVADO       RECUSADO
     │              │
     │         atualiza pedido
     │         → PAYMENT_DECLINED
     │
     │  atualiza pedido → PAYMENT_APPROVED
     │  publica em logistics.exchange
     ▼
  ┌────────────────┐
  │ logistics.queue │
  └────────────────┘
          │
          │ consome
          ▼
┌───────────────────────┐
│   LogisticsConsumer   │  ── chama FakeLogisticsService (90% sucesso)
└───────────────────────┘
          │
     ┌────┴─────┐
  SUCESSO     FALHA
     │            │
     │       atualiza pedido
     │       → LOGISTICS_FAILED
     │
     │  atualiza pedido com trackingCode
     └─ → TRACKING_GENERATED

Cliente
  │
  │  GET /orders/{orderNumber}
  └─ consulta status + código de rastreio
```

---

## Arquitetura

```
┌─────────────────────────────────────────────────────┐
│                   Spring Boot App                    │
│                                                      │
│  controller/     → REST endpoints                    │
│  service/        → lógica de negócio + fakes         │
│  producer/       → publica mensagens no RabbitMQ     │
│  consumer/       → consome filas do RabbitMQ         │
│  config/         → configuração de exchanges/filas   │
│  model/          → entidade JPA Order + enum Status  │
│  dto/            → PaymentRequest, OrderResponse,    │
│                     PaymentMessage, LogisticsMessage  │
│  repository/     → OrderRepository (JPA/H2)          │
└───────────────────────────┬─────────────────────────┘
                            │ AMQP
                ┌───────────▼───────────┐
                │       RabbitMQ        │
                │  management UI :15672 │
                └───────────────────────┘
```

**Banco de Dados:** H2 in-memory (sem persistência entre reinicializações — ideal para praticar).
Console H2 disponível em `http://localhost:8080/h2-console`.

---

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 26 | Linguagem |
| Spring Boot | 4.1.0 | Framework principal |
| Spring AMQP | 4.1.0 | Integração RabbitMQ |
| Spring Data JPA | 4.1.0 | Persistência |
| Spring Validation | 4.1.0 | Validação de entradas |
| RabbitMQ | 3.13 | Broker de mensagens |
| H2 Database | — | Banco in-memory |
| Lombok | — | Redução de boilerplate |
| Docker + Compose | — | Containerização |

---

## Estrutura do Projeto

```
messaging/
├── src/main/java/org/messaging/messaging/
│   ├── MessagingApplication.java         # Entry point
│   ├── config/
│   │   └── RabbitMQConfig.java           # Exchanges, filas, bindings, converter
│   ├── controller/
│   │   ├── PaymentController.java        # POST /payments | GET /orders/{number}
│   │   └── GlobalExceptionHandler.java   # Tratamento global de erros (RFC 7807)
│   ├── consumer/
│   │   ├── PaymentConsumer.java          # Consome payment.queue
│   │   └── LogisticsConsumer.java        # Consome logistics.queue
│   ├── producer/
│   │   └── PaymentProducer.java          # Publica em payment e logistics exchanges
│   ├── service/
│   │   ├── OrderService.java             # Criação e consulta de pedidos
│   │   ├── FakePaymentGatewayService.java # Gateway fake (80% aprovação)
│   │   └── FakeLogisticsService.java      # Logística fake (90% sucesso)
│   ├── model/
│   │   ├── Order.java                    # Entidade JPA
│   │   └── OrderStatus.java              # Enum de estados do pedido
│   ├── dto/
│   │   ├── PaymentRequest.java           # Payload de entrada (validado)
│   │   ├── PaymentMessage.java           # Mensagem → payment.queue
│   │   ├── LogisticsMessage.java         # Mensagem → logistics.queue
│   │   └── OrderResponse.java            # Resposta dos endpoints
│   └── repository/
│       └── OrderRepository.java          # JPA Repository
├── src/main/resources/
│   └── application.properties
├── Dockerfile                            # Multi-stage build
├── docker-compose.yml                    # App + RabbitMQ
├── .dockerignore
└── build.gradle
```

---

## Endpoints REST

### `POST /payments` — Iniciar pagamento

Recebe os dados do cartão e o número do pedido. Persiste o pedido com status `PENDING` e dispara o fluxo assíncrono. Retorna imediatamente com **HTTP 202 Accepted**.

**Request Body:**
```json
{
  "orderNumber": "ORD-001",
  "cardNumber": "4111111111111111",
  "cardHolder": "João da Silva",
  "cardExpiry": "12/27",
  "cardCvv": "123",
  "amount": 299.99
}
```

**Response (202 Accepted):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "orderNumber": "ORD-001",
  "cardHolder": "João da Silva",
  "amount": 299.99,
  "status": "PENDING",
  "trackingCode": null,
  "createdAt": "2026-07-25T01:00:00",
  "updatedAt": "2026-07-25T01:00:00"
}
```

---

### `GET /orders/{orderNumber}` — Consultar pedido

Retorna o estado atual do pedido, incluindo o `trackingCode` quando disponível.

**Exemplo:** `GET /orders/ORD-001`

**Response (200 OK) — após processamento completo:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "orderNumber": "ORD-001",
  "cardHolder": "João da Silva",
  "amount": 299.99,
  "status": "TRACKING_GENERATED",
  "trackingCode": "BR4F9A2C1DBR",
  "createdAt": "2026-07-25T01:00:00",
  "updatedAt": "2026-07-25T01:00:05"
}
```

---

## Filas e Exchanges RabbitMQ

| Componente | Tipo | Nome | Descrição |
|---|---|---|---|
| Exchange | Direct | `payment.exchange` | Recebe eventos de pagamento |
| Exchange | Direct | `logistics.exchange` | Recebe eventos de logística |
| Exchange | Direct | `dead.letter.exchange` | Recebe mensagens com falha |
| Fila | Durable | `payment.queue` | Autorização de pagamento |
| Fila | Durable | `logistics.queue` | Geração de código de rastreio |
| Fila | Durable | `payment.queue.dlq` | Dead letter de pagamento |
| Fila | Durable | `logistics.queue.dlq` | Dead letter de logística |

As filas principais estão configuradas com **Dead Letter Queue (DLQ)**: mensagens que falham são redirecionadas automaticamente para a DLQ, evitando perda de dados.

---

## Status do Pedido

```
PENDING
  ├── (gateway aprova)   → PAYMENT_APPROVED → TRACKING_GENERATED
  │                                         └── LOGISTICS_FAILED
  └── (gateway recusa)  → PAYMENT_DECLINED
```

| Status | Descrição |
|---|---|
| `PENDING` | Pedido criado, aguardando autorização |
| `PAYMENT_APPROVED` | Pagamento autorizado pelo gateway |
| `PAYMENT_DECLINED` | Pagamento recusado pelo gateway |
| `TRACKING_GENERATED` | Código de rastreio gerado com sucesso |
| `LOGISTICS_FAILED` | Falha ao gerar código de rastreio |

---

## Executando com Docker

### Pré-requisitos

- [Docker](https://docs.docker.com/get-docker/) ≥ 24
- [Docker Compose](https://docs.docker.com/compose/) ≥ 2

### Subir a stack completa

```bash
# Na raiz do projeto
docker compose up --build
```

Isso irá:
1. Construir a imagem da aplicação (multi-stage build)
2. Subir o RabbitMQ e aguardar ele estar pronto (healthcheck)
3. Subir a aplicação Spring Boot

| Serviço | URL |
|---|---|
| API REST | http://localhost:8080 |
| RabbitMQ Management UI | http://localhost:15672 (guest/guest) |
| H2 Console | http://localhost:8080/h2-console |

### Parar e remover os containers

```bash
docker compose down
```

### Parar e remover volumes (limpa dados do RabbitMQ)

```bash
docker compose down -v
```

---

## Executando Localmente

### Pré-requisitos

- Java 24+ (eclipse-temurin recomendado)
- RabbitMQ rodando localmente ou via Docker
- Gradle (ou use o wrapper `./gradlew`)

### Subir apenas o RabbitMQ via Docker

```bash
docker run -d --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  rabbitmq:3.13-management
```

### Rodar a aplicação

```bash
./gradlew bootRun
```

### Build do JAR

```bash
./gradlew bootJar
java -jar build/libs/messaging-0.0.1-SNAPSHOT.jar
```

---

## Exemplos de Uso

### cURL

```bash
# 1. Criar um pedido e iniciar o pagamento
curl -s -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderNumber": "ORD-001",
    "cardNumber": "4111111111111111",
    "cardHolder": "Maria Oliveira",
    "cardExpiry": "08/28",
    "cardCvv": "456",
    "amount": 149.90
  }' | jq

# 2. Aguardar ~1 segundo e consultar o status
sleep 1

curl -s http://localhost:8080/orders/ORD-001 | jq
```

### HTTPie

```bash
# Criar pedido
http POST :8080/payments \
  orderNumber=ORD-002 \
  cardNumber=5500005555555559 \
  cardHolder="Carlos Souza" \
  cardExpiry=03/29 \
  cardCvv=789 \
  amount:=599.00

# Consultar pedido
http :8080/orders/ORD-002
```

---

## Dicas para Testes

### Forçar recusa do pagamento

Qualquer cartão cujo número **termine em `0000`** será sempre recusado pelo gateway fake:

```json
{
  "orderNumber": "ORD-FAIL",
  "cardNumber": "4111000000000000",
  "cardHolder": "Teste Recusa",
  "cardExpiry": "01/30",
  "cardCvv": "000",
  "amount": 10.00
}
```

### Observar o fluxo em tempo real

Acompanhe os logs da aplicação para ver cada etapa do processamento:

```bash
docker compose logs -f app
```

Você verá mensagens como:

```
[ORDER]             Pedido criado: ORD-001 — status: PENDING
[PRODUCER]          Publicando mensagem de pagamento → pedido: ORD-001
[PAYMENT-CONSUMER]  Mensagem recebida → pedido: ORD-001 | valor: R$ 149.9
[GATEWAY]           Pagamento APROVADO — cartão **** **** **** 1111 | valor: R$ 149.9
[PAYMENT-CONSUMER]  Pedido ORD-001 marcado como PAYMENT_APPROVED
[PRODUCER]          Publicando mensagem de logística → pedido: ORD-001
[LOGISTICS-CONSUMER] Mensagem recebida → pedido: ORD-001 | destinatário: Maria Oliveira
[LOGISTICS]         Rastreio gerado: BR4F9A2C1DBR — pedido: ORD-001
[LOGISTICS-CONSUMER] Pedido ORD-001 atualizado — rastreio: BR4F9A2C1DBR
```

### RabbitMQ Management UI

Acesse `http://localhost:15672` (usuário: `guest` / senha: `guest`) para:
- Visualizar as filas e o número de mensagens
- Inspecionar mensagens nas DLQs
- Monitorar throughput em tempo real

### H2 Console

Acesse `http://localhost:8080/h2-console` com a URL JDBC `jdbc:h2:mem:messagingdb` para consultar diretamente a tabela `ORDERS` e acompanhar as mudanças de status.
