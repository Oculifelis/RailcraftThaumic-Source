package com.arcanelocomotion.block;

import com.arcanelocomotion.RTRegistry;
import com.arcanelocomotion.entity.EssentiaTankCart;
import com.leclowndu93150.thaumaturge.api.aspect.AspectIndexAccess;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/** Holds the tuned aspect and checks the cart above every few ticks. */
public class RuneSignalBlockEntity extends BlockEntity {
    public static final int INTERVAL = 4;
    private @Nullable ResourceKey<IAspect> aspect;

    public RuneSignalBlockEntity(BlockPos pos, BlockState state) {
        super(RTRegistry.RUNE_SIGNAL_BE.get(), pos, state);
    }

    public @Nullable ResourceKey<IAspect> aspect() {
        return aspect;
    }

    public void tune(@Nullable ResourceKey<IAspect> key) {
        aspect = key;
        setChanged();
    }

    public void clear() {
        tune(null);
    }

    /** Takes the aspect from an essentia container's contents, a label's filter, or the item's strongest aspect. */
    public boolean tuneFrom(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        IEssentiaItemStorage storage = stack.getCapability(EssentiaCapabilities.ITEM_STORAGE);
        if (storage != null && !storage.contents().isEmpty()) {
            return tuneTo(storage.contents().sortedByAmount().getFirst().aspect());
        }
        ResourceKey<IAspect> filter = EssentiaAccess.aspectFilter(stack);
        if (filter != null) {
            tune(filter);
            return true;
        }
        AspectList list = AspectIndexAccess.of(stack);
        return !list.isEmpty() && tuneTo(list.sortedByAmount().getFirst().aspect());
    }

    private boolean tuneTo(Holder<IAspect> holder) {
        return holder.unwrapKey().map(k -> {
            tune(k);
            return true;
        }).orElse(false);
    }

    public Component status() {
        return aspect == null ? RuneSignalBlock.unset()
                : Component.translatable("message.arcanelocomotion.rune_tuned", aspect.location().getPath());
    }

    void serverTick() {
        if (level == null || level.getGameTime() % INTERVAL != 0) {
            return;
        }
        boolean on = false;
        if (aspect != null) {
            for (AbstractMinecart cart : TrackBed.cartsAbove(level, worldPosition)) {
                if (carries(cart, aspect)) {
                    on = true;
                    break;
                }
            }
        }
        BlockState state = level.getBlockState(worldPosition);
        if (state.getValue(RuneSignalBlock.POWERED) != on) {
            level.setBlock(worldPosition, state.setValue(RuneSignalBlock.POWERED, on), Block.UPDATE_ALL);
        }
    }

    /** Tank cart essentia, or any item in the cart's inventory whose aspects include {@code key}. */
    public static boolean carries(AbstractMinecart cart, ResourceKey<IAspect> key) {
        if (cart instanceof EssentiaTankCart tank) {
            Holder<IAspect> held = tank.aspect();
            return held != null && held.is(key);
        }
        IItemHandler items = cart.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, Direction.UP);
        if (items == null) {
            items = cart.getCapability(Capabilities.ItemHandler.ENTITY, null);
        }
        if (items == null) {
            return false;
        }
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack res = items.getStackInSlot(i);
            if (res.isEmpty()) {
                continue;
            }
            for (Holder<IAspect> h : AspectIndexAccess.of(res).aspects()) {
                if (h.is(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag output, HolderLookup.Provider registries) {
        super.saveAdditional(output, registries);
        if (aspect != null) {
            output.putString("aspect", aspect.location().toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag input, HolderLookup.Provider registries) {
        super.loadAdditional(input, registries);
        aspect = java.util.Optional.ofNullable(ResourceLocation.tryParse(input.getString("aspect"))).map(id -> ResourceKey.create(IAspect.REGISTRY_KEY, id)).orElse(null);
    }
}
