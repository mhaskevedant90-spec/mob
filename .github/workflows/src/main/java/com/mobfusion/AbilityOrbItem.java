package com.mobfusion;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Right-click to use the ability stored in the orb (consumes one). */
public class AbilityOrbItem extends Item {
    public AbilityOrbItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        FusionAbility ab = FusionAbility.fromStack(stack);
        if (ab == null) return TypedActionResult.pass(stack);

        if (!world.isClient) {
            if (ab == FusionAbility.WINGS) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 240));
                player.setVelocity(player.getVelocity().add(0, 1.1, 0));
                player.velocityModified = true;
            } else {
                AbilityProjectileEntity p = new AbilityProjectileEntity(world, player, ab);
                p.setVelocity(player, player.getPitch(), player.getYaw(), 0f, 1.6f, 1f);
                world.spawnEntity(p);
            }
        }
        player.getItemCooldownManager().set(this, 10);
        if (!player.isCreative()) stack.decrement(1);
        return TypedActionResult.success(stack, world.isClient);
    }
}
