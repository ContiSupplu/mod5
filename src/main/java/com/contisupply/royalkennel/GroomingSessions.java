package com.contisupply.royalkennel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.DyeColor;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Server-side bookkeeping for players currently grooming a dog.
 *
 * While a session is open the dog is ordered to sit so it cannot wander away
 * from the camera; its previous sitting preference is restored when the
 * session ends.
 */
public final class GroomingSessions {

    private record Session(int wolfId, boolean wasSitting) {
    }

    private static final Map<UUID, Session> ACTIVE = new HashMap<>();
    private static final double MAX_DISTANCE = 16.0;
    private static final int MAX_NAME_LENGTH = 24;

    private GroomingSessions() {
    }

    public static void open(ServerPlayer player, Wolf wolf) {
        // Re-opening while a session is somehow live: tidy up the old one first.
        close(player, false);

        ACTIVE.put(player.getUUID(), new Session(wolf.getId(), wolf.isOrderedToSit()));
        wolf.setOrderedToSit(true);
        wolf.getNavigation().stop();
        ServerPlayNetworking.send(player, new KennelNet.OpenGroomingPayload(wolf.getId()));
    }

    public static void applyUpdate(ServerPlayer player, KennelNet.UpdateDogPayload update) {
        Session session = ACTIVE.get(player.getUUID());
        if (session == null || session.wolfId() != update.wolfId()) {
            return;
        }
        Wolf wolf = resolveWolf(player, session);
        if (wolf == null) {
            return;
        }

        boolean lookChanged = false;

        // Bloodline (wolf variant), sent as a registry id so datapack variants work too.
        ResourceLocation variantId = ResourceLocation.tryParse(update.variantId());
        if (variantId != null && !wolf.getVariant().is(variantId)) {
            var registry = player.level().registryAccess().lookupOrThrow(Registries.WOLF_VARIANT);
            var holder = registry.get(ResourceKey.create(Registries.WOLF_VARIANT, variantId));
            if (holder.isPresent()) {
                wolf.setVariant(holder.get());
                lookChanged = true;
            }
        }

        // Headwear + back gear.
        DogStyle newStyle = DogStyle.clamped(update.hat(), update.back());
        DogStyle oldStyle = wolf.getAttached(KennelAttachments.DOG_STYLE);
        if (!newStyle.equals(oldStyle == null ? DogStyle.PLAIN : oldStyle)) {
            wolf.setAttached(KennelAttachments.DOG_STYLE, newStyle);
            lookChanged = true;
        }

        // Collar hue.
        DyeColor collar = DyeColor.byId(Math.floorMod(update.collar(), 16));
        if (wolf.getCollarColor() != collar) {
            wolf.setCollarColor(collar);
            lookChanged = true;
        }

        // Title & name (kept visible above the dog for the camera).
        String name = update.name().strip();
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH);
        }
        String current = wolf.getCustomName() == null ? "" : wolf.getCustomName().getString();
        if (!current.equals(name)) {
            wolf.setCustomName(name.isEmpty() ? null : Component.literal(name));
            wolf.setCustomNameVisible(!name.isEmpty());
        }

        if (lookChanged && player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    wolf.getX(), wolf.getY() + 0.6, wolf.getZ(),
                    4, 0.35, 0.3, 0.35, 0.0);
        }
    }

    public static void close(ServerPlayer player, boolean sealed) {
        Session session = ACTIVE.remove(player.getUUID());
        if (session == null) {
            return;
        }
        Wolf wolf = resolveWolf(player, session);
        if (wolf == null) {
            return;
        }
        wolf.setOrderedToSit(session.wasSitting());
        if (sealed && player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                    wolf.getX(), wolf.getY() + 0.7, wolf.getZ(),
                    7, 0.45, 0.35, 0.45, 0.02);
        }
    }

    public static void onDisconnect(ServerPlayer player) {
        close(player, false);
    }

    private static Wolf resolveWolf(ServerPlayer player, Session session) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        if (!(serverLevel.getEntity(session.wolfId()) instanceof Wolf wolf)) {
            return null;
        }
        if (!wolf.isAlive() || !wolf.isOwnedBy(player) || player.distanceTo(wolf) > MAX_DISTANCE) {
            return null;
        }
        return wolf;
    }
}
