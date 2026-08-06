# Simply Light - Smart!

> **Early-stage development:** Simply Light - Smart! is under active development.
> Features, balance, recipes, and compatibility may change as the mod evolves.

**Simply Light - Smart!** is a NeoForge addon for
[Simply Light](https://www.curseforge.com/minecraft/mc-mods/simply-light) that
adds redstone-dimmable lighting and a wireless-style **Smart Switch** control
system.

## Features

### Dimmable Simply Light blocks

Every color of these Simply Light families has a Smart equivalent:

- Bulbs
- Illuminant Blocks
- Illuminant Slabs
- Illuminant Panels
- Fixtures

Normal dimmers start at full brightness and become dimmer as redstone power
increases. Inverted dimmers start off and become brighter as redstone power
increases. All variants support the full 0–15 redstone range.

![Normal Smart dimmer becoming dimmer as its redstone signal increases](https://github.com/radaelilucca/mc-simply-light-smart/blob/main/docs/gallery/AE%20-%20dimmer.gif?raw=true)

![Inverted Smart dimmer becoming brighter as its redstone signal increases](https://github.com/radaelilucca/mc-simply-light-smart/blob/main/docs/gallery/AE%20-%20inverted-dimmer.gif?raw=true)

Existing Simply Light blocks are never changed.

### **Smart Switch** and **Smart Linker**

The **Smart Switch** receives a redstone signal and relays that exact strength
to linked dimmers. A **Smart Switch** supports up to 64 dimmers within a
128-block physical range.

Use the **Smart Linker** to configure it:

1. Place a **Smart Switch** and connect redstone.
2. Right-click each dimmer with the **Smart Linker** to add or remove it from the
   selection.
3. Hold Shift and right-click the **Smart Switch** to replace its linked dimmers.
   The **Smart Linker** selection remains available for additional changes.
4. Hold Shift and left-click in the air to clear the **Smart Linker** selection manually.

Linked dimmers ignore their local redstone input and are controlled exclusively
by their **Smart Switch**. A dimmer can belong to only one **Smart Switch** at a
time.

The **Smart Linker** shows the number of selected dimmers and highlights them
with a simple green outline.

![Smart Switch relaying its redstone signal to linked dimmers](https://github.com/radaelilucca/mc-simply-light-smart/blob/main/docs/gallery/AE%20-%20switch.gif?raw=true)

![Smart Switch blinking as its linked dimmers update](https://github.com/radaelilucca/mc-simply-light-smart/blob/main/docs/gallery/AE%20-%20switch-blink.gif?raw=true)

## Crafting

- Convert supported Simply Light blocks into normal dimmers with eight matching
  blocks around redstone dust.
- Convert eight matching blocks into inverted dimmers with a redstone torch in
  the center. This works with either normal Smart dimmers or the original
  Simply Light blocks.
- Craft a **Smart Switch** with iron, redstone, a comparator, an ender pearl, and
  a repeater.
- Craft a **Smart Linker** with a redstone torch, iron ingot, and amethyst shard.

## Shader support

Shader support is currently being implemented. The initial plan is to support:

- BSL Shaders
- Complementary Shaders - Reimagined
- Complementary Shaders - Unbound

<img src="https://github.com/radaelilucca/mc-simply-light-smart/blob/main/docs/gallery/AE%20-%20shader.gif?raw=true" alt="Smart dimmers with shader support" width="240">

## Compatibility

- Minecraft 1.21.1
- NeoForge 21.1.234
- [Simply Light 1.5.3+1.21.1-b4](https://www.curseforge.com/minecraft/mc-mods/simply-light)
- Optional [Sable](https://github.com/ryanhcode/sable) 2.0.3 support for
  sublevel-aware linking

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
