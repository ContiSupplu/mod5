package com.contisupply.royalkennel.client;

import com.contisupply.royalkennel.KennelNet;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

import net.minecraft.world.entity.animal.wolf.Wolf;

public class RoyalKennelClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> KennelClock.onClientTick());

        WorldRenderEvents.AFTER_ENTITIES.register(AccessoryRenderer::render);

        ClientPlayNetworking.registerGlobalReceiver(KennelNet.OpenGroomingPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    var client = context.client();
                    if (client.level == null) {
                        return;
                    }
                    if (client.level.getEntity(payload.wolfId()) instanceof Wolf wolf) {
                        client.setScreen(new GroomingScreen(wolf));
                        ClientGroomingSession.begin(wolf);
                    }
                }));
    }
}
