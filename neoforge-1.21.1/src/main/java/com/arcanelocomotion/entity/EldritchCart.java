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
import org.jetbrains.annotations.Nullable;

/**
 * A rideable cart of eldritch make. On eldritch rail it runs far past a normal cart's top speed, but each new rail it
 * crosses there may leave a little temporary warp on its rider. Fireproof, and creatures cannot break it.
 */
public class EldritchCart extends net.minecraft.world.entity.vehicle.Minecart {
    private @Nullable BlockPos lastRail;

    public EldritchCart(EntityType<?> type, Level level) {
        super(type, level);
    }

    public static EldritchCart create(ItemStack stack, double x, double y, double z, net.minecraft.server.level.ServerLevel level) {
        EldritchCart cart = new EldritchCart(RTRegistry.ELDRITCH_CART.get(), level);
        cart.setPos(x, y, z);
        return cart;
    }

    private BlockPos railPos() {
        BlockPos pos = blockPosition();
        return level().getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.RAILS) ? pos.below() : pos;
    }

    private @Nullable BlockState eldritchRailUnder() {
        BlockState state = level().getBlockState(railPos());
        return state.is(RTRegistry.ELDRITCH_RAIL.get()) ? state : null;
    }

    @Override
    public double getMaxSpeedWithRail() {
        BlockState rail = eldritchRailUnder();
        return rail == null ? super.getMaxSpeedWithRail() : RTDataMaps.rail(rail.getBlock()).maxSpeed();
    }

    /** The top speed here, as the cart's own movement uses it (for tests and tooltips). */
    public double topSpeed(ServerLevel level) {
        return getMaxSpeedWithRail();
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
        BlockPos here = railPos();
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
    public boolean hurt(DamageSource source, float damage) {
        if (source.is(DamageTypeTags.IS_FIRE) || source.getEntity() instanceof Mob) {
            return false;
        }
        return super.hurt(source, damage);
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
