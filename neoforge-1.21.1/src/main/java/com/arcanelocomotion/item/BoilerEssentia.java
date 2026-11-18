package com.arcanelocomotion.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/** Data component on a Thaumic Steam Locomotive item: the Ignis and Aqua it held when it was picked up. */
public record BoilerEssentia(int ignis, int aqua) {
    public static final Codec<BoilerEssentia> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("ignis", 0).forGetter(BoilerEssentia::ignis),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("aqua", 0).forGetter(BoilerEssentia::aqua)
    ).apply(i, BoilerEssentia::new));
    public static final StreamCodec<ByteBuf, BoilerEssentia> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BoilerEssentia::ignis,
            ByteBufCodecs.VAR_INT, BoilerEssentia::aqua,
            BoilerEssentia::new);
}
