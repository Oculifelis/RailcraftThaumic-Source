package com.arcanelocomotion.item;

import com.arcanelocomotion.RTRegistry;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.wands.WandAccess;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Lays a short run of spectral rail forward from the block it is used on, in the direction the player faces. Each
 * rail costs {@link #VIS_PER_RAIL} centivis each of Aer and Ordo from a wand in the player's inventory.
 */
public class SpectralRailRodItem extends Item {
    public static final int MAX_RAILS = 8;
    public static final int VIS_PER_RAIL = 50;
    private static final List<ResourceKey<IAspect>> COST = List.of(TTAspects.AER, TTAspects.ORDO);

    public SpectralRailRodItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        int laid = lay(level, player, context.getClickedPos(), player.getDirection());
        if (laid == 0) {
            player.sendOverlayMessage(Component.translatable("message.arcanelocomotion.rod_no_vis"));
            return InteractionResult.FAIL;
        }
        level.playSound(null, context.getClickedPos(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.4f);
        return InteractionResult.SUCCESS;
    }

    /** Lays rails from {@code clicked.above()} along {@code facing}; returns how many. */
    public static int lay(Level level, Player player, BlockPos clicked, Direction facing) {
        BlockState rail = RTRegistry.SPECTRAL_RAIL.get().defaultBlockState();
        BlockPos pos = clicked.above();
        int laid = 0;
        while (laid < MAX_RAILS && level.getBlockState(pos).isAir()) {
            if (!player.getAbilities().instabuild && !pay(player)) {
                break;
            }
            level.setBlock(pos, rail, Block.UPDATE_ALL);
            laid++;
            pos = pos.relative(facing);
        }
        return laid;
    }

    private static boolean pay(Player player) {
        ItemStack wand = findWand(player);
        if (wand == null) {
            return false;
        }
        for (ResourceKey<IAspect> key : COST) {
            if (WandAccess.getVis(wand, key) < VIS_PER_RAIL) {
                return false;
            }
        }
        ItemStack paid = wand;
        for (ResourceKey<IAspect> key : COST) {
            paid = WandAccess.withVis(paid, key, WandAccess.getVis(paid, key) - VIS_PER_RAIL);
        }
        if (paid != wand) {
            replace(player, wand, paid);
        }
        return true;
    }

    private static @Nullable ItemStack findWand(Player player) {
        for (ItemStack s : player.getInventory()) {
            if (s.getItem() instanceof ItemWand) {
                return s;
            }
        }
        return null;
    }

    private static void replace(Player player, ItemStack old, ItemStack now) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i) == old) {
                inv.setItem(i, now);
                return;
            }
        }
    }
}
