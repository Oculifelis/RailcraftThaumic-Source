package com.arcanelocomotion.entity;

import com.arcanelocomotion.RTDataMaps;
import com.arcanelocomotion.RTRegistry;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A rideable cart of eldritch make. On eldritch rail it runs far past a normal cart's top speed, but each new rail it
 * crosses there may leave a little temporary warp on its rider. Fireproof, and creatures cannot break it.
 */
public class EldritchCart extends net.minecraft.world.entity.vehicle.minecart.Minecart {
    private @Nullable BlockPos lastRail;

    public EldritchCart(EntityType<?> type, Level level) {
        super(type, level);
    }

    public static EldritchCart create(ItemStack stack, Level level, double x, double y, double z) {
        EldritchCart cart = new EldritchCart(RTRegistry.ELDRITCH_CART.get(), level);
        cart.setInitialPos(x, y, z);
        return cart;
    }

    private @Nullable BlockState eldritchRailUnder() {
        BlockState state = level().getBlockState(getCurrentBlockPosOrRailBelow());
        return state.is(RTRegistry.ELDRITCH_RAIL.get()) ? state : null;
    }

    @Override
    protected double getMaxSpeed(ServerLevel level) {
        BlockState rail = eldritchRailUnder();
        return rail == null ? super.getMaxSpeed(level) : RTDataMaps.rail(rail.getBlock()).maxSpeed();
    }

    /** The top speed here, as the cart's own movement uses it (for tests and tooltips). */
    public double topSpeed(ServerLevel level) {
        return getMaxSpeed(level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        BlockState rail = eldritchRailUnder();
        if (rail == null) {
            lastRail = null;
            return;
        }
        BlockPos here = getCurrentBlockPosOrRailBelow();
        if (!here.equals(lastRail)) {
            lastRail = here;
            double chance = RTDataMaps.rail(rail.getBlock()).warpChance();
            for (Entity passenger : getPassengers()) {
                if (passenger instanceof ServerPlayer player && server.getRandom().nextDouble() < chance) {
                    applyWarp(player);
                }
            }
        }
    }

    public static void applyWarp(ServerPlayer player) {
        WarpHelper.addWarp(player, 1, WarpType.TEMPORARY);
    }

    /** Its floor is a slab of runed abyssal brick, so it reads as eldritch make without its own entity texture. */
    @Override
    public BlockState getDefaultDisplayBlockState() {
        return RTRegistry.DECO.get(3).slab().get().defaultBlockState();
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 2;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.IS_FIRE) || source.getEntity() instanceof Mob) {
            return false;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    protected Item getDropItem() {
        return RTRegistry.ELDRITCH_CART_ITEM.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(getDropItem());
    }
}
