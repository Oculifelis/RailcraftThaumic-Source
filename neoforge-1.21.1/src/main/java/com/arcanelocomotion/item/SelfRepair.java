package com.arcanelocomotion.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/** Data component: the item mends {@code amount} durability every {@code interval} ticks while carried. */
public record SelfRepair(int interval, int amount) {
    public static final Codec<SelfRepair> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("interval").forGetter(SelfRepair::interval),
            ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(SelfRepair::amount)
    ).apply(i, SelfRepair::new));
    public static final StreamCodec<ByteBuf, SelfRepair> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelfRepair::interval,
            ByteBufCodecs.VAR_INT, SelfRepair::amount,
            SelfRepair::new);
}
