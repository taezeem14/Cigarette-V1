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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class Tracers extends Module {

    private final ColorSetting color = new ColorSetting("Color", 0xFF3882F6);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.5, 0.5, 3.0, 0.1);
    private final BooleanSetting invisibles = new BooleanSetting("Invisible Players", false);
    private final BooleanSetting distanceFade = new BooleanSetting("Distance Color Fade", true);

    public Tracers() {
        super("Tracers", "Draws lines toward nearby players", Category.RENDER);
        addSettings(color, lineWidth, invisibles, distanceFade);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.level == null || mc.player == null || event.getCameraState() == null || event.getCameraState().pos == null) {
            return;
        }
        float pt = event.getPartialTicks();
        Vec3 forward = mc.player.getViewVector(pt);
        Vec3 start = event.getCameraState().pos.add(forward.scale(0.5));

        for (Player player : mc.level.players()) {
            if (player == mc.player || !player.isAlive() || AntiBot.isBot(player)) continue;
            if (player.isInvisible() && !invisibles.isEnabled()) continue;

            double px = player.xo + (player.getX() - player.xo) * pt;
            double py = player.yo + (player.getY() - player.yo) * pt + player.getBbHeight() * 0.5;
            double pz = player.zo + (player.getZ() - player.zo) * pt;

            int col;
            if (distanceFade.isEnabled()) {
                float dist = mc.player.distanceTo(player);
                float t = Math.max(0.0F, Math.min(1.0F, dist / 60.0F));
                col = ColorUtil.interpolateColor(0xFFEF4444, 0xFF10B981, t);
            } else if (Teams.isTeammate(player)) {
                col = 0xFF10B981;
            } else {
                col = color.getRGB();
            }

            Gizmos.line(start, new Vec3(px, py, pz), col, lineWidth.getValueFloat());
        }
    }
}
