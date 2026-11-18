package com.arcanelocomotion.compat;

import com.arcanelocomotion.ArcaneLocomotion;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI: information pages for the locomotive (fed with Ignis and Aqua essentia, not a recipe) and the two crowbars,
 * reusing their research text. Their arcane recipes are Thaumaturge's type, which its plugin shows. Loaded only with JEI.
 */
@JeiPlugin
public final class RTJeiPlugin implements IModPlugin {
    private static final String[] INFO = {"thaumic_steam_locomotive", "thaumium_crowbar", "void_crowbar"};

    @Override
    public ResourceLocation getPluginUid() {
        return ArcaneLocomotion.id("jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (String name : INFO) {
            BuiltInRegistries.ITEM.getOptional(ArcaneLocomotion.id(name)).ifPresent(item ->
                    registration.addIngredientInfo(item, Component.translatable("research.arcanelocomotion." + name + ".stage_1")));
        }
    }
}
