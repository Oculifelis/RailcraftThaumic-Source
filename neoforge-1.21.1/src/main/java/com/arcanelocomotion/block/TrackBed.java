package com.arcanelocomotion.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** Railside machines sit under a rail and act on the cart standing on it. */
public final class TrackBed {
    private TrackBed() {
    }

    public static List<AbstractMinecart> cartsAbove(Level level, BlockPos bed) {
        return level.getEntitiesOfClass(AbstractMinecart.class, new AABB(bed.above()).deflate(0.05), AbstractMinecart::isAlive);
    }

    public static @Nullable AbstractMinecart cartAbove(Level level, BlockPos bed) {
        List<AbstractMinecart> carts = cartsAbove(level, bed);
        return carts.isEmpty() ? null : carts.getFirst();
    }
}
