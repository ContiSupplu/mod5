package com.contisupply.royalkennel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The cosmetic state of a dog. Hats and back gear are plain indices so the
 * whole thing stays one tiny synced attachment.
 *
 * hat:  0 none | 1 crown | 2 mage's cap | 3 knight's helm
 * back: 0 none | 1 adventurer's pack | 2 rescue keg | 3 royal cape
 */
public record DogStyle(int hat, int back) {

    public static final DogStyle PLAIN = new DogStyle(0, 0);

    public static final int HAT_COUNT = 4;
    public static final int BACK_COUNT = 4;

    public static final Codec<DogStyle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("hat").forGetter(DogStyle::hat),
            Codec.INT.fieldOf("back").forGetter(DogStyle::back)
    ).apply(instance, DogStyle::new));

    public static final StreamCodec<ByteBuf, DogStyle> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DogStyle::hat,
            ByteBufCodecs.VAR_INT, DogStyle::back,
            DogStyle::new
    );

    public boolean isPlain() {
        return hat == 0 && back == 0;
    }

    public static DogStyle clamped(int hat, int back) {
        return new DogStyle(
                Math.floorMod(hat, HAT_COUNT),
                Math.floorMod(back, BACK_COUNT)
        );
    }
}
