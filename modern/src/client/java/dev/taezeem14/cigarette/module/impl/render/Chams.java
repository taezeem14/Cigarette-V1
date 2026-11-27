package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.utility.AntiBot;
import dev.taezeem14.cigarette.module.impl.utility.Teams;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class Chams extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Static", "Static", "Rainbow", "Team");
    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", true);
    private final ColorSetting visibleColor = new ColorSetting("Color", 0xFF3882F6);

    public Chams() {
        super("Chams", "Highlights entity silhouettes cleanly through terrain", Category.RENDER);
        addSettings(mode, players, mobs, visibleColor);
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.level == null || mc.player == null) return;
        float pt = event.getPartialTicks();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living == mc.player || !living.isAlive()) continue;
            if (living instanceof ArmorStand || living.getId() == -1337) continue;

            if (living instanceof Player player) {
                if (!players.isEnabled() || AntiBot.isBot(player)) continue;
            } else if (!mobs.isEnabled()) {
                continue;
            }

            double ix = living.xo + (living.getX() - living.xo) * pt;
            double iy = living.yo + (living.getY() - living.yo) * pt;
            double iz = living.zo + (living.getZ() - living.zo) * pt;
            AABB box = living.getBoundingBox().move(ix - living.getX(), iy - living.getY(), iz - living.getZ());
            int col = getEntityColor(living);
            Gizmos.cuboid(box, GizmoStyle.strokeAndFill(col, 1.8F, ColorUtil.modifyAlpha(col, 65)));
        }
    }

    private int getEntityColor(LivingEntity entity) {
        if (mode.is("Rainbow")) return ColorUtil.getRainbow(4, entity.getId() * 100);
        if (mode.is("Team")) {
            if (entity instanceof Player player) {
                return Teams.isTeammate(player) ? 0xFF10B981 : 0xFFEF4444;
            }
            return entity instanceof Animal ? 0xFF10B981 : 0xFFEF4444;
        }
        return visibleColor.getRGB();
    }
}
