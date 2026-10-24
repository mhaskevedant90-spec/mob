package com.mobfusion;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

import java.util.List;
import java.util.function.Supplier;

/**
 * Special hybrids. Only these pairs get a custom name, boss-bar colour, fixed ability set
 * and a UNIQUE relic item that drops when you kill them. Every other pair drops plain Ability Orbs.
 * To add a new one: add a line to ALL, register an item in ModItems, give it a behaviour in RelicItem.
 */
public final class Relics {
    public record Def(String a, String b, String name, BossBar.Color color,
                      List<FusionAbility> abilities, Supplier<Item> relic) {}

    private static final List<Def> ALL = List.of(
            new Def("pig", "bee", "Pee", BossBar.Color.PINK,
                    List.of(FusionAbility.POOP, FusionAbility.POISON, FusionAbility.WINGS), () -> ModItems.PEE_SADDLE),
            new Def("zombie", "skeleton", "Zombeton", BossBar.Color.GREEN,
                    List.of(FusionAbility.BONE, FusionAbility.ROT), () -> ModItems.ZOMBIE_GAUNTLET),
            new Def("ghast", "iron_golem", "Ghastlem", BossBar.Color.RED,
                    List.of(FusionAbility.FIREBALL, FusionAbility.SMASH, FusionAbility.WINGS), () -> ModItems.GHAST_CANNON)
    );

    private Relics() {}

    private static String key(EntityType<?> t) {
        return Registries.ENTITY_TYPE.getId(t).getPath();
    }

    /** Order of the two parents does not matter. */
    public static Def find(EntityType<?> x, EntityType<?> y) {
        String a = key(x), b = key(y);
        for (Def d : ALL) {
            if ((d.a().equals(a) && d.b().equals(b)) || (d.a().equals(b) && d.b().equals(a))) return d;
        }
        return null;
    }
}
