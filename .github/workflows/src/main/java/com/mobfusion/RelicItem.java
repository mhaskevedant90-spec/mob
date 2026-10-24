package com.mobfusion;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** Unique drops of special hybrids (see Relics). */
public class RelicItem extends Item {
    public enum Kind { MOUNT, BONE_BLAST, FIRE_BARRAGE }

    private final Kind kind;

    public RelicItem(Settings settings, Kind kind) {
        super(settings);
        this.kind = kind;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        switch (kind) {
            case BONE_BLAST -> {
                if (!world.isClient) volley(world, player, FusionAbility.BONE, 2, 7f, 1.8f);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_SKELETON_SHOOT, SoundCategory.PLAYERS, 1f, 0.7f);
                player.getItemCooldownManager().set(this, 25);
            }
            case FIRE_BARRAGE -> {
                if (!world.isClient) volley(world, player, FusionAbility.FIREBALL, 1, 9f, 1.5f);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 0.8f);
                player.getItemCooldownManager().set(this, 40);
            }
            case MOUNT -> {
                if (!world.isClient) useMount((ServerWorld) world, player);
            }
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    /** Fires (2*half+1) projectiles in a fan. */
    private static void volley(World world, PlayerEntity player, FusionAbility ab, int half, float spread, float speed) {
        for (int i = -half; i <= half; i++) {
            AbilityProjectileEntity p = new AbilityProjectileEntity(world, player, ab);
            p.setVelocity(player, player.getPitch(), player.getYaw() + i * spread, 0f, speed, 0.5f);
            world.spawnEntity(p);
        }
    }

    /** Pee Saddle: summon / ride your flying Pee, shoot poop + poison while riding, sneak-use to dismiss. */
    private void useMount(ServerWorld world, PlayerEntity player) {
        List<HybridMobEntity> mine = world.getEntitiesByClass(HybridMobEntity.class,
                player.getBoundingBox().expand(32),
                h -> h.isSummoned() && player.getUuid().equals(h.getOwnerUuid()));

        if (player.getVehicle() instanceof HybridMobEntity h && h.isSummoned()) {
            // riding: shoot alternating poop / poison
            FusionAbility ab = (world.getTime() / 8) % 2 == 0 ? FusionAbility.POOP : FusionAbility.POISON;
            AbilityProjectileEntity p = new AbilityProjectileEntity(world, player, ab);
            p.setVelocity(player, player.getPitch(), player.getYaw(), 0f, 1.6f, 1f);
            world.spawnEntity(p);
            player.getItemCooldownManager().set(this, 8);
            return;
        }
        if (player.isSneaking()) {
            mine.forEach(HybridMobEntity::discard); // dismiss
            return;
        }
        HybridMobEntity pee = mine.isEmpty() ? null : mine.get(0);
        if (pee == null) {
            pee = ModEntities.HYBRID.create(world);
            if (pee == null) return;
            pee.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0f);
            pee.summonFrom(EntityType.PIG, EntityType.BEE);
            pee.setOwnerUuid(player.getUuid());
            world.spawnEntity(pee);
            world.spawnParticles(ParticleTypes.POOF, pee.getX(), pee.getY() + 0.5, pee.getZ(), 25, 0.4, 0.4, 0.4, 0.05);
        }
        player.startRiding(pee);
    }
}
