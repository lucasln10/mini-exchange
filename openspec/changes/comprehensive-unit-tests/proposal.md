## Why

Atualmente, o projeto possui apenas testes básicos e integrados limitados. A cobertura de testes unitários é insuficiente para garantir a robustez do motor de casamento de ordens, do processamento de mensagens FIX e da integridade do livro de ofertas. Sem testes unitários abrangentes, é impossível detectar bugs lógicos, edge cases em algoritmos de matching, e problemas de concorrência que podem ocorrer em produção.

## What Changes

- Criar testes unitários abrangentes para todas as classes principais
- Implementar testes que forçam falhas e edge cases (não apenas caminhos felizes)
- Adicionar testes para cobertura de 95%+ do código fonte
- Configurar JUnit 5 como framework de testes principal
- Criar utilitários de teste para simulação de cenários complexos de FIX
- Implementar testes de regressão para algoritmos de matching

## Capabilities

### New Capabilities
- `order-matching-tests`: Testes unitários para MatchingEngine e OrderBook com edge cases
- `fix-message-tests`: Testes para processamento e validação de mensagens FIX
- `model-validation-tests`: Testes para modelo Order e regras de negócio
- `engine-integration-tests`: Testes de integração entre componentes do engine
- `error-scenario-tests`: Testes que forçam erros para validar tratamento de exceções

### Modified Capabilities

## Impact

- **Classes afetadas**: MatchingEngine, OrderBook, Order, EchoServer, FixClient
- **Cobertura**: Todo o código fonte em `src/main/java/com/exchange/v1/`
- **Dependências**: Adicionar JUnit 5 e Mockito ao pom.xml
- **Ferramentas**: Configurar JaCoCo para métricas de cobertura
- **Arquivos de teste**: Novos arquivos em `src/test/java/com/exchange/v1/` com estrutura paralela aos main packages