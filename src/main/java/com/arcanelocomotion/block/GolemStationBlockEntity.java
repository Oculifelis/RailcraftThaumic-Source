package com.arcanelocomotion.block;

import com.arcanelocomotion.RTRegistry;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Stops a cart that rolls over it and holds it while golems (or hoppers, pipes, anything that uses an item handler on
 * this block) fill or empty it. Load mode lets the cart go once its inventory is full, unload mode once it is empty,
 * and a redstone signal lets it go at once. The cart leaves with the speed it came in with.
 */
public class GolemStationBlockEntity extends BlockEntity {
    private @Nullable UUID held;
    private @Nullable UUID released;
    private Vec3 motion = Vec3.ZERO;
    private final Proxy proxy = new Proxy();

    public GolemStationBlockEntity(BlockPos pos, BlockState state) {
        super(RTRegistry.GOLEM_STATION_BE.get(), pos, state);
    }

    public ResourceHandler<ItemResource> handler() {
        return proxy;
    }

    public @Nullable AbstractMinecart heldCart() {
        if (level == null || held == null) {
            return null;
        }
        for (AbstractMinecart c : TrackBed.cartsAbove(level, worldPosition)) {
            if (c.getUUID().equals(held)) {
                return c;
            }
        }
        return null;
    }

    private static @Nullable ResourceHandler<ItemResource> items(@Nullable AbstractMinecart cart) {
        if (cart == null) {
            return null;
        }
        ResourceHandler<ItemResource> h = cart.getCapability(Capabilities.Item.ENTITY_AUTOMATION, Direction.DOWN);
        return h != null ? h : cart.getCapability(Capabilities.Item.ENTITY, null);
    }

    void serverTick() {
        if (level == null) {
            return;
        }
        var carts = TrackBed.cartsAbove(level, worldPosition);
        if (released != null && carts.stream().noneMatch(c -> c.getUUID().equals(released))) {
            released = null;
        }
        AbstractMinecart cart = heldCart();
        if (cart == null) {
            held = null;
            for (AbstractMinecart c : carts) {
                if (!c.getUUID().equals(released) && items(c) != null) {
                    held = c.getUUID();
                    motion = c.getDeltaMovement();
                    cart = c;
                    setChanged();
                    break;
                }
            }
            if (cart == null) {
                return;
            }
        }
        ResourceHandler<ItemResource> items = items(cart);
        boolean unload = level.getBlockState(worldPosition).getValue(GolemStationBlock.UNLOAD);
        if (items == null || level.hasNeighborSignal(worldPosition) || (unload ? isEmpty(items) : isFull(items))) {
            release(cart);
            return;
        }
        cart.setDeltaMovement(Vec3.ZERO);
        cart.setPos(worldPosition.getX() + 0.5, cart.getY(), worldPosition.getZ() + 0.5);
    }

    private void release(AbstractMinecart cart) {
        cart.setDeltaMovement(motion);
        released = cart.getUUID();
        held = null;
        motion = Vec3.ZERO;
        setChanged();
    }

    static boolean isEmpty(ResourceHandler<ItemResource> h) {
        for (int i = 0; i < h.size(); i++) {
            if (!h.getResource(i).isEmpty() && h.getAmountAsLong(i) > 0) {
                return false;
            }
        }
        return true;
    }

    static boolean isFull(ResourceHandler<ItemResource> h) {
        for (int i = 0; i < h.size(); i++) {
            ItemResource r = h.getResource(i);
            if (r.isEmpty()) {
                return false;
            }
            long cap = Math.min(h.getCapacityAsLong(i, r), r.getMaxStackSize());
            if (h.getAmountAsLong(i) < cap) {
                return false;
            }
        }
        return h.size() > 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (held != null) {
            output.putString("held", held.toString());
            output.putDouble("mx", motion.x);
            output.putDouble("my", motion.y);
            output.putDouble("mz", motion.z);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        held = input.getString("held").map(s -> {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).orElse(null);
        motion = new Vec3(input.getDoubleOr("mx", 0), input.getDoubleOr("my", 0), input.getDoubleOr("mz", 0));
    }

    /** The held cart's inventory, seen through this block. Empty when no cart is held. */
    private final class Proxy implements ResourceHandler<ItemResource> {
        private @Nullable ResourceHandler<ItemResource> target() {
            return items(heldCart());
        }

        @Override
        public int size() {
            ResourceHandler<ItemResource> t = target();
            return t == null ? 0 : t.size();
        }

        @Override
        public ItemResource getResource(int index) {
            ResourceHandler<ItemResource> t = target();
            return t == null || index >= t.size() ? ItemResource.EMPTY : t.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            ResourceHandler<ItemResource> t = target();
            return t == null || index >= t.size() ? 0 : t.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            ResourceHandler<ItemResource> t = target();
            return t == null || index >= t.size() ? 0 : t.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            ResourceHandler<ItemResource> t = target();
            return t != null && index < t.size() && t.isValid(index, resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
            ResourceHandler<ItemResource> t = target();
            return t == null || index >= t.size() ? 0 : t.insert(index, resource, amount, tx);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
            ResourceHandler<ItemResource> t = target();
            return t == null || index >= t.size() ? 0 : t.extract(index, resource, amount, tx);
        }
    }
}
