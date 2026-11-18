package com.arcanelocomotion.entity;

import com.arcanelocomotion.RTRegistry;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaItemStorage;
import com.leclowndu93150.thaumaturge.api.essentia.ItemEssentiaTransferResult;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A minecart carrying a warded jar: one aspect of essentia, up to {@link #CAPACITY} points. Jars and phials used on it
 * pour in (or, sneaking, draw out); the Essentia Loader moves it to and from tubes. Breaking it keeps the load.
 */
public class EssentiaTankCart extends AbstractMinecart {
    public static final int CAPACITY = 1024;
    private static final EntityDataAccessor<String> ASPECT = SynchedEntityData.defineId(EssentiaTankCart.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> AMOUNT = SynchedEntityData.defineId(EssentiaTankCart.class, EntityDataSerializers.INT);

    public EssentiaTankCart(EntityType<?> type, Level level) {
        super(type, level);
    }

    public static EssentiaTankCart create(ItemStack stack, Level level, double x, double y, double z) {
        EssentiaTankCart cart = new EssentiaTankCart(RTRegistry.ESSENTIA_TANK_CART.get(), level);
        cart.setInitialPos(x, y, z);
        AspectInstance load = stack.get(RTRegistry.CART_ESSENTIA.get());
        if (load != null && load.amount() > 0) {
            cart.set(load.aspect(), Math.min(load.amount(), CAPACITY));
        }
        return cart;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ASPECT, "");
        builder.define(AMOUNT, 0);
    }

    public int amount() {
        return entityData.get(AMOUNT);
    }

    public @Nullable Holder<IAspect> aspect() {
        String id = entityData.get(ASPECT);
        if (id.isEmpty() || amount() <= 0) {
            return null;
        }
        Identifier rl = Identifier.tryParse(id);
        if (rl == null) {
            return null;
        }
        return level().registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).get(ResourceKey.create(IAspect.REGISTRY_KEY, rl)).orElse(null);
    }

    private void set(@Nullable Holder<IAspect> aspect, int amount) {
        if (aspect == null || amount <= 0) {
            entityData.set(ASPECT, "");
            entityData.set(AMOUNT, 0);
        } else {
            entityData.set(ASPECT, aspect.unwrapKey().map(k -> k.identifier().toString()).orElse(""));
            entityData.set(AMOUNT, amount);
        }
    }

    /** Adds up to {@code n} of {@code aspect}; returns what fit. A tank holds one aspect at a time. */
    public int fill(Holder<IAspect> aspect, int n, boolean simulate) {
        Holder<IAspect> held = aspect();
        if (n <= 0 || (held != null && !held.equals(aspect))) {
            return 0;
        }
        int moved = Math.min(n, CAPACITY - amount());
        if (moved > 0 && !simulate) {
            set(aspect, amount() + moved);
        }
        return moved;
    }

    /** Takes up to {@code n}; {@code aspect} null takes whatever is held. */
    public int drain(@Nullable Holder<IAspect> aspect, int n, boolean simulate) {
        Holder<IAspect> held = aspect();
        if (n <= 0 || held == null || (aspect != null && !held.equals(aspect))) {
            return 0;
        }
        int moved = Math.min(n, amount());
        if (!simulate) {
            set(held, amount() - moved);
        }
        return moved;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack held = player.getItemInHand(hand);
        IEssentiaItemStorage storage = held.getCapability(EssentiaCapabilities.ITEM_STORAGE);
        if (storage == null) {
            return super.interact(player, hand, location);
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        boolean draw = player.isSecondaryUseActive();
        int moved = draw ? drawInto(player, hand, storage) : pourFrom(player, hand, storage);
        if (moved > 0) {
            IEssentiaItemStorage after = player.getItemInHand(hand).getCapability(EssentiaCapabilities.ITEM_STORAGE);
            if (after != null) {
                after.playTransferFeedback(player, draw ? IEssentiaItemStorage.TransferDirection.FILL : IEssentiaItemStorage.TransferDirection.DRAIN);
            }
        }
        player.sendOverlayMessage(status());
        return InteractionResult.SUCCESS;
    }

    public Component status() {
        Holder<IAspect> a = aspect();
        return a == null
                ? Component.translatable("message.arcanelocomotion.tank_empty", CAPACITY)
                : Component.translatable("message.arcanelocomotion.tank", amount(), CAPACITY, a.value().tag());
    }

    private int pourFrom(Player player, InteractionHand hand, IEssentiaItemStorage storage) {
        AspectList contents = storage.contents();
        Holder<IAspect> want = aspect();
        if (want == null) {
            if (contents.isEmpty()) {
                return 0;
            }
            want = contents.sortedByAmount().getFirst().aspect();
        }
        int room = fill(want, contents.amountOf(want), true);
        if (room <= 0) {
            return 0;
        }
        ItemEssentiaTransferResult r = storage.extract(want, room);
        if (r.amountMoved() > 0) {
            fill(want, r.amountMoved(), false);
            player.setItemInHand(hand, r.resultingStack());
        }
        return r.amountMoved();
    }

    private int drawInto(Player player, InteractionHand hand, IEssentiaItemStorage storage) {
        Holder<IAspect> a = aspect();
        if (a == null || !storage.canInsert(a)) {
            return 0;
        }
        ItemEssentiaTransferResult r = storage.insert(a, amount());
        if (r.amountMoved() > 0) {
            drain(a, r.amountMoved(), false);
            player.setItemInHand(hand, r.resultingStack());
        }
        return r.amountMoved();
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath("thaumaturge", "jar_normal"))
                .map(Block::defaultBlockState).orElse(Blocks.GLASS.defaultBlockState());
    }

    @Override
    protected Item getDropItem() {
        return RTRegistry.ESSENTIA_TANK_CART_ITEM.get();
    }

    @Override
    public ItemStack getPickResult() {
        ItemStack stack = new ItemStack(getDropItem());
        Holder<IAspect> a = aspect();
        if (a != null) {
            stack.set(RTRegistry.CART_ESSENTIA.get(), new AspectInstance(a, amount()));
        }
        return stack;
    }

    /** Drops the cart item with its load on it. */
    @Override
    protected void destroy(ServerLevel level, DamageSource source) {
        kill(level);
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            ItemStack stack = getPickResult();
            stack.set(DataComponents.CUSTOM_NAME, getCustomName());
            spawnAtLocation(level, stack);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("aspect", entityData.get(ASPECT));
        output.putInt("amount", amount());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(ASPECT, input.getStringOr("aspect", ""));
        entityData.set(AMOUNT, Math.clamp(input.getIntOr("amount", 0), 0, CAPACITY));
    }
}
