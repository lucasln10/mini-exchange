## 1. Setup e Configuração

- [ ] 1.1 Adicionar dependências JUnit 5 e Mockito ao pom.xml
- [ ] 1.2 Configurar JaCoCo para métricas de cobertura
- [ ] 1.3 Criar estrutura de diretórios src/test/java/com/exchange/v1/
- [ ] 1.4 Configurar Maven Surefire para execução de testes

## 2. Testes para Classe Order

- [ ] 2.1 Criar OrderTest.java com testes de validação de campos obrigatórios
- [ ] 2.2 Implementar testes para cenários de preço inválido
- [ ] 2.3 Implementar testes para cenários de quantidade inválida
- [ ] 2.4 Implementar testes para cálculo de remainingQty
- [ ] 2.5 Implementar testes para execução parcial e completa
- [ ] 2.6 Implementar testes de concorrência para classe Order

## 3. Testes para Classe OrderBook

- [ ] 3.1 Criar OrderBookTest.java com testes básicos de adição/remoção
- [ ] 3.2 Implementar testes para prioridade price-time
- [ ] 3.3 Implementar testes para melhor bid/ask
- [ ] 3.4 Implementar testes para múltiplos símbolos
- [ ] 3.5 Implementar testes para condições de erro (preço zero, quantidade zero)
- [ ] 3.6 Implementar testes de concorrência para OrderBook

## 4. Testes para MatchingEngine

- [ ] 4.1 Criar MatchingEngineTest.java com testes básicos
- [ ] 4.2 Implementar testes para casamento perfeito
- [ ] 4.3 Implementar testes para casamento parcial
- [ ] 4.4 Implementar testes para cenário sem match
- [ ] 4.5 Implementar testes para cancelamento de ordens
- [ ] 4.6 Implementar testes para edge cases (mesmo preço, FIFO)
- [ ] 4.7 Implementar testes que forçam erros (ordens inválidas, concorrência)

## 5. Testes para EchoServer

- [ ] 5.1 Criar EchoServerTest.java com testes unitários
- [ ] 5.2 Implementar testes para conversão de mensagens FIX para Order
- [ ] 5.3 Implementar testes para validação de mensagens FIX
- [ ] 5.4 Implementar testes para tratamento de erros de conversão
- [ ] 5.5 Implementar testes para cenários de mensagem inválida
- [ ] 5.6 Implementar testes de concorrência para EchoServer

## 6. Testes para FixClient

- [ ] 6.1 Criar FixClientTest.java com testes unitários
- [ ] 6.2 Implementar testes para envio de ordens
- [ ] 6.3 Implementar testes para recebimento de ExecutionReports
- [ ] 6.4 Implementar testes para cancelamento de ordens
- [ ] 6.5 Implementar testes para reconexão automática
- [ ] 6.6 Implementar testes para condições de erro de rede

## 7. Testes de Integração

- [ ] 7.1 Criar EngineIntegrationTest.java para testes de integração
- [ ] 7.2 Implementar testes para integração EchoServer + MatchingEngine
- [ ] 7.3 Implementar testes para integração MatchingEngine + OrderBook
- [ ] 7.4 Implementar testes para fluxo completo (Cliente → Servidor → Engine)
- [ ] 7.5 Implementar testes para múltiplos ativos simultaneamente
- [ ] 7.6 Implementar testes para concorrência entre componentes

## 8. Testes de Erro e Edge Cases

- [ ] 8.1 Criar ErrorScenarioTest.java para testes de erro forcado
- [ ] 8.2 Implementar testes para condições de memória insuficiente
- [ ] 8.3 Implementar testes para condições de rede (timeout, perda de pacote)
- [ ] 8.4 Implementar testes para violações de negócio
- [ ] 8.5 Implementar testes para corrupção de dados
- [ ] 8.6 Implementar testes para deadlocks e livelocks

## 9. Utilitários e Helpers

- [ ] 9.1 Criar OrderBuilder para facilitar criação de ordens de teste
- [ ] 9.2 Criar FixMessageBuilder para facilitar criação de mensagens FIX
- [ ] 9.3 Criar TestUtils com métodos auxiliares para testes
- [ ] 9.4 Criar MockDataFactory com dados de teste realistas
- [ ] 9.5 Criar assertions customizadas para mensagens FIX

## 10. Execução e Validação

- [ ] 10.1 Executar todos os testes unitários e verificar sucesso
- [ ] 10.2 Gerar relatório de cobertura com JaCoCo
- [ ] 10.3 Verificar que cobertura mínima de 95% foi atingida
- [ ] 10.4 Executar testes com múltiplas threads para validar concorrência
- [ ] 10.5 Executar testes de regressão para garantir não quebra funcionalidade
- [ ] 10.6 Documentar resultados de cobertura e métricas de qualidade