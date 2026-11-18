package com.arcanelocomotion;

import com.arcanelocomotion.gametest.RTGameTests;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Railcraft Reborn x Thaumaturge integration, a new take on the Thaumcraft module that Railcraft 1.7.10 shipped:
 * Thaumium and Void Metal crowbars, a Railcraft research tab, aspects on Railcraft items and carts (all datapack JSON),
 * and the essentia-fired steam locomotive the old module never finished.
 */
@Mod(ArcaneLocomotion.MODID)
public final class ArcaneLocomotion {
    public static final String MODID = "arcanelocomotion";
    public static final Logger LOG = LogUtils.getLogger();

    public ArcaneLocomotion(IEventBus modBus) {
        RTRegistry.register(modBus);
        RTDataMaps.register(modBus);
        RTGameTests.register(modBus);
        modBus.addListener(ArcaneLocomotion::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(EssentiaCapabilities.TRANSPORT, RTRegistry.ESSENTIA_LOADER_BE.get(), (be, dir) -> be);
        event.registerBlockEntity(EssentiaCapabilities.TRANSPORT, RTRegistry.ARCANE_BOOSTER_BE.get(), (be, dir) -> be);
        event.registerBlockEntity(Capabilities.Item.BLOCK, RTRegistry.GOLEM_STATION_BE.get(), (be, dir) -> be.handler());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
