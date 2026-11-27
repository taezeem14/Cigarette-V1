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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class RodAimbot extends Module {

    private final NumberSetting fov = new NumberSetting("FOV", 90.0, 15.0, 180.0, 5.0);
    private final NumberSetting range = new NumberSetting("Range", 12.0, 4.0, 20.0, 0.5);
    private final BooleanSetting predict = new BooleanSetting("Velocity Prediction", true);
    private final BooleanSetting targetMobs = new BooleanSetting("Mobs", true);

    public RodAimbot() {
        super("RodAimbot", "Predicts target movement to aim fishing rods accurately", Category.COMBAT);
        addSettings(fov, range, predict, targetMobs);
    }

    @Override
    public String getSuffix() {
        return String.format("%.1f", range.getValue());
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (!event.isPre() || mc.player == null || mc.level == null) return;
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty() || !held.is(Items.FISHING_ROD)) return;
        if (!RotationUtil.isPhysicalKeyDown(mc.options.keyUse)) return;

        LivingEntity target = getBestTarget();
        if (target == null) return;

        double dist = mc.player.distanceTo(target);
        Vec3 targetPos = new Vec3(target.getX(), target.getY() + target.getEyeHeight() * 0.7, target.getZ());
        if (predict.isEnabled()) {
            double flightTicks = dist / 1.5;
            double vx = target.getX() - target.xo;
            double vy = target.getY() - target.yo;
            double vz = target.getZ() - target.zo;
            targetPos = targetPos.add(vx * flightTicks, vy * flightTicks * 0.5 + 0.03 * flightTicks * flightTicks, vz * flightTicks);
        }

        float[] rots = RotationUtil.getRotationsToVec(targetPos);
        float[] fixed = RotationUtil.applySensitivityGCD(new float[]{event.getYaw(), event.getPitch()}, rots, RotationUtil.getMouseSensitivity());
        event.setYaw(fixed[0]);
        event.setPitch(fixed[1]);
    }

    private LivingEntity getBestTarget() {
        LivingEntity best = null;
        double bestDist = range.getValue();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living == mc.player) continue;
            if (living instanceof ArmorStand || living.getId() == -1337) continue;
            if (!living.isAlive() || living.isInvisible()) continue;

            if (living instanceof Player p) {
                if (AntiBot.isBot(p) || FriendManager.isFriend(p.getName().getString()) || Teams.isTeammate(p)) continue;
            } else if (!targetMobs.isEnabled()) {
                continue;
            }

            double dist = mc.player.distanceTo(living);
            if (dist <= bestDist && RotationUtil.isEntityInFov(living, fov.getValueFloat())) {
                if (best instanceof Player && !(living instanceof Player) && dist >= bestDist * 0.75) {
                    continue;
                }
                best = living;
                bestDist = dist;
            }
        }
        return best;
    }
}
