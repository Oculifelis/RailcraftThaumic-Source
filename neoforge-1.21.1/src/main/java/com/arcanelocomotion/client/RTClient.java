package com.arcanelocomotion.client;

import com.arcanelocomotion.RTRegistry;
import com.arcanelocomotion.ArcaneLocomotion;
import mods.railcraft.client.renderer.entity.cart.SteamLocomotiveRenderer;
import mods.railcraft.client.renderer.entity.cart.ElectricLocomotiveRenderer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import mods.railcraft.world.item.LocomotiveItem;
import mods.railcraft.world.item.component.LocomotiveColorComponent;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** The Thaumic locomotive borrows Railcraft Reborn's steam locomotive model and its dyeable paint. */
@EventBusSubscriber(modid = ArcaneLocomotion.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class RTClient {
    private RTClient() {
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RTRegistry.THAUMIC_STEAM_LOCOMOTIVE.get(), SteamLocomotiveRenderer::new);
        event.registerEntityRenderer(RTRegistry.AURA_LOCOMOTIVE.get(), ElectricLocomotiveRenderer::new);
        event.registerEntityRenderer(RTRegistry.ESSENTIA_TANK_CART.get(), ctx -> new MinecartRenderer<>(ctx, ModelLayers.MINECART));
        event.registerEntityRenderer(RTRegistry.ELDRITCH_CART.get(), ctx -> new MinecartRenderer<>(ctx, ModelLayers.MINECART));
    }

    /** Layer 0 takes the primary paint, layer 1 the secondary, like Railcraft's own locomotive items. */
    @SubscribeEvent
    static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> {
            if (tint > 1) {
                return -1;
            }
            LocomotiveColorComponent color = LocomotiveItem.getColor(stack);
            if (color == null) {
                return -1;
            }
            return FastColor.ARGB32.opaque((tint == 0 ? color.primary() : color.secondary()).getTextureDiffuseColor());
        }, RTRegistry.THAUMIC_STEAM_LOCOMOTIVE_ITEM.get(), RTRegistry.AURA_LOCOMOTIVE_ITEM.get());
    }
}
