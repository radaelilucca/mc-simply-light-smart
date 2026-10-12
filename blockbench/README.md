# Modelos e texturas do Simply Light - Smart!

As fontes atuais ficam em `definitive/`:

| Asset | Projeto editável | Atlas | Exportação Java | Prévia nativa |
| --- | --- | --- | --- | --- |
| Smart Switch | [smart_switch.bbmodel](definitive/smart_switch.bbmodel) | [smart_switch.png](definitive/smart_switch.png) | [smart_switch.json](definitive/smart_switch.json) | [Prévia](definitive/smart_switch-preview.png) |
| Smart Linker | [smart_linker.bbmodel](definitive/smart_linker.bbmodel) | [smart_linker.png](definitive/smart_linker.png) | [smart_linker.json](definitive/smart_linker.json) | [Prévia](definitive/smart_linker-preview.png) |

## Direção visual

- Formas simples e legíveis, seguindo as luminárias do Simply Light.
- Grafite na carcaça e na empunhadura, metal cinza-claro, insertos brancos e pequenos LEDs verde-menta.
- Arte original criada para o addon. As texturas do JAR do Simply Light foram consultadas como referência, sem serem usadas nos atlas exportados.
- Smart Switch: oito cubos dentro do volume de um bloco, com tecla branca, rebaixo escuro e LED separado nas quatro laterais. O topo é branco liso. As teclas são desenhadas na textura dos painéis; não possuem animação. O bloco atual não possui orientação horizontal.
- Smart Linker: nove cubos, empunhadura estriada e conector aberto, sensor grafite entre as pontas e LED quadrado de 1 × 1 unidade na frente. Display ajustado no Blockbench para inventário e primeira/terceira pessoa, nas duas mãos.

## Arquivos usados pelo mod

- `src/main/resources/assets/simply_light_smart/models/block/smart_switch.json`
- `src/main/resources/assets/simply_light_smart/textures/block/smart_switch.png`
- `src/main/resources/assets/simply_light_smart/models/item/smart_linker.json`
- `src/main/resources/assets/simply_light_smart/textures/item/smart_linker.png`

As exportações Java foram feitas pelo Blockbench. Após editar uma fonte, salve o `.bbmodel`, salve a textura e exporte o JSON novamente; copie os arquivos para os caminhos acima. Mantenha o namespace `simply_light_smart` e as pastas `block`/`item` da textura.

O Linker 3D deve manter `parent: minecraft:block/block`, com os ajustes de mão definidos no próprio `display`. Não use `minecraft:item/handheld` nem `minecraft:item/generated`: essa cadeia termina em `builtin/generated`, e o Minecraft 1.21.1 substitui os cubos pela geometria gerada a partir de `layer0`. Como o atlas do Linker usa a chave `0`, essa herança produzia um modelo vazio, tanto no inventário quanto nas mãos. A fonte `.bbmodel` e a exportação Java foram corrigidas juntas para evitar reintroduzir o problema ao exportar.

## Estado da revisão

Os dois projetos foram abertos, inspecionados e exportados no Blockbench 5.2.1. O Switch recebeu acabamento de pintura; o Linker teve rotações, deslocamentos e escalas ajustados no modo Display. As prévias PNG foram exportadas pela ação nativa Screenshot Model.

Na criação inicial, foram conferidos: geometria das fontes contra os JSONs exportados, UVs normalizados, dimensões e pixels dos atlas, caminhos de textura, transforms de Display e igualdade dos arquivos copiados para os recursos do mod. A tarefa Gradle `processResources --offline` concluiu com sucesso nessa etapa.

No refinamento de 12/10/2026, os retângulos verdes grandes e a faixa do topo do Switch foram substituídos por teclas brancas com sombra e LEDs pontuais. Os quatro painéis compartilham a mesma região do atlas para manter o desenho uniforme. O Linker recebeu o mesmo tom de LED, indicador menor e sensor escuro. A pintura foi feita no Blockbench; os UVs e o tamanho do indicador foram ajustados nas fontes, reabertos no editor e exportados novamente. As prévias e os recursos do mod foram atualizados. Não houve nova execução de Gradle nem validação dentro do jogo nessa revisão.

Após o relato de invisibilidade no `runClient`, a herança do Linker foi corrigida conforme explicado acima. `assemble --offline` concluiu com sucesso; o JAR e `build/resources/main` contêm o parent 3D corrigido, os nove cubos e o atlas. Com o cliente de desenvolvimento aberto, use `F3 + T` para carregar os recursos atualizados. A correção ainda precisa de confirmação visual dentro do Minecraft.

Os detalhes verde-menta são parte da textura estática: não indicam o sinal atual nem adicionam emissão de luz. A aparência dentro do Minecraft e a interação com shaders ainda precisam de validação no jogo.

`sls-linker.bbmodel` é a fonte experimental anterior. `archive/pre-definitive/` preserva o modelo e as texturas do Linker que estavam no working tree antes da substituição. Os antigos atlas `smart_linker_model.png` e `smart_switch_top/side.png` não são referenciados pelos modelos atuais.
