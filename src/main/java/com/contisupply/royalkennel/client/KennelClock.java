package com.contisupply.royalkennel.client;

import net.minecraft.util.Mth;

/**
 * A tiny self-contained frame clock.
 *
 * We track when the last client tick finished and derive our own partial-tick
 * value from wall time. This keeps the accessory renderer and the cinematic
 * camera perfectly smooth without depending on any particular partial-tick
 * accessor in the rendering pipeline.
 */
public final class KennelClock {

    private static volatile long lastTickNanos = System.nanoTime();

    private KennelClock() {
    }

    /** Called at END_CLIENT_TICK. */
    public static void onClientTick() {
        lastTickNanos = System.nanoTime();
    }

    /** Fraction [0..1] of the way from the last client tick to the next one. */
    public static float partialTick() {
        float partial = (System.nanoTime() - lastTickNanos) / 50_000_000.0f;
        return Mth.clamp(partial, 0.0f, 1.0f);
    }

    public static double secondsSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000_000.0;
    }
}
