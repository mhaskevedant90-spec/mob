package com.mobfusion;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class FusionBallEntity extends ThrownItemEntity {
    public FusionBallEntity(EntityType<? extends FusionBallEntity> type, World world) {
        super(type, world);
    }

    public FusionBallEntity(World world, LivingEntity owner) {
        super(ModEntities.FUSION_BALL, owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FUSION_BALL;
    }

    @Override
    protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        if (!(getWorld() instanceof ServerWorld sw)) return;

        ItemStack back = getStack().copyWithCount(1);
        NbtCompound stored = FusionBallItem.getStored(back);
        Entity owner = getOwner();
        Vec3d p = hit.getPos();

        if (stored != null) {
            // RELEASE the captured hybrid as an ally
            HybridMobEntity h = ModEntities.HYBRID.create(sw);
            if (h != null) {
                NbtCompound n = stored.copy();
                n.remove("UUID");
                h.readNbt(n);
                h.refreshPositionAndAngles(p.x, p.y, p.z, random.nextFloat() * 360f, 0f);
                if (owner != null) h.setOwnerUuid(owner.getUuid());
                sw.spawnEntity(h);
                sw.spawnParticles(ParticleTypes.POOF, p.x, p.y + 0.5, p.z, 25, 0.4, 0.4, 0.4, 0.05);
                FusionBallItem.clear(back);
            }
        } else if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof HybridMobEntity h && h.isAlive()) {
            // CAPTURE a hybrid
            NbtCompound n = new NbtCompound();
            if (h.saveSelfNbt(n)) {
                FusionBallItem.store(back, n, h.getDisplayName());
                sw.spawnParticles(ParticleTypes.END_ROD, h.getX(), h.getY() + 0.7, h.getZ(), 25, 0.4, 0.5, 0.4, 0.05);
                h.discard();
            }
        }

        // the ball always returns to the thrower
        if (owner instanceof PlayerEntity pl) pl.getInventory().offerOrDrop(back);
        else dropStack(back);
        discard();
    }
}
