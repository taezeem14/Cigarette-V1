package dev.taezeem14.cigarette.mixin.impl;

import com.google.common.base.Predicates;
import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.Setting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.util.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Shadow
    private Minecraft mc;

    @Shadow
    private Entity pointedEntity;

    @Inject(method = "renderWorldPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand:Z", shift = At.Shift.BEFORE))
    private void onRenderWorldPass(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getEventBus() != null) {
            Cigarette.INSTANCE.getEventBus().post(new Render3DEvent(partialTicks));
        }
    }

    /**
     * @author taezeem14
     * @reason Dynamic reach and hitbox expansion hook
     */
    @Overwrite
    public void getMouseOver(float partialTicks) {
        Entity entity = this.mc.getRenderViewEntity();
        if (entity == null || this.mc.theWorld == null) {
            return;
        }

        this.mc.mcProfiler.startSection("pick");
        this.mc.pointedEntity = null;

        double reachDist = this.mc.playerController != null ? this.mc.playerController.getBlockReachDistance() : 4.5D;
        this.mc.objectMouseOver = entity.rayTrace(reachDist, partialTicks);
        double d1 = reachDist;
        Vec3 eyesVec = entity.getPositionEyes(partialTicks);
        boolean customReach = false;

        double maxEntityReach = 3.0D;
        float hitBoxExpand = 0.0F;

        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            Module reachMod = Cigarette.INSTANCE.getModuleManager().getModuleByName("Reach");
            if (reachMod != null && reachMod.isEnabled()) {
                for (Setting<?> setting : reachMod.getSettings()) {
                    if (setting instanceof NumberSetting) {
                        maxEntityReach = ((NumberSetting) setting).getValue();
                        customReach = true;
                        break;
                    }
                }
            }

            Module hitBoxMod = Cigarette.INSTANCE.getModuleManager().getModuleByName("HitBoxes");
            if (hitBoxMod != null && hitBoxMod.isEnabled()) {
                for (Setting<?> setting : hitBoxMod.getSettings()) {
                    if (setting instanceof NumberSetting) {
                        hitBoxExpand = ((NumberSetting) setting).getValueFloat();
                        break;
                    }
                }
            }
        }

        if (this.mc.playerController != null && this.mc.playerController.extendedReach()) {
            reachDist = 6.0D;
            d1 = 6.0D;
        } else if (customReach) {
            reachDist = Math.max(reachDist, maxEntityReach);
            d1 = reachDist;
        }

        if (this.mc.objectMouseOver != null) {
            d1 = this.mc.objectMouseOver.hitVec.distanceTo(eyesVec);
        }

        Vec3 lookVec = entity.getLook(partialTicks);
        Vec3 reachTargetVec = eyesVec.addVector(lookVec.xCoord * reachDist, lookVec.yCoord * reachDist, lookVec.zCoord * reachDist);
        this.pointedEntity = null;
        Vec3 hitVec = null;

        List<Entity> list = this.mc.theWorld.getEntitiesInAABBexcluding(
                entity,
                entity.getEntityBoundingBox().addCoord(lookVec.xCoord * reachDist, lookVec.yCoord * reachDist, lookVec.zCoord * reachDist).expand(1.0D, 1.0D, 1.0D),
                Predicates.and(EntitySelectors.NOT_SPECTATING, e -> e != null && e.canBeCollidedWith())
        );

        double closestDist = d1;

        for (Entity targetCandidate : list) {
            float borderSize = targetCandidate.getCollisionBorderSize() + hitBoxExpand;
            AxisAlignedBB expandedAABB = targetCandidate.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
            MovingObjectPosition intercept = expandedAABB.calculateIntercept(eyesVec, reachTargetVec);

            if (expandedAABB.isVecInside(eyesVec)) {
                if (closestDist >= 0.0D) {
                    this.pointedEntity = targetCandidate;
                    hitVec = intercept == null ? eyesVec : intercept.hitVec;
                    closestDist = 0.0D;
                }
            } else if (intercept != null) {
                double interceptDist = eyesVec.distanceTo(intercept.hitVec);
                if (interceptDist < closestDist || closestDist == 0.0D) {
                    if (targetCandidate == entity.ridingEntity && !entity.canRiderInteract()) {
                        if (closestDist == 0.0D) {
                            this.pointedEntity = targetCandidate;
                            hitVec = intercept.hitVec;
                        }
                    } else {
                        this.pointedEntity = targetCandidate;
                        hitVec = intercept.hitVec;
                        closestDist = interceptDist;
                    }
                }
            }
        }

        if (this.pointedEntity != null && (closestDist < d1 || this.mc.objectMouseOver == null)) {
            this.mc.objectMouseOver = new MovingObjectPosition(this.pointedEntity, hitVec);
            if (this.pointedEntity instanceof EntityLivingBase || this.pointedEntity instanceof EntityItemFrame) {
                this.mc.pointedEntity = this.pointedEntity;
            }
        }

        this.mc.mcProfiler.endSection();
    }
}
