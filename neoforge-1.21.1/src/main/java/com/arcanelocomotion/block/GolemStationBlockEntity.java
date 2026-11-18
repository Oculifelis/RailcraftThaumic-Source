package com.arcanelocomotion.block;

import com.arcanelocomotion.RTRegistry;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

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

    public IItemHandler handler() {
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

    private static @Nullable IItemHandler items(@Nullable AbstractMinecart cart) {
        if (cart == null) {
            return null;
        }
        IItemHandler h = cart.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, Direction.DOWN);
        return h != null ? h : cart.getCapability(Capabilities.ItemHandler.ENTITY, null);
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
        IItemHandler items = items(cart);
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

    static boolean isEmpty(IItemHandler h) {
        for (int i = 0; i < h.getSlots(); i++) {
            if (!h.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    static boolean isFull(IItemHandler h) {
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack stack = h.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < Math.min(h.getSlotLimit(i), stack.getMaxStackSize())) return false;
        }
        return h.getSlots() > 0;
    }

    @Override
    protected void saveAdditional(CompoundTag output, HolderLookup.Provider registries) {
        super.saveAdditional(output, registries);
        if (held != null) {
            output.putUUID("held", held);
            output.putDouble("mx", motion.x);
            output.putDouble("my", motion.y);
            output.putDouble("mz", motion.z);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag input, HolderLookup.Provider registries) {
        super.loadAdditional(input, registries);
        held = input.hasUUID("held") ? input.getUUID("held") : null;
        motion = new Vec3(input.getDouble("mx"), input.getDouble("my"), input.getDouble("mz"));
    }

    /** The held cart's inventory, seen through this block. Empty when no cart is held. */
    private final class Proxy implements IItemHandler {
        private @Nullable IItemHandler target(int slot) {
            IItemHandler h = items(heldCart());
            return h != null && slot >= 0 && slot < h.getSlots() ? h : null;
        }

        @Override
        public int getSlots() {
            IItemHandler h = items(heldCart());
            return h == null ? 0 : h.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            IItemHandler h = target(slot);
            return h == null ? ItemStack.EMPTY : h.getStackInSlot(slot);
        }

        @Override
        public int getSlotLimit(int slot) {
            IItemHandler h = target(slot);
            return h == null ? 0 : h.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            IItemHandler h = target(slot);
            return h != null && h.isItemValid(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            IItemHandler h = target(slot);
            return h == null ? stack : h.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            IItemHandler h = target(slot);
            return h == null ? ItemStack.EMPTY : h.extractItem(slot, amount, simulate);
        }
    }
}
