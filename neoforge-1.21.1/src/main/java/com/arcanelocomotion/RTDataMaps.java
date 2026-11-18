package com.arcanelocomotion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Tuning numbers kept in datapacks: {@code data/<ns>/data_maps/block/rail_tuning.json} for rails and
 * {@code data/<ns>/data_maps/entity_type/engine_tuning.json} for the aura locomotive. Missing entries use the defaults.
 */
public final class RTDataMaps {
    /** Per rail block. {@code max_speed} for eldritch carts, {@code boost} launch speed, Motus per boost, warp chance per block, spectral lifetime. */
    public record RailTuning(float maxSpeed, float boost, int essentiaPerBoost, float warpChance, int lifetime) {
        public static final RailTuning DEFAULT = new RailTuning(0.4f, 1.2f, 1, 0.05f, 1200);
        public static final Codec<RailTuning> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("max_speed", DEFAULT.maxSpeed).forGetter(RailTuning::maxSpeed),
                Codec.FLOAT.optionalFieldOf("boost", DEFAULT.boost).forGetter(RailTuning::boost),
                Codec.INT.optionalFieldOf("essentia_per_boost", DEFAULT.essentiaPerBoost).forGetter(RailTuning::essentiaPerBoost),
                Codec.FLOAT.optionalFieldOf("warp_chance", DEFAULT.warpChance).forGetter(RailTuning::warpChance),
                Codec.INT.optionalFieldOf("lifetime", DEFAULT.lifetime).forGetter(RailTuning::lifetime)
        ).apply(i, RailTuning::new));
    }

    /** Aura locomotive: vis drained per fuel request, fuel given for it. */
    public record EngineTuning(float visPerRequest, int fuelPerRequest) {
        public static final EngineTuning DEFAULT = new EngineTuning(0.5f, 400);
        public static final Codec<EngineTuning> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("vis_per_request", DEFAULT.visPerRequest).forGetter(EngineTuning::visPerRequest),
                Codec.INT.optionalFieldOf("fuel_per_request", DEFAULT.fuelPerRequest).forGetter(EngineTuning::fuelPerRequest)
        ).apply(i, EngineTuning::new));
    }

    public static final DataMapType<Block, RailTuning> RAIL_TUNING = DataMapType.builder(ArcaneLocomotion.id("rail_tuning"), Registries.BLOCK, RailTuning.CODEC).build();
    public static final DataMapType<EntityType<?>, EngineTuning> ENGINE_TUNING = DataMapType.builder(ArcaneLocomotion.id("engine_tuning"), Registries.ENTITY_TYPE, EngineTuning.CODEC).build();

    private RTDataMaps() {
    }

    public static RailTuning rail(Block block) {
        RailTuning t = BuiltInRegistries.BLOCK.wrapAsHolder(block).getData(RAIL_TUNING);
        return t == null ? RailTuning.DEFAULT : t;
    }

    public static EngineTuning engine(EntityType<?> type) {
        Holder<EntityType<?>> h = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type);
        EngineTuning t = h.getData(ENGINE_TUNING);
        return t == null ? EngineTuning.DEFAULT : t;
    }

    static void register(IEventBus bus) {
        bus.addListener((RegisterDataMapTypesEvent e) -> {
            e.register(RAIL_TUNING);
            e.register(ENGINE_TUNING);
        });
    }
}
