package com.lucca.simplylightsmart.registry;

import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.smart.SmartSwitchBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registrations for the physical control blocks added by this addon. */
public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SimplyLightSmart.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SimplyLightSmart.MOD_ID);

    public static final DeferredBlock<SmartSwitchBlock> SMART_SWITCH = BLOCKS.register("smart_switch", () ->
            new SmartSwitchBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> SMART_SWITCH_ITEM = ITEMS.registerSimpleBlockItem("smart_switch", SMART_SWITCH);

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    public static ItemStack creativeTabIcon() {
        return SMART_SWITCH_ITEM.get().getDefaultInstance();
    }

    public static void addControlItems(CreativeModeTab.Output output) {
        output.accept(SMART_SWITCH_ITEM.get());
    }
}
