# Pivot de arquitetura — Fase 0: spike de viabilidade

## Escopo implementado

Somente a Lightbulb original do Simply Light participa do spike. O addon não
registra um bloco, item, receita, modelo ou ID novo para esse comportamento.

- A propriedade `smart_power=0..15` é acrescentada ao `BlockState` herdado de
  `RotatableLamp`.
- A emissão da `LightBulb` lê `smart_power` por Mixin, preservando o ID,
  modelo, item, orientação, água, loot e demais propriedades originais.
- No servidor, colocar a Lightbulb ou receber uma notificação de vizinhança
  grava `15 - sinal de redstone` no estado do bloco.
- A declaração de dependência é limitada a Simply Light `1.5.3`.

Outras subclasses de `RotatableLamp` recebem a propriedade estrutural por
herança, mas ainda usam a emissão original e não são reconhecidas pelo código
de atualização. Portanto, o único comportamento ativo nesta fase é a
Lightbulb.

## Contrato técnico a validar no jogo

| Item | Contrato |
| --- | --- |
| Método de estado | `RotatableLamp#createBlockStateDefinition` recebe `smart_power` no fim. |
| Método de emissão | `LightBulb#lambda$new$0(BlockState)` retorna `smart_power`. |
| Atualização | `BlockEvent.NeighborNotifyEvent` atualiza somente Lightbulbs nas posições notificadas. |
| Escrita de estado | `Level#setBlockAndUpdate` preserva todas as propriedades existentes. |
| Curva | sinais 0, 1, 7, 14 e 15 devem produzir emissões 15, 14, 8, 1 e 0. |

## Roteiro de validação manual

1. Iniciar um mundo com Simply Light `1.5.3`, colocar uma Lightbulb original e
   confirmar que ela aparece com o item, modelo e orientação nativos.
2. Aplicar os sinais 0, 1, 7, 14 e 15 e verificar a curva acima, inclusive a
   atualização visual da luz no mundo.
3. Salvar, sair e reabrir o mundo; confirmar que `smart_power` e a emissão se
   mantêm e que um novo sinal ainda atualiza o bloco.
4. Repetir em servidor dedicado e com Complementary/Euphoria, sem alterar
   `block.properties`.

O gate da Fase 0 só é aprovado depois dessa validação manual. A Fase 1 não
deve ser retomada antes disso.
