package com.lucca.simplylightstoggle.registry;

import com.lucca.simplylightstoggle.SimplyLightsToggle;
import com.lucca.simplylightstoggle.block.DimmerFixture;
import com.lucca.simplylightstoggle.block.DimmerLightBulb;
import com.lucca.simplylightstoggle.block.DimmerThinLamp;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registrations for blocks added by this addon. */
public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SimplyLightsToggle.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SimplyLightsToggle.MOD_ID);
    private static final List<DeferredBlock<? extends Block>> DIMMER_BLOCKS = new ArrayList<>();

    private static final Map<DyeColor, DeferredItem<BlockItem>> DIMMER_BULBS = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, DeferredItem<BlockItem>> INVERTED_DIMMER_BULBS = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, DeferredItem<BlockItem>> DIMMER_SLABS = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, DeferredItem<BlockItem>> INVERTED_DIMMER_SLABS = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, DeferredItem<BlockItem>> DIMMER_PANELS = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, DeferredItem<BlockItem>> INVERTED_DIMMER_PANELS = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, DeferredItem<BlockItem>> DIMMER_FIXTURES = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, DeferredItem<BlockItem>> INVERTED_DIMMER_FIXTURES = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            registerBulb(color, false, DIMMER_BULBS);
            registerBulb(color, true, INVERTED_DIMMER_BULBS);
            registerBlock("dimmer_illuminant_slab", color, false, DIMMER_SLABS, (dyeColor, inverted) -> new DimmerThinLamp(8, dyeColor, inverted));
            registerBlock("dimmer_illuminant_slab", color, true, INVERTED_DIMMER_SLABS, (dyeColor, inverted) -> new DimmerThinLamp(8, dyeColor, inverted));
            registerBlock("dimmer_illuminant_panel", color, false, DIMMER_PANELS, (dyeColor, inverted) -> new DimmerThinLamp(4, dyeColor, inverted));
            registerBlock("dimmer_illuminant_panel", color, true, INVERTED_DIMMER_PANELS, (dyeColor, inverted) -> new DimmerThinLamp(4, dyeColor, inverted));
            registerBlock("dimmer_wall_lamp", color, false, DIMMER_FIXTURES, DimmerFixture::new);
            registerBlock("dimmer_wall_lamp", color, true, INVERTED_DIMMER_FIXTURES, DimmerFixture::new);
        }
    }

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    public static void addBulbsToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        DIMMER_BULBS.values().forEach(event::accept);
        INVERTED_DIMMER_BULBS.values().forEach(event::accept);
        DIMMER_SLABS.values().forEach(event::accept);
        INVERTED_DIMMER_SLABS.values().forEach(event::accept);
        DIMMER_PANELS.values().forEach(event::accept);
        INVERTED_DIMMER_PANELS.values().forEach(event::accept);
        DIMMER_FIXTURES.values().forEach(event::accept);
        INVERTED_DIMMER_FIXTURES.values().forEach(event::accept);
    }

    public static Block[] dimmerBlocks() {
        return DIMMER_BLOCKS.stream().map(DeferredBlock::get).toArray(Block[]::new);
    }

    private static void registerBulb(DyeColor color, boolean inverted, Map<DyeColor, DeferredItem<BlockItem>> itemRegistry) {
        registerBlock("dimmer_lightbulb", color, inverted, itemRegistry, DimmerLightBulb::new);
    }

    private static void registerBlock(String baseId, DyeColor color, boolean inverted, Map<DyeColor, DeferredItem<BlockItem>> itemRegistry, BiFunction<DyeColor, Boolean, ? extends Block> factory) {
        String id = variantId(baseId, color, inverted);
        DeferredBlock<? extends Block> block = BLOCKS.register(id, () -> factory.apply(color, inverted));
        DIMMER_BLOCKS.add(block);
        itemRegistry.put(color, ITEMS.registerSimpleBlockItem(id, block));
    }

    private static String variantId(String baseId, DyeColor color, boolean inverted) {
        String colorSuffix = color == DyeColor.WHITE ? "" : "_" + color.getName();
        return baseId + colorSuffix + (inverted ? "_inverted" : "");
    }
}
