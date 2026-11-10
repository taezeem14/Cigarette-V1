package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.client.ArrayListModule;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.player.CPSTracker;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HUD extends Module {

    private final BooleanSetting watermark = new BooleanSetting("Watermark", true);
    private final BooleanSetting arrayList = new BooleanSetting("ArrayList", true);
    private final BooleanSetting sideBar = new BooleanSetting("Side Bar", true);
    private final BooleanSetting showCoords = new BooleanSetting("Coordinates", true);
    private final BooleanSetting showFps = new BooleanSetting("FPS Display", true);
    private final BooleanSetting showCps = new BooleanSetting("CPS Display", true);

    private float watermarkX = 6.0f;
    private float watermarkY = 6.0f;
    private float arrayListX = -1.0f; // -1 means right aligned
    private float arrayListY = 6.0f;

    public HUD() {
        super("HUD", "In-game heads-up display and active modules", Category.RENDER);
        addSettings(watermark, arrayList, sideBar, showCoords, showFps, showCps);
        setState(true);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.gameSettings.showDebugInfo) return;

        FontUtil.ensureInitialized();
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            ClickGUIModule clickGUI = Cigarette.INSTANCE.getModuleManager().getModuleByClass(ClickGUIModule.class);
            if (clickGUI != null) {
                clickGUI.syncThemeFromSetting();
            }
        }
        ScaledResolution sr = event.getScaledResolution();
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        // 1. Watermark
        if (watermark.isEnabled()) {
            renderWatermark();
        }

        // 2. Active Module ArrayList (synchronized with ArrayListModule)
        ArrayListModule alModule = getArrayListModule();
        boolean showArrayList = arrayList.isEnabled() && (alModule == null || alModule.isEnabled());
        if (showArrayList && Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            renderArrayList(screenW, alModule);
        }

        // 3. Info / Stats Display (Bottom Left)
        renderInfoOverlay(screenH);

        // 4. Toast Notifications (Bottom Right)
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getNotificationManager() != null) {
            Cigarette.INSTANCE.getNotificationManager().render(screenW, screenH);
        }
    }

    private void renderWatermark() {
        String brand = "CIGARETTE";
        String rest = " \u2022 V1 \u2022 " + Minecraft.getDebugFPS() + " FPS \u2022 "
                + CPSTracker.getLeftCPS() + " | " + CPSTracker.getRightCPS() + " CPS";

        int brandW = FontUtil.bold.getStringWidth(brand);
        int restW = FontUtil.small.getStringWidth(rest);
        float totalW = brandW + restW + 18.0f;
        float totalH = 19.0f;
        int accent = getHUDColor(0);

        RenderUtil.drawDropShadow(watermarkX, watermarkY, totalW, totalH, 4.0f, 0x55000000);
        RenderUtil.drawRoundedRect(watermarkX, watermarkY, totalW, totalH, 4.0f, 0xDC141418);
        RenderUtil.drawRoundedOutline(watermarkX, watermarkY, totalW, totalH, 4.0f, 1.0f, ColorUtil.BORDER);

        // Top subtle highlight bar + left accent pill
        RenderUtil.drawRoundedRectTop(watermarkX + 1.0f, watermarkY, totalW - 2.0f, 1.5f, 1.5f, ColorUtil.modifyAlpha(accent, 180));
        RenderUtil.drawRoundedRect(watermarkX + 4.0f, watermarkY + 4.5f, 2.0f, totalH - 9.0f, 1.0f, accent);

        FontUtil.bold.drawString(brand, watermarkX + 9.0f, watermarkY + (totalH - FontUtil.bold.getHeight()) / 2.0f - 0.5f, accent);
        FontUtil.small.drawString(rest, watermarkX + 9.0f + brandW, watermarkY + (totalH - FontUtil.small.getHeight()) / 2.0f - 0.5f, ColorUtil.TEXT_PRIMARY);
    }

    private void renderArrayList(float screenW, ArrayListModule al) {
        boolean showSuffix = al == null || al.getSuffixSetting().isEnabled();
        boolean showBackground = al == null || al.getBackground().isEnabled();
        boolean useAnimation = al == null || al.getAnimation().isEnabled();
        boolean useCustomFont = al == null || !al.getFont().is("Minecraft");

        List<Module> renderModules = new ArrayList<>();
        for (Module m : Cigarette.INSTANCE.getModuleManager().getModules()) {
            if (m instanceof ClickGUIModule || m.isHidden()) {
                continue;
            }
            float target = m.isEnabled() ? 1.0f : 0.0f;
            float anim = useAnimation ? AnimationUtil.lerp(m.getAnimationProgress(), target, 14.0f) : target;
            m.setAnimationProgress(anim);

            if (anim > 0.02f) {
                renderModules.add(m);
            }
        }

        renderModules.sort(Comparator.comparingInt((Module m) -> getDisplayWidth(formatModuleDisplay(m, showSuffix), useCustomFont)).reversed());

        float startY = arrayListY;
        int count = 0;

        for (Module m : renderModules) {
            float anim = Math.max(0.0f, Math.min(1.0f, m.getAnimationProgress()));
            String display = formatModuleDisplay(m, showSuffix);
            int strW = getDisplayWidth(display, useCustomFont);
            float itemW = strW + 10.0f;
            float itemH = 13.5f;

            float baseX;
            if (arrayListX < 0) {
                baseX = screenW - itemW - (sideBar.isEnabled() ? 4.0f : 2.0f);
            } else {
                baseX = arrayListX;
            }

            float slideOffset = (1.0f - AnimationUtil.easeOutCubic(anim)) * (itemW + 8.0f);
            float itemX = arrayListX < 0 ? baseX + slideOffset : baseX - slideOffset;

            int baseColor = getHUDColor(count * 120);
            int alpha = Math.max(10, Math.min(255, (int) (255 * anim)));
            int color = ColorUtil.modifyAlpha(baseColor, alpha);

            if (showBackground) {
                int bgCol = ColorUtil.modifyAlpha(0xFF141418, (int) (185 * anim));
                RenderUtil.drawRoundedRect(itemX, startY, itemW, itemH - 0.5f, 2.0f, bgCol);
            }

            if (useCustomFont) {
                FontUtil.regular.drawStringWithShadow(display, itemX + 5.0f, startY + (itemH - FontUtil.regular.getHeight()) / 2.0f - 0.5f, color);
            } else {
                mc.fontRendererObj.drawStringWithShadow(display, itemX + 5.0f, startY + (itemH - mc.fontRendererObj.FONT_HEIGHT) / 2.0f, color);
            }

            if (sideBar.isEnabled()) {
                float barX = itemX + itemW + 0.5f;
                RenderUtil.drawRect(barX - 0.5f, startY, 3.0f, itemH - 0.5f, ColorUtil.modifyAlpha(baseColor, (int) (60 * anim)));
                RenderUtil.drawRoundedRect(barX, startY + 0.5f, 2.0f, itemH - 1.5f, 1.0f, color);
            }

            startY += itemH * anim;
            count++;
        }
    }

    private String formatModuleDisplay(Module m, boolean showSuffix) {
        if (!showSuffix) {
            return m.getName();
        }
        String suffix = m.getSuffix();
        if (suffix != null && !suffix.trim().isEmpty()) {
            return m.getName() + " \u00a77" + suffix.trim();
        }
        return m.getName();
    }

    private int getDisplayWidth(String text, boolean useCustomFont) {
        if (useCustomFont) {
            return FontUtil.regular.getStringWidth(text);
        }
        return mc.fontRendererObj.getStringWidth(text);
    }

    private void renderInfoOverlay(float screenH) {
        float infoY = screenH - 13.0f;

        if (showCoords.isEnabled() && mc.thePlayer != null) {
            String coords = String.format("\u00a77XYZ \u00a7f%.1f, %.1f, %.1f", mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
            FontUtil.small.drawStringWithShadow(coords, 6.0f, infoY, ColorUtil.TEXT_PRIMARY);
            infoY -= 11.0f;
        }

        if (showFps.isEnabled()) {
            String fps = "\u00a77FPS \u00a7f" + Minecraft.getDebugFPS();
            FontUtil.small.drawStringWithShadow(fps, 6.0f, infoY, ColorUtil.TEXT_PRIMARY);
            infoY -= 11.0f;
        }

        if (showCps.isEnabled()) {
            String cpsStr = "\u00a77CPS \u00a7f" + CPSTracker.getLeftCPS() + " \u00a77| \u00a7f" + CPSTracker.getRightCPS();
            FontUtil.small.drawStringWithShadow(cpsStr, 6.0f, infoY, ColorUtil.TEXT_PRIMARY);
        }
    }

    public int getHUDColor(int offset) {
        ArrayListModule al = getArrayListModule();
        if (al == null) {
            return ColorUtil.getAccent();
        }

        String mode = al.getColorMode().getMode();
        float speed = Math.max(0.2f, al.getRainbowSpeed().getValueFloat());
        long cycleMs = (long) (4000.0f / speed);

        switch (mode.toLowerCase()) {
            case "rainbow": {
                float hue = ((System.currentTimeMillis() + offset) % cycleMs) / (float) cycleMs;
                return Color.HSBtoRGB(hue, 0.78f, 1.0f);
            }
            case "astolfo": {
                float hue = ((System.currentTimeMillis() + offset) % cycleMs) / (float) cycleMs;
                hue = hue > 0.5f ? 1.0f - hue : hue;
                hue += 0.5f;
                return Color.HSBtoRGB(hue, 0.6f, 1.0f);
            }
            case "static":
                return al.getStaticColor().getRGB();
            case "theme":
            default:
                return ColorUtil.getAccent();
        }
    }

    private ArrayListModule getArrayListModule() {
        ArrayListModule al = ArrayListModule.getInstance();
        if (al == null && Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            al = Cigarette.INSTANCE.getModuleManager().getModuleByClass(ArrayListModule.class);
        }
        return al;
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
        if (arrayListX < 0) {
            if (mc != null) {
                ScaledResolution sr = new ScaledResolution(mc);
                return Math.max(10.0f, sr.getScaledWidth() - 102.0f);
            }
            return 10.0f;
        }
        return arrayListX;
    }

    public float getRawArrayListX() {
        return arrayListX;
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
