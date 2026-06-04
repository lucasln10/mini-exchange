## ADDED Requirements

### Requirement: Servidor deve processar mensagens FIX válidas
O sistema SHALL aceitar e processar mensagens FIX 4.2 válidas corretamente.

#### Scenario: NewOrderSingle com campos obrigatórios válidos
- **WHEN** mensagem NewOrderSingle (Tag 35=D) recebida com todos campos obrigatórios válidos
- **THEN** mensagem deve ser convertida para objeto Order
- **AND** Order deve ser processado pelo MatchingEngine
- **AND** ExecutionReport com status New deve ser gerado

#### Scenario: NewOrderSingle com campos obrigatórios ausentes
- **WHEN** mensagem NewOrderSingle recebida sem campos obrigatórios (ex: Tag 11, 40, 54)
- **THEN** mensagem deve ser rejeitada
- **AND** ExecutionReport com status Rejected deve ser gerado
- **AND** erro apropriado deve ser registrado

#### Scenario: NewOrderSingle com valores inválidos
- **WHEN** mensagem NewOrderSingle recebida com valores inválidos em campos obrigatórios
- **THEN** mensagem deve ser rejeitada
- **AND** ExecutionReport com status Rejected deve ser gerado
- **AND** erro específico deve indicar o campo inválido

### Requirement: Servidor deve processar ordens cancelamento
O sistema SHALL aceitar e processar mensagens OrderCancelRequest corretamente.

#### Scenario: OrderCancelRequest para ordem existente
- **WHEN** mensagem OrderCancelRequest (Tag 35=F) recebida para ordem existente
- **THEN** sistema deve identificar a ordem
- **AND** ordem deve ser cancelada
- **AND** ExecutionReport com status Cancelled deve ser gerado

#### Scenario: OrderCancelRequest para ordem inexistente
- **WHEN** mensagem OrderCancelRequest recebida para ordem não encontrada
- **THEN** mensagem deve ser rejeitada
- **AND** ExecutionReport com status OrderCancelReject deve ser gerado
- **AND** erro deve indicar ordem não encontrada

#### Scenario: OrderCancelRequest para ordem já executada
- **WHEN** mensagem OrderCancelRequest recebida para ordem completamente executada
- **THEN** cancelamento deve ser rejeitado
- **AND** ExecutionReport com status OrderCancelReject deve ser gerado
- **AND** deve indicar que ordem já foi executada

### Requirement: Cliente deve enviar mensagens FIX corretamente
O sistema SHALL gerar mensagens FIX de resposta corretamente.

#### Scenario: ExecutionReport bem sucedido
- **WHEN** ordem processada com sucesso
- **THEN** ExecutionReport (Tag 35=8) deve ser gerada
- **AND** campos obrigatórios devem estar presentes (Tag 11, 37, 55, 54, 38, 39)
- **AND** campos devem refletir status correto (OrdStatus='0' para New)

#### Scenario: ExecutionReport parcial
- **WHEN** ordem parcialmente executada
- **THEN** ExecutionReport deve refletir execução parcial
- **AND** cumQty deve refletir quantidade executada
- **AND** leavesQty deve refletir quantidade restante
- **AND** OrdStatus deve ser '1' para Partially Filled

#### Scenario: ExecutionReport completamente executado
- **WHEN** ordem completamente executada
- **THEN** ExecutionReport deve refletir execução completa
- **AND** cumQty deve igual ordem original
- **AND** leavesQty deve ser zero
- **AND** OrdStatus deve ser '2' para Filled

### Requirement: Sistema deve validar formatação de mensagens FIX
O sistema SHALL validar a estrutura e formatação de mensagens FIX.

#### Scenario: Mensagem com formato inválido
- **WHEN** mensagem FIX recebida com formato inválido (ex: tags mal ordenados)
- **THEN** mensagem deve ser rejeitada
- **AND** erro de formatação deve ser registrado
- **AND** nenhuma execução deve ocorrer

#### Scenario: Mensagem com checksum inválido
- **WHEN** mensagem FIX recebida com checksum inválido (Tag 10)
- **THEN** mensagem deve ser rejeitada
- **AND** erro de checksum deve ser registrado
- **AND** reenvio deve ser solicitado se aplicável

#### Scenario: Mensagem com versão FIX incompatível
- **WHEN** mensagem FIX recebida com BeginString não suportado
- **THEN** mensagem deve ser rejeitada
- **AND** erro de versão deve ser registrado
- **AND** conexão deve ser mantida para versões suportadas

### Requirement: Sistema deve tratar condição de rede
O sistema SHALL lidar com problemas de rede e reconexão apropriadamente.

#### Scenario: Perda de conexão durante processamento
- **WHEN** conexão perdida enquanto processando ordem
- **THEN** estado interno deve ser consistente
- **AND** ordens pendentes devem ser mantidas
- **AND** reconexão deve ser tentada automaticamente

#### Scenario: Recepção de mensagem duplicada
- **WHEN** mensagem duplicada recebida (mesmo SeqNum)
- **THEN** mensagem duplicada deve ser ignorada
- **AND** ACK deve ser enviado para confirmar recebida
- **AND** estado deve permanecer consistente

#### Scenario: Mensagem fora de sequência
- **WHEN** mensagem recebida com SeqNum fora de ordem
- **THEN** sistema deve detectar e tratar
- **AND** mensagens posteriores devem ser processadas
- **AND** erro de sequência deve ser registrado se necessário