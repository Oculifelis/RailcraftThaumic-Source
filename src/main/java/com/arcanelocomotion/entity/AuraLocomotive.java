package com.arcanelocomotion.entity;

import com.arcanelocomotion.RTDataMaps;
import com.arcanelocomotion.RTRegistry;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import mods.railcraft.world.entity.vehicle.locomotive.ElectricLocomotive;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A locomotive that runs on the aura: each time its firebox runs low it draws a little vis from the chunk it is in.
 * No fuel to carry, but it never goes faster than normal speed and stalls in drained land. Vis cost and the fuel it
 * buys come from the {@code arcanelocomotion:engine_tuning} data map.
 */
public class AuraLocomotive extends ElectricLocomotive {
    public AuraLocomotive(EntityType<?> type, Level level) {
        super(type, level);
    }

    public static AuraLocomotive create(ItemStack stack, double x, double y, double z, net.minecraft.server.level.ServerLevel level) {
        AuraLocomotive loco = new AuraLocomotive(RTRegistry.AURA_LOCOMOTIVE.get(), level);
        loco.setPos(x, y, z);
        loco.loadFromItemStack(stack);
        return loco;
    }

    @Override
    public int retrieveFuel() {
        if (level().isClientSide()) {
            return 0;
        }
        RTDataMaps.EngineTuning t = RTDataMaps.engine(getType());
        float drained = AuraHelper.drainVis(level(), blockPosition(), t.visPerRequest(), false);
        return drained > 0 ? Math.max(1, Math.round(t.fuelPerRequest() * drained / t.visPerRequest())) : 0;
    }

    @Override
    public boolean needsFuel() {
        return false;
    }

    @Override
    public void setSpeed(Speed speed) {
        super.setSpeed(speed == Speed.MAX ? Speed.NORMAL : speed);
    }

    @Override
    public void increaseSpeed() {
        if (getSpeed() != Speed.NORMAL) {
            super.increaseSpeed();
        }
    }

    @Override
    protected DyeColor getDefaultPrimaryColor() {
        return DyeColor.CYAN;
    }

    @Override
    protected DyeColor getDefaultSecondaryColor() {
        return DyeColor.WHITE;
    }

    @Override
    protected Item getDropItem() {
        return RTRegistry.AURA_LOCOMOTIVE_ITEM.get();
    }
}
