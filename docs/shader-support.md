# Suporte a shaders

O addon controla a emissão vanilla dos blocos originais do Simply Light por
meio do estado `smart_power=0..15`. Nenhum bloco de luz, item, modelo ou ID de
shader é criado pelo addon.

Shader packs que calculam a emissão a partir da luz vanilla acompanham o
dimmer automaticamente. Para packs que aplicam material emissivo fixo por ID,
o addon inclui um patcher cliente que cria uma variante `SLSmart` para pastas
geradas pelo Euphoria Patches. O patcher reconhece `smart_power` para ajustar a
emissão e o escurecimento das texturas, e preserva os arquivos-fonte do pack.

O patcher é opcional e não altera os packs Complementary base. Consulte a
seção **Shader support** do `README.md` para as combinações testadas e o fluxo
visível ao jogador.

## Cor durante a redução de luz no Euphoria

O patch de formato `13` mantém os materiais do dimmer (`21100..21124`)
na mesma família de cor dos materiais nativos (`21000..21024`). A conversão
acontece na entrada de `GetVoxelIDs`, antes das condições internas do pack.
A intensidade continua acompanhando o lightmap vanilla; a curva visual da
textura permanece independente. Com emissão zero, o voxel deixa de ser uma
fonte colorida quando o atributo de emissão do Iris está disponível.

No formato `12`, os casos `211xx` eram inseridos junto ao material rosa
nativo. No Euphoria 1.10, esse trecho fica dentro de `mat < 21026`, tornando
os novos casos inalcançáveis. Ao passar de sinal 3 para 4 no modo invertido,
`smart_power` cai de 12 para 11 e a slab vermelha troca o material `21004`
pelo `21104`. Este retornava voxel `1` (bloco comum), em vez de `70`
(fonte vermelha). A emissão vanilla continuava, mas a iluminação colorida
desaparecia. O defeito atingia todas as cores e famílias mapeadas.

### Evidência e validação em 2026-10-12

O vídeo `2026-10-12 14-21-57.mp4` tem 491 quadros a 60 fps. Foram extraídos
todos os quadros e medidas as cores da lateral da slab e do chão. A inspeção
dos quadros consecutivos nas transições mostrou:

| Quadro, contado de zero | Tempo | Comportamento observado |
| --- | --- | --- |
| 190 | 3,167 s | Sinal 4: lateral fica clara e a iluminação vermelha começa a desaparecer. |
| 336 | 5,600 s | Retorno ao sinal 0: a iluminação vermelha reaparece. |
| 398 | 6,633 s | Nova passagem para sinal 4: a mesma perda de cor se repete. |
| 456 | 7,600 s | Retorno ao sinal 3: a iluminação vermelha reaparece. |

O número exibido pelo controle muda alguns quadros antes da iluminação;
as marcas acima registram a mudança visual, não o instante do comando.
O vídeo demonstra a slab vermelha e o limite 3/4; outros blocos, cores e
níveis foram verificados no mapeamento gerado, sem prova visual em jogo.

Validações da correção:

- `gradlew assemble --offline`: compilação e JAR concluídos.
- Patcher executado sobre cópias limpas dos quatro packs locais: Reimagined
  e Unbound, Euphoria 1.9.3 e 1.10.0.
- Em cada pack, conferidos 1.920 seletores de estado: 16 cores, 10 variantes
  de bloco e `smart_power=0..11`.
- A árvore de decisão gerada foi executada como Java para conferir os 13
  grupos de cor, emissão zero e níveis 1..11; os demais IDs de material
  mantiveram seus retornos. Isso verifica a lógica de seleção, sem substituir
  a compilação GLSL ou a validação visual na GPU.

Para validar em jogo, reinicie o `runClient` recompilado. O novo formato
invalida a variante `SLSmart` anterior e provoca sua regeneração automática.
Depois da notificação, recarregue ou selecione novamente o shader gerado.
Confira os sinais 3/4, 13/14 (luz fraca) e 15 (apagado), incluindo ao menos
vermelho, azul e verde e as famílias slab, painel e bloco inteiro. No modo
`default_off`, confira também os sinais 0/1. A confirmação visual da correção
permanece pendente desse teste.
