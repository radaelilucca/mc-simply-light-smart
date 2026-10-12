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
