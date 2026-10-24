package com.mobfusion;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModBlocks {
    /** Level 1 press: cobblestone look. */
    public static final Block FUSION_PRESS = register("fusion_press",
            new FusionPressBlock(AbstractBlock.Settings.copy(Blocks.STONE).strength(3.5f).nonOpaque(), 1));
    /** Level 2 press: diamond look, can fuse big/hard mobs. */
    public static final Block DIAMOND_FUSION_PRESS = register("diamond_fusion_press",
            new FusionPressBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5f).nonOpaque(), 2));

    private static Block register(String name, Block block) {
        Registry.register(Registries.BLOCK, MobFusion.id(name), block);
        Registry.register(Registries.ITEM, MobFusion.id(name), new BlockItem(block, new Item.Settings()));
        return block;
    }

    public static void register() {}
}
