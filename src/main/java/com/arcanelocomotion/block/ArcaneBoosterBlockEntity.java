package com.arcanelocomotion.block;

import com.arcanelocomotion.RTDataMaps;
import com.arcanelocomotion.RTRegistry;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaItemStorage;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaTransport;
import com.leclowndu93150.thaumaturge.api.essentia.ItemEssentiaTransferResult;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The booster rail's Motus reservoir and its kick. */
public class ArcaneBoosterBlockEntity extends BlockEntity implements IEssentiaTransport {
    public static final int CAPACITY = 64;
    public static final int SUCTION = 64;
    private int motus;
    private final Set<UUID> inside = new HashSet<>();

    public ArcaneBoosterBlockEntity(BlockPos pos, BlockState state) {
        super(RTRegistry.ARCANE_BOOSTER_BE.get(), pos, state);
    }

    public int motus() {
        return motus;
    }

    public void setMotus(int amount) {
        motus = Math.clamp(amount, 0, CAPACITY);
        setChanged();
    }

    private @Nullable Holder<IAspect> motusHolder() {
        return level == null ? null : level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).get(TTAspects.MOTUS).orElse(null);
    }

    public Component status() {
        return Component.translatable("message.arcanelocomotion.booster", motus, CAPACITY);
    }

    boolean pourFrom(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        IEssentiaItemStorage storage = held.getCapability(EssentiaCapabilities.ITEM_STORAGE);
        Holder<IAspect> m = motusHolder();
        if (storage == null || m == null) {
            return false;
        }
        int want = Math.min(CAPACITY - motus, storage.contents().amountOf(m));
        if (want > 0) {
            ItemEssentiaTransferResult r = storage.extract(m, want);
            if (r.amountMoved() > 0) {
                setMotus(motus + r.amountMoved());
                player.setItemInHand(hand, r.resultingStack());
                IEssentiaItemStorage after = r.resultingStack().getCapability(EssentiaCapabilities.ITEM_STORAGE);
                if (after != null) {
                    after.playTransferFeedback(player, IEssentiaItemStorage.TransferDirection.DRAIN);
                }
            }
        }
        player.sendOverlayMessage(status());
        return true;
    }

    void serverTick() {
        if (level == null) {
            return;
        }
        if (level.getGameTime() % 5 == 0 && motus < CAPACITY) {
            pull();
        }
        // The live state: the cached one can miss a power change made in the same tick.
        BlockState state = level.getBlockState(worldPosition);
        Set<UUID> now = new HashSet<>();
        for (AbstractMinecart cart : level.getEntitiesOfClass(AbstractMinecart.class, new AABB(worldPosition).inflate(0, 0.2, 0))) {
            UUID id = cart.getUUID();
            now.add(id);
            if (inside.contains(id)) {
                continue;
            }
            RTDataMaps.RailTuning t = RTDataMaps.rail(state.getBlock());
            Vec3 v = cart.getDeltaMovement();
            Vec3 flat = new Vec3(v.x, 0, v.z);
            if (state.getValue(PoweredRailBlock.POWERED) && motus >= t.essentiaPerBoost() && flat.length() > 0.01) {
                cart.setDeltaMovement(flat.normalize().scale(t.boost()).add(0, v.y, 0));
                setMotus(motus - t.essentiaPerBoost());
            }
        }
        inside.retainAll(now);
        inside.addAll(now);
    }

    private void pull() {
        Holder<IAspect> m = motusHolder();
        if (m == null || level == null) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) {
                continue;
            }
            Direction face = dir.getOpposite();
            IEssentiaTransport other = EssentiaAccess.transport(level, worldPosition.relative(dir), face);
            if (other == null || !other.isConnectable(face) || !other.canOutputTo(face)) {
                continue;
            }
            if (other.getEssentiaAmount(face) > 0 && m.equals(other.getEssentiaType(face)) && other.getSuctionAmount(face) < SUCTION) {
                int taken = other.takeEssentia(m, Math.min(4, CAPACITY - motus), face);
                setMotus(motus + taken);
                if (motus >= CAPACITY) {
                    return;
                }
            }
        }
    }

    // --- IEssentiaTransport: a Motus sink on every face but the top. ---

    @Override
    public boolean isConnectable(Direction face) {
        return face != Direction.UP;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face != Direction.UP;
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return false;
    }

    @Override
    public void setSuction(Holder<IAspect> aspect, int amount) {
    }

    @Override
    public @Nullable Holder<IAspect> getSuctionType(Direction face) {
        return motusHolder();
    }

    @Override
    public int getSuctionAmount(Direction face) {
        return motus < CAPACITY ? SUCTION : 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public @Nullable Holder<IAspect> getEssentiaType(Direction face) {
        return motus > 0 ? motusHolder() : null;
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        return motus;
    }

    @Override
    public int addEssentia(Holder<IAspect> aspect, int amount, Direction face) {
        if (face == Direction.UP || !aspect.is(TTAspects.MOTUS)) {
            return 0;
        }
        int moved = Math.min(amount, CAPACITY - motus);
        setMotus(motus + moved);
        return moved;
    }

    @Override
    public int takeEssentia(Holder<IAspect> aspect, int amount, Direction face) {
        return 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("motus", motus);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        motus = Math.clamp(input.getIntOr("motus", 0), 0, CAPACITY);
    }
}
