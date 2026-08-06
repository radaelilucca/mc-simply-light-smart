# Pivot de arquitetura: dimmers nos blocos originais

## Objetivo

Substituir a arquitetura baseada em blocos `dimmer_*` por uma integração que
torne os blocos já registrados pelo **Simply Light** dimerizáveis. O addon
continuará fornecendo o **Smart Switch** e o **Smart Linker**, mas não criará
duplicatas de lâmpadas, itens, modelos, receitas ou IDs de bloco.

O resultado esperado é que construções existentes com Simply Light passem a
responder a redstone e ao Smart Switch sem substituição manual de blocos. Como
os IDs, modelos e texturas permanecem os do mod original, a compatibilidade de
resource packs e shaders também deixa de depender de um patch por família.

Este pivot não inclui migração dos blocos `dimmer_*` já registrados pelo addon.
Eles serão removidos junto com seus itens, receitas e assets quando a nova
arquitetura estiver pronta.

## Comportamento de redstone proposto

Para famílias que o Simply Light disponibiliza em uma única variante, o
comportamento será sempre de dimmer inverso:

| Sinal de redstone | Emissão de luz |
| --- | --- |
| 0 | 15 (brilho máximo) |
| 1–14 | `15 - sinal` |
| 15 | 0 (desligado) |

Isso se aplica a lightbulbs, slabs, panels, fixtures, rods, edge lights e
columns, em todas as cores. Não haverá item normal e item invertido para essas
famílias.

Os Illuminant Blocks são a exceção intencional. O Simply Light já tem dois IDs
nativos para cada cor: `illuminant_*_block` e `illuminant_*_block_on`.
Manteremos essa distinção, sem criar novos blocos:

| Variante original | Comportamento |
| --- | --- |
| `…_block` | Dimmer normal: 0 desligado, 15 no brilho máximo |
| `…_block_on` | Dimmer inverso: 0 no brilho máximo, 15 desligado |

O Smart Switch entrega sua intensidade de 0–15 ao bloco vinculado e substitui
somente a leitura local de redstone enquanto o vínculo existir.

## Premissas técnicas validadas

- O alvo é NeoForge 21.1.234 para Minecraft 1.21.1.
- Simply Light 1.5.3 registra classes distintas para `LampBlock`, `LightBulb`,
  `ThinLamp`, `Fixture`, `RodLamp`, `EdgeLight` e `LampPost`.
- `LampBlock` já contém o estado binário `on` e é a origem dos dois tipos de
  Illuminant Block. As demais famílias são registradas uma única vez.
- A emissão dos blocos é definida nas classes do Simply Light. Para alterá-la
  sem trocar o ID do bloco, será necessário um ponto de extensão em runtime
  (o caminho de referência é Mixin), acompanhado por uma verificação rígida da
  versão suportada do mod base.
- O código atual de Smart Switch, Smart Linker, seleção persistente e limite de
  64 destinos pode ser reaproveitado. A abstração `DimmerLight`, que pressupõe
  blocos do addon, precisará ser substituída por uma ponte para blocos
  originais.

## Arquitetura de destino

### Estado sincronizado no bloco original

Adicionar, por Mixin, uma propriedade de estado do addon — por exemplo,
`smart_power=0..15` — às classes de bloco do Simply Light. Essa propriedade
fica salva no chunk e é sincronizada ao cliente; portanto o motor de luz, o
renderizador e shaders podem observar a intensidade sem bloco-entidade por
lâmpada.

O Mixin também deverá fornecer a emissão de luz a partir de `smart_power`.
Para os tipos de uma única variante, `smart_power` recebe `15 - sinal`; para
os dois Illuminant Blocks, recebe a curva correspondente à variante original.
O estado `on` original será mantido coerente para os Illuminant Blocks, pois os
modelos originais o usam para a aparência ligada/desligada.

### Camada `OriginalLightAdapter`

Criar uma camada de serviço, sem herança dos blocos do addon, responsável por:

- reconhecer se um `BlockState` pertence a uma família Simply Light suportada;
- calcular a potência local conforme a semântica da variante;
- ler e escrever `smart_power` preservando orientação, água, conexões e demais
  propriedades originais;
- definir a posição canônica de controle e o volume de seleção;
- atualizar todos os segmentos de estruturas multipartes, como Lamp Posts;
- expor cor e família para a integração visual e de shader.

O Smart Switch, o Smart Linker e os eventos de chunk chamam essa camada. Eles
não devem depender de `instanceof` em classes de dimmer próprias.

### Controle e persistência

`SmartLinkSavedData` continua sendo a autoridade de vínculos entre um Smart
Switch e posições globais. Ela será generalizada para validar destinos por meio
do `OriginalLightAdapter` em vez de `DimmerLight`.

Quando a posição não estiver vinculada, a atualização usa o sinal local. Quando
estiver vinculada, usa o sinal do Smart Switch. A potência calculada é escrita
no estado do bloco original e persiste naturalmente em save, unload e reload.

Para Lamp Posts, a fase de protótipo definirá explicitamente qual segmento é a
entrada de redstone e a posição canônica do Linker. A implementação deve
atualizar os três segmentos e destacar a estrutura inteira, nunca somente o
segmento clicado.

## Fases de implementação

### Fase 0 — Spike de viabilidade e contrato com o mod base

Objetivo: provar a técnica antes de reestruturar o addon.

1. Adicionar a infraestrutura Mixin em modo de desenvolvimento e restringir o
   suporte inicialmente a Simply Light 1.5.3 para Minecraft 1.21.1.
2. Em uma única Lightbulb original, adicionar `smart_power`, sobrescrever a
   emissão de luz e atualizar o estado após mudança de redstone.
3. Confirmar que o bloco conserva ID, item, orientação, água, loot e modelo
   original após salvar e recarregar o mundo.
4. Validar cliente dedicado, servidor dedicado, mundo existente e o shader
   Complementary/Euphoria sem alteração de `block.properties`.
5. Registrar o contrato técnico descoberto: métodos alvo, assinaturas Mixin,
   flags corretas de `setBlock` e comportamento do motor de luz.

**Gate:** seguir somente se o protótipo não exigir substituição de bloco,
modelo ou item e se o estado for sincronizado corretamente em multiplayer.

### Fase 1 — Fundação independente de famílias

Objetivo: criar a integração reutilizável antes de habilitar o catálogo inteiro.

1. Criar `OriginalLightAdapter` e um registro declarativo de famílias, cores e
   curvas de brilho.
2. Implementar o estado `smart_power` e a emissão dinâmica para todas as
   classes-alvo do Simply Light.
3. Centralizar as atualizações em eventos de colocação, alteração de vizinhança
   e carregamento de chunk, mantendo o lado servidor como autoridade.
4. Interceptar a lógica binária nativa de `LampBlock` somente quando necessário
   para que ela não sobrescreva a potência graduada calculada pelo addon.
5. Criar testes de integração para os valores 0, 1, 7, 14 e 15.

**Gate:** cada bloco de variante única deve ir de máximo a desligado sem criar
IDs, itens ou assets no namespace `simply_light_smart`.

### Fase 2 — Famílias simples e comportamento local

Objetivo: entregar a base jogável com o menor risco geométrico.

1. Habilitar Lightbulbs, Illuminant Slabs, Illuminant Panels e Fixtures.
2. Aplicar a regra única `emissão = 15 - sinal` a essas famílias.
3. Habilitar os dois tipos originais de Illuminant Block com as curvas normal e
   inversa descritas neste plano.
4. Implementar atualização visual por `BlockColor`/tint quando o modelo
   original oferecer faces tintáveis; manter os assets originais e confirmar a
   degradação visual aceitável quando não oferecer.
5. Validar redstone, rotação, água, relog, troca de dimensão e bordas de chunk.

**Gate:** nenhum modelo, receita, item ou blockstate duplicado é necessário
para essas famílias.

### Fase 3 — Geometrias conectadas e multipartes

Objetivo: adicionar famílias que exigem regras espaciais próprias.

1. Habilitar Rod Lamps preservando eixo e waterlogging.
2. Habilitar Edge Lights e Top Edge Lights sem interferir no editor/conexões
   do Simply Light.
3. Habilitar Lamp Posts/Columns com atualização atômica de seus três segmentos,
   posição canônica e shape completo para o Linker.
4. Testar quebra, pistões quando aplicável, substituição por fluido e edição de
   conexões durante mudanças de potência.

**Gate:** cada forma continua se comportando exatamente como o bloco original
fora da emissão e do controle por redstone/Smart Switch.

### Fase 4 — Smart Switch e Smart Linker sobre blocos originais

Objetivo: reaproveitar a lógica de rede sem manter uma segunda hierarquia de
blocos.

1. Trocar todas as verificações de `DimmerLight` pelo `OriginalLightAdapter`.
2. Preservar o formato de seleção do Linker, a limpeza manual, a seleção que
   sobrevive ao push e o limite de 64 destinos por Switch.
3. Fazer o Smart Switch gravar `smart_power` nos destinos carregados e reidratar
   a potência nos eventos de chunk.
4. Adaptar o outline do Linker às shapes originais e à posição canônica de
   estruturas multipartes.
5. Manter a regra de exclusividade: um bloco pertence a somente um Smart Switch.

**Gate:** um mundo com somente blocos Simply Light originais oferece a mesma
experiência de Switch/Linker da versão atual.

### Fase 5 — Compatibilidade visual e shaders

Objetivo: remover a superfície de manutenção criada pelos blocos duplicados.

1. Validar BSL, Complementary Reimagined e Complementary Unbound usando os IDs
   nativos do Simply Light.
2. Confirmar que o brilho de 0–15 permanece funcional com shader ativado e
   desativado.
3. Remover o patcher de shader do fluxo suportado, pois o novo caminho não
   introduz IDs de bloco que o shader precise conhecer.
4. Validar resource packs que substituem modelos/texturas do Simply Light.

**Gate:** nenhuma configuração de shader precisa conhecer
`simply_light_smart:dimmer_*` para o comportamento novo.

## Reuso e descarte deliberado

| Reaproveitar | Substituir ou aposentar |
| --- | --- |
| Smart Switch, bloco-entidade e limite de 64 links | Hierarquia `DimmerLight` e subclasses `Dimmer*` |
| `SmartLinkSavedData` e seleção persistida no item | Registros, itens, receitas, lang e assets de `dimmer_*` |
| Protocolo de limpeza manual e seleção preservada | Patcher de shader como requisito do caminho novo |
| Lógica de posições globais, alcance e exclusividade | Checagens baseadas em `instanceof DimmerLight` |
| Outline e atualização de chunk, generalizados | Modelos de item/bloco específicos do addon |

## Riscos e mitigação

- **Mudanças internas no Simply Light:** Mixins são acoplados a classes e
  assinaturas do mod base. Fixar a faixa de versões suportada, validar no
  startup e falhar com mensagem clara em vez de aplicar em versão desconhecida.
- **Propriedade adicional no BlockState:** validar serialização, carregamento de
  mundo e compatibilidade de blockstates/resource packs antes de expandir as
  famílias.
- **Luz em multiplayer:** a emissão precisa vir de estado sincronizado, nunca de
  dados disponíveis somente no servidor.
- **Lógica nativa do `LampBlock`:** interceptar apenas sua troca binária de
  redstone; não substituir colocação, loot ou demais comportamentos sem prova.
- **Colunas e edges:** introduzir depois das famílias simples e cobrir com
  cenários dedicados de shape, conexão e atualização de segmentos.
- **Conteúdo `dimmer_*` já criado:** está deliberadamente fora de escopo. A
  remoção dos IDs legados será uma alteração incompatível e precisa constar de
  forma explícita no changelog da versão do pivot.

## Matriz mínima de aceitação

- Cada família e cada cor responde aos sinais 0, 1, 7, 14 e 15.
- Variantes únicas obedecem a curva invertida; os dois Illuminant Blocks mantêm
  suas curvas normal/inversa nativas.
- O bloco mantém ID, item, modelo, textura, shape, orientação, água e conexões
  originais.
- Smart Switch e Smart Linker funcionam em singleplayer, servidor dedicado,
  chunk descarregado/recarregado e entre dimensões segundo as regras atuais.
- Lamp Posts atualizam todos os segmentos e recebem outline integral.
- BSL, Complementary Reimagined e Complementary Unbound funcionam sem patch de
  IDs de bloco do addon.
- O catálogo `dimmer_*`, suas receitas, assets e patcher deixam de fazer parte
  da distribuição após a conclusão do pivot.
