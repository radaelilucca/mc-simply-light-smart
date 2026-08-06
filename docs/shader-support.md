# Suporte a shaders

O addon controla a emissão vanilla dos blocos originais do Simply Light por
meio do estado `smart_power=0..15`. Nenhum bloco de luz, item, modelo ou ID de
shader é criado pelo addon.

Shader packs que calculam a emissão a partir da luz vanilla acompanham o
dimmer automaticamente. Alguns packs identificam uma lâmpada apenas pelo seu
ID e aplicam um material emissivo fixo. Nesses packs, o estado `smart_power`
precisa ser reconhecido pela própria integração do shader para que uma luz em
`0` não continue brilhando.

A compatibilidade completa para esses packs deve ser tratada no upstream do
shader: a regra do material precisa respeitar `smart_power` e, idealmente,
mapear seus níveis visuais. O addon não inclui nem requer um patch manual de
shader.
