package com.arcanelocomotion.block;

import com.arcanelocomotion.RTRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Track-bed block: an essentia tube endpoint that loads the tank cart above from tubes, or unloads it into them. */
public class EssentiaLoaderBlock extends Block implements EntityBlock {
    public static final BooleanProperty UNLOAD = BooleanProperty.create("unload");

    public EssentiaLoaderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(UNLOAD, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UNLOAD);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockState next = state.cycle(UNLOAD);
            level.setBlock(pos, next, Block.UPDATE_ALL);
            player.displayClientMessage(Component.translatable(next.getValue(UNLOAD) ? "message.arcanelocomotion.mode_unload" : "message.arcanelocomotion.mode_load"), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssentiaLoaderBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || type != RTRegistry.ESSENTIA_LOADER_BE.get() ? null
                : (l, p, s, be) -> ((EssentiaLoaderBlockEntity) be).serverTick();
    }
}
