package dev.taezeem14.cigarette.mixin.impl;

import com.mojang.authlib.GameProfile;
import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.event.EventState;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.module.Module;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovementInput;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends EntityPlayer {

    @Shadow
    public MovementInput movementInput;

    private double cachedX;
    private double cachedY;
    private double cachedZ;
    private float cachedRotationYaw;
    private float cachedRotationPitch;
    private boolean cachedOnGround;

    public MixinEntityPlayerSP(World worldIn, GameProfile gameProfileIn) {
        super(worldIn, gameProfileIn);
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"))
    private void onUpdateWalkingPlayerPre(CallbackInfo ci) {
        this.cachedX = this.posX;
        this.cachedY = this.posY;
        this.cachedZ = this.posZ;
        this.cachedRotationYaw = this.rotationYaw;
        this.cachedRotationPitch = this.rotationPitch;
        this.cachedOnGround = this.onGround;

        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getEventBus() != null) {
            MotionEvent event = new MotionEvent(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch, this.onGround, EventState.PRE);
            Cigarette.INSTANCE.getEventBus().post(event);

            this.posX = event.getX();
            this.posY = event.getY();
            this.posZ = event.getZ();
            this.rotationYaw = event.getYaw();
            this.rotationPitch = event.getPitch();
            this.onGround = event.isOnGround();
        }
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("RETURN"))
    private void onUpdateWalkingPlayerPost(CallbackInfo ci) {
        this.posX = this.cachedX;
        this.posY = this.cachedY;
        this.posZ = this.cachedZ;
        this.rotationYaw = this.cachedRotationYaw;
        this.rotationPitch = this.cachedRotationPitch;
        this.onGround = this.cachedOnGround;

        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getEventBus() != null) {
            MotionEvent event = new MotionEvent(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch, this.onGround, EventState.POST);
            Cigarette.INSTANCE.getEventBus().post(event);
        }
    }

    @Inject(method = "onLivingUpdate", at = @At("RETURN"))
    private void onLivingUpdateNoSlow(CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            Module noSlow = Cigarette.INSTANCE.getModuleManager().getModuleByName("NoSlow");
            if (noSlow != null && noSlow.isEnabled() && this.isUsingItem() && !this.isRiding()) {
                if (this.movementInput != null) {
                    this.movementInput.moveForward *= 5.0F;
                    this.movementInput.moveStrafe *= 5.0F;
                }
            }
        }
    }
}
