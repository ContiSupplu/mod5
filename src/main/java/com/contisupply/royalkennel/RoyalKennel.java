package com.contisupply.royalkennel;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoyalKennel implements ModInitializer {

    public static final String MOD_ID = "royalkennel";
    public static final Logger LOGGER = LoggerFactory.getLogger("RoyalKennel");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        KennelAttachments.init();
        KennelNet.register();

        // Sneak + right-click your own tamed dog with an empty hand -> open the atelier.
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (hand != net.minecraft.world.InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (!canGroom(player, entity)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && entity instanceof Wolf wolf) {
                GroomingSessions.open(serverPlayer, wolf);
            }
            return InteractionResult.SUCCESS;
        });

        ServerPlayNetworking.registerGlobalReceiver(KennelNet.UpdateDogPayload.TYPE,
                (payload, context) -> GroomingSessions.applyUpdate(context.player(), payload));

        ServerPlayNetworking.registerGlobalReceiver(KennelNet.CloseGroomingPayload.TYPE,
                (payload, context) -> GroomingSessions.close(context.player(), payload.sealed()));

        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> GroomingSessions.onDisconnect(handler.player));

        ServerTickEvents.END_SERVER_TICK.register(server -> GroomingSessions.tick());

        LOGGER.info("The Royal Kennel is open. Bring thy hound.");
    }

    private static boolean canGroom(Player player, net.minecraft.world.entity.Entity entity) {
        return player.isShiftKeyDown()
                && player.getMainHandItem().isEmpty()
                && entity instanceof Wolf wolf
                && wolf.isAlive()
                && wolf.isTame()
                && wolf.isOwnedBy(player);
    }
}
