# Smart Switch — plano de implementação

## Objetivo

Adicionar um **Smart Switch** que recebe um sinal de redstone e o retransmite
para várias lâmpadas dimerizáveis vinculadas. O vínculo é configurado com um
item separado, o **Linker**.

O Smart Switch e o Linker permanecem independentes de Create. A compatibilidade
com Sable será opcional e implementada por reflexão sobre o Sable Companion
fornecido pelo próprio Sable. Sem Sable instalado, o comportamento normal
continua disponível.

## Matriz de compatibilidade alvo

A implementação será validada na instância base **Create Ult. Sel. Custom
11.0.0**, que usa Minecraft 1.21.1 e NeoForge 21.1.234 — exatamente a versão
do projeto. As versões relevantes encontradas na instância são:

- Simply Light `1.5.3+1.21.1-b4`;
- Create `6.0.10`;
- Sable `2.0.3`;
- Sable Companion para 1.21.1 `1.6.0`, já embutido pelo Sable.

O addon **não** terá dependência de runtime nem de desenvolvimento em Create.
Ele também não empacota Sable Companion: o Sable `2.0.3` já fornece internamente
a versão `1.6.0`. A integração usa os símbolos dessa versão por reflexão e
reverte automaticamente à lógica vanilla quando Sable não está instalado;
portanto não há uma biblioteca paralela nem um mod Sable Companion separado.

## Experiência do jogador

1. O jogador coloca um Smart Switch no mundo.
2. Com o Linker na mão, clica com o botão direito em uma lâmpada dimerizável
   para adicioná-la ou removê-la da seleção.
3. O Linker exibe uma tooltip dinâmica, por exemplo: `6 dimmers linked`.
4. Enquanto o Linker está na mão, cada alvo selecionado recebe apenas um outline
   verde simples.
5. Com Shift pressionado, o jogador clica com o botão direito no Smart Switch.
   A lista atual do Linker substitui toda a lista de vínculos daquele switch.
6. Uma lista vazia aplicada ao switch remove todos os seus vínculos.

## Regras funcionais

- O alcance máximo para criar um vínculo é **128 blocos físicos**.
- Os blocos devem pertencer à mesma dimensão Minecraft, mas podem estar em
  sublevels Sable diferentes.
- O sinal emitido pelo Smart Switch é exatamente o maior sinal de redstone
  recebido pelo bloco (`0` a `15`).
- Enquanto uma lâmpada está vinculada, ela ignora redstone local e é controlada
  exclusivamente pelo Smart Switch.
- Uma lâmpada pode pertencer a apenas um Smart Switch. O servidor rejeita
  tentativas de vinculá-la a outro switch.
- Vínculos não carregam chunks. Se um alvo está descarregado, sua atualização é
  aplicada quando ele voltar a carregar.
- O alcance é validado na criação. Um vínculo já criado continua ativo se uma
  estrutura Sable se mover depois disso.
- O limite inicial é 64 lâmpadas por switch, exposto como configuração de
  servidor para ajuste futuro.

## Arquitetura

### Smart Switch

- `SmartSwitchBlock`: bloco que detecta mudanças de redstone e possui uma
  `SmartSwitchBlockEntity`.
- `SmartSwitchBlockEntity`: persiste os alvos, acompanha o último sinal e pede
  atualização das lâmpadas carregadas quando o sinal ou a lista muda.
- `SmartLinkSavedData`: índice persistente `lâmpada -> switch`. Ele garante
  exclusividade, permite desvincular com segurança e informa ao dimmer se o
  controle remoto está ativo.

### Dimmers existentes

- Adicionar ao caminho de atualização de potência uma consulta ao índice de
  vínculos.
- Se vinculado, usar somente a potência remota do Smart Switch; se não, manter
  a lógica atual de redstone local.
- Ao remover um vínculo ou destruir o switch, recalcular imediatamente a
  potência local do dimmer quando o bloco estiver carregado.

### Linker

- `SmartLinkerItem` com um componente de dados próprio que guarda as posições
  selecionadas e a dimensão de origem.
- O clique direito em dimmers alterna a seleção no lado servidor, validando que
  o alvo pertence ao addon.
- Shift + clique direito no Smart Switch envia a lista ao Block Entity, que faz
  todas as validações de forma atômica e limpa a seleção somente em caso de
  sucesso.
- A tooltip lê o componente e mostra a quantidade atual de alvos.

### Outline e Sable

- Registrar um renderizador cliente simples em `RenderLevelStageEvent` para
  desenhar caixas de contorno verdes; não usar Create, Catnip ou Ponder.
- Fora de Sable, desenhar a caixa do `VoxelShape` do alvo com APIs vanilla.
- Com Sable, usar Sable Companion para medir distância no espaço global e
  aplicar a pose de renderização do sublevel ao outline. As posições persistidas
  permanecem nas coordenadas locais do plot, para acompanharem estruturas móveis.

## Progresso conferido no código

| Fase | Estado atual | Evidência no código | Validação manual |
| --- | --- | --- | --- |
| 1. Núcleo e sinal remoto | Implementada | `SmartSwitchBlock`, `SmartSwitchBlockEntity`, `SmartLinkSavedData`, `OriginalLightAdapter` | O changelog registra o suporte e correções em releases; a matriz completa desta fase não está documentada como executada. |
| 2. Linker e vínculos | Implementada | `SmartLinkerItem`, `SmartTargetAddress`, `SmartSwitchBlockEntity.applyLinks` | O changelog registra seleção e persistência; não há registro de execução de todos os casos da matriz. |
| 3. Feedback visual e Sable | Implementada | `SmartLinkerSelectionOutline`, `SableOutlineProjection`, `SableLinkDistance`, `SmartTargetAddress` | O changelog registra operação entre level e sublevels; split/merge seguem fora do escopo. |
| 4. Conteúdo e acabamento | Implementada | registros, receitas, loot table, modelos, texturas e traduções | Conteúdo incluído nos releases publicados. |
| 5. Verificação final | Parcial / sem checklist registrado | Build/resource e cenários listados abaixo não possuem relatório de execução neste plano | Não marcar como concluída até registrar a matriz executada. |

### Fase 1 — Base e sinal remoto

Implementada. O projeto registra Smart Switch, Block Entity e Saved Data; mantém o índice persistente de propriedade exclusiva; guarda o último sinal; e usa o sinal remoto nas famílias originais habilitadas pelo `OriginalLightAdapter`. A implementação atual cobre Illuminant Blocks, Bulbs, Slabs/Panels (`ThinLamp`), Fixtures, Rods, Edges/Top Edges e Columns. O plano original falava em quatro famílias; o escopo cresceu junto com a migração para os blocos originais do Simply Light.

Os limites atuais de 64 alvos e 128 blocos estão como constantes de implementação, não como configuração de servidor.

### Fase 2 — Linker e configuração de vínculos

Implementada. O Linker mantém seleção persistente no componente do item; clique alterna alvos; Shift + clique no switch aplica a lista; Shift + clique no ar limpa a seleção. `applyLinks` valida duplicatas, dimensão, distância, alvo carregado e suportado, limite de alvos e exclusividade antes de substituir os vínculos.

### Fase 3 — Feedback visual e Sable

Implementada no código. O renderer desenha outlines verdes para a seleção, incluindo shapes compostos das Columns. A ponte cliente usa Sable Companion por reflexão para projetar o outline no espaço renderizado do sublevel. No servidor, `SableLinkDistance` calcula a distância física; `SmartTargetAddress` persiste UUID do sublevel e posição local, com fallback para posições globais. Sem Sable, a integração opcional retorna à posição e distância vanilla quando aplicável.

O changelog registra suporte de links entre level e sublevels. O código e o histórico consultados não comprovam a matriz completa de assemble/disassemble, split/merge ou todas as combinações de instalação; split/merge permanecem explicitamente adiados no plano de contraptions.

### Fase 4 — Conteúdo e acabamento

Implementada e incluída nos releases: modelos, texturas, nomes, aba criativa, receitas, loot table, traduções e feedback de vínculo.

### Fase 5 — Verificação final

O código cobre os cenários funcionais listados, mas este repositório não contém checklist ou relatório que comprove a execução completa em mundo novo e existente, incluindo destruição, chunks descarregados, relink, lista vazia, conflito entre switches e a matriz NeoForge/Create/Sable. Registrar essa validação antes de promover a fase a concluída.

## Registro original de commits

### Fase 1 — Base e sinal remoto

- Usar, opcionalmente e por reflexão, o Sable Companion `1.6.0` já fornecido
  pelo Sable `2.0.3`, sem adicionar uma dependência paralela ao addon.
- Preparar o perfil de teste do `runClient` com os JARs exatos de Sable `2.0.3`
  e Create `6.0.10`; o teste de sublevels não será aceito no ambiente de
  desenvolvimento sem Sable.
- Registrar Smart Switch, Block Entity, tipo de Block Entity e Saved Data.
- Implementar índice de vínculos, exclusividade, persistência, limpeza e o
  controle remoto exclusivo das quatro famílias de dimmers.
- Validar sinal `0`, intermediário e `15` em dimmers normais e invertidos.

**Commit previsto:** `feat: add smart switch relay core`

### Fase 2 — Linker e configuração de vínculos

- Registrar o Linker, seu componente de dados e textos de tooltip.
- Implementar seleção por clique direito, remoção por novo clique e aplicação
  da lista por Shift + clique direito no Smart Switch.
- Aplicar validações de tipo, dimensão, limite de 128 blocos, exclusividade e
  máximo de alvos.

**Commit previsto:** `feat: add smart switch linker`

### Fase 3 — Feedback visual e Sable

- Implementar outline verde mínimo para a seleção presente no Linker.
- Integrar projeção de distância e renderização para sublevels Sable.
- Testar seleção e vínculo entre mundo normal, um sublevel e dois sublevels.

**Commit previsto:** `feat: add smart switch selection feedback`

## Critérios de aceite

- Um Smart Switch transmite corretamente os 16 níveis de redstone aos seus
  dimmers vinculados.
- Dimmers normais e invertidos preservam sua resposta atual ao sinal recebido.
- O Linker mostra a contagem correta, destaca os alvos e substitui a lista de
  vínculos do switch de forma previsível.
- Nenhuma lâmpada recebe sinais de dois switches.
- O alcance físico de 128 blocos funciona corretamente em sublevels Sable.
- O addon continua carregando e funcionando sem Sable instalado.
