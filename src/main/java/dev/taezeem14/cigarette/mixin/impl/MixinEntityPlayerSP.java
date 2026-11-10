package dev.taezeem14.cigarette.mixin.impl;

import com.mojang.authlib.GameProfile;
import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.event.EventState;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.event.impl.MoveEvent;
import dev.taezeem14.cigarette.module.impl.blatant.KillAura;
import dev.taezeem14.cigarette.module.impl.blatant.NoSlow;
import dev.taezeem14.cigarette.module.impl.blatant.Scaffold;
import dev.taezeem14.cigarette.module.impl.combat.SilentAim;
import dev.taezeem14.cigarette.util.player.MoveUtil;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MovementInput;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer {

    @Shadow
    public MovementInput movementInput;

    @Shadow
    private boolean serverSprintState;

    @Shadow
    protected int sprintToggleTimer;

    private double cachedX;
    private double cachedY;
    private double cachedZ;
    private float cachedRotationYaw;
    private float cachedRotationPitch;
    private boolean cachedOnGround;
    private boolean spoofedThisTick;

    public MixinEntityPlayerSP(World worldIn, GameProfile gameProfileIn) {
        super(worldIn, gameProfileIn);
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"))
    private void onUpdateWalkingPlayerPre(CallbackInfo ci) {
        if (Cigarette.INSTANCE == null || Cigarette.INSTANCE.isDestructed()) {
            this.spoofedThisTick = false;
            return;
        }

        this.cachedX = this.posX;
        this.cachedY = this.posY;
        this.cachedZ = this.posZ;
        this.cachedRotationYaw = this.rotationYaw;
        this.cachedRotationPitch = this.rotationPitch;
        this.cachedOnGround = this.onGround;
        this.spoofedThisTick = true;

        if (Cigarette.INSTANCE.getEventBus() != null) {
            MotionEvent event = new MotionEvent(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch, this.onGround, EventState.PRE);
            Cigarette.INSTANCE.getEventBus().post(event);

            this.posX = event.getX();
            this.posY = event.getY();
            this.posZ = event.getZ();
            this.rotationYaw = event.getYaw();
            this.rotationPitch = event.getPitch();
            this.onGround = event.isOnGround();

            if (event.getYaw() != this.cachedRotationYaw) {
                this.renderYawOffset = event.getYaw();
                this.rotationYawHead = event.getYaw();
            }
        }
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("RETURN"))
    private void onUpdateWalkingPlayerPost(CallbackInfo ci) {
        if (!this.spoofedThisTick) {
            return;
        }
        this.spoofedThisTick = false;

        this.posX = this.cachedX;
        this.posY = this.cachedY;
        this.posZ = this.cachedZ;
        this.rotationYaw = this.cachedRotationYaw;
        this.rotationPitch = this.cachedRotationPitch;
        this.onGround = this.cachedOnGround;

        if (Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed() && Cigarette.INSTANCE.getEventBus() != null) {
            MotionEvent event = new MotionEvent(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch, this.onGround, EventState.POST);
            Cigarette.INSTANCE.getEventBus().post(event);
        }
    }

    @Override
    public void moveEntity(double x, double y, double z) {
        boolean active = Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed();
        if (active && Cigarette.INSTANCE.getEventBus() != null) {
            MoveEvent event = new MoveEvent(x, y, z);
            Cigarette.INSTANCE.getEventBus().post(event);
            if (event.isCancelled()) {
                return;
            }
            x = event.getX();
            y = event.getY();
            z = event.getZ();
        }

        if (active && !this.noClip && this.onGround && !this.isSneaking()
                && Scaffold.getInstance() != null && Scaffold.getInstance().isSafeWalk()) {
            double step = 0.05D;
            while (x != 0.0D && this.worldObj.getCollidingBoundingBoxes(this, this.getEntityBoundingBox().offset(x, -1.0D, 0.0D)).isEmpty()) {
                if (x < step && x >= -step) {
                    x = 0.0D;
                } else if (x > 0.0D) {
                    x -= step;
                } else {
                    x += step;
                }
            }
            while (z != 0.0D && this.worldObj.getCollidingBoundingBoxes(this, this.getEntityBoundingBox().offset(0.0D, -1.0D, z)).isEmpty()) {
                if (z < step && z >= -step) {
                    z = 0.0D;
                } else if (z > 0.0D) {
                    z -= step;
                } else {
                    z += step;
                }
            }
            while (x != 0.0D && z != 0.0D && this.worldObj.getCollidingBoundingBoxes(this, this.getEntityBoundingBox().offset(x, -1.0D, z)).isEmpty()) {
                if (x < step && x >= -step) {
                    x = 0.0D;
                } else if (x > 0.0D) {
                    x -= step;
                } else {
                    x += step;
                }
                if (z < step && z >= -step) {
                    z = 0.0D;
                } else if (z > 0.0D) {
                    z -= step;
                } else {
                    z += step;
                }
            }
        }

        super.moveEntity(x, y, z);
    }

    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V", shift = At.Shift.AFTER))
    private void onUpdateMoveState(CallbackInfo ci) {
        if (Cigarette.INSTANCE == null || Cigarette.INSTANCE.isDestructed()) {
            return;
        }

        float[] killAuraRots = KillAura.getActiveRotations();
        if (killAuraRots != null) {
            MoveUtil.fixMovement(killAuraRots[0], false);
            return;
        }

        float[] silentAimRots = SilentAim.getActiveRotations();
        if (silentAimRots != null && SilentAim.isMovementFixEnabled()) {
            MoveUtil.fixMovement(silentAimRots[0], SilentAim.isStrictMovementFix());
        }
    }

    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;pushOutOfBlocks(DDD)Z", ordinal = 0, shift = At.Shift.BEFORE))
    private void onLivingUpdateNoSlow(CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed()
                && NoSlow.shouldCancelSlowdown() && this.isUsingItem() && !this.isRiding()) {
            if (this.movementInput != null) {
                this.movementInput.moveForward *= 5.0F;
                this.movementInput.moveStrafe *= 5.0F;
            }
        }
    }
}
