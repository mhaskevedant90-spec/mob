package com.mobfusion;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Pokeball-style item. Throw it at a hybrid to capture it; throw a full one to release the hybrid as your ally. */
public class FusionBallItem extends Item {
    public FusionBallItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW,
                SoundCategory.NEUTRAL, 0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!world.isClient) {
            FusionBallEntity ball = new FusionBallEntity(world, player);
            ball.setItem(stack.copyWithCount(1));
            ball.setVelocity(player, player.getPitch(), player.getYaw(), 0f, 1.5f, 1f);
            world.spawnEntity(ball);
        }
        // the ball always comes back to your inventory when it lands (see FusionBallEntity)
        stack.decrement(1);
        return TypedActionResult.success(stack, world.isClient);
    }

    // ---- stored hybrid helpers ----
    public static NbtCompound getStored(ItemStack s) {
        NbtComponent c = s.get(DataComponentTypes.CUSTOM_DATA);
        if (c == null) return null;
        NbtCompound n = c.copyNbt();
        return n.contains("Hybrid") ? n.getCompound("Hybrid") : null;
    }

    public static void store(ItemStack s, NbtCompound hybrid, Text hybridName) {
        NbtCompound n = new NbtCompound();
        n.put("Hybrid", hybrid);
        s.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(n));
        s.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(1));
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(hybridName.getString() + " Ball")
                .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.LIGHT_PURPLE)));
    }

    public static void clear(ItemStack s) {
        s.remove(DataComponentTypes.CUSTOM_DATA);
        s.remove(DataComponentTypes.CUSTOM_MODEL_DATA);
        s.remove(DataComponentTypes.CUSTOM_NAME);
    }
}
