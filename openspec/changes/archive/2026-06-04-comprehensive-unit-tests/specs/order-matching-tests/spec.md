## ADDED Requirements

### Requirement: Engine deve processar ordens com prioridade price-time
O sistema SHALL processar ordens seguindo a regra price-time priority (melhor preço primeiro, mesmo preço = FIFO).

#### Scenario: Ordem de compra melhor que existente
- **WHEN** nova ordem de compra com preço maior que o melhor bid atual
- **THEN** ordem deve se tornar o novo melhor bid
- **AND** ordem deve ser posicionada no início da fila para aquele preço

#### Scenario: Ordem de venda melhor que existente
- **WHEN** nova ordem de venda com preço menor que o melhor ask atual
- **THEN** ordem deve se tornar o novo melhor ask
- **AND** ordem deve ser posicionada no início da fila para aquele preço

#### Scenario: Mesmo preço ordem anterior
- **WHEN** nova ordem com mesmo preço que ordem existente
- **THEN** ordem deve ser adicionada no final da fila para aquele preço (FIFO)

### Requirement: Engine deve casar ordens quando possível
O sistema SHALL tentar casar novas ordens contra ordens existentes no oposto do livro.

#### Scenario: Match perfeito
- **WHEN** nova ordem de compra exata preço e quantidade de ordem de venda existente
- **THEN** ordens devem ser completamente executadas
- **AND** ExecutionReport deve ser gerado para ambas as ordens
- **AND** ordens devem ser removidas dos livros

#### Scenario: Match parcial
- **WHEN** nova ordem de compra maior quantidade que ordem de venda existente
- **THEN** ordem de venda deve ser completamente executada
- **AND** ordem de compra deve ser parcialmente preenchida
- **AND** restante da ordem de compra deve permanecer no livro
- **AND** ExecutionReport parcial deve ser gerado

#### Scenario: Sem match possível
- **WHEN** nova ordem de compra com preço menor que melhor ask
- **THEN** ordem deve ser adicionada ao livro de compras
- **AND** nenhuma execução deve ocorrer
- **AND** ExecutionReport com status New deve ser gerado

### Requirement: Engine deve tratar cancelamentos corretamente
O sistema SHALL processar pedidos de cancelamento e remover ordens correspondentes.

#### Scenario: Cancelamento de ordem existente
- **WHEN** cancelamento recebido para ordem existente no livro
- **THEN** ordem deve ser removida do livro
- **AND** ExecutionReport com status Cancelled deve ser gerado
- **AND** ordem não deve participar de futuros matches

#### Scenario: Cancelamento de ordem já executada
- **WHEN** cancelamento recebido para ordem já completamente executada
- **THEN** sistema deve rejeitar cancelamento
- **AND** ExecutionReport com status Rejected deve ser gerado
- **AND** ordem deve permanecer como executada

#### Scenario: Cancelamento de ordem parcialmente executada
- **WHEN** cancelamento recebido para ordem parcialmente executada
- **THEN** restante não executado deve ser removido
- **AND** ExecutionReport com status Cancelled deve ser gerado para o restante
- **AND** parte já executada deve permanecer inalterada

### Requirement: Engine deve detectar e tratar condições de erro
O sistema SHALL validar entradas e tratar condições de erro apropriadamente.

#### Scenario: Ordem com preço inválido
- **WHEN** ordem recebida com preço zero ou negativo
- **THEN** ordem deve ser rejeitada
- **AND** ExecutionReport com status Rejected deve ser gerado
- **AND** ordem não deve ser adicionada ao livro

#### Scenario: Ordem com quantidade inválida
- **WHEN** ordem recebida com quantidade zero ou negativa
- **THEN** ordem deve ser rejeitada
- **AND** ExecutionReport com status Rejected deve ser gerado
- **AND** ordem não deve ser adicionada ao livro

#### Scenario: Ordem com ID duplicado
- **WHEN** nova ordem recebida com clOrdID já existente no livro
- **THEN** ordem deve ser rejeitada
- **AND** ExecutionReport com status Rejected deve ser gerado
- **AND** ordem existente deve permanecer inalterada