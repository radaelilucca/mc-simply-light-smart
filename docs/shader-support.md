# Suporte a shaders

No Minecraft, a emissao de luz normal e monocromatica. A luz colorida vista com
Complementary Reimagined + Euphoria Patches vem das regras do proprio shaderpack:
ele identifica os blocos do Simply Light pelo ID de registro, como
`simplylight:lightbulb_red`.

Como este addon registra blocos novos no namespace `simply_light_smart`, ele
precisa de uma pequena integracao com o shaderpack. O script abaixo adiciona os
IDs dos quatro tipos dimerizaveis aos mesmos perfis do Simply Light, para todas
as cores e para os dois modos (normal e invertido). Ele registra apenas os
estados `power=1..15`; `power=0` continua escuro.

O mesmo script tambem atualiza `item.properties`. Isso faz os blocos do addon
usarem o perfil colorido correto quando estiverem na mao principal ou secundária;
itens nao possuem o estado de redstone, portanto o shader aplica o perfil da cor
da variante.

No PowerShell, na raiz do projeto, execute:

```powershell
.\tools\apply-euphoria-colored-lighting.ps1
```

O alvo padrao e a copia de desenvolvimento em
`run/shaderpacks/ComplementaryReimagined_r5.8.1 + EuphoriaPatches_1.9.3`. Para
outra copia do mesmo shaderpack, informe o caminho:

```powershell
.\tools\apply-euphoria-colored-lighting.ps1 -ShaderPackPath 'C:\caminho\para\o\shaderpack'
```

O arquivo original e preservado ao lado de `block.properties` com o sufixo
`.simply-light-smart-backup`. Recarregue o shaderpack (ou reinicie o cliente)
depois de aplicar a integracao.

Outros shaderpacks podem usar tabelas diferentes; eles precisam de uma ponte
equivalente. O addon nao pode declarar essa cor por conta propria, porque esse
metadado pertence ao shaderpack ativo.
