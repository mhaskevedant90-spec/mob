package com.mobfusion.client;

import com.mobfusion.FusionAbility;
import com.mobfusion.HybridMobEntity;
import com.mobfusion.MobFusion;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws a hybrid by re-using the parents' own vanilla renderers.
 *  - one parent flies  -> the ground parent is drawn + flapping wings (pig + bee = Pee)
 *  - nobody flies      -> parent A full size with a small parent B riding on top
 */
public class HybridMobRenderer extends EntityRenderer<HybridMobEntity> {
    private static final Identifier WINGS_TEX = MobFusion.id("textures/entity/wings.png");
    private final EntityRenderDispatcher dispatcher;
    private final WingsModel wings;
    private final Map<HybridMobEntity, Entity[]> dummies = new WeakHashMap<>();

    public HybridMobRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.dispatcher = ctx.getRenderDispatcher();
        this.wings = new WingsModel(ctx.getPart(WingsModel.LAYER));
        this.shadowRadius = 0.5f;
    }

    @Override
    public Identifier getTexture(HybridMobEntity entity) {
        return WINGS_TEX;
    }

    @Override
    public void render(HybridMobEntity e, float yaw, float tickDelta, MatrixStack m, VertexConsumerProvider vc, int light) {
        EntityType<?> a = e.getParentA();
        EntityType<?> b = e.getParentB();
        boolean aFly = FusionAbility.canFly(a);
        boolean bFly = FusionAbility.canFly(b);

        int baseIdx = (aFly && !bFly) ? 1 : 0;
        Entity[] pair = dummies.computeIfAbsent(e, k -> new Entity[]{a.create(e.getWorld()), b.create(e.getWorld())});
        Entity base = pair[baseIdx];
        Entity extra = pair[1 - baseIdx];
        if (base == null) return;
        boolean baseFlies = baseIdx == 0 ? aFly : bFly;

        draw(base, e, yaw, tickDelta, m, vc, light, 1f, 0f);

        if (e.isFlyer() && !baseFlies) {
            drawWings(e, base, tickDelta, m, vc, light);
        } else if (extra != null && !e.isFlyer()) {
            draw(extra, e, yaw, tickDelta, m, vc, light, 0.55f, base.getHeight() * 0.95f);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void draw(Entity d, HybridMobEntity e, float yaw, float tickDelta, MatrixStack m,
                      VertexConsumerProvider vc, int light, float scale, float yOff) {
        boolean newTick = d.age != e.age;
        d.age = e.age;
        d.setPosition(e.getPos());
        d.setYaw(e.getYaw());
        d.setPitch(e.getPitch());
        d.prevYaw = e.prevYaw;
        d.prevPitch = e.prevPitch;
        if (d instanceof LivingEntity ld) {
            ld.bodyYaw = e.bodyYaw;
            ld.prevBodyYaw = e.prevBodyYaw;
            ld.headYaw = e.headYaw;
            ld.prevHeadYaw = e.prevHeadYaw;
            if (newTick) ld.limbAnimator.updateLimbs(e.limbAnimator.getSpeed(), 1f);
        }
        m.push();
        m.translate(0, yOff, 0);
        m.scale(scale, scale, scale);
        try {
            EntityRenderer r = dispatcher.getRenderer(d);
            r.render(d, yaw, tickDelta, m, vc, light);
        } catch (Exception ignored) {
            // a parent whose renderer can't draw as a stand-in is simply skipped
        }
        m.pop();
    }

    private void drawWings(HybridMobEntity e, Entity base, float tickDelta, MatrixStack m, VertexConsumerProvider vc, int light) {
        m.push();
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, e.prevBodyYaw, e.bodyYaw);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - bodyYaw));
        m.scale(-1f, -1f, 1f);
        m.translate(0f, -1.501f, 0f);
        float pivotY = 24f - base.getHeight() * 16f * 0.9f;
        float flap = MathHelper.sin((e.age + tickDelta) * 1.9f) * 0.7f;
        wings.render(m, vc.getBuffer(RenderLayer.getEntityTranslucent(WINGS_TEX)), light, OverlayTexture.DEFAULT_UV, pivotY, flap);
        m.pop();
    }
}
