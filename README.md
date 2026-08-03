# Simply Light - Smart!

Addon NeoForge para Minecraft 1.21.1 que acrescenta versões dimerizáveis das
famílias Bulbs, Illuminant Slabs, Illuminant Panels e Fixtures do
[Simply Light](https://www.curseforge.com/minecraft/mc-mods/simply-light).

Cada cor possui uma variante normal e uma invertida. A intensidade de emissão é
proporcional ao sinal de redstone recebido.

## Requisitos

- Minecraft 1.21.1
- NeoForge 21.1.234
- Simply Light 1.5.3 ou compatível
- Java 21 para desenvolvimento

O JAR do Simply Light utilizado no desenvolvimento está fixado em
`libs/simplylight-1.5.3+1.21.1-b4.jar`.

## Desenvolvimento

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

O JAR gerado fica em `build/libs/`.

## Receitas

Cada variante normal e criada em uma mesa de trabalho com oito luzes
correspondentes do Simply Light ao redor de uma redstone. A receita devolve
oito blocos dimerizaveis da mesma familia e cor. As variantes invertidas usam
o mesmo padrao 3x3, com uma redstone torch no centro: podem partir de oito
blocos dimerizaveis normais ou diretamente de oito luzes do Simply Light.

## Luz colorida com shaders

Shaders atribuem perfis de luz colorida pelo ID do bloco. Para que
Complementary Reimagined + Euphoria Patches use o mesmo perfil do Simply Light
nos blocos novos, aplique a integração descrita em
[docs/shader-support.md](docs/shader-support.md):

```powershell
.\tools\apply-euphoria-colored-lighting.ps1
```

Outros shaderpacks podem exigir uma integração equivalente.

## Licença

Este addon é distribuído sob a [licença MIT](LICENSE).
