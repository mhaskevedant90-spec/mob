package com.mobfusion;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** Poop / honey / fireball / etc. shot by hybrids and by ability orbs. */
public class AbilityProjectileEntity extends ThrownItemEntity {
    private FusionAbility ability = FusionAbility.POOP;

    public AbilityProjectileEntity(EntityType<? extends AbilityProjectileEntity> type, World world) {
        super(type, world);
    }

    public AbilityProjectileEntity(World world, LivingEntity owner, FusionAbility ability) {
        super(ModEntities.ABILITY_PROJECTILE, owner, world);
        this.ability = ability;
        setItem(new ItemStack(ability.projectileItem()));
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.POOP;
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);
        Entity target = hit.getEntity();
        Entity owner = getOwner();
        if (target == owner) return;
        if (owner instanceof HybridMobEntity h && target.getUuid().equals(h.getOwnerUuid())) return;
        ability.onHit(this, target);
    }

    @Override
    protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        World w = getWorld();
        if (!w.isClient) {
            if (ability == FusionAbility.EXPLODE) {
                w.createExplosion(this, getX(), getY(), getZ(), 1.6f, World.ExplosionSourceType.NONE);
            }
            if (ability == FusionAbility.POOP && random.nextFloat() < 0.5f) {
                dropStack(new ItemStack(ModItems.POOP));
            }
            discard();
        }
    }
}
