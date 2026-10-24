package com.mobfusion;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModEntities {
    public static final EntityType<HybridMobEntity> HYBRID = Registry.register(Registries.ENTITY_TYPE,
            MobFusion.id("hybrid_mob"),
            EntityType.Builder.create(HybridMobEntity::new, SpawnGroup.MISC)
                    .dimensions(0.9f, 1.3f).maxTrackingRange(10).build("hybrid_mob"));

    public static final EntityType<AbilityProjectileEntity> ABILITY_PROJECTILE = Registry.register(Registries.ENTITY_TYPE,
            MobFusion.id("ability_projectile"),
            EntityType.Builder.<AbilityProjectileEntity>create(AbilityProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f).maxTrackingRange(4).trackingTickInterval(10).build("ability_projectile"));

    public static final EntityType<FusionBallEntity> FUSION_BALL = Registry.register(Registries.ENTITY_TYPE,
            MobFusion.id("fusion_ball"),
            EntityType.Builder.<FusionBallEntity>create(FusionBallEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f).maxTrackingRange(4).trackingTickInterval(10).build("fusion_ball"));

    public static void register() {
        FabricDefaultAttributeRegistry.register(HYBRID, HybridMobEntity.createHybridAttributes());
    }
}
