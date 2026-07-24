## 1. Setup e Configuração

- [x] 1.1 Adicionar dependências JUnit 5 e Mockito ao pom.xml
- [x] 1.2 Configurar JaCoCo para métricas de cobertura
- [x] 1.3 Criar estrutura de diretórios src/test/java/com/exchange/v1/
- [x] 1.4 Configurar Maven Surefire para execução de testes

## 2. Testes para Classe Order

- [x] 2.1 Criar OrderTest.java com testes de validação de campos obrigatórios
- [x] 2.2 Implementar testes para cenários de preço inválido
- [x] 2.3 Implementar testes para cenários de quantidade inválida
- [x] 2.4 Implementar testes para cálculo de remainingQty
- [x] 2.5 Implementar testes para execução parcial e completa
- [x] 2.6 Implementar testes de concorrência para classe Order

## 3. Testes para Classe OrderBook

- [x] 3.1 Criar OrderBookTest.java com testes básicos de adição/remoção
- [x] 3.2 Implementar testes para prioridade price-time
- [x] 3.3 Implementar testes para melhor bid/ask
- [x] 3.4 Implementar testes para múltiplos símbolos
- [x] 3.5 Implementar testes para condições de erro (preço zero, quantidade zero)
- [x] 3.6 Implementar testes de concorrência para OrderBook

## 4. Testes para MatchingEngine

- [x] 4.1 Criar MatchingEngineTest.java com testes básicos
- [x] 4.2 Implementar testes para casamento perfeito
- [x] 4.3 Implementar testes para casamento parcial
- [x] 4.4 Implementar testes para cenário sem match
- [x] 4.5 Implementar testes para cancelamento de ordens
- [x] 4.6 Implementar testes para edge cases (mesmo preço, FIFO)
- [x] 4.7 Implementar testes que forçam erros (ordens inválidas, concorrência)

## 5. Testes para EchoServer

- [x] 5.1 Criar EchoServerTest.java com testes unitários
- [x] 5.2 Implementar testes para conversão de mensagens FIX para Order
- [x] 5.3 Implementar testes para validação de mensagens FIX
- [x] 5.4 Implementar testes para tratamento de erros de conversão
- [x] 5.5 Implementar testes para cenários de mensagem inválida
- [x] 5.6 Implementar testes de concorrência para EchoServer

## 6. Testes para FixClient

- [x] 6.1 Criar FixClientTest.java com testes unitários
- [x] 6.2 Implementar testes para envio de ordens
- [x] 6.3 Implementar testes para recebimento de ExecutionReports
- [x] 6.4 Implementar testes para cancelamento de ordens
- [x] 6.5 Implementar testes para reconexão automática
- [x] 6.6 Implementar testes para condições de erro de rede

## 7. Testes de Integração

- [x] 7.1 Criar EngineIntegrationTest.java para testes de integração
- [x] 7.2 Implementar testes para integração EchoServer + MatchingEngine
- [x] 7.3 Implementar testes para integração MatchingEngine + OrderBook
- [x] 7.4 Implementar testes para fluxo completo (Cliente → Servidor → Engine)
- [x] 7.5 Implementar testes para múltiplos ativos simultaneamente
- [x] 7.6 Implementar testes para concorrência entre componentes

## 8. Testes de Erro e Edge Cases

- [x] 8.1 Criar ErrorScenarioTest.java para testes de erro forcado
- [x] 8.2 Implementar testes para condições de memória insuficiente
- [x] 8.3 Implementar testes para condições de rede (timeout, perda de pacote)
- [x] 8.4 Implementar testes para violações de negócio
- [x] 8.5 Implementar testes para corrupção de dados
- [x] 8.6 Implementar testes para deadlocks e livelocks

## 9. Utilitários e Helpers

- [x] 9.1 Criar OrderBuilder para facilitar criação de ordens de teste
- [x] 9.2 Criar FixMessageBuilder para facilitar criação de mensagens FIX
- [x] 9.3 Criar TestUtils com métodos auxiliares para testes
- [x] 9.4 Criar MockDataFactory com dados de teste realistas
- [x] 9.5 Criar assertions customizadas para mensagens FIX

## 10. Execução e Validação

- [x] 10.1 Executar todos os testes unitários e verificar sucesso
- [x] 10.2 Gerar relatório de cobertura com JaCoCo
- [x] 10.3 Verificar que cobertura mínima de 95% foi atingida
- [x] 10.4 Executar testes com múltiplas threads para validar concorrência
- [x] 10.5 Executar testes de regressão para garantir não quebra funcionalidade
- [x] 10.6 Documentar resultados de cobertura e métricas de qualidade