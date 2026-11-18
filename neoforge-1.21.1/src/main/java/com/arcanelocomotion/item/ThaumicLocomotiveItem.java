package com.arcanelocomotion.item;

import com.arcanelocomotion.RTRegistry;
import com.arcanelocomotion.entity.ThaumicSteamLocomotive;
import java.util.List;
import mods.railcraft.api.item.MinecartFactory;
import mods.railcraft.world.item.LocomotiveItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Railcraft's locomotive item, plus a tooltip line for the essentia carried in its {@link BoilerEssentia}. */
public class ThaumicLocomotiveItem extends LocomotiveItem {
    public ThaumicLocomotiveItem(MinecartFactory factory, Properties properties) {
        super(factory, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        BoilerEssentia essentia = stack.get(RTRegistry.BOILER_ESSENTIA.get());
        if (essentia != null) {
            tooltip.add(Component.translatable("message.arcanelocomotion.essentia", essentia.ignis(), essentia.aqua(),
                    ThaumicSteamLocomotive.ESSENTIA_CAPACITY).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
