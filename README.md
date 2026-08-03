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

## Smart Switch e Linker

O **Smart Switch** recebe redstone e repassa o mesmo nível de sinal para até
64 dimmers vinculados, em um alcance físico de 128 blocos. Enquanto vinculado,
o dimmer ignora seu redstone local. A integração com sublevels Sable é
opcional; sem Sable, o comportamento vanilla permanece igual.

### Uso

1. Coloque um Smart Switch e conecte a fonte de redstone.
2. Segure o Smart Linker e clique com o botão direito nos dimmers desejados.
   Um novo clique remove o alvo da seleção.
   Use Shift + clique esquerdo no ar para limpar toda a seleção atual.
3. Com Shift pressionado, clique com o botão direito no Smart Switch para
   substituir a lista de vínculos. Aplicar uma seleção vazia remove todos os
   vínculos.

O Linker mostra a quantidade selecionada e destaca os alvos com um contorno
verde. Um dimmer não pode pertencer a dois switches.

### Fabricação

- **Smart Switch:** ferro e redstone como estrutura, com comparator,
  ender pearl e repeater (`I C I / R E R / I P I`).
- **Smart Linker:** redstone torch, lingote de ferro e fragmento de ametista
  em diagonal (`  R / I / A  `).

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
