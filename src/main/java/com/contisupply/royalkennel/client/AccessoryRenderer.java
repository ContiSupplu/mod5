package com.contisupply.royalkennel.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import com.contisupply.royalkennel.DogStyle;
import com.contisupply.royalkennel.KennelAttachments;
import com.contisupply.royalkennel.RoyalKennel;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.Vec3;

/**
 * Renders hats on every dog that has a {@link DogStyle}.
 *
 * Everything here is deliberately low-tech: we anchor to the wolf's real
 * position and head yaw and emit textured cubes straight into the world's
 * entity buffers. No models, no layers, no render states - which keeps it
 * working across Minecraft's frequent renderer refactors.
 *
 * The anchor matches the vanilla wolf model's head pivot (~0.44 blocks in
 * front of the entity center, ~0.66 up). Sitting dogs keep their head at
 * roughly the same spot, so one anchor set covers both poses; only the rare
 * "sprawled flat" idle drifts.
 */
public final class AccessoryRenderer {

    private static final Identifier ATLAS = RoyalKennel.id("textures/entity/accessories.png");
    private static final double MAX_RENDER_DISTANCE_SQ = 64.0 * 64.0;

    // ---- Hand-tuned anchors (blocks, in the wolf's local frame) -----------
    /** Head pivot: forward of the entity center, above the ground. */
    private static final float HEAD_PIVOT_UP = 0.655f;
    private static final float HEAD_PIVOT_FWD = 0.44f;
    /** From the pivot up to the top of the skull, where hat bases sit. */
    private static final float SKULL_TOP = 0.20f;

    // 16x16 cells inside the 64x64 atlas.
    private static final int GOLD = cell(0, 0);
    private static final int IRON = cell(1, 0);
    private static final int PURPLE = cell(2, 0);
    private static final int RED_CLOTH = cell(3, 0);
    private static final int STRAP = cell(1, 1);
    private static final int GOLD_BRIGHT = cell(0, 2);
    private static final int RUBY = cell(1, 2);

    private AccessoryRenderer() {
    }

    private static int cell(int cx, int cy) {
        return cx | (cy << 4);
    }

    public static void render(WorldRenderContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        MultiBufferSource consumers = context.consumers();
        if (consumers == null) {
            return;
        }
        PoseStack poseStack = context.matrices();
        if (poseStack == null) {
            poseStack = new PoseStack();
        }

        Vec3 cameraPos = context.gameRenderer().getMainCamera().position();
        float partial = KennelClock.partialTick();
        VertexConsumer buffer = consumers.getBuffer(RenderTypes.entityCutoutNoCull(ATLAS));

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Wolf wolf) || wolf.isRemoved() || wolf.isInvisible()) {
                continue;
            }
            DogStyle style = wolf.getAttached(KennelAttachments.DOG_STYLE);
            if (style == null || style.hat() == 0) {
                continue;
            }
            if (wolf.distanceToSqr(cameraPos.x, cameraPos.y, cameraPos.z) > MAX_RENDER_DISTANCE_SQ) {
                continue;
            }
            renderHat(poseStack, buffer, wolf, style.hat(), cameraPos, partial);
        }
    }

    private static void renderHat(PoseStack ps, VertexConsumer vc, Wolf wolf, int hat,
                                  Vec3 cam, float partial) {
        double x = Mth.lerp(partial, wolf.xo, wolf.getX()) - cam.x;
        double y = Mth.lerp(partial, wolf.yo, wolf.getY()) - cam.y;
        double z = Mth.lerp(partial, wolf.zo, wolf.getZ()) - cam.z;
        float headYaw = Mth.rotLerp(partial, wolf.yHeadRotO, wolf.yHeadRot);
        float headPitch = Mth.lerp(partial, wolf.xRotO, wolf.getXRot());
        float scale = wolf.isBaby() ? 0.55f : 1.0f;

        ps.pushPose();
        ps.translate(x, y, z);
        ps.mulPose(Axis.YP.rotationDegrees(-headYaw));
        ps.scale(scale, scale, scale);
        ps.translate(0.0f, HEAD_PIVOT_UP, HEAD_PIVOT_FWD); // move to the head pivot
        ps.mulPose(Axis.XP.rotationDegrees(headPitch));    // tilt with the head
        ps.translate(0.0f, SKULL_TOP, 0.0f);               // hat base on top of the skull
        switch (hat) {
            case 1 -> crown(ps, vc);
            case 2 -> mageCap(ps, vc);
            case 3 -> knightHelm(ps, vc);
            default -> {
            }
        }
        ps.popPose();
    }

    // ---- The royal hat rack ------------------------------------------------
    // Local origin: center of the top of the skull; +Z toward the nose.

    private static void crown(PoseStack ps, VertexConsumer vc) {
        // Band wrapping the brow.
        box(ps, vc, 0.0f, 0.038f, 0.145f, 0.30f, 0.075f, 0.026f, GOLD);
        box(ps, vc, 0.0f, 0.038f, -0.145f, 0.30f, 0.075f, 0.026f, GOLD);
        box(ps, vc, 0.145f, 0.038f, 0.0f, 0.026f, 0.075f, 0.30f, GOLD);
        box(ps, vc, -0.145f, 0.038f, 0.0f, 0.026f, 0.075f, 0.30f, GOLD);
        // Points on the corners.
        box(ps, vc, 0.125f, 0.098f, 0.125f, 0.042f, 0.05f, 0.042f, GOLD_BRIGHT);
        box(ps, vc, -0.125f, 0.098f, 0.125f, 0.042f, 0.05f, 0.042f, GOLD_BRIGHT);
        box(ps, vc, 0.125f, 0.098f, -0.125f, 0.042f, 0.05f, 0.042f, GOLD_BRIGHT);
        box(ps, vc, -0.125f, 0.098f, -0.125f, 0.042f, 0.05f, 0.042f, GOLD_BRIGHT);
        // The royal ruby, front and center.
        box(ps, vc, 0.0f, 0.045f, 0.162f, 0.042f, 0.042f, 0.016f, RUBY);
    }

    private static void mageCap(PoseStack ps, VertexConsumer vc) {
        box(ps, vc, 0.0f, 0.010f, 0.0f, 0.37f, 0.022f, 0.37f, PURPLE);     // brim
        box(ps, vc, 0.0f, 0.038f, 0.0f, 0.26f, 0.028f, 0.26f, GOLD);       // hat band
        box(ps, vc, 0.0f, 0.078f, 0.0f, 0.25f, 0.09f, 0.25f, PURPLE);      // cone...
        box(ps, vc, 0.0f, 0.158f, -0.015f, 0.17f, 0.08f, 0.17f, PURPLE);
        box(ps, vc, 0.0f, 0.228f, -0.032f, 0.105f, 0.07f, 0.105f, PURPLE);
        box(ps, vc, 0.0f, 0.288f, -0.05f, 0.055f, 0.055f, 0.055f, PURPLE); // ...to the tip
    }

    private static void knightHelm(PoseStack ps, VertexConsumer vc) {
        box(ps, vc, 0.0f, -0.175f, 0.02f, 0.41f, 0.40f, 0.30f, IRON);      // shell around the head
        box(ps, vc, 0.0f, -0.10f, 0.176f, 0.26f, 0.032f, 0.014f, STRAP);   // visor slit
        box(ps, vc, 0.0f, 0.042f, 0.0f, 0.04f, 0.055f, 0.27f, GOLD);       // crest ridge
        box(ps, vc, 0.0f, 0.10f, -0.10f, 0.042f, 0.115f, 0.042f, RED_CLOTH); // plume
        box(ps, vc, 0.0f, 0.13f, -0.185f, 0.034f, 0.06f, 0.115f, RED_CLOTH); // plume tail
    }

    // ---- Cube plumbing -----------------------------------------------------

    /** Emits an axis-aligned box centered at (cx, cy, cz) with full size (sx, sy, sz). */
    private static void box(PoseStack ps, VertexConsumer vc,
                            float cx, float cy, float cz, float sx, float sy, float sz, int region) {
        float x0 = cx - sx / 2.0f, x1 = cx + sx / 2.0f;
        float y0 = cy - sy / 2.0f, y1 = cy + sy / 2.0f;
        float z0 = cz - sz / 2.0f, z1 = cz + sz / 2.0f;

        // Atlas cell -> UV rect, inset half a pixel to avoid bleeding.
        float inset = 0.5f / 64.0f;
        float u0 = (region & 0xF) * 16.0f / 64.0f + inset;
        float v0 = ((region >> 4) & 0xF) * 16.0f / 64.0f + inset;
        float u1 = u0 + 16.0f / 64.0f - 2.0f * inset;
        float v1 = v0 + 16.0f / 64.0f - 2.0f * inset;

        PoseStack.Pose pose = ps.last();
        // Classic Minecraft face shading, baked into the vertex color.
        face(pose, vc, u0, v0, u1, v1, 1.00f, 0.0f, 1.0f, 0.0f,
                x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0); // up
        face(pose, vc, u0, v0, u1, v1, 0.60f, 0.0f, -1.0f, 0.0f,
                x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1); // down
        face(pose, vc, u0, v0, u1, v1, 0.85f, 0.0f, 0.0f, -1.0f,
                x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0); // north
        face(pose, vc, u0, v0, u1, v1, 0.85f, 0.0f, 0.0f, 1.0f,
                x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1); // south
        face(pose, vc, u0, v0, u1, v1, 0.72f, -1.0f, 0.0f, 0.0f,
                x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0); // west
        face(pose, vc, u0, v0, u1, v1, 0.72f, 1.0f, 0.0f, 0.0f,
                x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1); // east
    }

    private static void face(PoseStack.Pose pose, VertexConsumer vc,
                             float u0, float v0, float u1, float v1, float shade,
                             float nx, float ny, float nz,
                             float ax, float ay, float az,
                             float bx, float by, float bz,
                             float cx, float cy, float cz,
                             float dx, float dy, float dz) {
        int c = (int) (255.0f * shade);
        vertex(pose, vc, ax, ay, az, u0, v0, c, nx, ny, nz);
        vertex(pose, vc, bx, by, bz, u0, v1, c, nx, ny, nz);
        vertex(pose, vc, cx, cy, cz, u1, v1, c, nx, ny, nz);
        vertex(pose, vc, dx, dy, dz, u1, v0, c, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc,
                               float x, float y, float z, float u, float v, int c,
                               float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z)
                .setColor(c, c, c, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }
}
