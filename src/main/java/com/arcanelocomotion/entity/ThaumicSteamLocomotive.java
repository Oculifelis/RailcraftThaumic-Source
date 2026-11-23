package com.arcanelocomotion.entity;

import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaItemStorage;
import com.leclowndu93150.thaumaturge.api.essentia.ItemEssentiaTransferResult;
import com.arcanelocomotion.RTRegistry;
import com.arcanelocomotion.item.BoilerEssentia;
import mods.railcraft.world.entity.vehicle.locomotive.SteamLocomotive;
import mods.railcraft.world.level.material.FuelProvider;
import mods.railcraft.world.level.material.steam.SolidFuelProvider;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/**
 * Railcraft Reborn steam locomotive whose firebox burns Ignis essentia before it touches its solid fuel, and whose
 * Aqua essentia condenses into boiler water. Essentia is poured in from any Thaumaturge jar or phial by using it on
 * the locomotive. Railcraft 1.7.10 sketched this as its "magic" steam locomotive but never registered it.
 */
public class ThaumicSteamLocomotive extends SteamLocomotive {
    public static final int ESSENTIA_CAPACITY = 256;
    /** Firebox heat from one point of Ignis; a piece of coal gives 1600. */
    public static final int HEAT_PER_IGNIS = 400;
    /** Boiler water from one point of Aqua. */
    public static final int WATER_PER_AQUA = 250;
    private static final int SOLID_FUEL_SLOT = 3;

    private static final EntityDataAccessor<Integer> IGNIS = SynchedEntityData.defineId(ThaumicSteamLocomotive.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AQUA = SynchedEntityData.defineId(ThaumicSteamLocomotive.class, EntityDataSerializers.INT);

    public ThaumicSteamLocomotive(EntityType<?> type, Level level) {
        super(type, level);
        boiler().setFuelProvider(new EssentiaFuel(new SolidFuelProvider(this, SOLID_FUEL_SLOT)));
    }

    public static ThaumicSteamLocomotive create(ItemStack stack, double x, double y, double z, ServerLevel level) {
        ThaumicSteamLocomotive loco = new ThaumicSteamLocomotive(RTRegistry.THAUMIC_STEAM_LOCOMOTIVE.get(), level);
        loco.setPos(x, y, z);
        loco.xo = x;
        loco.yo = y;
        loco.zo = z;
        loco.loadFromItemStack(stack);
        return loco;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IGNIS, 0);
        builder.define(AQUA, 0);
    }

    public int ignis() {
        return entityData.get(IGNIS);
    }

    public int aqua() {
        return entityData.get(AQUA);
    }

    @Override
    protected DyeColor getDefaultPrimaryColor() {
        return DyeColor.PURPLE;
    }

    @Override
    protected DyeColor getDefaultSecondaryColor() {
        return DyeColor.BLACK;
    }

    @Override
    protected Item getDropItem() {
        return RTRegistry.THAUMIC_STEAM_LOCOMOTIVE_ITEM.get();
    }

    /** Railcraft drops this stack when the locomotive is broken, so the essentia rides along on the item. */
    @Override
    public ItemStack getPickResult() {
        ItemStack stack = super.getPickResult();
        if (ignis() > 0 || aqua() > 0) {
            stack.set(RTRegistry.BOILER_ESSENTIA.get(), new BoilerEssentia(ignis(), aqua()));
        }
        return stack;
    }

    @Override
    protected void loadFromItemStack(ItemStack stack) {
        super.loadFromItemStack(stack);
        BoilerEssentia essentia = stack.get(RTRegistry.BOILER_ESSENTIA.get());
        if (essentia != null) {
            entityData.set(IGNIS, Math.min(essentia.ignis(), ESSENTIA_CAPACITY));
            entityData.set(AQUA, Math.min(essentia.aqua(), ESSENTIA_CAPACITY));
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        IEssentiaItemStorage storage = held.getCapability(EssentiaCapabilities.ITEM_STORAGE);
        if (storage == null || storage.contents().isEmpty()) {
            return super.interact(player, hand);
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        int moved = pour(player, hand, storage, TTAspects.IGNIS, IGNIS);
        if (moved == 0) {
            moved = pour(player, hand, storage, TTAspects.AQUA, AQUA);
        }
        if (moved == 0) {
            return super.interact(player, hand);
        }
        storage = player.getItemInHand(hand).getCapability(EssentiaCapabilities.ITEM_STORAGE);
        if (storage != null) {
            storage.playTransferFeedback(player, IEssentiaItemStorage.TransferDirection.DRAIN);
        }
        player.displayClientMessage(Component.translatable("message.arcanelocomotion.essentia", ignis(), aqua(), ESSENTIA_CAPACITY), true);
        return InteractionResult.SUCCESS;
    }

    private int pour(Player player, InteractionHand hand, IEssentiaItemStorage storage, ResourceKey<IAspect> key, EntityDataAccessor<Integer> slot) {
        Holder<IAspect> aspect = Aspects.resolve(level(), key);
        int room = ESSENTIA_CAPACITY - entityData.get(slot);
        int available = storage.contents().amountOf(aspect);
        if (room <= 0 || available <= 0) {
            return 0;
        }
        ItemEssentiaTransferResult result = storage.extract(aspect, Math.min(room, available));
        if (result.amountMoved() > 0) {
            entityData.set(slot, entityData.get(slot) + result.amountMoved());
            player.setItemInHand(hand, result.resultingStack());
        }
        return result.amountMoved();
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        condenseAqua();
    }

    private void condenseAqua() {
        int aqua = aqua();
        if (aqua <= 0 || waterTank.getRemainingSpace() < WATER_PER_AQUA) {
            return;
        }
        FluidStack water = new FluidStack(Fluids.WATER, WATER_PER_AQUA);
        if (waterTank.fill(water, IFluidHandler.FluidAction.SIMULATE) == WATER_PER_AQUA) {
            waterTank.fill(water, IFluidHandler.FluidAction.EXECUTE);
            entityData.set(AQUA, aqua - 1);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        output.putInt("ignis", ignis());
        output.putInt("aqua", aqua());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        entityData.set(IGNIS, Math.clamp(input.getInt("ignis"), 0, ESSENTIA_CAPACITY));
        entityData.set(AQUA, Math.clamp(input.getInt("aqua"), 0, ESSENTIA_CAPACITY));
    }

    /** Burns a point of Ignis per firebox load; falls back to the locomotive's normal solid fuel. */
    private final class EssentiaFuel implements FuelProvider {
        private final FuelProvider solid;

        EssentiaFuel(FuelProvider solid) {
            this.solid = solid;
        }

        @Override
        public float consumeFuel() {
            if (isShutdown()) {
                return 0;
            }
            int ignis = ignis();
            if (ignis > 0) {
                entityData.set(IGNIS, ignis - 1);
                return HEAT_PER_IGNIS;
            }
            return solid.consumeFuel();
        }

        @Override
        public float getHeatStep() {
            return solid.getHeatStep();
        }
    }
}
