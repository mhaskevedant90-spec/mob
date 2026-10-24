package com.mobfusion.client;

import com.mobfusion.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

public class MobFusionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(WingsModel.LAYER, WingsModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.HYBRID, HybridMobRenderer::new);
        EntityRendererRegistry.register(ModEntities.ABILITY_PROJECTILE, FlyingItemEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.FUSION_BALL, FlyingItemEntityRenderer::new);
    }
}
