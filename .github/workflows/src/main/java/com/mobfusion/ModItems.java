package com.mobfusion;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModItems {
    public static final Item POOP = register("poop", new Item(new Item.Settings()));
    public static final Item ABILITY_ORB = register("ability_orb", new AbilityOrbItem(new Item.Settings()));
    public static final Item FUSION_BALL = register("fusion_ball", new FusionBallItem(new Item.Settings().maxCount(16)));

    public static final Item PEE_SADDLE = register("pee_saddle", new RelicItem(new Item.Settings().maxCount(1), RelicItem.Kind.MOUNT));
    public static final Item ZOMBIE_GAUNTLET = register("zombie_gauntlet", new RelicItem(new Item.Settings().maxCount(1), RelicItem.Kind.BONE_BLAST));
    public static final Item GHAST_CANNON = register("ghast_cannon", new RelicItem(new Item.Settings().maxCount(1), RelicItem.Kind.FIRE_BARRAGE));

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, MobFusion.id(name), item);
    }

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(e -> {
            e.add(ModBlocks.FUSION_PRESS);
            e.add(ModBlocks.DIAMOND_FUSION_PRESS);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(e -> {
            e.add(FUSION_BALL);
            e.add(PEE_SADDLE);
            e.add(ZOMBIE_GAUNTLET);
            e.add(GHAST_CANNON);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(e -> {
            e.add(POOP);
            for (FusionAbility a : FusionAbility.values()) e.add(FusionAbility.orb(a));
        });
    }
}
