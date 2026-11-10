package dev.taezeem14.cigarette.mixin.impl;

import com.google.common.base.Predicates;
import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.module.impl.combat.HitBoxes;
import dev.taezeem14.cigarette.module.impl.combat.Reach;
import dev.taezeem14.cigarette.module.impl.combat.SilentAim;
import dev.taezeem14.cigarette.util.player.RaycastUtil;
import dev.taezeem14.cigarette.util.render.FrustumUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
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
        if (Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed()) {
            FrustumUtil.updateFrustum();
            if (Cigarette.INSTANCE.getEventBus() != null) {
                Cigarette.INSTANCE.getEventBus().post(new Render3DEvent(partialTicks));
            }
        }
    }

    /**
     * @author taezeem14
     * @reason Dynamic reach, hitbox expansion, and silent aim raytrace integration
     */
    @Overwrite
    public void getMouseOver(float partialTicks) {
        Entity entity = this.mc.getRenderViewEntity();
        if (entity != null && this.mc.theWorld != null) {
            this.mc.mcProfiler.startSection("pick");
            this.mc.pointedEntity = null;

            boolean active = Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed();
            Reach reachModule = active ? Reach.getInstance() : null;
            boolean reachEnabled = reachModule != null && reachModule.isEnabled();
            boolean hitThroughWalls = reachEnabled && reachModule.canHitThroughWalls();
            double customReach = active ? Reach.getReachDistance() : 3.0D;
            float hitBoxExpand = active ? HitBoxes.getExpansionSize() : 0.0F;
            float[] silentRotations = active ? SilentAim.getActiveRotations() : null;

            double blockReach = this.mc.playerController != null ? this.mc.playerController.getBlockReachDistance() : 4.5D;
            this.mc.objectMouseOver = entity.rayTrace(blockReach, partialTicks);
            double d0 = blockReach;
            double d1 = d0;
            Vec3 vec3 = entity.getPositionEyes(partialTicks);
            boolean flag = false;
            double maxEntityReach = 3.0D;

            if (this.mc.playerController != null && this.mc.playerController.extendedReach()) {
                d0 = 6.0D;
                d1 = 6.0D;
                maxEntityReach = 6.0D;
            } else {
                maxEntityReach = Math.max(3.0D, customReach);
                d0 = Math.max(d0, maxEntityReach);
                d1 = d0;
                if (d0 > maxEntityReach) {
                    flag = true;
                }
            }

            Vec3 vec31 = (silentRotations != null && entity == this.mc.thePlayer)
                    ? RaycastUtil.getVectorForRotation(silentRotations[1], silentRotations[0])
                    : entity.getLook(partialTicks);

            if (!hitThroughWalls) {
                if (silentRotations != null && entity == this.mc.thePlayer) {
                    Vec3 blockTarget = vec3.addVector(vec31.xCoord * blockReach, vec31.yCoord * blockReach, vec31.zCoord * blockReach);
                    MovingObjectPosition silentBlockHit = this.mc.theWorld.rayTraceBlocks(vec3, blockTarget, false, false, true);
                    if (silentBlockHit != null) {
                        d1 = silentBlockHit.hitVec.distanceTo(vec3);
                    }
                } else if (this.mc.objectMouseOver != null) {
                    d1 = this.mc.objectMouseOver.hitVec.distanceTo(vec3);
                }
            }

            Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
            this.pointedEntity = null;
            Vec3 vec33 = null;
            float f = 1.0F;

            List<Entity> list = this.mc.theWorld.getEntitiesInAABBexcluding(
                    entity,
                    entity.getEntityBoundingBox().addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0).expand(f, f, f),
                    Predicates.and(EntitySelectors.NOT_SPECTATING, candidate -> candidate != null && candidate.canBeCollidedWith())
            );

            double d2 = d1;

            for (Entity entity1 : list) {
                float borderSize = entity1.getCollisionBorderSize() + hitBoxExpand;
                AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
                MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);

                if (axisalignedbb.isVecInside(vec3)) {
                    if (d2 >= 0.0D) {
                        this.pointedEntity = entity1;
                        vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                        d2 = 0.0D;
                    }
                } else if (movingobjectposition != null) {
                    double d3 = vec3.distanceTo(movingobjectposition.hitVec);
                    if (d3 < d2 || d2 == 0.0D) {
                        if (entity1 == entity.ridingEntity && !entity.canRiderInteract()) {
                            if (d2 == 0.0D) {
                                this.pointedEntity = entity1;
                                vec33 = movingobjectposition.hitVec;
                            }
                        } else {
                            this.pointedEntity = entity1;
                            vec33 = movingobjectposition.hitVec;
                            d2 = d3;
                        }
                    }
                }
            }

            if (this.pointedEntity != null && flag && (vec33 == null || vec3.distanceTo(vec33) > maxEntityReach)) {
                this.pointedEntity = null;
                if (silentRotations == null && vec33 != null) {
                    this.mc.objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec33, null, new BlockPos(vec33));
                }
            }

            if (this.pointedEntity != null && (d2 < d1 || this.mc.objectMouseOver == null)) {
                this.mc.objectMouseOver = new MovingObjectPosition(this.pointedEntity, vec33);
                if (this.pointedEntity instanceof EntityLivingBase || this.pointedEntity instanceof EntityItemFrame) {
                    this.mc.pointedEntity = this.pointedEntity;
                }
            }

            this.mc.mcProfiler.endSection();
        }
    }
}
