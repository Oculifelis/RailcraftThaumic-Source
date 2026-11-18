package com.arcanelocomotion.item;

import com.arcanelocomotion.RTRegistry;
import mods.railcraft.world.item.CrowbarItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A Railcraft Reborn crowbar (whacks tracks, links and boosts carts through Railcraft's own {@code Crowbar} handling)
 * that also mends itself when its stack carries a {@link SelfRepair} component.
 */
public class ThaumicCrowbarItem extends CrowbarItem {
    public ThaumicCrowbarItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        SelfRepair repair = stack.get(RTRegistry.SELF_REPAIR.get());
        if (repair != null && entity instanceof LivingEntity && stack.isDamaged() && entity.tickCount % repair.interval() == 0) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - repair.amount()));
        }
    }
}
