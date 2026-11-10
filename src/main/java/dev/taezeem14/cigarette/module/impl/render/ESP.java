package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.FrustumUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.util.vector.Vector4f;

public class ESP extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Full", "Full", "Corner", "Outline");
    private final BooleanSetting healthBar = new BooleanSetting("Health Bar", true);
    private final BooleanSetting armorBar = new BooleanSetting("Armor Bar", true);
    private final BooleanSetting invisibles = new BooleanSetting("Invisible Players", false);
    private final ModeSetting colorMode = new ModeSetting("Color Mode", "Static", "Static", "Team", "Rainbow", "Astolfo");
    private final ColorSetting staticColor = new ColorSetting("Color", 0xFF3882F6);

    public ESP() {
        super("ESP", "Renders 2D projected bounding boxes and status bars on players", Category.RENDER);
        addSettings(mode, healthBar, armorBar, invisibles, colorMode, staticColor);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        FrustumUtil.updateFrustum();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player.isDead) continue;
            if (player.isInvisible() && !invisibles.isEnabled()) continue;

            Vector4f bounds = FrustumUtil.getEntityPositionsOn2D(player, event.getPartialTicks());
            if (bounds == null) continue;

            float x = bounds.x;
            float y = bounds.y;
            float w = bounds.z - bounds.x;
            float h = bounds.w - bounds.y;

            if (w <= 0.0f || h <= 0.0f) continue;

            int boxColor = getEntityColor(player);

            // Render Box
            renderBox(x, y, w, h, boxColor);

            // Render Health Bar
            if (healthBar.isEnabled()) {
                renderHealthBar(player, x, y, h);
            }

            // Render Armor Bar
            if (armorBar.isEnabled()) {
                renderArmorBar(player, x, y, w, h);
            }
        }
    }

    private void renderBox(float x, float y, float w, float h, int color) {
        String boxMode = mode.getMode();
        switch (boxMode.toLowerCase()) {
            case "corner":
                renderCornerBox(x, y, w, h, color);
                break;
            case "outline":
                RenderUtil.drawOutline(x, y, w, h, 1.0f, color);
                break;
            case "full":
            default:
                // Inner black shadow, colored line, outer black shadow
                RenderUtil.drawOutline(x - 0.5f, y - 0.5f, w + 1.0f, h + 1.0f, 1.0f, 0x99000000);
                RenderUtil.drawOutline(x, y, w, h, 1.0f, color);
                RenderUtil.drawOutline(x + 0.5f, y + 0.5f, w - 1.0f, h - 1.0f, 1.0f, 0x99000000);
                break;
        }
    }

    private void renderCornerBox(float x, float y, float w, float h, int color) {
        float lineW = w / 4.0f;
        float lineH = h / 4.0f;

        // Black shadow
        drawCornerLines(x - 0.5f, y - 0.5f, w + 1.0f, h + 1.0f, lineW, lineH, 0x99000000);
        // Color lines
        drawCornerLines(x, y, w, h, lineW, lineH, color);
    }

    private void drawCornerLines(float x, float y, float w, float h, float lw, float lh, int color) {
        // Top-Left
        RenderUtil.drawRect(x, y, lw, 1.0f, color);
        RenderUtil.drawRect(x, y, 1.0f, lh, color);

        // Top-Right
        RenderUtil.drawRect(x + w - lw, y, lw, 1.0f, color);
        RenderUtil.drawRect(x + w, y, 1.0f, lh, color);

        // Bottom-Left
        RenderUtil.drawRect(x, y + h, lw, 1.0f, color);
        RenderUtil.drawRect(x, y + h - lh, 1.0f, lh, color);

        // Bottom-Right
        RenderUtil.drawRect(x + w - lw, y + h, lw, 1.0f, color);
        RenderUtil.drawRect(x + w, y + h - lh, 1.0f, lh, color);
    }

    private void renderHealthBar(EntityPlayer player, float x, float y, float h) {
        float barWidth = 2.0f;
        float barX = x - barWidth - 3.0f;

        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float ratio = Math.max(0.0f, Math.min(1.0f, health / maxHealth));

        float barHeight = h * ratio;

        // Background / Border
        RenderUtil.drawRect(barX - 0.5f, y - 0.5f, barWidth + 1.0f, h + 1.0f, 0xAA000000);

        // Dynamic health color gradient
        int hpColor = ColorUtil.getHealthColor(health, maxHealth);
        RenderUtil.drawRect(barX, y + h - barHeight, barWidth, barHeight, hpColor);
    }

    private void renderArmorBar(EntityPlayer player, float x, float y, float w, float h) {
        float barHeight = 2.0f;
        float barY = y + h + 3.0f;

        int totalArmor = player.getTotalArmorValue(); // 0 to 20
        float ratio = Math.max(0.0f, Math.min(1.0f, (float) totalArmor / 20.0f));

        if (ratio > 0.0f) {
            float barW = w * ratio;

            RenderUtil.drawRect(x - 0.5f, barY - 0.5f, w + 1.0f, barHeight + 1.0f, 0xAA000000);
            RenderUtil.drawRect(x, barY, barW, barHeight, 0xFF3882F6);
        }
    }

    public int getEntityColor(EntityPlayer player) {
        String cm = colorMode.getMode();
        switch (cm.toLowerCase()) {
            case "team":
                if (player.getTeam() != null) {
                    String formatted = player.getTeam().formatString(player.getName());
                    for (int i = 0; i < formatted.length() - 1; i++) {
                        if (formatted.charAt(i) == '§') {
                            char code = formatted.charAt(i + 1);
                            int colIndex = "0123456789abcdef".indexOf(Character.toLowerCase(code));
                            if (colIndex != -1) {
                                return 0xFF000000 | mc.fontRendererObj.getColorCode(code);
                            }
                        }
                    }
                }
                return 0xFFFFFFFF;
            case "rainbow":
                return ColorUtil.getRainbow(4, player.getEntityId() * 100);
            case "astolfo":
                return ColorUtil.getAstolfo(4, player.getEntityId() * 100);
            case "static":
            default:
                return staticColor.getRGB();
        }
    }
}
