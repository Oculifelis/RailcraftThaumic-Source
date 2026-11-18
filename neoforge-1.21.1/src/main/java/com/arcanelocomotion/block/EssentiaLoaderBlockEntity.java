package com.arcanelocomotion.block;

import com.arcanelocomotion.RTRegistry;
import com.arcanelocomotion.entity.EssentiaTankCart;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Essentia endpoint for the tank cart standing on the rail above. Load mode: draws from neighbouring tubes and jars
 * (suction {@link #SUCTION}) into the cart. Unload mode: offers the cart's essentia and pushes it into neighbours that
 * want it. Every face but the top connects.
 */
public class EssentiaLoaderBlockEntity extends BlockEntity implements IEssentiaTransport {
    public static final int SUCTION = 128;
    public static final int RATE = 4;

    public EssentiaLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(RTRegistry.ESSENTIA_LOADER_BE.get(), pos, state);
    }

    private boolean unloading() {
        return (level == null ? getBlockState() : level.getBlockState(worldPosition)).getValue(EssentiaLoaderBlock.UNLOAD);
    }

    private @Nullable EssentiaTankCart cart() {
        if (level == null) {
            return null;
        }
        for (AbstractMinecart c : TrackBed.cartsAbove(level, worldPosition)) {
            if (c instanceof EssentiaTankCart tank) {
                return tank;
            }
        }
        return null;
    }

    void serverTick() {
        EssentiaTankCart cart = cart();
        if (cart == null || level == null) {
            return;
        }
        boolean unload = unloading();
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) {
                continue;
            }
            Direction face = dir.getOpposite();
            IEssentiaTransport other = EssentiaAccess.transport(level, worldPosition.relative(dir), face);
            if (other == null || !other.isConnectable(face)) {
                continue;
            }
            if (unload) {
                Holder<IAspect> aspect = cart.aspect();
                if (aspect == null) {
                    return;
                }
                if (other.canInputFrom(face) && other.getSuctionAmount(face) > 0) {
                    Holder<IAspect> wanted = other.getSuctionType(face);
                    if (wanted == null || wanted.equals(aspect)) {
                        int offered = cart.drain(aspect, RATE, true);
                        int taken = other.addEssentia(aspect, offered, face);
                        cart.drain(aspect, taken, false);
                    }
                }
            } else if (other.canOutputTo(face) && other.getEssentiaAmount(face) > 0 && other.getSuctionAmount(face) < SUCTION) {
                Holder<IAspect> type = other.getEssentiaType(face);
                if (type == null) {
                    continue;
                }
                int room = cart.fill(type, Math.min(RATE, other.getEssentiaAmount(face)), true);
                if (room > 0) {
                    int taken = other.takeEssentia(type, room, face);
                    cart.fill(type, taken, false);
                }
            }
        }
    }

    // --- IEssentiaTransport: tubes see the cart above through this block. ---

    @Override
    public boolean isConnectable(Direction face) {
        return face != Direction.UP;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face != Direction.UP && !unloading();
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return face != Direction.UP && unloading();
    }

    @Override
    public void setSuction(Holder<IAspect> aspect, int amount) {
    }

    @Override
    public @Nullable Holder<IAspect> getSuctionType(Direction face) {
        EssentiaTankCart cart = cart();
        return cart == null || unloading() ? null : cart.aspect();
    }

    @Override
    public int getSuctionAmount(Direction face) {
        EssentiaTankCart cart = cart();
        return cart != null && !unloading() && cart.amount() < EssentiaTankCart.CAPACITY ? SUCTION : 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public @Nullable Holder<IAspect> getEssentiaType(Direction face) {
        EssentiaTankCart cart = cart();
        return cart == null || !unloading() ? null : cart.aspect();
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        EssentiaTankCart cart = cart();
        return cart == null || !unloading() ? 0 : cart.amount();
    }

    @Override
    public int addEssentia(Holder<IAspect> aspect, int amount, Direction face) {
        EssentiaTankCart cart = cart();
        return cart == null || unloading() || face == Direction.UP ? 0 : cart.fill(aspect, amount, false);
    }

    @Override
    public int takeEssentia(Holder<IAspect> aspect, int amount, Direction face) {
        EssentiaTankCart cart = cart();
        return cart == null || !unloading() || face == Direction.UP ? 0 : cart.drain(aspect, amount, false);
    }
}
