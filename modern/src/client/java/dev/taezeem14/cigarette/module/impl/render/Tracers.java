package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.utility.AntiBot;
import dev.taezeem14.cigarette.module.impl.utility.Teams;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class Tracers extends Module {

    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", true);
    private final ColorSetting color = new ColorSetting("Color", 0xFF3882F6);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.5, 0.5, 3.0, 0.1);
    private final BooleanSetting invisibles = new BooleanSetting("Invisibles", false);
    private final BooleanSetting distanceFade = new BooleanSetting("Distance Color Fade", true);

    public Tracers() {
        super("Tracers", "Draws lines toward nearby players and mobs", Category.RENDER);
        addSettings(players, mobs, color, lineWidth, invisibles, distanceFade);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.level == null || mc.player == null || event.getCameraState() == null || event.getCameraState().pos == null) {
            return;
        }
        float pt = event.getPartialTicks();
        Vec3 forward = mc.player.getViewVector(pt);
        Vec3 start = event.getCameraState().pos.add(forward.scale(0.5));

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living == mc.player || !living.isAlive()) continue;
            if (living instanceof ArmorStand || living.getId() == -1337) continue;
            if (living.isInvisible() && !invisibles.isEnabled()) continue;

            if (living instanceof Player player) {
                if (!players.isEnabled() || AntiBot.isBot(player)) continue;
            } else if (!mobs.isEnabled()) {
                continue;
            }

            double px = living.xo + (living.getX() - living.xo) * pt;
            double py = living.yo + (living.getY() - living.yo) * pt + living.getBbHeight() * 0.5;
            double pz = living.zo + (living.getZ() - living.zo) * pt;

            int col;
            if (distanceFade.isEnabled()) {
                float dist = mc.player.distanceTo(living);
                float t = Math.max(0.0F, Math.min(1.0F, dist / 60.0F));
                col = ColorUtil.interpolateColor(0xFFEF4444, 0xFF10B981, t);
            } else if (living instanceof Player player && Teams.isTeammate(player)) {
                col = 0xFF10B981;
            } else {
                col = color.getRGB();
            }

            Gizmos.line(start, new Vec3(px, py, pz), col, lineWidth.getValueFloat());
        }
    }
}
