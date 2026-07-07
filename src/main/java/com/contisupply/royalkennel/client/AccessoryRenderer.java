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
 * Renders hats and back gear on every dog that has a {@link DogStyle}.
 *
 * Everything here is deliberately low-tech: we anchor to the wolf's real
 * position/eye height/head yaw and emit textured cubes straight into the
 * world's entity buffers. No models, no layers, no render states - which
 * keeps it working across Minecraft's frequent renderer refactors, at the
 * cost of accessories not following the tiniest idle head animations.
 * Every offset below is hand-tuned and safe to tweak.
 */
public final class AccessoryRenderer {

    private static final Identifier ATLAS = RoyalKennel.id("textures/entity/accessories.png");
    private static final double MAX_RENDER_DISTANCE_SQ = 64.0 * 64.0;

    // 16x16 cells inside the 64x64 atlas.
    private static final int GOLD = cell(0, 0);
    private static final int IRON = cell(1, 0);
    private static final int PURPLE = cell(2, 0);
    private static final int RED_CLOTH = cell(3, 0);
    private static final int LEATHER = cell(0, 1);
    private static final int STRAP = cell(1, 1);
    private static final int BARREL = cell(2, 1);
    private static final int BEDROLL = cell(3, 1);
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

        Vec3 cameraPos = context.gameRenderer().getMainCamera().getPosition();
        float partial = KennelClock.partialTick();
        VertexConsumer buffer = consumers.getBuffer(RenderTypes.entityCutoutNoCull(ATLAS));

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Wolf wolf) || wolf.isRemoved() || wolf.isInvisible()) {
                continue;
            }
            DogStyle style = wolf.getAttached(KennelAttachments.DOG_STYLE);
            if (style == null || style.isPlain()) {
                continue;
            }
            if (wolf.distanceToSqr(cameraPos.x, cameraPos.y, cameraPos.z) > MAX_RENDER_DISTANCE_SQ) {
                continue;
            }
            renderWolf(poseStack, buffer, wolf, style, cameraPos, partial);
        }
    }

    private static void renderWolf(PoseStack ps, VertexConsumer vc, Wolf wolf, DogStyle style,
                                   Vec3 cam, float partial) {
        double x = Mth.lerp(partial, wolf.xo, wolf.getX()) - cam.x;
        double y = Mth.lerp(partial, wolf.yo, wolf.getY()) - cam.y;
        double z = Mth.lerp(partial, wolf.zo, wolf.getZ()) - cam.z;
        float bodyYaw = Mth.rotLerp(partial, wolf.yBodyRotO, wolf.yBodyRot);
        float headYaw = Mth.rotLerp(partial, wolf.yHeadRotO, wolf.yHeadRot);
        float headPitch = Mth.lerp(partial, wolf.xRotO, wolf.getXRot());
        boolean sitting = wolf.isInSittingPose();
        float scale = wolf.isBaby() ? 0.55f : 1.0f;
        double eyeHeight = wolf.getEyeY() - wolf.getY();

        if (style.hat() != 0) {
            ps.pushPose();
            ps.translate(x, y + eyeHeight, z);
            ps.mulPose(Axis.YP.rotationDegrees(-headYaw));
            ps.mulPose(Axis.XP.rotationDegrees(headPitch));
            ps.scale(scale, scale, scale);
            // Hat base plane just above the skull; sitting dogs carry the head lower.
            ps.translate(0.0f, sitting ? 0.05f : 0.11f, 0.02f);
            switch (style.hat()) {
                case 1 -> crown(ps, vc);
                case 2 -> mageCap(ps, vc);
                case 3 -> knightHelm(ps, vc);
                default -> {
                }
            }
            ps.popPose();
        }

        if (style.back() != 0) {
            ps.pushPose();
            ps.translate(x, y, z);
            ps.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            ps.scale(scale, scale, scale);
            if (sitting) {
                ps.translate(0.0f, 0.37f, -0.06f);
                ps.mulPose(Axis.XP.rotationDegrees(-33.0f)); // chest rises when sitting
            } else {
                ps.translate(0.0f, 0.51f, -0.02f);
            }
            switch (style.back()) {
                case 1 -> backpack(ps, vc);
                case 2 -> keg(ps, vc);
                case 3 -> cape(ps, vc);
                default -> {
                }
            }
            ps.popPose();
        }
    }

    // ---- The wardrobe ------------------------------------------------------

    private static void crown(PoseStack ps, VertexConsumer vc) {
        box(ps, vc, 0.0f, 0.035f, 0.095f, 0.21f, 0.07f, 0.022f, GOLD);
        box(ps, vc, 0.0f, 0.035f, -0.095f, 0.21f, 0.07f, 0.022f, GOLD);
        box(ps, vc, 0.095f, 0.035f, 0.0f, 0.022f, 0.07f, 0.21f, GOLD);
        box(ps, vc, -0.095f, 0.035f, 0.0f, 0.022f, 0.07f, 0.21f, GOLD);
        box(ps, vc, 0.085f, 0.085f, 0.085f, 0.036f, 0.036f, 0.036f, GOLD_BRIGHT);
        box(ps, vc, -0.085f, 0.085f, 0.085f, 0.036f, 0.036f, 0.036f, GOLD_BRIGHT);
        box(ps, vc, 0.085f, 0.085f, -0.085f, 0.036f, 0.036f, 0.036f, GOLD_BRIGHT);
        box(ps, vc, -0.085f, 0.085f, -0.085f, 0.036f, 0.036f, 0.036f, GOLD_BRIGHT);
        box(ps, vc, 0.0f, 0.04f, 0.11f, 0.032f, 0.032f, 0.014f, RUBY);
    }

    private static void mageCap(PoseStack ps, VertexConsumer vc) {
        box(ps, vc, 0.0f, 0.008f, 0.0f, 0.30f, 0.018f, 0.30f, PURPLE);
        box(ps, vc, 0.0f, 0.03f, 0.0f, 0.20f, 0.024f, 0.20f, GOLD);
        box(ps, vc, 0.0f, 0.06f, 0.0f, 0.19f, 0.08f, 0.19f, PURPLE);
        box(ps, vc, 0.0f, 0.13f, -0.012f, 0.125f, 0.07f, 0.125f, PURPLE);
        box(ps, vc, 0.0f, 0.193f, -0.026f, 0.078f, 0.065f, 0.078f, PURPLE);
        box(ps, vc, 0.0f, 0.248f, -0.042f, 0.04f, 0.05f, 0.04f, PURPLE);
    }

    private static void knightHelm(PoseStack ps, VertexConsumer vc) {
        box(ps, vc, 0.0f, -0.05f, 0.01f, 0.235f, 0.20f, 0.24f, IRON);
        box(ps, vc, 0.0f, -0.035f, 0.132f, 0.16f, 0.022f, 0.012f, STRAP);
        box(ps, vc, 0.0f, 0.062f, 0.0f, 0.03f, 0.05f, 0.20f, GOLD);
        box(ps, vc, 0.0f, 0.115f, -0.06f, 0.035f, 0.10f, 0.035f, RED_CLOTH);
        box(ps, vc, 0.0f, 0.148f, -0.125f, 0.028f, 0.05f, 0.10f, RED_CLOTH);
    }

    private static void backpack(PoseStack ps, VertexConsumer vc) {
        box(ps, vc, 0.0f, 0.09f, -0.02f, 0.24f, 0.18f, 0.13f, LEATHER);
        box(ps, vc, 0.0f, 0.185f, -0.02f, 0.25f, 0.04f, 0.14f, STRAP);
        box(ps, vc, 0.0f, 0.235f, -0.02f, 0.28f, 0.09f, 0.10f, BEDROLL);
        box(ps, vc, 0.127f, 0.05f, -0.02f, 0.016f, 0.16f, 0.05f, STRAP);
        box(ps, vc, -0.127f, 0.05f, -0.02f, 0.016f, 0.16f, 0.05f, STRAP);
    }

    private static void keg(PoseStack ps, VertexConsumer vc) {
        // The little rescue barrel rides at the chest, St. Bernard style.
        box(ps, vc, 0.0f, -0.06f, 0.30f, 0.15f, 0.13f, 0.13f, BARREL);
        box(ps, vc, 0.0f, 0.02f, 0.30f, 0.02f, 0.05f, 0.10f, STRAP);
    }

    private static void cape(PoseStack ps, VertexConsumer vc) {
        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees(-8.0f)); // drape toward the tail
        box(ps, vc, 0.0f, 0.10f, -0.12f, 0.26f, 0.02f, 0.32f, RED_CLOTH);
        box(ps, vc, 0.0f, 0.10f, -0.29f, 0.27f, 0.026f, 0.05f, GOLD_BRIGHT);
        ps.popPose();
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
