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

## Fases e commits

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

### Fase 4 — Conteúdo e acabamento

**Status: implementada.**

- Criar modelos, texturas, nomes e abas criativas para Smart Switch e Linker.
- Adicionar receitas, loot tables e documentação de uso.
- Revisar mensagens de erro e sucesso do vínculo.

**Commit previsto:** `feat: add smart switch content and recipes`

### Fase 5 — Verificação final

- Executar build e validar recursos JSON.
- Testar em mundo novo e mundo existente.
- Testar destruição de switch, destruição de dimmer, chunks descarregados,
  relink, lista vazia e conflito entre switches.
- Repetir o conjunto de testes na matriz exata: NeoForge `21.1.234`, Create
  `6.0.10` e Sable `2.0.3`, com os alvos em estruturas móveis.

## Critérios de aceite

- Um Smart Switch transmite corretamente os 16 níveis de redstone aos seus
  dimmers vinculados.
- Dimmers normais e invertidos preservam sua resposta atual ao sinal recebido.
- O Linker mostra a contagem correta, destaca os alvos e substitui a lista de
  vínculos do switch de forma previsível.
- Nenhuma lâmpada recebe sinais de dois switches.
- O alcance físico de 128 blocos funciona corretamente em sublevels Sable.
- O addon continua carregando e funcionando sem Sable instalado.
