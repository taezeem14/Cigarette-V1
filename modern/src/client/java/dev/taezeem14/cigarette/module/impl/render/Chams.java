package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.utility.AntiBot;
import dev.taezeem14.cigarette.module.impl.utility.Teams;
import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class Chams extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Static", "Static", "Rainbow", "Team");
    private final ColorSetting visibleColor = new ColorSetting("Color", 0xFF3882F6);

    public Chams() {
        super("Chams", "Highlights player silhouettes cleanly through terrain", Category.RENDER);
        addSettings(mode, visibleColor);
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.level == null || mc.player == null) return;
        float pt = event.getPartialTicks();
        for (Player player : mc.level.players()) {
            if (player == mc.player || !player.isAlive() || AntiBot.isBot(player)) continue;
            double ix = player.xo + (player.getX() - player.xo) * pt;
            double iy = player.yo + (player.getY() - player.yo) * pt;
            double iz = player.zo + (player.getZ() - player.zo) * pt;
            AABB box = player.getBoundingBox().move(ix - player.getX(), iy - player.getY(), iz - player.getZ());
            int col = getPlayerColor(player);
            Gizmos.cuboid(box, GizmoStyle.strokeAndFill(col, 1.8F, ColorUtil.modifyAlpha(col, 65)));
        }
    }

    private int getPlayerColor(Player player) {
        if (mode.is("Rainbow")) return ColorUtil.getRainbow(4, player.getId() * 100);
        if (mode.is("Team")) return Teams.isTeammate(player) ? 0xFF10B981 : 0xFFEF4444;
        return visibleColor.getRGB();
    }
}
