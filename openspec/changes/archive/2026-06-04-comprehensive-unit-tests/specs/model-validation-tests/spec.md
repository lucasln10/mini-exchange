## ADDED Requirements

### Requirement: Classe Order deve validar campos obrigatórios
O sistema SHALL validar que objetos Order têm campos obrigatórios preenchidos corretamente.

#### Scenario: Order com todos campos obrigatórios válidos
- **WHEN** Order criado com clOrdID, symbol, side, type, price, qty válidos
- **THEN** objeto deve ser criado sem erros
- **AND** remainingQty deve igualar qty original
- **AND** isFilled deve retornar false

#### Scenario: Order sem clOrdID
- **WHEN** Order criado sem clOrdID
- **THEN** deve lançar IllegalArgumentException
- **AND** objeto não deve ser criado

#### Scenario: Order sem symbol
- **WHEN** Order criado sem symbol
- **THEN** deve lançar IllegalArgumentException
- **AND** objeto não deve ser criado

#### Scenario: Order com side inválido
- **WHEN** Order criado com side diferente de BUY ou SELL
- **THEN** deve lançar IllegalArgumentException
- **AND** objeto não deve ser criado

#### Scenario: Order com type inválido
- **WHEN** Order criado com type diferente de MARKET ou LIMIT
- **THEN** deve lançar IllegalArgumentException
- **AND** objeto não deve ser criado

### Requirement: Classe Order deve calcular remainingQty corretamente
O sistema SHALL manter e calcular quantidade restante corretamente durante execução.

#### Scenario: Ordem não executada
- **WHEN** Order criado com qty=100
- **THEN** remainingQty deve ser 100
- **AND** isFilled deve retornar false

#### Scenario: Execução parcial
- **WHEN** Order com qty=100 recebe execução de 30
- **THEN** remainingQty deve ser 70
- **AND** isFilled deve retornar false

#### Scenario: Execução completa
- **WHEN** Order com qty=100 recebe execução de 100
- **THEN** remainingQty deve ser 0
- **AND** isFilled deve retornar true

#### Scenario: Execução excedente
- **WHEN** Order com qty=100 recebe execução de 150
- **THEN** remainingQty deve ser 0
- **AND** isFilled deve retornar true
- **AND** excesso de 50 deve ser tratado como erro

### Requirement: Classe OrderBook deve gerenciar ordens corretamente
O sistema SHALL adicionar, remover e localizar ordens no livro de ofertas.

#### Scenario: Adição de ordem de compra
- **WHEN** OrderBook recebe ordem de compra com preço 50.0
- **THEN** ordem deve ser adicionada ao lado de bids
- **AND** bestBid deve retornar 50.0
- **AND** bestAsk deve retornar null (sem vendas)

#### Scenario: Adição de ordem de venda
- **WHEN** OrderBook recebe ordem de venda com preço 55.0
- **THEN** ordem deve ser adicionada ao lado de asks
- **AND** bestAsk deve retornar 55.0
- **AND** bestBid deve retornar null (sem compras)

#### Scenario: Remoção de ordem existente
- **WHEN** OrderBook com ordem de clOrdID=123 recebe removeOrder(123)
- **THEN** ordem deve ser removida do livro
- **AND** findOrder(123) deve retornar null

#### Scenario: Remoção de ordem inexistente
- **WHEN** OrderBook recebe removeOrder para ordem inexistente
- **THEN** deve lançar IllegalArgumentException
- **AND** livro deve permanecer inalterado

#### Scenario: Adição de ordem com preço inválido
- **WHEN** OrderBook recebe ordem com preço negativo
- **THEN** deve lançar IllegalArgumentException
- **AND** ordem não deve ser adicionada ao livro

### Requirement: Classe OrderBook deve manter prioridade correta
O sistema SHALL manter ordens na ordem correta de prioridade (price-time).

#### Scenario: Melhor preço primeiro
- **WHEN** OrderBook recebe ordem de compra com preço 60.0 depois de ordem com 50.0
- **THEN** ordem de 60.0 deve ser depois de 50.0 no livro
- **AND** bestBid deve retornar 60.0 apenas se não houver melhor

#### Scenario: Mesmo preço FIFO
- **WHEN** OrderBook recebe duas ordens de compra com mesmo preço 50.0
- **THEN** primeira ordem recebida deve ter prioridade
- **AND** segunda ordem deve ser adicionada após a primeira

#### Scenario: Atualização de melhor preço
- **WHEN** nova ordem de compra com preço maior que atual bestBid
- **THEN** nova ordem deve se tornar o novo bestBid
- **AND** antigo bestBid deve permanecer segundo melhor

#### Scenario: Ordem com quantidade zero
- **WHEN** OrderBook recebe ordem com qty=0
- **THEN** deve lançar IllegalArgumentException
- **AND** ordem não deve ser adicionada ao livro

### Requirement: Classes devem ser thread-safe
O sistema SHALL operar corretamente em ambiente multithread.

#### Scenario: Acesso concorrente ao OrderBook
- **WHEN** múltiplas threads adicionam/removem ordens simultaneamente
- **THEN** estado interno deve permanecer consistente
- **AND** nenhuma exceção de concorrência deve ser lançada

#### Scenario: Execução concorrente de ordens
- **WHEN** múltiplas threads executam ordens no mesmo OrderBook
- **THEN** quantidade total deve ser consistente
- **AND** nenhuma ordem deve ser executada mais de uma vez

#### Scenario: Leitura e escrita concorrente
- **WHEN** uma thread lendo enquanto outra escreve no OrderBook
- **THEN** leitores devem ver estado consistente
- **AND** escritores devem conseguir modificar sem interferência