package com.contisupply.royalkennel.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.contisupply.royalkennel.client.ClientGroomingSession;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

/**
 * After vanilla finishes positioning the camera each frame, we take over
 * while a grooming session is active and park the camera on a slow cinematic
 * orbit around the dog.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public abstract float yRot();

    @Shadow
    public abstract float xRot();

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"))
    private void royalkennel$focusOnHound(CallbackInfo ci) {
        ClientGroomingSession.CameraPose pose =
                ClientGroomingSession.solve(this.position(), this.yRot(), this.xRot());
        if (pose != null) {
            this.setRotation(pose.yaw(), pose.pitch());
            this.setPosition(pose.x(), pose.y(), pose.z());
        }
    }
}
