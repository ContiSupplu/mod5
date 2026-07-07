package com.contisupply.royalkennel.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side state for the active grooming session, including all of the
 * cinematic camera math. The actual camera override happens in
 * {@link com.contisupply.royalkennel.mixin.client.CameraMixin}, which asks
 * this class for a pose every frame.
 */
public final class ClientGroomingSession {

    /** Result of the camera solve for one frame. */
    public record CameraPose(double x, double y, double z, float yaw, float pitch) {
    }

    // ---- Cinematography constants (tweak freely) --------------------------
    /** Distance from the dog, in blocks. */
    private static final double ORBIT_DISTANCE = 2.8;
    /** Camera height above the dog's feet. */
    private static final double ORBIT_HEIGHT = 0.9;
    /** Point on the dog the camera looks at (above feet). */
    private static final double LOOK_AT_HEIGHT = 0.45;
    /** Degrees per second of the slow orbit. */
    private static final double ORBIT_SPEED_DEG = 9.0;
    /**
     * Extra yaw so the dog sits right-of-center, clear of the left panel.
     * If your dog drifts the wrong way, flip the sign.
     */
    private static final float FRAME_YAW_OFFSET = -11.0f;
    /** Seconds for the glide from the player's view to the orbit. */
    private static final double GLIDE_SECONDS = 0.9;
    // -----------------------------------------------------------------------

    private static Wolf wolf;
    private static boolean active;
    private static long startNanos;
    private static float baseAngleDeg;
    private static CameraType previousCameraType;

    private static boolean glideCaptured;
    private static Vec3 glideFromPos = Vec3.ZERO;
    private static float glideFromYaw;
    private static float glideFromPitch;

    private ClientGroomingSession() {
    }

    public static void begin(Wolf target) {
        Minecraft minecraft = Minecraft.getInstance();
        wolf = target;
        active = true;
        startNanos = System.nanoTime();
        // Start the orbit in front of the dog's face.
        baseAngleDeg = target.yBodyRot;
        glideCaptured = false;
        previousCameraType = minecraft.options.getCameraType();
        // Third person = no first-person hand in frame while we steer the camera.
        minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    public static void end() {
        if (!active) {
            return;
        }
        active = false;
        wolf = null;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.setCameraType(previousCameraType == null ? CameraType.FIRST_PERSON : previousCameraType);
        previousCameraType = null;
    }

    public static boolean isActive() {
        return active;
    }

    public static Wolf wolf() {
        return wolf;
    }

    /**
     * Solves the camera for this frame, or returns null when the vanilla
     * camera should be left alone. Called from the Camera mixin with the
     * camera's current (vanilla-computed) pose so the first frames can glide
     * away from the player's own point of view.
     */
    public static CameraPose solve(Vec3 currentPos, float currentYaw, float currentPitch) {
        if (!active || wolf == null || wolf.isRemoved()) {
            return null;
        }

        if (!glideCaptured) {
            glideCaptured = true;
            glideFromPos = currentPos;
            glideFromYaw = currentYaw;
            glideFromPitch = currentPitch;
        }

        float partial = KennelClock.partialTick();
        double dogX = Mth.lerp(partial, wolf.xo, wolf.getX());
        double dogY = Mth.lerp(partial, wolf.yo, wolf.getY());
        double dogZ = Mth.lerp(partial, wolf.zo, wolf.getZ());

        double t = KennelClock.secondsSince(startNanos);
        double angleRad = Math.toRadians(baseAngleDeg + t * ORBIT_SPEED_DEG);

        // Orbit point around the dog. (-sin, cos) is Minecraft's yaw->forward mapping,
        // so angle == body yaw puts the camera directly in front of the dog's face.
        double camX = dogX - Math.sin(angleRad) * ORBIT_DISTANCE;
        double camY = dogY + ORBIT_HEIGHT;
        double camZ = dogZ + Math.cos(angleRad) * ORBIT_DISTANCE;

        // Aim at the dog.
        double lookX = dogX - camX;
        double lookY = (dogY + LOOK_AT_HEIGHT) - camY;
        double lookZ = dogZ - camZ;
        double horizontal = Math.sqrt(lookX * lookX + lookZ * lookZ);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-lookX, lookZ)) + FRAME_YAW_OFFSET;
        float targetPitch = (float) Math.toDegrees(-Math.atan2(lookY, horizontal));

        // Ease-in glide from wherever the player was looking.
        float k = (float) Mth.clamp(t / GLIDE_SECONDS, 0.0, 1.0);
        k = k * k * (3.0f - 2.0f * k); // smoothstep

        double x = Mth.lerp(k, glideFromPos.x, camX);
        double y = Mth.lerp(k, glideFromPos.y, camY);
        double z = Mth.lerp(k, glideFromPos.z, camZ);
        float yaw = Mth.rotLerp(k, glideFromYaw, targetYaw);
        float pitch = Mth.lerp(k, glideFromPitch, targetPitch);

        return new CameraPose(x, y, z, yaw, pitch);
    }
}
