package com.mobfusion.client;

import com.mobfusion.MobFusion;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;

/** Two flapping wings, bolted onto the back of any flying hybrid (like Pee's bee wings). */
public class WingsModel {
    public static final EntityModelLayer LAYER = new EntityModelLayer(MobFusion.id("wings"), "main");
    private final ModelPart left;
    private final ModelPart right;

    public WingsModel(ModelPart root) {
        this.left = root.getChild("left_wing");
        this.right = root.getChild("right_wing");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("left_wing", ModelPartBuilder.create().uv(0, 0).cuboid(0f, -0.5f, -4f, 10f, 1f, 8f), ModelTransform.NONE);
        root.addChild("right_wing", ModelPartBuilder.create().uv(0, 16).cuboid(-10f, -0.5f, -4f, 10f, 1f, 8f), ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 32);
    }

    public void render(MatrixStack m, VertexConsumer vc, int light, int overlay, float pivotY, float flap) {
        left.setPivot(2f, pivotY, 0f);
        right.setPivot(-2f, pivotY, 0f);
        left.roll = flap;
        right.roll = -flap;
        left.render(m, vc, light, overlay, -1);
        right.render(m, vc, light, overlay, -1);
    }
}
