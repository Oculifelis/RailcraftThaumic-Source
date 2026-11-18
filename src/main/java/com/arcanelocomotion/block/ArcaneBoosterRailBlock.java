package com.arcanelocomotion.block;

import com.arcanelocomotion.RTRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A powered rail with a Motus reservoir. While powered it spends Motus to throw each cart that enters well past the
 * normal top speed; with no Motus it is an ordinary powered rail. Fed by essentia tubes or by pouring a jar on it.
 */
public class ArcaneBoosterRailBlock extends PoweredRailBlock implements EntityBlock {
    public ArcaneBoosterRailBlock(Properties properties) {
        super(properties, true);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ArcaneBoosterBlockEntity be)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return be.pourFrom(player, hand) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ArcaneBoosterBlockEntity be) {
            player.sendOverlayMessage(be.status());
        }
        return InteractionResult.SUCCESS;
    }

    /** While powered, arcane glyphs drift down into the track. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERED) || random.nextInt(2) != 0) {
            return;
        }
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.ENCHANT, x, pos.getY() + 0.15, z,
                (random.nextDouble() - 0.5) * 1.2, 0.4 + random.nextDouble() * 0.6, (random.nextDouble() - 0.5) * 1.2);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneBoosterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || type != RTRegistry.ARCANE_BOOSTER_BE.get() ? null
                : (l, p, s, be) -> ((ArcaneBoosterBlockEntity) be).serverTick();
    }
}
