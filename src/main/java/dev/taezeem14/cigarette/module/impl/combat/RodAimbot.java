package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.client.FriendManager;
import dev.taezeem14.cigarette.module.impl.utility.Teams;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;

public class RodAimbot extends Module {

    private final NumberSetting range = new NumberSetting("Range", 12.0, 5.0, 20.0, 1.0);
    private final NumberSetting fov = new NumberSetting("FOV", 120.0, 15.0, 360.0, 5.0);

    public RodAimbot() {
        super("RodAimbot", "Automatically predicts and aims rod throws at opponents", Category.COMBAT);
        addSettings(range, fov);
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (!event.isPre() || mc.thePlayer == null || mc.theWorld == null) return;

        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemFishingRod)) {
            return;
        }

        if (!mc.gameSettings.keyBindUseItem.isKeyDown()) {
            return;
        }

        EntityPlayer target = getBestTarget();
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
        event.setPitch(rotations[1]);
    }

    private EntityPlayer getBestTarget() {
        EntityPlayer best = null;
        double bestDist = range.getValue();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer) || entity == mc.thePlayer) continue;

            EntityPlayer player = (EntityPlayer) entity;
            if (player.isDead || player.getHealth() <= 0.0F || player.isInvisible()) continue;
            if (FriendManager.isFriend(player.getName())) continue;
            if (Teams.isTeammate(player)) continue;

            double dist = mc.thePlayer.getDistanceToEntity(player);
            if (dist > bestDist) continue;
            if (!RotationUtil.isEntityInFov(player, fov.getValueFloat())) continue;

            best = player;
            bestDist = dist;
        }
        return best;
    }
}
