package com.mobfusion;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.RangedAttackMob;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * The fused mob. Remembers its two parent mob types, inherits stats + abilities from them,
 * flies if a parent flies, shoots its abilities, and drops them as Ability Orbs.
 */
public class HybridMobEntity extends PathAwareEntity implements RangedAttackMob {
    private static final TrackedData<String> PARENT_A =
            DataTracker.registerData(HybridMobEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> PARENT_B =
            DataTracker.registerData(HybridMobEntity.class, TrackedDataHandlerRegistry.STRING);

    private final ServerBossBar bossBar =
            new ServerBossBar(Text.literal("Hybrid"), BossBar.Color.PINK, BossBar.Style.NOTCHED_10);
    private UUID ownerUuid;
    private boolean flyer;
    private boolean summoned;
    private List<FusionAbility> abilities = List.of();

    public HybridMobEntity(EntityType<? extends HybridMobEntity> type, World world) {
        super(type, world);
        setPersistent();
    }

    public static DefaultAttributeContainer.Builder createHybridAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(PARENT_A, "minecraft:pig");
        builder.add(PARENT_B, "minecraft:bee");
    }

    // ---------- parents / traits ----------
    public EntityType<?> getParentA() {
        return typeOf(dataTracker.get(PARENT_A));
    }

    public EntityType<?> getParentB() {
        return typeOf(dataTracker.get(PARENT_B));
    }

    private static EntityType<?> typeOf(String s) {
        Identifier id = Identifier.tryParse(s);
        return id == null ? EntityType.PIG : Registries.ENTITY_TYPE.get(id);
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID uuid) {
        this.ownerUuid = uuid;
    }

    /** Called by the Fusion Press with the two live mobs being merged. */
    public void fuseFrom(MobEntity a, MobEntity b) {
        dataTracker.set(PARENT_A, Registries.ENTITY_TYPE.getId(a.getType()).toString());
        dataTracker.set(PARENT_B, Registries.ENTITY_TYPE.getId(b.getType()).toString());
        refreshTraits();

        double hp = Math.min(400, a.getMaxHealth() + b.getMaxHealth());
        setAttr(EntityAttributes.GENERIC_MAX_HEALTH, hp);
        setHealth((float) hp);
        setAttr(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                Math.max(2.0, Math.max(attr(a, EntityAttributes.GENERIC_ATTACK_DAMAGE),
                        attr(b, EntityAttributes.GENERIC_ATTACK_DAMAGE))));
        setAttr(EntityAttributes.GENERIC_MOVEMENT_SPEED,
                Math.max(attr(a, EntityAttributes.GENERIC_MOVEMENT_SPEED),
                        attr(b, EntityAttributes.GENERIC_MOVEMENT_SPEED)));

        setCustomName(Text.literal(hybridName(a.getType(), b.getType())).formatted(Formatting.LIGHT_PURPLE));
        setCustomNameVisible(true);
    }

    private static double attr(LivingEntity e, RegistryEntry<EntityAttribute> a) {
        return e.getAttributes().hasAttribute(a) ? e.getAttributeValue(a) : 0;
    }

    private void setAttr(RegistryEntry<EntityAttribute> a, double v) {
        EntityAttributeInstance i = getAttributeInstance(a);
        if (i != null) i.setBaseValue(v);
    }

    /** pig + bee -> "Pee": first third of A + last two thirds of B. */
    public static String hybridName(EntityType<?> a, EntityType<?> b) {
        Relics.Def special = Relics.find(a, b);
        if (special != null) return special.name();
        String na = Registries.ENTITY_TYPE.getId(a).getPath().replace("_", "");
        String nb = Registries.ENTITY_TYPE.getId(b).getPath().replace("_", "");
        int first = Math.max(1, Math.round(na.length() / 3f));
        int last = Math.max(1, Math.round(nb.length() * 2 / 3f));
        first = Math.min(first, na.length());
        last = Math.min(last, nb.length());
        String s = na.substring(0, first) + nb.substring(nb.length() - last);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void refreshTraits() {
        Relics.Def special = Relics.find(getParentA(), getParentB());
        bossBar.setColor(special != null ? special.color() : BossBar.Color.PINK);
        abilities = FusionAbility.abilities(getParentA(), getParentB());
        flyer = FusionAbility.canFly(getParentA()) || FusionAbility.canFly(getParentB());
        if (flyer) {
            moveControl = new FlightMoveControl(this, 20, true);
            BirdNavigation nav = new BirdNavigation(this, getWorld());
            nav.setCanPathThroughDoors(false);
            nav.setCanSwim(false);
            navigation = nav;
        } else {
            moveControl = new MoveControl(this);
            navigation = new MobNavigation(this, getWorld());
            setNoGravity(false);
        }
    }

    public boolean isFlyer() {
        return flyer;
    }

    private boolean hasRanged() {
        return abilities.stream().anyMatch(FusionAbility::isRanged);
    }

    // ---------- AI ----------
    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(2, new ProjectileAttackGoal(this, 1.0, 40, 16f) {
            @Override
            public boolean canStart() {
                return hasRanged() && super.canStart();
            }
        });
        goalSelector.add(3, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            public boolean canStart() {
                return !hasRanged() && super.canStart();
            }
        });
        goalSelector.add(5, new FollowOwnerGoal());
        goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
        goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8f));
        goalSelector.add(8, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<PlayerEntity>(this, PlayerEntity.class, true, e -> ownerUuid == null));
        targetSelector.add(3, new ActiveTargetGoal<HostileEntity>(this, HostileEntity.class, true, e -> ownerUuid != null));
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        if (ownerUuid != null && target.getUuid().equals(ownerUuid)) return false;
        return super.canTarget(target);
    }

    @Override
    public void shootAt(LivingEntity target, float pullProgress) {
        List<FusionAbility> ranged = abilities.stream().filter(FusionAbility::isRanged).toList();
        if (ranged.isEmpty()) return;
        FusionAbility ab = ranged.get(random.nextInt(ranged.size()));
        int count = ab == FusionAbility.BONE ? 3 : 1;
        for (int i = 0; i < count; i++) {
            AbilityProjectileEntity p = new AbilityProjectileEntity(getWorld(), this, ab);
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            double dy = target.getBodyY(0.5) - p.getY();
            double flat = Math.sqrt(dx * dx + dz * dz);
            p.setVelocity(dx, dy + flat * 0.1, dz, 1.3f, count > 1 ? 12f : 6f);
            getWorld().spawnEntity(p);
        }
    }

    // ---------- summoned mount + riding ----------
    public boolean isSummoned() {
        return summoned;
    }

    /** Used by the Pee Saddle: makes a ready-to-ride hybrid out of two entity types (no live mobs needed). */
    public void summonFrom(EntityType<?> a, EntityType<?> b) {
        summoned = true;
        dataTracker.set(PARENT_A, Registries.ENTITY_TYPE.getId(a).toString());
        dataTracker.set(PARENT_B, Registries.ENTITY_TYPE.getId(b).toString());
        refreshTraits();
        setAttr(EntityAttributes.GENERIC_MAX_HEALTH, 40);
        setHealth(40f);
        setAttr(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3);
        setAttr(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3);
        setCustomName(Text.literal(hybridName(a, b)).formatted(Formatting.LIGHT_PURPLE));
        setCustomNameVisible(true);
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (ownerUuid != null && player.getUuid().equals(ownerUuid)
                && player.getStackInHand(hand).isEmpty() && !player.shouldCancelInteraction()) {
            if (!getWorld().isClient) player.startRiding(this);
            return ActionResult.success(getWorld().isClient);
        }
        return super.interactMob(player, hand);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof PlayerEntity p ? p : null;
    }

    @Override
    public void travel(Vec3d input) {
        if (isAlive() && getControllingPassenger() instanceof PlayerEntity rider) {
            setYaw(rider.getYaw());
            prevYaw = getYaw();
            setPitch(rider.getPitch() * 0.5f);
            setRotation(getYaw(), getPitch());
            bodyYaw = getYaw();
            headYaw = bodyYaw;
            float side = rider.sidewaysSpeed * 0.5f;
            float fwd = rider.forwardSpeed;
            if (fwd <= 0) fwd *= 0.25f;
            if (isLogicalSideForUpdatingMovement()) {
                if (flyer) {
                    // fly where the rider looks
                    float vertical = -MathHelper.sin(rider.getPitch() * 0.017453292f) * Math.max(fwd, 0f);
                    updateVelocity(0.06f, new Vec3d(side, vertical, fwd));
                    move(MovementType.SELF, getVelocity());
                    setVelocity(getVelocity().multiply(0.85));
                } else {
                    setMovementSpeed((float) getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
                    super.travel(new Vec3d(side, input.y, fwd));
                }
            } else {
                setVelocity(Vec3d.ZERO);
            }
            return;
        }
        super.travel(input);
    }

    private class FollowOwnerGoal extends Goal {
        private PlayerEntity owner;

        FollowOwnerGoal() {
            setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (ownerUuid == null) return false;
            owner = getWorld().getPlayerByUuid(ownerUuid);
            return owner != null && squaredDistanceTo(owner) > 64;
        }

        @Override
        public boolean shouldContinue() {
            return owner != null && squaredDistanceTo(owner) > 9;
        }

        @Override
        public void tick() {
            navigation.startMovingTo(owner, 1.2);
            if (squaredDistanceTo(owner) > 1600) {
                refreshPositionAndAngles(owner.getX(), owner.getY(), owner.getZ(), getYaw(), getPitch());
            }
        }
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return !flyer && super.handleFallDamage(fallDistance, damageMultiplier, source);
    }

    // ---------- boss bar ----------
    @Override
    public void setCustomName(Text name) {
        super.setCustomName(name);
        bossBar.setName(getDisplayName());
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        bossBar.setPercent(getHealth() / getMaxHealth());
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        if (ownerUuid == null) bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        bossBar.clearPlayers();
    }

    // ---------- drops: abilities become items ----------
    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        for (FusionAbility ab : abilities) {
            ItemStack orb = FusionAbility.orb(ab);
            orb.setCount(1 + random.nextInt(3));
            dropStack(orb);
        }
        // unique relic for special pairs (Pee Saddle, Zombie Gauntlet, ...)
        Relics.Def special = Relics.find(getParentA(), getParentB());
        if (special != null && !summoned) dropStack(new ItemStack(special.relic().get()));
    }

    // ---------- save / load ----------
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("ParentA", dataTracker.get(PARENT_A));
        nbt.putString("ParentB", dataTracker.get(PARENT_B));
        if (ownerUuid != null) nbt.putUuid("Owner", ownerUuid);
        nbt.putBoolean("Summoned", summoned);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("ParentA")) dataTracker.set(PARENT_A, nbt.getString("ParentA"));
        if (nbt.contains("ParentB")) dataTracker.set(PARENT_B, nbt.getString("ParentB"));
        ownerUuid = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
        summoned = nbt.getBoolean("Summoned");
        refreshTraits();
        if (hasCustomName()) bossBar.setName(getDisplayName());
    }
}
