package com.mobfusion;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Place two presses facing each other (up to 8 blocks apart), put exactly 2 mobs between them
 * and power a press with a button. The pistons squash the mobs together into a hybrid.
 * Tier 1 = normal mobs. Tier 2 (diamond) = mobs with 40+ HP. Both presses must be tier 2 for big mobs.
 */
public class FusionPressBlock extends Block {
    public static final MapCodec<FusionPressBlock> CODEC = createCodec(s -> new FusionPressBlock(s, 1));
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final BooleanProperty PRESSING = BooleanProperty.of("pressing");

    private static final Set<EntityType<?>> BLOCKED = Set.<EntityType<?>>of(EntityType.ENDER_DRAGON, EntityType.WITHER);

    private final int tier;

    public FusionPressBlock(Settings settings, int tier) {
        super(settings);
        this.tier = tier;
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(PRESSING, false));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, PRESSING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // the press faces the player who placed it, i.e. towards the gap where the mobs go
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite()).with(PRESSING, false);
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (world.isClient || state.get(PRESSING)) return;
        if (world.isReceivingRedstonePower(pos)) tryStart(state, (ServerWorld) world, pos);
    }

    private record Match(BlockPos other, FusionPressBlock otherBlock, List<MobEntity> mobs, Box gap) {}

    private static boolean canFuse(MobEntity e) {
        return e.isAlive() && !(e instanceof HybridMobEntity) && !BLOCKED.contains(e.getType());
    }

    private Match find(ServerWorld world, BlockPos pos, BlockState state) {
        Direction dir = state.get(FACING);
        for (int i = 2; i <= 9; i++) {
            BlockPos other = pos.offset(dir, i);
            BlockState os = world.getBlockState(other);
            if (os.getBlock() instanceof FusionPressBlock fb && os.get(FACING) == dir.getOpposite()) {
                Box gap = new Box(pos.offset(dir, 1)).union(new Box(other.offset(dir.getOpposite(), 1)));
                List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, gap, FusionPressBlock::canFuse);
                return new Match(other, fb, mobs, gap);
            }
        }
        return null;
    }

    private void tryStart(BlockState state, ServerWorld world, BlockPos pos) {
        Match m = find(world, pos, state);
        if (m == null) {
            tell(world, pos, "Put a second Fusion Press facing this one (max 8 blocks apart).");
            return;
        }
        if (m.mobs.size() != 2) {
            tell(world, pos, "Put exactly 2 mobs between the presses.");
            return;
        }
        int required = 1;
        for (MobEntity e : m.mobs) if (e.getMaxHealth() >= 40) required = 2;
        if (Math.min(tier, m.otherBlock.tier) < required) {
            tell(world, pos, "These mobs are too big - use two Diamond Fusion Presses.");
            return;
        }
        world.setBlockState(pos, state.with(PRESSING, true));
        BlockState os = world.getBlockState(m.other);
        world.setBlockState(m.other, os.with(PRESSING, true));
        world.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 1f, 0.8f);
        world.scheduleBlockTick(pos, this, 12);
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!state.get(PRESSING)) return;
        Match m = find(world, pos, state);
        world.setBlockState(pos, state.with(PRESSING, false));
        if (m == null) return;
        BlockState os = world.getBlockState(m.other);
        if (os.getBlock() == m.otherBlock) world.setBlockState(m.other, os.with(PRESSING, false));
        world.playSound(null, pos, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 1f, 0.8f);

        if (m.mobs.size() == 2) fuse(world, pos, m);
    }

    private static void fuse(ServerWorld world, BlockPos pos, Match m) {
        List<MobEntity> mobs = new ArrayList<>(m.mobs);
        mobs.sort(Comparator.comparingDouble(Entity::getX).thenComparingDouble(Entity::getZ));
        Vec3d c = m.gap.getCenter();
        HybridMobEntity h = ModEntities.HYBRID.create(world);
        if (h == null) return;
        h.refreshPositionAndAngles(c.x, pos.getY(), c.z, world.random.nextFloat() * 360f, 0f);
        h.fuseFrom(mobs.get(0), mobs.get(1));
        mobs.forEach(Entity::discard);
        world.spawnEntity(h);
        world.spawnParticles(ParticleTypes.POOF, c.x, pos.getY() + 0.5, c.z, 40, 0.4, 0.4, 0.4, 0.06);
        world.playSound(null, pos, SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.BLOCKS, 1f, 1.2f);
    }

    private static void tell(ServerWorld w, BlockPos p, String msg) {
        for (ServerPlayerEntity pl : w.getPlayers()) {
            if (pl.squaredDistanceTo(Vec3d.ofCenter(p)) < 144) pl.sendMessage(Text.literal(msg), true);
        }
    }
}
