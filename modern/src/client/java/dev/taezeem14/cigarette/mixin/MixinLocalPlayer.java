package dev.taezeem14.cigarette.mixin;

import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.event.EventState;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.event.impl.MoveEvent;
import dev.taezeem14.cigarette.module.impl.blatant.NoSlow;
import dev.taezeem14.cigarette.module.impl.blatant.Scaffold;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {

    @Unique private float cigarette$prevYaw;
    @Unique private float cigarette$prevPitch;
    @Unique private float cigarette$spoofedYaw;

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void onSendPositionPre(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        this.cigarette$prevYaw = self.getYRot();
        this.cigarette$prevPitch = self.getXRot();
        this.cigarette$spoofedYaw = this.cigarette$prevYaw;

        CigaretteModernClient inst = CigaretteModernClient.getInstance();
        if (inst != null && !inst.isDestructed() && inst.getEventBus() != null) {
            MotionEvent event = new MotionEvent(self.getX(), self.getY(), self.getZ(),
                    this.cigarette$prevYaw, this.cigarette$prevPitch, self.onGround(), EventState.PRE);
            inst.getEventBus().post(event);
            self.setYRot(event.getYaw());
            self.setXRot(event.getPitch());
            this.cigarette$spoofedYaw = event.getYaw();
        }
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void onSendPositionPost(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        self.setYRot(this.cigarette$prevYaw);
        self.setXRot(this.cigarette$prevPitch);
        if (this.cigarette$spoofedYaw != this.cigarette$prevYaw) {
            self.yHeadRot = this.cigarette$spoofedYaw;
            self.yBodyRot = this.cigarette$spoofedYaw;
        }
        CigaretteModernClient inst = CigaretteModernClient.getInstance();
        if (inst != null && !inst.isDestructed() && inst.getEventBus() != null) {
            MotionEvent event = new MotionEvent(self.getX(), self.getY(), self.getZ(),
                    self.getYRot(), self.getXRot(), self.onGround(), EventState.POST);
            inst.getEventBus().post(event);
        }
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 onMove(Vec3 vec) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        CigaretteModernClient inst = CigaretteModernClient.getInstance();
        if (inst == null || inst.isDestructed()) {
            return vec;
        }

        double x = vec.x;
        double y = vec.y;
        double z = vec.z;

        if (inst.getEventBus() != null) {
            MoveEvent event = new MoveEvent(x, y, z);
            inst.getEventBus().post(event);
            if (event.isCancelled()) {
                return Vec3.ZERO;
            }
            x = event.getX();
            y = event.getY();
            z = event.getZ();
        }

        if (!self.noPhysics && self.onGround() && !self.isShiftKeyDown()
                && Scaffold.getInstance() != null && Scaffold.getInstance().isSafeWalk()) {
            double step = 0.05;
            double drop = -self.maxUpStep() - 0.5;
            while (x != 0.0 && self.level().noCollision(self, self.getBoundingBox().move(x, drop, 0.0))) {
                if (x < step && x >= -step) x = 0.0;
                else if (x > 0.0) x -= step;
                else x += step;
            }
            while (z != 0.0 && self.level().noCollision(self, self.getBoundingBox().move(0.0, drop, z))) {
                if (z < step && z >= -step) z = 0.0;
                else if (z > 0.0) z -= step;
                else z += step;
            }
            while (x != 0.0 && z != 0.0 && self.level().noCollision(self, self.getBoundingBox().move(x, drop, z))) {
                if (x < step && x >= -step) x = 0.0;
                else if (x > 0.0) x -= step;
                else x += step;
                if (z < step && z >= -step) z = 0.0;
                else if (z > 0.0) z -= step;
                else z += step;
            }
        }
        return new Vec3(x, y, z);
    }

    @Inject(method = "isSlowDueToUsingItem", at = @At("HEAD"), cancellable = true)
    private void onIsSlowDueToUsingItem(CallbackInfoReturnable<Boolean> cir) {
        if (CigaretteModernClient.getInstance() != null && !CigaretteModernClient.getInstance().isDestructed()
                && NoSlow.shouldCancelSlowdown()) {
            cir.setReturnValue(false);
        }
    }
}
