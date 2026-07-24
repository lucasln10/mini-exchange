# v1 Exchange FIX Engine

Motor de correspondência de ordens (**matching engine**) para uma exchange brasileira, utilizando o protocolo **FIX 4.2** (Financial Information eXchange) via **QuickFIX/J** com **Spring Boot**.

O sistema recebe ordens FIX `NewOrderSingle` (tag 35=D), compara e executa ordens de compra e venda usando **price-time priority** e responde com `ExecutionReport` (35=8) ou `OrderCancelReject` (35=9).

> Tanto o cliente FIX (Initiator) quanto o servidor FIX (Acceptor) rodam na **mesma JVM** e compartilham uma única instância do `MatchingEngine`, tornando este um sistema auto-contido para demonstração e testes.

---

## Sumario

- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Componentes Principais](#componentes-principais)
- [Fluxo de Execucao](#fluxo-de-execucao)
- [Configuracao FIX](#configuracao-fix)
- [Como Executar](#como-executar)
- [Testes](#testes)
- [Cobertura de Codigo](#cobertura-de-codigo)
- [Propriedades da Aplicacao](#propriedades-da-aplicacao)

---

## Arquitetura

```
┌─────────────────────┐                         ┌─────────────────────┐
│   FixClient         │                         │   EchoServer        │
│   (Initiator)       │                         │   (Acceptor)        │
│                     │   NewOrderSingle (35=D) │                     │
│   Conecta a         │ ──────────────────────► │   Recebe ordens     │
│   localhost:9876    │                         │   na porta 9876     │
│                     │                         │         │           │
│                     │                         │         ▼           │
│                     │                         │  MatchingEngine     │
│                     │                         │     .process()      │
│                     │                         │         │           │
│                     │                         │  ┌──────┴──────┐    │
│                     │                         │  │  OrderBook   │    │
│                     │                         │  │  (por ativo) │    │
│                     │                         │  │  bids / asks │    │
│                     │                         │  └──────┬──────┘    │
│                     │                         │         │           │
│                     │   ExecutionReport (35=8)│         │           │
│                     │ ◄────────────────────── │ ◄───────┘           │
│                     │   OrderCancelReject     │                     │
│                     │ ◄────────────────────── │                     │
└─────────────────────┘                         └─────────────────────┘
```

**Fluxo simplificado:**

```
Initiator ──► Acceptor ──► MatchingEngine ──► OrderBook ──► ExecutionReport
                                       │
                                       └──► OrderCancelReject (se aplicavel)
```

---

## Tecnologias

| Categoria | Tecnologia | Versao |
|-----------|-----------|--------|
| **Linguagem** | Java | 21 |
| **Framework** | Spring Boot | 3.4.5 |
| **Protocolo FIX** | QuickFIX/J (core + FIX4.2 messages) | 2.3.1 |
| **Build** | Maven (wrapper incluso) | 3.9.16 |
| **Testes** | JUnit 5 (Jupiter) | via spring-boot-starter-test |
| **Mocking** | Mockito | via spring-boot-starter-test |
| **Cobertura** | JaCoCo | 0.8.12 (meta: 95%) |
| **Dev Tools** | Spring Boot DevTools | via spring-boot-starter-test |

---

## Estrutura do Projeto

```
src/
├── main/java/com/exchange/v1/
│   ├── V1ExchangeFixApplication.java          # Entry point Spring Boot
│   ├── config/
│   │   └── QuickFixConfig.java                # Config do SocketAcceptor (servidor FIX)
│   ├── engine/
│   │   ├── MatchingEngine.java                # Motor de correspondencia (price-time priority)
│   │   └── OrderBook.java                     # Livro de ordens por ativo
│   ├── model/
│   │   └── Order.java                         # Modelo de ordem (POJO parcialmente imutavel)
│   ├── acceptor/
│   │   └── fix/
│   │       └── EchoServer.java                # Servidor FIX (Acceptor) que recebe ordens
│   └── initiator/
│       ├── config/
│       │   └── FixClientConfig.java           # Config do SocketInitiator (cliente FIX)
│       └── fix/
│           └── FixClient.java                 # Cliente FIX (Initiator) com cenario de teste
│
├── main/resources/
│   ├── application.properties                 # Propriedades da aplicacao (sem web, logging DEBUG)
│   └── fix/
│       ├── server.cfg                         # Config QuickFIX do Acceptor (porta 9876)
│       └── client.cfg                         # Config QuickFIX do Initiator
│
└── test/java/com/exchange/v1/
    ├── engine/
    │   ├── MatchingEngineTest.java            # Testes do motor de correspondencia
    │   ├── OrderBookTest.java                 # Testes do livro de ordens
    │   ├── EngineIntegrationTest.java         # Testes de integracao (engine + order book)
    │   └── ErrorScenarioTest.java             # Testes de cenarios de erro e edge cases
    ├── model/
    │   └── OrderTest.java                     # Testes do modelo Order
    ├── acceptor/fix/
    │   └── EchoServerTest.java                # Testes do Acceptor FIX
    ├── initiator/fix/
    │   └── FixClientTest.java                 # Testes do Initiator FIX
    ├── fix/
    │   └── EchoServerApplicationTest.java     # Teste end-to-end (sessao FIX real, @Disabled)
    └── test/                                  # Helpers de teste
        ├── TestUtils.java                     # Metodos estaticos auxiliares
        ├── OrderBuilder.java                  # Builder fluent para Order
        ├── MockDataFactory.java               # Fabrica de dados de teste realistas
        ├── FixMessageBuilder.java             # Construtor de mensagens FIX
        └── FixAssertions.java                 # Asserts customizados para mensagens FIX
```

---

## Componentes Principais

### MatchingEngine (`engine/MatchingEngine.java`)

O coracao do sistema. Motor de correspondencia que implementa **price-time priority**:

- **Algoritmo**: melhor preco primeiro; mesmo preco = FIFO
- Compra casa com venda quando `preco compra >= preco venda`
- Ordens de mercado (`MARKET`) sempre casam (preco = 0)
- Mantem um `ConcurrentHashMap<String, OrderBook>` chaveado por simbolo
- Processa ordens novas e cancelamentos
- Envia `ExecutionReport` (35=8) e `OrderCancelReject` (35=9) via FIX session
- Metodos `process()` e `cancel()` sao `synchronized` para thread safety
- Usa `AtomicLong` para gerar IDs unicos de ordem e execucao

### OrderBook (`engine/OrderBook.java`)

Livro de ordens limitado para um unico ativo (ex: PETR4):

- **Bids (compras)**: `TreeMap<Double, LinkedList<Order>>` com ordenacao reversa (maior preco primeiro)
- **Asks (vendas)**: `TreeMap<Double, LinkedList<Order>>` com ordenacao natural (menor preco primeiro)
- Cada nivel de preco usa `LinkedList` para manter ordem FIFO
- Metodos: `addOrder()`, `removeOrder()`, `findOrder()`, `bestBid()`, `bestAsk()`
- Valida: preco >= 0 (0 = market), quantidade > 0

### Order (`model/Order.java`)

Modelo de dominio para ordens de troca:

| Campo | Tipo | Descricao |
|-------|------|-----------|
| `clOrdID` | `String` | ID unico da ordem do cliente |
| `symbol` | `String` | Simbolo do ativo (ex: PETR4) |
| `side` | `Side` | BUY ou SELL |
| `type` | `Type` | MARKET ou LIMIT |
| `price` | `double` | Preco da ordem (0 para market) |
| `qty` | `int` | Quantidade total |
| `executedQty` | `int` | Quantidade ja executada (mutavel) |
| `status` | `Status` | NEW, PARTIAL, FILLED, CANCELED (mutavel) |
| `sessionID` | `SessionID` | Sessao FIX de origem |

Metodos principais:
- `remainingQty()` — retorna quantidade restante
- `execute(amount)` — atualiza quantidade executada e status
- `isFilled()` — verifica se a ordem foi totalmente executada

### EchoServer (`acceptor/fix/EchoServer.java`)

Servidor FIX (Acceptor) que:

- Implementa a interface `Application` do QuickFIX/J
- Usa `MessageCracker` com anotacao `@Handler` para rotear mensagens
- Recebe `NewOrderSingle` (35=D) e converte para o modelo `Order`
- Delega para `MatchingEngine.process()`

### FixClient (`initiator/fix/FixClient.java`)

Cliente FIX (Initiator) que, ao conectar, envia um **cenario de teste automatizado**:

**PETR4:**
1. Vende 100 @ R$99,00
2. Compra 300 @ R$99,00 (partial fill — 100 executados, 200 restantes)
3. Vende 200 @ R$99,00 (full fill — completa a compra)

**VALE3:**
1. Compra 500 @ R$75,00
2. Cancela a ordem

Processa `ExecutionReport` (35=8) e `OrderCancelReject` (35=9) via `MessageCracker`.

---

## Fluxo de Execucao

```
1. Aplicacao inicia
   │
   ├── QuickFixConfig cria e inicia SocketAcceptor (porta 9876)
   │
   └── FixClientConfig cria e inicia SocketInitiator (conecta a localhost:9876)
       │
       ▼
2. Sessao FIX estabelecida (logon)
   │
   ▼
3. FixClient.onLogon() dispara cenario de teste
   │
   ├── Envia NewOrderSingle (35=D)
   │   │
   │   ▼
   │   EchoServer recebe e converte para Order
   │   │
   │   ▼
   │   MatchingEngine.process() tenta casar
   │   │
   │   ├── Matching encontrado → ExecutionReport (35=8) com preenchimento
   │   └── Sem matching → Order adicionada ao OrderBook
   │
   ├── Envia OrderCancelRequest (35=F) (se aplicavel)
   │   │
   │   ▼
   │   MatchingEngine.cancel() remove do OrderBook
   │   │
   │   ├── Sucesso → ExecutionReport (35=8) com status CANCELED
   │   └── Falha → OrderCancelReject (35=9)
   │
   ▼
4. Execucao completa
```

---

## Configuracao FIX

### Acceptor (Servidor) — `server.cfg`

| Parametro | Valor | Descricao |
|-----------|-------|-----------|
| ConnectionType | `acceptor` | Aceita conexoes |
| BeginString | `FIX.4.2` | Versao do protocolo FIX |
| SocketAcceptPort | `9876` | Porta de escuta |
| SenderCompID | `EXCHANGE` | Identificador do remetente |
| TargetCompID | `CLIENT` | Identificador do destinatario |
| HeartBtInt | `30` | Intervalo de heartbeat (segundos) |
| ResetOnLogon | `Y` | Reseta sequencia no logon |
| ResetOnLogout | `Y` | Reseta sequencia no logout |
| DataDictionary | `FIX42.xml` | Dicionario de dados FIX 4.2 |
| FileStorePath | `store` | Diretorio de persistencia de sessao |
| FileLogPath | `log` | Diretorio de logs FIX |
| StartTime / EndTime | `00:00:00` / `23:59:59` | Janela de operacao 24h |

### Initiator (Cliente) — `client.cfg`

| Parametro | Valor | Descricao |
|-----------|-------|-----------|
| ConnectionType | `initiator` | Inicia conexoes |
| BeginString | `FIX.4.2` | Versao do protocolo FIX |
| SocketConnectHost | `localhost` | Host do Acceptor |
| SocketConnectPort | `9876` | Porta do Acceptor |
| SenderCompID | `CLIENT` | Identificador do remetente |
| TargetCompID | `EXCHANGE` | Identificador do destinatario |
| HeartBtInt | `30` | Intervalo de heartbeat (segundos) |
| ReconnectInterval | `5` | Intervalo de reconexao (segundos) |
| ResetOnLogon | `Y` | Reseta sequencia no logon |
| ResetOnLogout | `Y` | Reseta sequencia no logout |
| DataDictionary | `FIX42.xml` | Dicionario de dados FIX 4.2 |
| FileStorePath | `store/client` | Diretorio de persistencia de sessao |
| FileLogPath | `log/client` | Diretorio de logs FIX |
| StartTime / EndTime | `00:00:00` / `23:59:59` | Janela de operacao 24h |

---

## Como Executar

### Pre-requisitos

- Java 21 ou superior
- Maven 3.9+ (ou use o wrapper `mvnw`/`mvnw.cmd`)

### Comandos

```bash
# Compilar o projeto
./mvnw compile

# Rodar os testes
./mvnw test

# Executar a aplicacao (Acceptor + Initiator na mesma JVM)
./mvnw spring-boot:run

# Gerar JAR executavel
./mvnw package

# Gerar relatorio de cobertura (JaCoCo)
./mvnw verify
# Relatorio disponivel em: target/site/jacoco/index.html
```

### Execucao no Windows

```cmd
mvnw.cmd compile
mvnw.cmd test
mvnw.cmd spring-boot:run
mvnw.cmd package
mvnw.cmd verify
```

### Saida Esperada ao Executar

Ao iniciar a aplicacao:

1. `QuickFixConfig` cria e inicia o `SocketAcceptor` na porta 9876
2. `FixClientConfig` cria e inicia o `SocketInitiator` que conecta a localhost:9876
3. Ao logon bem-sucedido, `FixClient.onLogon()` dispara e envia as ordens de teste
4. O cenario completo e executado e os logs aparecem no console

---

## Testes

### Visao Geral

| Classe de Teste | Foco | Cenarios Principais |
|----------------|------|-------------------|
| `MatchingEngineTest` | Logica de matching | Match exato, partial fill, sem match, market orders, FIFO, cancel, multi-simbolo, concorrencia |
| `OrderBookTest` | Operacoes do livro | Add/remove, best bid/ask, ordenacao FIFO, validacao, find/remove por ID, concorrencia |
| `EngineIntegrationTest` | Engine + OrderBook | Fluxo completo, multi-asset, concorrencia (20 e 100 threads), price-time priority cross-symbol |
| `ErrorScenarioTest` | Cenarios de erro | Input invalido, null handling, stress (10K ordens), deteccao de deadlock, concurrent modification |
| `OrderTest` | Modelo Order | Validacao do construtor, remainingQty, execucao parcial/full, builder pattern, concorrencia |
| `EchoServerTest` | Processamento FIX | Buy/sell/market orders, campos faltantes, mensagens nao suportadas, logon/logout |
| `FixClientTest` | Respostas do cliente | Execution reports (todos os status), cancel rejects, mensagens admin, tipos nao suportados |
| `EchoServerApplicationTest` | End-to-end FIX | Sessao FIX real (`@Disabled` — requer sessao ativa) |

### Helpers de Teste

| Helper | Descricao |
|--------|-----------|
| `TestUtils` | Metodos estaticos auxiliares: `createBuyLimit()`, `createSellLimit()`, `createBuyMarket()`, constantes de sessao |
| `OrderBuilder` | Builder fluent para construir objetos `Order` nos testes |
| `MockDataFactory` | Dados de teste pre-construidos e realistas (cenarios de matching, filled, partial) |
| `FixMessageBuilder` | Construtor de mensagens FIX: `NewOrderSingle`, `ExecutionReport` |
| `FixAssertions` | Asserts customizados para mensagens FIX: tipo, ordStatus, ClOrdID, campos obrigatorios |

### Executar Testes

```bash
# Todos os testes
./mvnw test

# Testes especificos
./mvnw test -Dtest=MatchingEngineTest
./mvnw test -Dtest=OrderBookTest
./mvnw test -Dtest=EngineIntegrationTest

# Com relatorio de cobertura
./mvnw verify
```

---

## Cobertura de Codigo

O projeto utiliza **JaCoCo 0.8.12** com meta de **95% de cobertura de linhas**.

A verificacao e feita automaticamente durante `mvnw verify`:

```xml
<rule>
  <element>BUNDLE</element>
  <limits>
    <limit>
      <counter>LINE</counter>
      <value>COVEREDRATIO</value>
      <minimum>0.95</minimum>
    </limit>
  </limits>
</rule>
```

**Gerar relatorio:**

```bash
./mvnw verify
# Relatorio HTML: target/site/jacoco/index.html
# Relatorio XML:  target/site/jacoco/jacoco.xml
# CSV:            target/site/jacoco/jacoco.csv
```

---

## Propriedades da Aplicacao

| Propriedade | Valor | Descricao |
|-------------|-------|-----------|
| `spring.application.name` | `FIX_Engine` | Nome da aplicacao |
| `spring.main.web-application-type` | `none` | Sem servidor web (headless) |
| `logging.level.com.exchange` | `DEBUG` | Logging detalhado para o pacote da exchange |
| `logging.level.quickfix` | `INFO` | Logging do QuickFIX/J em nivel INFO |

---

## Notas Tecnicas

- **Sem banco de dados**: O sistema e totalmente in-memory (mais persistencia de sessao via arquivos QuickFIX)
- **Sem REST API**: Comunicacao exclusiva via protocolo FIX 4.2 sobre TCP
- **Thread safety**: `MatchingEngine.process()` e `cancel()` sao `synchronized`; `OrderBook` usa `ConcurrentHashMap` para o mapa de livros
- **Ordens de mercado**: Preco 0 e permitido para market orders; ordens LIMIT exigem preco positivo
- **Self-contained**: Ambos client e server rodam na mesma JVM — ideal para demonstracao e testes

---

## Licenca

Este projeto nao possui licenca especifica.
