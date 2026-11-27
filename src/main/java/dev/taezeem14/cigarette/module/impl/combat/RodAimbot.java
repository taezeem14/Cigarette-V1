package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.client.FriendManager;
import dev.taezeem14.cigarette.module.impl.utility.AntiBot;
import dev.taezeem14.cigarette.module.impl.utility.Teams;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class RodAimbot extends Module {

    private final NumberSetting range = new NumberSetting("Range", 12.0, 5.0, 20.0, 1.0);
    private final NumberSetting fov = new NumberSetting("FOV", 120.0, 15.0, 360.0, 5.0);
    private final BooleanSetting targetMobs = new BooleanSetting("Mobs", true);

    public RodAimbot() {
        super("RodAimbot", "Automatically predicts and aims rod throws at opponents", Category.COMBAT);
        addSettings(range, fov, targetMobs);
    }

    @Override
    public String getSuffix() {
        return fov.getValueInt() + "°";
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (!event.isPre() || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemFishingRod)) {
            return;
        }

        if (!RotationUtil.isPhysicalKeyDown(mc.gameSettings.keyBindUseItem.getKeyCode())) {
            return;
        }

        EntityLivingBase target = getBestTarget();
        if (target == null) return;

        double distance = mc.thePlayer.getDistanceToEntity(target);
        // Prediction ticks based on fishing rod velocity (~1.5 blocks/tick)
        double ticks = distance / 1.5;

        double predictedX = target.posX + (target.posX - target.lastTickPosX) * ticks;
        double predictedY = target.posY + (target.posY - target.lastTickPosY) * ticks + (target.getEyeHeight() * 0.75);
        double predictedZ = target.posZ + (target.posZ - target.lastTickPosZ) * ticks;

        // Compensation for gravity (~0.04/tick^2)
        predictedY += 0.04 * ticks * ticks * 0.5;

        Vec3 predictedVec = new Vec3(predictedX, predictedY, predictedZ);
        float[] rotations = RotationUtil.getRotationsToVec(predictedVec);

        event.setYaw(rotations[0]);
        event.setPitch(MathHelper.clamp_float(rotations[1], -90.0F, 90.0F));
    }

    private EntityLivingBase getBestTarget() {
        EntityLivingBase best = null;
        double bestDist = range.getValue();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase) || entity == mc.thePlayer) continue;

            EntityLivingBase living = (EntityLivingBase) entity;
            if (living.getEntityId() == -1337 || living instanceof EntityArmorStand) continue;
            if (living.isDead || living.getHealth() <= 0.0F || living.isInvisible()) continue;

            if (living instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) living;
                if (AntiBot.isBot(player)) continue;
                if (FriendManager.isFriend(player.getName())) continue;
                if (Teams.isTeammate(player)) continue;
            } else if (!targetMobs.isEnabled()) {
                continue;
            }

            double dist = mc.thePlayer.getDistanceToEntity(living);
            if (dist > bestDist) continue;
            if (!RotationUtil.isEntityInFov(living, fov.getValueFloat())) continue;

            best = living;
            bestDist = dist;
        }
        return best;
    }
}
