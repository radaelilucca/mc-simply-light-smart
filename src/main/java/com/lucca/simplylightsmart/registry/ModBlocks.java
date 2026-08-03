package com.lucca.simplylightsmart.registry;

import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.block.DimmerFixture;
import com.lucca.simplylightsmart.block.DimmerLightBulb;
import com.lucca.simplylightsmart.block.DimmerThinLamp;
import com.lucca.simplylightsmart.smart.SmartSwitchBlock;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registrations for blocks added by this addon. */
public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SimplyLightSmart.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SimplyLightSmart.MOD_ID);
    private static final List<DeferredBlock<? extends Block>> DIMMER_BLOCKS = new ArrayList<>();
    private static final List<DeferredItem<BlockItem>> INVERTED_DIMMER_ITEMS = new ArrayList<>();

    private static final Map<DyeColor, DeferredItem<BlockItem>> DIMMER_BULBS = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, DeferredItem<BlockItem>> INVERTED_DIMMER_BULBS = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, DeferredItem<BlockItem>> DIMMER_SLABS = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, DeferredItem<BlockItem>> INVERTED_DIMMER_SLABS = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, DeferredItem<BlockItem>> DIMMER_PANELS = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, DeferredItem<BlockItem>> INVERTED_DIMMER_PANELS = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, DeferredItem<BlockItem>> DIMMER_FIXTURES = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, DeferredItem<BlockItem>> INVERTED_DIMMER_FIXTURES = new EnumMap<>(DyeColor.class);

    public static final DeferredBlock<SmartSwitchBlock> SMART_SWITCH = BLOCKS.register("smart_switch", () ->
            new SmartSwitchBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> SMART_SWITCH_ITEM = ITEMS.registerSimpleBlockItem("smart_switch", SMART_SWITCH);

    static {
        for (DyeColor color : DyeColor.values()) {
            registerBulb(color, false, DIMMER_BULBS);
            registerBulb(color, true, INVERTED_DIMMER_BULBS);
            registerBlock("dimmer_illuminant_slab", color, false, DIMMER_SLABS, (dyeColor, inverted) -> new DimmerThinLamp(8, dyeColor, inverted));
            registerBlock("dimmer_illuminant_slab", color, true, INVERTED_DIMMER_SLABS, (dyeColor, inverted) -> new DimmerThinLamp(8, dyeColor, inverted));
            registerBlock("dimmer_illuminant_panel", color, false, DIMMER_PANELS, (dyeColor, inverted) -> new DimmerThinLamp(4, dyeColor, inverted));
            registerBlock("dimmer_illuminant_panel", color, true, INVERTED_DIMMER_PANELS, (dyeColor, inverted) -> new DimmerThinLamp(4, dyeColor, inverted));
            registerBlock("dimmer_fixture", color, false, DIMMER_FIXTURES, DimmerFixture::new);
            registerBlock("dimmer_fixture", color, true, INVERTED_DIMMER_FIXTURES, DimmerFixture::new);
        }
    }

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    public static ItemStack creativeTabIcon() {
        return DIMMER_BULBS.get(DyeColor.WHITE).get().getDefaultInstance();
    }

    public static void addDimmerItems(CreativeModeTab.Output output) {
        addItems(output, DIMMER_BULBS);
        addItems(output, INVERTED_DIMMER_BULBS);
        addItems(output, DIMMER_SLABS);
        addItems(output, INVERTED_DIMMER_SLABS);
        addItems(output, DIMMER_PANELS);
        addItems(output, INVERTED_DIMMER_PANELS);
        addItems(output, DIMMER_FIXTURES);
        addItems(output, INVERTED_DIMMER_FIXTURES);
        output.accept(SMART_SWITCH_ITEM.get());
    }

    public static Block[] dimmerBlocks() {
        return DIMMER_BLOCKS.stream().map(DeferredBlock::get).toArray(Block[]::new);
    }

    public static Item[] invertedDimmerItems() {
        return INVERTED_DIMMER_ITEMS.stream().map(DeferredItem::get).toArray(Item[]::new);
    }

    private static void addItems(CreativeModeTab.Output output, Map<DyeColor, DeferredItem<BlockItem>> items) {
        items.values().forEach(item -> output.accept(item.get()));
    }

    private static void registerBulb(DyeColor color, boolean inverted, Map<DyeColor, DeferredItem<BlockItem>> itemRegistry) {
        registerBlock("dimmer_lightbulb", color, inverted, itemRegistry, DimmerLightBulb::new);
    }

    private static void registerBlock(String baseId, DyeColor color, boolean inverted, Map<DyeColor, DeferredItem<BlockItem>> itemRegistry, BiFunction<DyeColor, Boolean, ? extends Block> factory) {
        String id = variantId(baseId, color, inverted);
        DeferredBlock<? extends Block> block = BLOCKS.register(id, () -> factory.apply(color, inverted));
        DIMMER_BLOCKS.add(block);
        DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(id, block);
        itemRegistry.put(color, item);
        if (inverted) {
            INVERTED_DIMMER_ITEMS.add(item);
        }
    }

    private static String variantId(String baseId, DyeColor color, boolean inverted) {
        String colorSuffix = color == DyeColor.WHITE ? "" : "_" + color.getName();
        return baseId + colorSuffix + (inverted ? "_inverted" : "");
    }
}
