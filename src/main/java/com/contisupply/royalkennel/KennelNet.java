package com.contisupply.royalkennel;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** All custom packets used by the Royal Kennel. */
public final class KennelNet {

    /** Server -> client: open the grooming screen for the given wolf. */
    public record OpenGroomingPayload(int wolfId) implements CustomPacketPayload {
        public static final Type<OpenGroomingPayload> TYPE = new Type<>(RoyalKennel.id("open_grooming"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenGroomingPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, OpenGroomingPayload::wolfId,
                OpenGroomingPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Client -> server: the full desired look of the dog being groomed.
     * Sent on every change so the real dog updates live while the camera
     * is pointed at it.
     */
    public record UpdateDogPayload(int wolfId, String variantId, int hat, int back, int collar,
                                   String name) implements CustomPacketPayload {
        public static final Type<UpdateDogPayload> TYPE = new Type<>(RoyalKennel.id("update_dog"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdateDogPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, UpdateDogPayload::wolfId,
                ByteBufCodecs.STRING_UTF8, UpdateDogPayload::variantId,
                ByteBufCodecs.VAR_INT, UpdateDogPayload::hat,
                ByteBufCodecs.VAR_INT, UpdateDogPayload::back,
                ByteBufCodecs.VAR_INT, UpdateDogPayload::collar,
                ByteBufCodecs.STRING_UTF8, UpdateDogPayload::name,
                UpdateDogPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: grooming finished. sealed=true means the Done button (celebrate!). */
    public record CloseGroomingPayload(boolean sealed) implements CustomPacketPayload {
        public static final Type<CloseGroomingPayload> TYPE = new Type<>(RoyalKennel.id("close_grooming"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CloseGroomingPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, CloseGroomingPayload::sealed,
                CloseGroomingPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private KennelNet() {
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(OpenGroomingPayload.TYPE, OpenGroomingPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateDogPayload.TYPE, UpdateDogPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CloseGroomingPayload.TYPE, CloseGroomingPayload.CODEC);
    }
}
