# Simply Light - Smart!

> **Early-stage development:** This mod is under active development. Features,
> balance, recipes, and compatibility may change. Before the first release,
> use it at your own risk.

**Simply Light - Smart!** is an addon for
[Simply Light](https://www.curseforge.com/minecraft/mc-mods/simply-light) that
lets its lights respond smoothly to redstone. It also adds the **Smart Switch**
and **Smart Linker** for controlling groups of lights wireless from one place.

## Redstone-controlled lights

Place and use the original Simply Light lights as usual. When a supported light
receives redstone, it changes brightness instead of only switching between on
and off.

For most lights, no redstone means full brightness. As the signal gets stronger,
the light gets dimmer; a strength of 15 turns it off. This works with every
color of:

- Bulbs
- Illuminant Slabs and Panels
- Fixtures
- Illuminant Rods
- Illuminant Edges and Top Edges
- Illuminant Columns

Illuminant Blocks keep their familiar two versions: one starts off and gets
brighter as the signal grows, while the other starts on and gets dimmer.

![A Simply Light lamp dimming as its redstone signal increases](https://github.com/radaelilucca/mc-simply-light-smart/blob/main/docs/gallery/AE%20-%20dimmer.gif?raw=true)

## **Smart Switch** and **Smart Linker**

The **Smart Switch** lets one redstone signal control many lights at once. It
sends its current strength to every linked light, up to 64 lights within 128
blocks.

To set it up:

1. Place a **Smart Switch** and connect it to redstone.
2. Right-click each Simply Light block with the **Smart Linker** to add it to
   the selection. Right-click it again to remove it.
3. Hold Shift and right-click the **Smart Switch** to apply the selection.
4. Keep using the **Smart Linker** to add more lights, then Shift-right-click
   the Switch again whenever you want to update its list.
5. Hold Shift and left-click in the air to clear the **Smart Linker** selection.

Linked lights follow the Switch and ignore their own nearby redstone. A light
can be linked to only one **Smart Switch** at a time. The **Smart Linker** keeps
your selection until you clear it and highlights selected lights with a green
outline.

![A Smart Switch relaying its redstone signal to linked lights](https://github.com/radaelilucca/mc-simply-light-smart/blob/main/docs/gallery/AE%20-%20switch.gif?raw=true)

## Crafting

Craft a **Smart Switch** with iron, redstone, a comparator, an ender pearl, and
a repeater. Craft a **Smart Linker** with a redstone torch, an iron ingot, and
an amethyst shard.

## Shader support

Lights dim and turn off correctly out of the box with Solas, BSL, and the base
versions of Complementary Reimagined and Complementary Unbound.

For Complementary Reimagined and Unbound, the addon can also prepare a version
for use with Euphoria Patches. When the game starts, it looks for an
Euphoria-generated shader folder and creates an optional `SLSmart` version
beside it. The original packs are never changed.

<img src="https://github.com/radaelilucca/mc-simply-light-smart/blob/main/docs/gallery/AE%20-%20shader.gif?raw=true" alt="Simply Light blocks with shader support" width="240">

## Compatibility

- Minecraft 1.21.1
- NeoForge 21.1.234
- [Simply Light 1.5.3+1.21.1-b4](https://www.curseforge.com/minecraft/mc-mods/simply-light)
- Optional [Sable](https://github.com/ryanhcode/sable) 2.0.3 support for
  sublevel-aware linking
- Shader patcher tested with Complementary Reimagined and Unbound r5.8.1 with
  Euphoria Patches 1.9.3, and r5.9 with Euphoria Patches 1.10.0

The addon does not require Create or Sable to run.

## License and credits

Simply Light - Smart! is distributed under the [MIT License](../LICENSE).
Simply Light remains its own project and is required separately.

### LLM-assisted development

This project was developed with assistance from large language model (LLM)
tools, including code implementation, review, documentation, and iteration.

### AI-assisted visual assets

The **Smart Switch** and **Smart Linker** textures were created with AI
assistance and then processed and integrated into the mod. They represent the
actual in-game assets shipped by this project.
