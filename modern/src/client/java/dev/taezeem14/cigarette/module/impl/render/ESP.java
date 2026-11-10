package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.utility.AntiBot;
import dev.taezeem14.cigarette.module.impl.utility.Teams;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.FrustumUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.awt.Color;

public class ESP extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Full", "Full", "Corner", "Outline", "3D Box");
    private final BooleanSetting healthBar = new BooleanSetting("Health Bar", true);
    private final BooleanSetting armorBar = new BooleanSetting("Armor Bar", true);
    private final BooleanSetting invisibles = new BooleanSetting("Invisible Players", false);
    private final ModeSetting colorMode = new ModeSetting("Color Mode", "Static", "Static", "Team", "Rainbow", "Astolfo");
    private final ColorSetting staticColor = new ColorSetting("Color", 0xFF3882F6);

    public ESP() {
        super("ESP", "Renders 2D/3D projected bounding boxes and health bars on players", Category.RENDER);
        addSettings(mode, healthBar, armorBar, invisibles, colorMode, staticColor);
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!mode.is("3D Box") || mc.level == null || mc.player == null) return;
        float pt = event.getPartialTicks();
        for (Player player : mc.level.players()) {
            if (player == mc.player || !player.isAlive()) continue;
            if (AntiBot.isBot(player) || (player.isInvisible() && !invisibles.isEnabled())) continue;

            double ix = player.xo + (player.getX() - player.xo) * pt;
            double iy = player.yo + (player.getY() - player.yo) * pt;
            double iz = player.zo + (player.getZ() - player.zo) * pt;
            AABB box = player.getBoundingBox().move(ix - player.getX(), iy - player.getY(), iz - player.getZ());
            int col = getEntityColor(player);
            Gizmos.cuboid(box, GizmoStyle.strokeAndFill(col, 1.5F, ColorUtil.modifyAlpha(col, 40)));
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mode.is("3D Box") || mc.level == null || mc.player == null) return;
        GuiGraphicsExtractor g = event.getGraphics();
        int screenW = event.getScreenWidth();
        int screenH = event.getScreenHeight();

        for (Player player : mc.level.players()) {
            if (player == mc.player || !player.isAlive()) continue;
            if (AntiBot.isBot(player) || (player.isInvisible() && !invisibles.isEnabled())) continue;

            float[] bounds = FrustumUtil.getEntityBounds2D(player, event.getPartialTicks(), screenW, screenH);
            if (bounds == null) continue;

            float x = bounds[0];
            float y = bounds[1];
            float w = bounds[2] - bounds[0];
            float h = bounds[3] - bounds[1];
            if (w <= 0.0F || h <= 0.0F) continue;

            int boxColor = getEntityColor(player);
            if (mode.is("Corner")) {
                float lw = Math.max(2.0F, w / 4.0F);
                float lh = Math.max(2.0F, h / 4.0F);
                drawCorners(g, x - 0.5F, y - 0.5F, w + 1.0F, h + 1.0F, lw, lh, 0x99000000);
                drawCorners(g, x, y, w, h, lw, lh, boxColor);
            } else if (mode.is("Outline")) {
                RenderUtil.drawOutline(g, x, y, w, h, 1.0F, boxColor);
            } else {
                RenderUtil.drawOutline(g, x - 0.5F, y - 0.5F, w + 1.0F, h + 1.0F, 1.0F, 0x99000000);
                RenderUtil.drawOutline(g, x, y, w, h, 1.0F, boxColor);
                RenderUtil.drawOutline(g, x + 0.5F, y + 0.5F, Math.max(1.0F, w - 1.0F), Math.max(1.0F, h - 1.0F), 1.0F, 0x99000000);
            }

            if (healthBar.isEnabled()) {
                float barW = 2.0F;
                float barX = x - barW - 3.0F;
                float maxHp = Math.max(1.0F, player.getMaxHealth());
                float ratio = Math.max(0.0F, Math.min(1.0F, player.getHealth() / maxHp));
                float barH = h * ratio;
                RenderUtil.drawRect(g, barX - 0.5F, y - 0.5F, barW + 1.0F, h + 1.0F, 0xAA000000);
                RenderUtil.drawRect(g, barX, y + h - barH, barW, barH, ColorUtil.getHealthColor(player.getHealth(), maxHp));
            }

            if (armorBar.isEnabled()) {
                float armorRatio = Math.max(0.0F, Math.min(1.0F, player.getArmorValue() / 20.0F));
                if (armorRatio > 0.0F) {
                    float barY = y + h + 2.5F;
                    RenderUtil.drawRect(g, x - 0.5F, barY - 0.5F, w + 1.0F, 3.0F, 0xAA000000);
                    RenderUtil.drawRect(g, x, barY, w * armorRatio, 2.0F, 0xFF38BDF8);
                }
            }
        }
    }

    private void drawCorners(GuiGraphicsExtractor g, float x, float y, float w, float h, float lw, float lh, int col) {
        RenderUtil.drawRect(g, x, y, lw, 1.0F, col);
        RenderUtil.drawRect(g, x, y, 1.0F, lh, col);
        RenderUtil.drawRect(g, x + w - lw, y, lw, 1.0F, col);
        RenderUtil.drawRect(g, x + w - 1.0F, y, 1.0F, lh, col);
        RenderUtil.drawRect(g, x, y + h - 1.0F, lw, 1.0F, col);
        RenderUtil.drawRect(g, x, y + h - lh, 1.0F, lh, col);
        RenderUtil.drawRect(g, x + w - lw, y + h - 1.0F, lw, 1.0F, col);
        RenderUtil.drawRect(g, x + w - 1.0F, y + h - lh, 1.0F, lh, col);
    }

    private int getEntityColor(Player player) {
        switch (colorMode.getMode().toLowerCase()) {
            case "team":
                return Teams.isTeammate(player) ? 0xFF10B981 : 0xFFEF4444;
            case "rainbow":
                return ColorUtil.getRainbow(4, player.getId() * 100);
            case "astolfo": {
                float hue = ((System.currentTimeMillis() + player.getId() * 100L) % 3000L) / 3000.0F;
                hue = hue > 0.5F ? 1.0F - hue : hue;
                return 0xFF000000 | (Color.HSBtoRGB(hue + 0.5F, 0.6F, 1.0F) & 0x00FFFFFF);
            }
            default:
                return staticColor.getRGB();
        }
    }
}
