package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HUD extends Module {

    private final BooleanSetting watermark = new BooleanSetting("Watermark", true);
    private final BooleanSetting arrayList = new BooleanSetting("ArrayList", true);
    private final ModeSetting colorMode = new ModeSetting("Color Mode", "Rainbow", "Rainbow", "Astolfo", "Static", "Theme");
    private final ColorSetting staticColor = new ColorSetting("Static Color", 0xFF3882F6);
    private final BooleanSetting sideBar = new BooleanSetting("Side Bar", true);
    private final BooleanSetting background = new BooleanSetting("Background", true);
    private final BooleanSetting showCoords = new BooleanSetting("Coordinates", true);
    private final BooleanSetting showFps = new BooleanSetting("FPS Display", true);
    private final BooleanSetting showCps = new BooleanSetting("CPS Display", true);

    private float watermarkX = 6.0f;
    private float watermarkY = 6.0f;
    private float arrayListX = -1.0f; // -1 means right aligned
    private float arrayListY = 6.0f;

    public HUD() {
        super("HUD", "In-game heads-up display and active modules", Category.RENDER);
        addSettings(watermark, arrayList, colorMode, staticColor, sideBar, background, showCoords, showFps, showCps);
        setState(true); // Enabled by default
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.gameSettings.showDebugInfo) return;

        ScaledResolution sr = event.getScaledResolution();
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        // 1. Watermark
        if (watermark.isEnabled()) {
            renderWatermark();
        }

        // 2. Active Module ArrayList
        if (arrayList.isEnabled() && Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            renderArrayList(screenW);
        }

        // 3. Info / Stats Display (Bottom Left)
        renderInfoOverlay(screenH);
    }

    private void renderWatermark() {
        String clientName = "CIGARETTE";
        String clientVersion = "V1";

        int nameW = FontUtil.bold.getStringWidth(clientName);
        int verW = FontUtil.small.getStringWidth(clientVersion);
        float totalW = nameW + verW + 16.0f;
        float totalH = 18.0f;

        if (background.isEnabled()) {
            RenderUtil.drawDropShadow(watermarkX, watermarkY, totalW, totalH, 3.0f, 0x55000000);
            RenderUtil.drawRoundedRect(watermarkX, watermarkY, totalW, totalH, 3.0f, ColorUtil.BACKGROUND);
            RenderUtil.drawRoundedOutline(watermarkX, watermarkY, totalW, totalH, 3.0f, 1.0f, 0xFF222228);
        }

        int themeColor = getHUDColor(0);
        FontUtil.bold.drawString(clientName, watermarkX + 5.0f, watermarkY + (totalH - FontUtil.bold.getHeight()) / 2.0f, themeColor);
        FontUtil.small.drawString(clientVersion, watermarkX + 5.0f + nameW + 3.0f, watermarkY + 4.0f, ColorUtil.TEXT_MUTED);
    }

    private void renderArrayList(float screenW) {
        List<Module> activeModules = new ArrayList<>();
        for (Module m : Cigarette.INSTANCE.getModuleManager().getModules()) {
            if (m.isEnabled()) {
                activeModules.add(m);
            }
        }

        activeModules.sort(Comparator.comparingInt((Module m) -> FontUtil.regular.getStringWidth(m.getName())).reversed());

        float startY = arrayListY;
        int count = 0;

        for (Module m : activeModules) {
            String name = m.getName();
            int strW = FontUtil.regular.getStringWidth(name);
            float itemW = strW + 8.0f;
            float itemH = 13.0f;

            float itemX;
            if (arrayListX < 0) {
                itemX = screenW - itemW - (sideBar.isEnabled() ? 4.0f : 2.0f);
            } else {
                itemX = arrayListX;
            }

            int color = getHUDColor(count * 150);

            if (background.isEnabled()) {
                RenderUtil.drawRect(itemX, startY, itemW, itemH, 0x88141416);
            }

            FontUtil.regular.drawString(name, itemX + 4.0f, startY + (itemH - FontUtil.regular.getHeight()) / 2.0f, color);

            if (sideBar.isEnabled()) {
                RenderUtil.drawRect(itemX + itemW, startY, 2.0f, itemH, color);
            }

            startY += itemH;
            count++;
        }
    }

    private void renderInfoOverlay(float screenH) {
        float infoY = screenH - 12.0f;

        if (showCoords.isEnabled() && mc.thePlayer != null) {
            String coords = String.format("XYZ: %.1f, %.1f, %.1f", mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
            FontUtil.small.drawStringWithShadow(coords, 4.0f, infoY, ColorUtil.TEXT_PRIMARY);
            infoY -= 11.0f;
        }

        if (showFps.isEnabled()) {
            String fps = "FPS: " + Minecraft.getDebugFPS();
            FontUtil.small.drawStringWithShadow(fps, 4.0f, infoY, ColorUtil.TEXT_PRIMARY);
            infoY -= 11.0f;
        }

        if (showCps.isEnabled()) {
            int cps = getEstimatedCPS();
            String cpsStr = "CPS: " + cps;
            FontUtil.small.drawStringWithShadow(cpsStr, 4.0f, infoY, ColorUtil.TEXT_PRIMARY);
        }
    }

    private int getEstimatedCPS() {
        return org.lwjgl.input.Mouse.isButtonDown(0) ? 12 : 0;
    }

    public int getHUDColor(int offset) {
        String mode = colorMode.getMode();
        switch (mode.toLowerCase()) {
            case "rainbow":
                return ColorUtil.getRainbow(4, offset);
            case "astolfo":
                return ColorUtil.getAstolfo(4, offset);
            case "static":
                return staticColor.getRGB();
            case "theme":
            default:
                return ColorUtil.ACCENT;
        }
    }

    public float getWatermarkX() {
        return watermarkX;
    }

    public void setWatermarkX(float watermarkX) {
        this.watermarkX = watermarkX;
    }

    public float getWatermarkY() {
        return watermarkY;
    }

    public void setWatermarkY(float watermarkY) {
        this.watermarkY = watermarkY;
    }

    public float getArrayListX() {
        return arrayListX < 0 ? 10.0f : arrayListX;
    }

    public void setArrayListX(float arrayListX) {
        this.arrayListX = arrayListX;
    }

    public float getArrayListY() {
        return arrayListY;
    }

    public void setArrayListY(float arrayListY) {
        this.arrayListY = arrayListY;
    }
}
