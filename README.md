# v1 Exchange FIX Engine

Motor de correspondência de ordens (matching engine) para uma exchange brasileira, utilizando o protocolo **FIX (Financial Information eXchange) 4.2** via **QuickFIX/J** com **Spring Boot**.

## Arquitetura

```
FixClient (Initiator)                    EchoServer (Acceptor)
       │                                       │
       │  ── NewOrderSingle (35=D) ──────────► │
       │                                       ▼
       │                              MatchingEngine.process()
       │                                       │
       │                              ┌────────┴────────┐
       │                              │  OrderBook       │
       │                              │  (por símbolo)   │
       │                              │  bids / asks     │
       │                              └────────┬────────┘
       │                                       │
       │  ◄── ExecutionReport (35=8) ──────────│
       │  ◄── OrderCancelReject (35=9) ────────│
```

Ambos — Initiator e Acceptor — rodam na **mesma JVM** e compartilham uma única instância do `MatchingEngine`.

## Tecnologias

| Tecnologia | Versão |
|-----------|--------|
| Java | 21 |
| Spring Boot | 3.4.5 |
| QuickFIX/J | 2.3.1 |
| Maven | Wrapper incluso (mvnw) |
| JaCoCo | 0.8.12 (meta: 95% cobertura) |

## Estrutura do Projeto

```
src/
├── main/java/com/exchange/v1/
│   ├── V1ExchangeFixApplication.java        # Entry point Spring Boot
│   ├── engine/
│   │   ├── MatchingEngine.java              # Motor de correspondência (price-time priority)
│   │   └── OrderBook.java                   # Livro de ordens por símbolo
│   ├── model/
│   │   └── Order.java                       # Modelo de ordem (POJO imutável parcial)
│   ├── initiator/
│   │   ├── fix/FixClient.java               # Cliente FIX (Initiator) com cenário de teste
│   │   └── config/FixClientConfig.java      # Config Spring do SocketInitiator
│   ├── acceptor/
│   │   └── fix/EchoServer.java              # Servidor FIX (Acceptor) que recebe ordens
│   └── config/
│       └── QuickFixConfig.java              # Config Spring do SocketAcceptor
│
├── main/resources/
│   ├── application.properties               # Desabilita web, logging DEBUG
│   └── fix/
│       ├── server.cfg                       # Config QuickFIX do Acceptor (porta 9876)
│       └── client.cfg                       # Config QuickFIX do Initiator
│
└── test/java/com/exchange/v1/
    ├── engine/                              # Testes unitários e integração do matching engine
    ├── model/                               # Testes do modelo Order
    ├── initiator/fix/                       # Testes do FixClient
    ├── acceptor/fix/                        # Testes do EchoServer
    ├── fix/                                 # Testes end-to-end (FIX session)
    └── test/                                # Helpers: builders, asserts, data factories
```

## Componentes Principais

### MatchingEngine (`engine/MatchingEngine.java`)
- **Algoritmo**: price-time priority — melhor preço primeiro; mesmo preço, FIFO.
- Compra casa com venda quando `preço compra >= preço venda` (market sempre casa).
- Processa ordens novas e cancelamentos.
- Envia `ExecutionReport` (35=8) e `OrderCancelReject` (35=9) via FIX session.

### OrderBook (`engine/OrderBook.java`)
- Livro de ordens limitado para um único ativo (ex: PETR4).
- `bids`: `TreeMap` reverso (maior preço primeiro).
- `asks`: `TreeMap` natural (menor preço primeiro).
- Cada nível de preço usa `LinkedList` para FIFO.
- Valida preço (≥ 0, onde 0 = market) e quantidade (> 0).

### Order (`model/Order.java`)
- Enums: `Side {BUY, SELL}`, `Type {MARKET, LIMIT}`, `Status {NEW, PARTIAL, FILLED, CANCELED}`.
- Campos principais: `clOrdID`, `symbol`, `side`, `type`, `price`, `qty`, `sessionID`, `executedQty`, `status`.
- Método `execute(amount)` atualiza quantidade executada e status.

### EchoServer (`acceptor/fix/EchoServer.java`)
- Acceptor FIX: recebe `NewOrderSingle` (35=D), converte para `Order` e delega ao `MatchingEngine`.

### FixClient (`initiator/fix/FixClient.java`)
- Initiator FIX: ao conectar, envia cenário de teste automatizado:
  - **PETR4**: vende 100 → compra 300 (partial fill) → vende 200 (full fill).
  - **VALE3**: compra 500 → cancela.

## Como Executar

```bash
# Compilar e rodar testes
./mvnw test

# Executar a aplicação
./mvnw spring-boot:run

# Verificar cobertura (JaCoCo)
./mvnw verify
# Relatório: target/site/jacoco/index.html
```

## Configuração FIX

| Parâmetro | Acceptor (server) | Initiator (client) |
|-----------|------------------|-------------------|
| Porta | 9876 | — |
| BeginString | FIX.4.2 | FIX.4.2 |
| SenderCompID | EXCHANGE | CLIENT |
| TargetCompID | CLIENT | EXCHANGE |
| HeartBtInt | 30s | 30s |
| Reconectar | — | a cada 5s |

## Testes

- **Unitários**: `MatchingEngineTest`, `OrderBookTest`, `OrderTest`, `EchoServerTest`, `FixClientTest`
- **Integração**: `EngineIntegrationTest` (engine + order book), `ErrorScenarioTest` (casos de erro)
- **End-to-end**: `EchoServerApplicationTest` (sessão FIX real, atualmente `@Disabled`)
- **Cobertura**: meta de 95% via JaCoCo
- **Helpers**: `OrderBuilder`, `FixMessageBuilder`, `MockDataFactory`, `FixAssertions`, `TestUtils`
