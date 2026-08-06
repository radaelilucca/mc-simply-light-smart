# Compatibilidade com contraptions legadas do Sable

## Objetivo

Permitir que mundos e contraptions do Sable que já continham luzes do Simply
Light antes da instalação deste addon carreguem sem escrita reentrante durante
a restauração dos plot chunks.

## Acompanhamento

| Fase | Estado | Commit | Validação manual |
| --- | --- | --- | --- |
| 0. Plano e reprodução | Concluída | `0cc8839` | Mapa de regressão identificado |
| 1. Fila segura pós-tick | Validada | `b62149c` | Contraption legada carregou normalmente |
| 2. Defaults e cálculo de estado | Validada | `a318d68` | Emissão e reinício corretos |
| 3. Metadado e índice persistentes | Validada | `480a6a3` | Reutilização confirmada; fallback seguro para chunks ausentes |
| 4. Ciclo seguro do Switch | Validada | `8163a41` | Links e redstone corretos entre level e sublevel |
| 5. Endereços conscientes de sublevel | Pronta para teste | — | Split, merge e realocação |

## Fase 0 — Base de teste

- Preservar um backup do mapa com a contraption criada sem o addon.
- Registrar no log quando um chunk é enfileirado, migrado e concluído.
- Usar esse mapa como regressão em cada fase que altera o carregamento.

## Fase 1 — Fila segura pós-tick

- `ChunkEvent.Load` somente enfileira o `LevelChunk`; não lê bloco, redstone
  ou luz.
- Um trabalho só pode rodar no `ServerTickEvent.Post` de um tick posterior ao
  enqueue.
- `onNeighborNotify` e `SmartSwitchBlockEntity.onLoad()` também só enfileiram
  trabalho.
- Nenhum callback de carga pode chamar `setBlock`, `setBlockAndUpdate`,
  `refreshLocal` ou atualizar alvos remotos.

Ponto de teste: abrir duas vezes o mapa de regressão sem remover as luzes.

## Fase 2 — Defaults e cálculo de estado

- Definir `smart_mode=native` e o brilho nativo correto no default state de
  cada família do Simply Light.
- Separar cálculo de estado de sua aplicação no mundo.
- Preparar a migração por chunk para a fase seguinte, sem duplicar lógica de
  cálculo ou escrita.

## Fase 3 — Metadado e índice persistentes

- Persistir o schema e as posições locais das luzes em um Data Attachment de
  chunk, recurso que o Sable 2.0.3 serializa em seus plot chunks.
- Indexar apenas a posição-base das Columns.
- Reconstruir o índice quando o attachment não acompanhar uma operação do
  Sable, em vez de assumir que ele está correto.

## Fase 4 — Ciclo seguro do Smart Switch

- Inicializar Switches, alterações de redstone e atualizações de links pela
  mesma fila segura.
- Atualizar somente alvos em chunks já carregados.
- Versionar e validar o `SmartLinkSavedData` sem apagar links cujo chunk ainda
  não foi carregado.

## Fase 5 — Endereços conscientes de sublevel

- Representar alvos do Sable por UUID persistente do sublevel e posição local,
  mantendo `GlobalPos` para o mundo normal.
- Migrar entradas legadas de forma preguiçosa quando elas puderem ser
  resolvidas.
- Validar assemble, disassemble, split, merge e realocação.

## Critérios de aceite

- Nenhuma escrita de bloco durante `ChunkEvent.Load` ou `BlockEntity.onLoad`.
- A contraption legada carrega repetidamente sem `Chunk already exists`.
- Luzes antigas preservam o comportamento nativo até a migração e então
  respeitam redstone e o Smart Switch.
- Cargas posteriores usam o índice persistente, sem varredura integral do
  chunk.
