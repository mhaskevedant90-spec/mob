package com.mobfusion;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.*;

/** Abilities a hybrid can inherit from its parents (and that drop as items when it dies). */
public enum FusionAbility {
    WINGS("Wings"), POOP("Poop"), HONEY("Honey"), ARROW("Arrow"),
    FIREBALL("Fireball"), WEB("Web"), EXPLODE("Explosion"),
    SONIC("Sonic Boom"), SMASH("Smash"), ENDER("Ender"), EGG("Egg"), FROST("Frost"),
    WITHER("Wither"), POISON("Witch Brew"), ROT("Rot"), TRIDENT("Trident"),
    SPIT("Spit"), LASER("Laser"), BULLET("Shulker Bullet"), BONE("Bone Blast");

    public final String displayName;

    FusionAbility(String displayName) {
        this.displayName = displayName;
    }

    public boolean isRanged() {
        return this != WINGS;
    }

    public Item projectileItem() {
        return switch (this) {
            case POOP -> ModItems.POOP;
            case HONEY -> Items.HONEY_BOTTLE;
            case ARROW -> Items.ARROW;
            case FIREBALL -> Items.FIRE_CHARGE;
            case WEB -> Items.COBWEB;
            case EXPLODE -> Items.TNT;
            case WINGS -> Items.FEATHER;
            case SONIC -> Items.ECHO_SHARD;
            case SMASH -> Items.IRON_INGOT;
            case ENDER -> Items.ENDER_PEARL;
            case EGG -> Items.EGG;
            case FROST -> Items.SNOWBALL;
            case WITHER -> Items.WITHER_SKELETON_SKULL;
            case POISON -> Items.FERMENTED_SPIDER_EYE;
            case ROT -> Items.ROTTEN_FLESH;
            case TRIDENT -> Items.TRIDENT;
            case SPIT -> Items.SLIME_BALL;
            case LASER -> Items.PRISMARINE_SHARD;
            case BULLET -> Items.SHULKER_SHELL;
            case BONE -> Items.BONE;
        };
    }

    // ---- which mob gives which ability (add more here!) ----
    private static final Map<String, FusionAbility> BY_MOB = new HashMap<>();
    private static final Set<String> FLYERS = Set.of(
            "bee", "bat", "parrot", "allay", "vex", "ghast", "phantom", "blaze", "ender_dragon");

    static {
        BY_MOB.put("pig", POOP);
        BY_MOB.put("bee", HONEY);
        BY_MOB.put("skeleton", ARROW);
        BY_MOB.put("stray", ARROW);
        BY_MOB.put("pillager", ARROW);
        BY_MOB.put("blaze", FIREBALL);
        BY_MOB.put("ghast", FIREBALL);
        BY_MOB.put("spider", WEB);
        BY_MOB.put("cave_spider", WEB);
        BY_MOB.put("creeper", EXPLODE);
        BY_MOB.put("warden", SONIC);
        BY_MOB.put("iron_golem", SMASH);
        BY_MOB.put("ravager", SMASH);
        BY_MOB.put("enderman", ENDER);
        BY_MOB.put("chicken", EGG);
        BY_MOB.put("snow_golem", FROST);
        BY_MOB.put("wither_skeleton", WITHER);
        BY_MOB.put("witch", POISON);
        BY_MOB.put("zombie", ROT);
        BY_MOB.put("husk", ROT);
        BY_MOB.put("zombie_villager", ROT);
        BY_MOB.put("drowned", TRIDENT);
        BY_MOB.put("llama", SPIT);
        BY_MOB.put("trader_llama", SPIT);
        BY_MOB.put("guardian", LASER);
        BY_MOB.put("elder_guardian", LASER);
        BY_MOB.put("shulker", BULLET);
    }

    private static String key(EntityType<?> t) {
        return Registries.ENTITY_TYPE.getId(t).getPath();
    }

    public static boolean canFly(EntityType<?> t) {
        return FLYERS.contains(key(t));
    }

    public static List<FusionAbility> abilities(EntityType<?> a, EntityType<?> b) {
        Relics.Def special = Relics.find(a, b);
        if (special != null) return new ArrayList<>(special.abilities());
        LinkedHashSet<FusionAbility> out = new LinkedHashSet<>();
        for (EntityType<?> t : List.of(a, b)) {
            FusionAbility ab = BY_MOB.get(key(t));
            if (ab != null) out.add(ab);
            if (canFly(t)) out.add(WINGS);
        }
        return new ArrayList<>(out);
    }

    // ---- ability items ----
    public static ItemStack orb(FusionAbility ab) {
        ItemStack s = new ItemStack(ModItems.ABILITY_ORB);
        NbtCompound n = new NbtCompound();
        n.putString("ability", ab.name());
        s.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(n));
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(ab.displayName + " Orb")
                .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GOLD)));
        return s;
    }

    public static FusionAbility fromStack(ItemStack s) {
        NbtComponent c = s.get(DataComponentTypes.CUSTOM_DATA);
        if (c == null) return null;
        NbtCompound n = c.copyNbt();
        if (!n.contains("ability")) return null;
        try {
            return valueOf(n.getString("ability"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Called when a projectile of this ability hits an entity. */
    public void onHit(AbilityProjectileEntity proj, Entity target) {
        World w = proj.getWorld();
        if (w.isClient) return;
        DamageSource src = proj.getDamageSources().thrown(proj, proj.getOwner());
        float dmg = switch (this) {
            case POOP -> 2f;
            case HONEY, WEB -> 1f;
            case ARROW, FIREBALL, BONE -> 4f;
            case SONIC -> 8f;
            case SMASH, TRIDENT -> 6f;
            case LASER, WITHER, ENDER -> 3f;
            case SPIT, EGG, FROST, ROT, POISON, BULLET -> 2f;
            default -> 0f;
        };
        if (dmg > 0) target.damage(src, dmg);
        if (target instanceof LivingEntity le) {
            double kx = proj.getX() - target.getX();
            double kz = proj.getZ() - target.getZ();
            switch (this) {
                case POOP -> le.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120));
                case HONEY -> le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 2));
                case FIREBALL -> le.setOnFireFor(5);
                case WEB -> {
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1));
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 80));
                }
                case SONIC -> {
                    le.takeKnockback(1.2, kx, kz);
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 100));
                }
                case SMASH -> le.takeKnockback(1.5, kx, kz);
                case ENDER -> le.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 60));
                case FROST -> {
                    le.setFrozenTicks(200);
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 1));
                }
                case WITHER -> le.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100));
                case POISON -> {
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100));
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100));
                }
                case ROT -> le.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 200));
                case SPIT -> {
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60));
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 80));
                }
                case BULLET -> le.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 100));
                default -> {}
            }
        }
    }
}
