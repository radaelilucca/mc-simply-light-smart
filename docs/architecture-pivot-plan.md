# Arquitetura: dimmer nos blocos originais

> **Estado atual:** arquitetura implementada nas famílias listadas abaixo. O
> Smart Switch, Linker e feedback visual/Sable também estão implementados; o
> plano de implementação e o registro de validação estão em
> `smart-switch-implementation-plan.md`.

## Objetivo

O addon torna os blocos já registrados pelo **Simply Light** dimerizáveis, sem
duplicar IDs de bloco, itens, receitas, modelos ou texturas. Ele fornece apenas
os controles próprios: **Smart Switch** e **Smart Linker**.

Construções existentes com Simply Light respondem a redstone e ao Smart Switch
sem substituição manual. Os blocos preservam seus IDs, modelos, shapes,
orientação, água e conexões originais.

## Controle de redstone

As famílias de variante única começam ligadas e dimerizam para baixo:

| Sinal de redstone | Emissão |
| --- | --- |
| 0 | 15 (brilho máximo) |
| 1–14 | `15 - sinal` |
| 15 | 0 (desligado) |

Cada bloco recebe os estados persistentes `smart_power=0..15` e `smart_mode`.
Além de `native`, o modo reserva `default_on` (dimeriza para baixo) e
`default_off` (dimeriza para cima), sem exigir bloco-entidade ou item adicional
para cada lâmpada.

Os Illuminant Blocks usam seus dois IDs nativos: a variante normal dimeriza
para cima e a variante `_on` dimeriza para baixo.

## Implementação

- Mixins adicionam os estados do addon às classes do Simply Light e fazem a
  emissão ler `smart_power`.
- `OriginalLightAdapter` identifica famílias suportadas, calcula a curva,
  atualiza estruturas multipartes e fornece a posição canônica ao Linker.
- `SmartLinkSavedData` guarda a associação exclusiva entre Switch e luzes.
- O sinal do Switch é cacheado em `SavedData`; cargas de chunk nunca forçam o
  carregamento de um chunk remoto.
- O **Smart Linker** mantém a seleção até a limpeza manual e suporta até 64
  luzes por Switch.

## Famílias habilitadas

- Illuminant Blocks
- Lightbulbs
- Illuminant Slabs e Panels
- Fixtures
- Illuminant Rods
- Illuminant Edges e Top Edges
- Illuminant Columns

## Integração com shaders

A emissão vanilla acompanha `smart_power`. Para Euphoria Patches, o addon
também fornece um patcher cliente opcional que gera uma variante `SLSmart` e
faz a emissão e as texturas respeitarem o estado. Detalhes e combinações
testadas estão em `shader-support.md` e no README.
