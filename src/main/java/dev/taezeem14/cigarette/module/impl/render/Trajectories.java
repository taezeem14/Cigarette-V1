package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.*;
import net.minecraft.util.*;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class Trajectories extends Module {

    private final ColorSetting color = new ColorSetting("Color", 0xFF3882F6);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.5, 0.5, 3.0, 0.1);
    private final BooleanSetting hitBox = new BooleanSetting("Landing Box", true);

    public Trajectories() {
        super("Trajectories", "Simulates and renders projectile flight paths", Category.RENDER);
        addSettings(color, lineWidth, hitBox);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        ItemStack stack = mc.thePlayer.getCurrentEquippedItem();
        if (stack == null) return;

        Item item = stack.getItem();
        boolean isBow = item instanceof ItemBow;
        boolean isPearl = item instanceof ItemEnderPearl;
        boolean isSnowball = item instanceof ItemSnowball;
        boolean isEgg = item instanceof ItemEgg;
        boolean isPotion = item instanceof ItemPotion && ItemPotion.isSplash(stack.getMetadata());

        if (!isBow && !isPearl && !isSnowball && !isEgg && !isPotion) return;

        float partialTicks = event.getPartialTicks();
        RenderManager rm = mc.getRenderManager();

        double renderPosX = rm.viewerPosX;
        double renderPosY = rm.viewerPosY;
        double renderPosZ = rm.viewerPosZ;

        float yaw = mc.thePlayer.rotationYaw;
        float pitch = mc.thePlayer.rotationPitch;

        double posX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks
                - Math.cos(Math.toRadians(yaw)) * 0.16;
        double posY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks
                + mc.thePlayer.getEyeHeight() - 0.1;
        double posZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks
                - Math.sin(Math.toRadians(yaw)) * 0.16;

        float velocity;
        float gravity;
        float drag = 0.99F;

        if (isBow) {
            int duration = mc.thePlayer.getItemInUseDuration();
            float power = (float) duration / 20.0F;
            power = (power * power + power * 2.0F) / 3.0F;
            if (power < 0.1F) {
                power = 1.0F;
            }
            if (power > 1.0F) {
                power = 1.0F;
            }
            velocity = power * 2.0F * 1.5F;
            gravity = 0.05F;
        } else if (isPotion) {
            velocity = 0.4F * 1.25F;
            gravity = 0.05F;
        } else {
            velocity = 0.4F * 1.5F;
            gravity = 0.03F;
        }

        float motionX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
        float motionY = -MathHelper.sin((pitch - (isPotion ? 20.0F : 0.0F)) / 180.0F * (float) Math.PI);
        float motionZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);

        float distance = MathHelper.sqrt_float(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX = (motionX / distance) * velocity;
        motionY = (motionY / distance) * velocity;
        motionZ = (motionZ / distance) * velocity;

        List<Vec3> points = new ArrayList<>();
        MovingObjectPosition landingPosition = null;

        for (int i = 0; i < 150; i++) {
            Vec3 currentPos = new Vec3(posX, posY, posZ);
            Vec3 nextPos = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);

            MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(currentPos, nextPos, false, true, false);
            if (mop != null) {
                landingPosition = mop;
                points.add(mop.hitVec);
                break;
            }

            // Entity collision check
            AxisAlignedBB aabb = new AxisAlignedBB(posX, posY, posZ, posX + motionX, posY + motionY, posZ + motionZ).expand(1.0, 1.0, 1.0);
            List<Entity> entities = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.thePlayer, aabb);
            for (Entity entity : entities) {
                if (entity.canBeCollidedWith() && entity != mc.thePlayer) {
                    AxisAlignedBB entityBB = entity.getEntityBoundingBox().expand(0.3, 0.3, 0.3);
                    MovingObjectPosition intercept = entityBB.calculateIntercept(currentPos, nextPos);
                    if (intercept != null) {
                        landingPosition = intercept;
                        points.add(intercept.hitVec);
                        break;
                    }
                }
            }

            if (landingPosition != null) break;

            posX += motionX;
            posY += motionY;
            posZ += motionZ;

            points.add(new Vec3(posX, posY, posZ));

            motionX *= drag;
            motionY *= drag;
            motionZ *= drag;
            motionY -= gravity;
        }

        // Render trajectory path
        RenderUtil.enableGL3D();
        RenderUtil.color(color.getRGB());
        GL11.glLineWidth(lineWidth.getValueFloat());

        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (Vec3 point : points) {
            GL11.glVertex3d(point.xCoord - renderPosX, point.yCoord - renderPosY, point.zCoord - renderPosZ);
        }
        GL11.glEnd();

        // Render landing box
        if (hitBox.isEnabled() && landingPosition != null) {
            Vec3 hit = landingPosition.hitVec;
            AxisAlignedBB bb = new AxisAlignedBB(
                    hit.xCoord - 0.25, hit.yCoord - 0.25, hit.zCoord - 0.25,
                    hit.xCoord + 0.25, hit.yCoord + 0.25, hit.zCoord + 0.25
            );
            RenderUtil.drawSolidBox(new AxisAlignedBB(
                    bb.minX - renderPosX, bb.minY - renderPosY, bb.minZ - renderPosZ,
                    bb.maxX - renderPosX, bb.maxY - renderPosY, bb.maxZ - renderPosZ
            ));
        }

        RenderUtil.disableGL3D();
    }
}
