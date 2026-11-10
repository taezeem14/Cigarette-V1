package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.client.ArrayListModule;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.util.player.CPSTracker;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

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

    private float watermarkX = 6.0F;
    private float watermarkY = 6.0F;
    private float arrayListX = -1.0F;
    private float arrayListY = 6.0F;

    public HUD() {
        super("HUD", "In-game heads-up display and active modules", Category.RENDER);
        addSettings(watermark, arrayList, sideBar, showCoords, showFps, showCps);
        setState(true);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.getDebugOverlay() != null && mc.getDebugOverlay().showDebugScreen()) return;

        CigaretteModernClient inst = CigaretteModernClient.getInstance();
        if (inst != null && inst.getModuleManager() != null) {
            ClickGUIModule guiMod = inst.getModuleManager().getModuleByClass(ClickGUIModule.class);
            if (guiMod != null) {
                guiMod.syncThemeFromSetting();
            }
        }

        GuiGraphicsExtractor g = event.getGraphics();
        int screenW = event.getScreenWidth();
        int screenH = event.getScreenHeight();

        if (watermark.isEnabled()) {
            renderWatermark(g);
        }

        ArrayListModule al = getArrayListModule();
        boolean showAl = arrayList.isEnabled() && (al == null || al.isEnabled());
        if (showAl && inst != null && inst.getModuleManager() != null) {
            renderArrayList(g, screenW, al);
        }

        renderInfoOverlay(g, screenH);

        if (inst != null && inst.getNotificationManager() != null) {
            inst.getNotificationManager().render(g, screenW, screenH);
        }
    }

    private void renderWatermark(GuiGraphicsExtractor g) {
        String brand = "CIGARETTE";
        String rest = " \u2022 26.3 \u2022 " + mc.getFps() + " FPS \u2022 "
                + CPSTracker.getLeftCPS() + " | " + CPSTracker.getRightCPS() + " CPS";

        int brandW = RenderUtil.getStringWidth(brand);
        float restW = RenderUtil.getSmallStringWidth(rest);
        float totalW = brandW + restW + 18.0F;
        float totalH = 19.0F;
        int accent = getHUDColor(0);

        RenderUtil.drawDropShadow(g, watermarkX, watermarkY, totalW, totalH, 4.0F, 0x55000000);
        RenderUtil.drawRoundedRect(g, watermarkX, watermarkY, totalW, totalH, 4.0F, 0xDC141418);
        RenderUtil.drawRoundedOutline(g, watermarkX, watermarkY, totalW, totalH, 4.0F, 1.0F, ColorUtil.BORDER);
        RenderUtil.drawRoundedRectTop(g, watermarkX + 1.0F, watermarkY, totalW - 2.0F, 1.5F, 1.5F, ColorUtil.modifyAlpha(accent, 180));
        RenderUtil.drawRoundedRect(g, watermarkX + 4.0F, watermarkY + 4.5F, 2.0F, totalH - 9.0F, 1.0F, accent);

        RenderUtil.drawText(g, brand, watermarkX + 9.0F, watermarkY + (totalH - RenderUtil.getFontHeight()) / 2.0F + 0.5F, accent, false);
        RenderUtil.drawSmallText(g, rest, watermarkX + 9.0F + brandW, watermarkY + (totalH - RenderUtil.getSmallFontHeight()) / 2.0F + 0.5F, ColorUtil.TEXT_PRIMARY, false);
    }

    private void renderArrayList(GuiGraphicsExtractor g, float screenW, ArrayListModule al) {
        boolean showSuffix = al == null || al.getSuffixSetting().isEnabled();
        boolean showBg = al == null || al.getBackground().isEnabled();
        boolean useAnim = al == null || al.getAnimation().isEnabled();

        List<Module> modules = new ArrayList<>();
        for (Module m : CigaretteModernClient.getInstance().getModuleManager().getModules()) {
            if (m instanceof ClickGUIModule || m.isHidden()) continue;
            float target = m.isEnabled() ? 1.0F : 0.0F;
            float anim = useAnim ? AnimationUtil.lerp(m.getAnimationProgress(), target, 14.0F) : target;
            m.setAnimationProgress(anim);
            if (anim > 0.02F) modules.add(m);
        }

        modules.sort(Comparator.comparingInt((Module m) -> RenderUtil.getStringWidth(formatDisplay(m, showSuffix))).reversed());

        float y = arrayListY;
        int idx = 0;
        for (Module m : modules) {
            float anim = AnimationUtil.clamp(m.getAnimationProgress(), 0.0F, 1.0F);
            String text = formatDisplay(m, showSuffix);
            int strW = RenderUtil.getStringWidth(text);
            float itemW = strW + 10.0F;
            float itemH = 13.5F;

            float baseX = arrayListX < 0 ? screenW - itemW - (sideBar.isEnabled() ? 4.0F : 2.0F) : arrayListX;
            float slide = (1.0F - AnimationUtil.easeOutCubic(anim)) * (itemW + 8.0F);
            float itemX = arrayListX < 0 ? baseX + slide : baseX - slide;

            int baseColor = getHUDColor(idx * 120);
            int alpha = Math.max(10, Math.min(255, (int) (255 * anim)));
            int color = ColorUtil.modifyAlpha(baseColor, alpha);

            if (showBg) {
                RenderUtil.drawRoundedRect(g, itemX, y, itemW, itemH - 0.5F, 2.0F, ColorUtil.modifyAlpha(0xFF141418, (int) (185 * anim)));
            }
            RenderUtil.drawText(g, text, itemX + 5.0F, y + (itemH - RenderUtil.getFontHeight()) / 2.0F, color, true);

            if (sideBar.isEnabled()) {
                float barX = itemX + itemW + 0.5F;
                RenderUtil.drawRect(g, barX - 0.5F, y, 3.0F, itemH - 0.5F, ColorUtil.modifyAlpha(baseColor, (int) (60 * anim)));
                RenderUtil.drawRoundedRect(g, barX, y + 0.5F, 2.0F, itemH - 1.5F, 1.0F, color);
            }
            y += itemH * anim;
            idx++;
        }
    }

    private String formatDisplay(Module m, boolean showSuffix) {
        if (!showSuffix) return m.getName();
        String suffix = m.getSuffix();
        if (suffix != null && !suffix.trim().isEmpty()) {
            return m.getName() + " \u00a77" + suffix.trim();
        }
        return m.getName();
    }

    private void renderInfoOverlay(GuiGraphicsExtractor g, float screenH) {
        float y = screenH - 12.0F;
        if (showCoords.isEnabled() && mc.player != null) {
            String coords = String.format("\u00a77XYZ \u00a7f%.1f, %.1f, %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            RenderUtil.drawSmallText(g, coords, 6.0F, y, ColorUtil.TEXT_PRIMARY, true);
            y -= 10.0F;
        }
        if (showFps.isEnabled()) {
            RenderUtil.drawSmallText(g, "\u00a77FPS \u00a7f" + mc.getFps(), 6.0F, y, ColorUtil.TEXT_PRIMARY, true);
            y -= 10.0F;
        }
        if (showCps.isEnabled()) {
            RenderUtil.drawSmallText(g, "\u00a77CPS \u00a7f" + CPSTracker.getLeftCPS() + " \u00a77| \u00a7f" + CPSTracker.getRightCPS(), 6.0F, y, ColorUtil.TEXT_PRIMARY, true);
        }
    }

    public int getHUDColor(int offset) {
        ArrayListModule al = getArrayListModule();
        if (al == null) return ColorUtil.getAccent();

        String m = al.getColorMode().getMode();
        float speed = Math.max(0.2F, al.getRainbowSpeed().getValueFloat());
        long cycleMs = (long) (4000.0F / speed);
        switch (m.toLowerCase()) {
            case "rainbow": {
                float hue = ((System.currentTimeMillis() + offset) % cycleMs) / (float) cycleMs;
                return 0xFF000000 | (Color.HSBtoRGB(hue, 0.78F, 1.0F) & 0x00FFFFFF);
            }
            case "astolfo": {
                float hue = ((System.currentTimeMillis() + offset) % cycleMs) / (float) cycleMs;
                hue = hue > 0.5F ? 1.0F - hue : hue;
                return 0xFF000000 | (Color.HSBtoRGB(hue + 0.5F, 0.6F, 1.0F) & 0x00FFFFFF);
            }
            case "static":
                return al.getStaticColor().getRGB();
            default:
                return ColorUtil.getAccent();
        }
    }

    private ArrayListModule getArrayListModule() {
        ArrayListModule al = ArrayListModule.getInstance();
        if (al == null && CigaretteModernClient.getInstance() != null && CigaretteModernClient.getInstance().getModuleManager() != null) {
            al = CigaretteModernClient.getInstance().getModuleManager().getModuleByClass(ArrayListModule.class);
        }
        return al;
    }

    public float getWatermarkX() { return watermarkX; }
    public void setWatermarkX(float x) { this.watermarkX = x; }
    public float getWatermarkY() { return watermarkY; }
    public void setWatermarkY(float y) { this.watermarkY = y; }
    public float getRawArrayListX() { return arrayListX; }
    public float getArrayListX() {
        if (arrayListX < 0 && mc.getWindow() != null) {
            return Math.max(10.0F, mc.getWindow().getGuiScaledWidth() - 102.0F);
        }
        return arrayListX < 0 ? 10.0F : arrayListX;
    }
    public void setArrayListX(float x) { this.arrayListX = x; }
    public float getArrayListY() { return arrayListY; }
    public void setArrayListY(float y) { this.arrayListY = y; }
}
