## Context

O projeto v1-exchange-FIX é um sistema de exchange com protocolo FIX, composto por:
- **MatchingEngine**: Motor de casamento de ordens com lógica price-time priority
- **OrderBook**: Gerenciamento de livro de ofertas com estrutura TreeMap
- **Order**: Modelo de ordem com métodos de execução e validação
- **EchoServer**: Servidor FIX que processa mensagens e converte para modelo interno
- **FixClient**: Cliente FIX para testes automatizados

Atualmente existem apenas testes básicos e integrados. A cobertura de testes unitários é mínima, não permitindo detecção de bugs em algoritmos complexos.

## Goals / Non-Goals

**Goals:**
- Criar testes unitários abrangentes com cobertura de 95%+ para todas as classes principais
- Implementar testes que forcem edge cases e cenários de erro (não apenas caminhos felizes)
- Estruturar testes de forma paralela aos pacotes main (mirror structure)
- Configurar JUnit 5 com assertions poderosas e Mockito para mocks
- Criar utilitários de teste para simulação de dados FIX complexos
- Implementar testes de concorrência para validar thread safety

**Non-Goals:**
- Testes de integração de sistema completo (já existem)
- Testes de performance (load testing)
- Testes de interface usuário (não aplicável)
- Mudanças na arquitetura principal do sistema
- Testes automatizados de contrato FIX (será feito como capability separada)

## Decisions

### 1. Estrutura de Testes Paralela
**Decisão:** Criar estrutura `src/test/java/com/exchange/v1/` paralela aos main packages
**Razão:** Manter organização clara, facilitar navegação e permitir builds independentes
**Alternativas consideradas:** 
- Testes em subdiretórios dos pacotes main (reduz organização)
- Testes em único pacote (difícil manutenção)

### 2. Framework JUnit 5 + Mockito
**Decisão:** Usar JUnit 5 com Jupiter engine e Mockito para mocks
**Razão:** JUnit 5 oferece assertivas modernas e suporte a parâmetros; Mockito permite simulação complexa de dependências
**Alternativas consideradas:**
- TestNG (mais complexo para este caso)
- JUnit 4 (descontinuado)

### 3. Test Data Builders Pattern
**Decisão:** Criar builders específicos para objetos de teste (OrderBuilder, FixMessageBuilder)
**Razão:** Reduz boilerplate, melhora legibilidade, permite fácil criação de cenários complexos
**Alternativas consideradas:**
- Factory methods (menos flexível)
- Objetos estáticos (difícis de estender)

### 4. Testes de Erro First
**Decisão:** Priorizar testes que forçam falhas sobre testes de caminho feliz
**Razão:** Descobrir bugs proativamente em vez de apenas validar funcionalidade básica
**Alternativas consideradas:**
- Testes balanceados (50% feliz, 50% erro) - menos eficaz para encontrar bugs

### 5. Coverage com JaCoCo
**Decisão:** Configurar JaCoCo com threshold de 95% coverage
**Razão:** Métrica objetiva de qualidade, integração com Maven, relatórios detalhados
**Alternativas consideradas:**
- SonarQube (overkill para este projeto)
- Coverage manual (subjetivo e difícil de manter)

## Risks / Trade-offs

### [Risk] Testes complexos podem mascarar bugs reais
**Mitigation:** Manter testes simples e focados, cada teste deve testar uma única preocupação

### [Risk] Mock excessivo pode esconder dependências reais
**Mitigation:** Usar mocks apenas para componentes externos (QuickFIX), testar integração com reais componentes

### [Risk] Testes de concorrência podem ser flaky
**Mitigation:** Usar CountDownLatch e timeouts controlados, evitar race conditions nos próprios testes

### [Risk] Edge cases podem não cobrir todos os cenários
**Mitigation:** Criar testes exploratórios manuais após automação, revisar código manualmente em áreas críticas

### [Trade-off] Performance vs. Coverage
**Compromisso:** Sacrificar alguns testes lentos (ex: concorrência) para manter velocidade de build, mas manter alto coverage em código crítico