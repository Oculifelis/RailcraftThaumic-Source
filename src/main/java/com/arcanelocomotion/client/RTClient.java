package com.arcanelocomotion.client;

import com.arcanelocomotion.RTRegistry;
import com.arcanelocomotion.ArcaneLocomotion;
import mods.railcraft.client.renderer.entity.cart.ElectricLocomotiveRenderer;
import mods.railcraft.client.renderer.entity.cart.SteamLocomotiveRenderer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The locomotives borrow Railcraft Reborn's steam and electric models and their dyeable paint; the tank and eldritch
 * carts are vanilla minecarts showing their display block.
 */
@EventBusSubscriber(modid = ArcaneLocomotion.MODID, value = Dist.CLIENT)
public final class RTClient {
    private RTClient() {
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RTRegistry.THAUMIC_STEAM_LOCOMOTIVE.get(), SteamLocomotiveRenderer::new);
        event.registerEntityRenderer(RTRegistry.AURA_LOCOMOTIVE.get(), ElectricLocomotiveRenderer::new);
        event.registerEntityRenderer(RTRegistry.ESSENTIA_TANK_CART.get(), ctx -> new MinecartRenderer(ctx, ModelLayers.MINECART));
        event.registerEntityRenderer(RTRegistry.ELDRITCH_CART.get(), ctx -> new MinecartRenderer(ctx, ModelLayers.MINECART));
    }
}
