package com.mobfusion;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class MobFusion implements ModInitializer {
    public static final String MOD_ID = "mobfusion";

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModBlocks.register();
        ModItems.register();
        ModEntities.register();
    }
}
