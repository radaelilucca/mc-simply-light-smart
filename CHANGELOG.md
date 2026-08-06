# Changelog

All notable changes to Simply Light - Smart! are documented in this file.

## Unreleased

## [0.3.1] - 2026-08-06

### Fixed

- Loading a Sable contraption with pre-existing Simply Light blocks now defers
  light and Smart Switch updates until chunk restoration has completed.

## [0.3.0] - 2026-08-06

### Added

- Original Simply Light Bulbs, Illuminant Blocks, Slabs, Panels, Fixtures,
  Rods, Edges, Top Edges, and Columns now support full redstone dimming and
  Smart Switch control without replacing the placed block.
- Smart Linker selections now target original Simply Light lights directly and
  retain their complete shapes, including Illuminant Columns.
- Smart Switch received a refreshed industrial texture.

### Changed

- The Smart Switch and Smart Linker now control the original Simply Light
  catalogue instead of separate addon copies of each light.
- Linker messages and the creative tab now describe lights rather than dimmer
  variants.

### Fixed

- Linked lights now restore safely as chunks load, without waiting on remote
  chunks or stalling world loading.

### Removed

- Removed the separate dimmer blocks, their item variants, conversion recipes,
  models, textures, translations, and creative-tab entries.

## [0.2.0] - 2026-08-05

### Added

- Redstone-dimmable Illuminant Blocks in every dye color, with normal and
  inverted variants that support Smart Switch linking.

### Changed

- Dimmer visuals now use seven brightness stages across every lighting family.
- Smart Linker selections now remain available after applying them to a Smart
  Switch, until cleared manually.

## [0.1.0] - Initial release

### Added

- Redstone-dimmable variants of Simply Light Bulbs, Illuminant Slabs,
  Illuminant Panels, and Fixtures in every dye color.
- Normal and inverted dimmer variants with 16 redstone-controlled brightness
  levels.
- Conversion recipes for all supported Simply Light lighting families:
  redstone creates normal dimmers, and redstone torches create inverted
  dimmers.
- A dedicated creative-mode tab for Simply Light - Smart! content.
- Smart Switch, a redstone relay that controls up to 64 linked dimmers within
  a 128-block physical range.
- Smart Linker for selecting dimmers, applying the selection to a Smart Switch,
  and clearing selections with Shift + left-click in the air.
- Green selection outlines, persistent links, safe unlinking, unloaded-chunk
  refreshes, and optional Sable sublevel-aware range handling.
- Smart Switch and Smart Linker crafting recipes, translations, models,
  textures, loot table, and mining tag.
### Compatibility

- Minecraft 1.21.1
- NeoForge 21.1.234
- Simply Light 1.5.3+1.21.1-b4
- Optional Sable 2.0.3 support
