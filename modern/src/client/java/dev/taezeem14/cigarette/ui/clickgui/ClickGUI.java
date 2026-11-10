package dev.taezeem14.cigarette.ui.clickgui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.render.ClickGUIModule;
import dev.taezeem14.cigarette.ui.hud.HUDEditor;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends Screen {

    private static final DecimalFormat SCALE_FORMAT = new DecimalFormat("0.##");

    private final List<CategoryFrame> frames = new ArrayList<>();
    private ProfilePanel profilePanel;
    private boolean showProfiles = false;

    private String searchFilter = "";
    private boolean searchFocused = false;
    private float guiScale = 1.0F;
    private boolean draggingScale = false;
    private float fadeProgress = 0.0F;
    private float lastViewportW = -1.0F;
    private float lastGuiScale = -1.0F;

    public ClickGUI() {
        super(Component.literal("Cigarette ClickGUI"));
        initFrames();
    }

    private void initFrames() {
        frames.clear();
        for (Category c : Category.values()) {
            List<Module> list = CigaretteModernClient.getInstance() != null && CigaretteModernClient.getInstance().getModuleManager() != null
                    ? CigaretteModernClient.getInstance().getModuleManager().getModulesByCategory(c)
                    : new ArrayList<>();
            if (!list.isEmpty()) {
                frames.add(new CategoryFrame(c, list, 20.0F, 48.0F));
            }
        }
        profilePanel = new ProfilePanel(120.0F, 54.0F);
        lastViewportW = -1.0F;
    }

    private void autoArrangeFrames(float viewportW, float viewportH) {
        if (frames.isEmpty()) return;
        for (CategoryFrame f : frames) {
            if (f.hasUserMoved()) return;
        }
        float frameW = 122.0F;
        float gap = 8.0F;
        float startY = 46.0F;
        int maxCols = Math.max(1, (int) ((viewportW - 16.0F + gap) / (frameW + gap)));
        int cols = Math.min(frames.size(), maxCols);
        float totalRowW = cols * frameW + (cols - 1) * gap;
        float startX = Math.max(8.0F, (viewportW - totalRowW) / 2.0F);

        for (int i = 0; i < frames.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            frames.get(i).setWidth(frameW);
            frames.get(i).setX(startX + col * (frameW + gap));
            frames.get(i).setY(Math.min(startY + row * 190.0F, Math.max(startY, viewportH - 60.0F)));
        }
        if (profilePanel != null) {
            profilePanel.setX(Math.max(8.0F, (viewportW - profilePanel.getWidth()) / 2.0F));
            profilePanel.setY(Math.max(46.0F, Math.min(72.0F, (viewportH - profilePanel.getHeight()) / 2.0F)));
        }
    }

    @Override
    protected void init() {
        super.init();
        fadeProgress = 0.0F;
        ClickGUIModule mod = getClickGUIModule();
        if (mod != null) mod.syncThemeFromSetting();
    }

    @Override
    public void removed() {
        super.removed();
        draggingScale = false;
        RenderUtil.clearScissorStack();
        ClickGUIModule mod = getClickGUIModule();
        if (mod != null && mod.isEnabled()) {
            mod.setState(false);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        fadeProgress = Math.min(1.0F, fadeProgress + AnimationUtil.getDeltaTime() * 6.5F);
        ClickGUIModule mod = getClickGUIModule();
        if (mod != null) mod.syncThemeFromSetting();

        float screenW = this.width;
        float screenH = this.height;

        if (mod == null || mod.isBlurEnabled()) {
            g.blurBeforeThisStratum();
            g.nextStratum();
        }
        int overlayAlpha = (int) (135 * fadeProgress);
        RenderUtil.drawRect(g, 0, 0, screenW, screenH, (overlayAlpha << 24) | 0x0A0A0E);

        float viewportW = screenW / guiScale;
        float viewportH = screenH / guiScale;
        if (Math.abs(viewportW - lastViewportW) > 1.0F || Math.abs(guiScale - lastGuiScale) > 0.01F) {
            autoArrangeFrames(viewportW, viewportH);
            lastViewportW = viewportW;
            lastGuiScale = guiScale;
        }

        g.pose().pushMatrix();
        try {
            g.pose().scale(guiScale, guiScale);
            float smx = mouseX / guiScale;
            float smy = mouseY / guiScale;

            renderTopDock(g, smx, smy, viewportW);

            Module hoveredModule = null;
            if (showProfiles) {
                profilePanel.render(g, smx, smy, partialTicks, guiScale, viewportW, viewportH);
            } else {
                for (CategoryFrame frame : frames) {
                    frame.render(g, smx, smy, partialTicks, searchFilter, guiScale, viewportW, viewportH);
                    if (hoveredModule == null) {
                        hoveredModule = frame.getHoveredModule(smx, smy, searchFilter);
                    }
                }
            }
            renderTooltipBar(g, viewportW, viewportH, hoveredModule);
        } finally {
            g.pose().popMatrix();
            RenderUtil.clearScissorStack();
        }
    }

    private DockLayout computeDock(float w) {
        float dockH = 28.0F;
        float dockY = 8.0F;
        float dockW = Math.min(w - 16.0F, 680.0F);
        float dockX = (w - dockW) / 2.0F;
        return new DockLayout(dockX, dockY, dockW, dockH);
    }

    private void renderTopDock(GuiGraphicsExtractor g, float mouseX, float mouseY, float width) {
        DockLayout dock = computeDock(width);
        int accent = ColorUtil.getAccent();

        RenderUtil.drawDropShadow(g, dock.x, dock.y, dock.w, dock.h, 5.0F, 0x60000000);
        RenderUtil.drawRoundedRect(g, dock.x, dock.y, dock.w, dock.h, 5.0F, ColorUtil.HEADER);
        RenderUtil.drawRoundedOutline(g, dock.x, dock.y, dock.w, dock.h, 5.0F, 1.0F, ColorUtil.BORDER);
        RenderUtil.drawRoundedRectTop(g, dock.x + 1.0F, dock.y, dock.w - 2.0F, 1.5F, 1.5F, ColorUtil.modifyAlpha(accent, 180));

        float logoX = dock.x + 10.0F;
        RenderUtil.drawText(g, "CIGARETTE", logoX, dock.y + (dock.h - RenderUtil.getFontHeight()) / 2.0F, ColorUtil.EMBER_ORANGE, false);
        float verX = logoX + RenderUtil.getStringWidth("CIGARETTE") + 4.0F;
        RenderUtil.drawSmallText(g, "26.3", verX, dock.y + (dock.h - RenderUtil.getSmallFontHeight()) / 2.0F, ColorUtil.TEXT_MUTED, false);

        float hudBtnW = 62.0F;
        float profBtnW = 52.0F;
        float themeW = 78.0F;
        float trackW = 48.0F;
        float scaleLabelW = 46.0F;
        float rightCursor = dock.x + dock.w - 8.0F;
        float btnY = dock.y + 5.0F;
        float btnH = 18.0F;

        float hudBtnX = rightCursor - hudBtnW;
        boolean hudHov = mouseX >= hudBtnX && mouseX <= hudBtnX + hudBtnW && mouseY >= btnY && mouseY <= btnY + btnH;
        RenderUtil.drawRoundedRect(g, hudBtnX, btnY, hudBtnW, btnH, 3.0F, hudHov ? 0xFF2B2B35 : 0xFF1F1F26);
        RenderUtil.drawRoundedOutline(g, hudBtnX, btnY, hudBtnW, btnH, 3.0F, 1.0F, ColorUtil.BORDER);
        RenderUtil.drawCenteredSmallText(g, "HUD Editor", hudBtnX + hudBtnW / 2.0F, btnY + (btnH - RenderUtil.getSmallFontHeight()) / 2.0F, ColorUtil.TEXT_PRIMARY, false);
        rightCursor = hudBtnX - 5.0F;

        float profBtnX = rightCursor - profBtnW;
        boolean profHov = mouseX >= profBtnX && mouseX <= profBtnX + profBtnW && mouseY >= btnY && mouseY <= btnY + btnH;
        RenderUtil.drawRoundedRect(g, profBtnX, btnY, profBtnW, btnH, 3.0F, showProfiles ? accent : (profHov ? 0xFF2B2B35 : 0xFF1F1F26));
        if (!showProfiles) RenderUtil.drawRoundedOutline(g, profBtnX, btnY, profBtnW, btnH, 3.0F, 1.0F, ColorUtil.BORDER);
        RenderUtil.drawCenteredSmallText(g, "Profiles", profBtnX + profBtnW / 2.0F, btnY + (btnH - RenderUtil.getSmallFontHeight()) / 2.0F,
                showProfiles ? 0xFFFFFFFF : ColorUtil.TEXT_PRIMARY, false);
        rightCursor = profBtnX - 5.0F;

        float themeX = rightCursor - themeW;
        boolean themeHov = mouseX >= themeX && mouseX <= themeX + themeW && mouseY >= btnY && mouseY <= btnY + btnH;
        RenderUtil.drawRoundedRect(g, themeX, btnY, themeW, btnH, 3.0F, themeHov ? 0xFF282832 : 0xFF1D1D24);
        RenderUtil.drawRoundedOutline(g, themeX, btnY, themeW, btnH, 3.0F, 1.0F, themeHov ? accent : ColorUtil.BORDER);
        RenderUtil.drawCircle(g, themeX + 8.0F, btnY + btnH / 2.0F, 3.2F, accent);
        RenderUtil.drawSmallText(g, ColorUtil.getThemePreset(), themeX + 14.5F, btnY + (btnH - RenderUtil.getSmallFontHeight()) / 2.0F, ColorUtil.TEXT_PRIMARY, false);
        rightCursor = themeX - 8.0F;

        float trackX = rightCursor - trackW;
        float scaleX = trackX - scaleLabelW;
        float trackY = dock.y + 12.5F;
        float trackH = 3.0F;

        RenderUtil.drawSmallText(g, "Scale " + SCALE_FORMAT.format(guiScale) + "x", scaleX, dock.y + (dock.h - RenderUtil.getSmallFontHeight()) / 2.0F, ColorUtil.TEXT_MUTED, false);
        RenderUtil.drawRoundedRect(g, trackX, trackY, trackW, trackH, 1.5F, 0xFF26262E);
        if (draggingScale) {
            float frac = AnimationUtil.clamp((mouseX - trackX) / trackW, 0.0F, 1.0F);
            this.guiScale = 0.75F + (1.5F - 0.75F) * frac;
        }
        float scaleFrac = AnimationUtil.clamp((guiScale - 0.75F) / (1.5F - 0.75F), 0.0F, 1.0F);
        float fillW = trackW * scaleFrac;
        if (fillW > 0.5F) RenderUtil.drawRoundedRect(g, trackX, trackY, fillW, trackH, 1.5F, accent);
        RenderUtil.drawCircle(g, trackX + fillW, trackY + trackH / 2.0F, 3.0F, 0xFFF5F5F7);

        float searchX = verX + RenderUtil.getSmallStringWidth("26.3") + 10.0F;
        float searchW = Math.max(70.0F, scaleX - searchX - 10.0F);
        RenderUtil.drawRoundedRect(g, searchX, btnY, searchW, btnH, 3.0F, 0xFF131317);
        RenderUtil.drawRoundedOutline(g, searchX, btnY, searchW, btnH, 3.0F, 1.0F, searchFocused ? accent : ColorUtil.BORDER);
        RenderUtil.drawFaIcon(g, "search", searchX + 5.5F, btnY + (btnH - 8.0F) / 2.0F, 8.0F, searchFocused ? accent : ColorUtil.TEXT_MUTED);

        String searchDisplay = searchFilter.isEmpty() && !searchFocused
                ? "Search modules..."
                : searchFilter + (searchFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : "");
        int searchCol = searchFilter.isEmpty() && !searchFocused ? 0xFF666670 : ColorUtil.TEXT_PRIMARY;
        RenderUtil.pushScissor(g, searchX + 16.0F, btnY, searchW - 30.0F, btnH, guiScale);
        try {
            RenderUtil.drawSmallText(g, searchDisplay, searchX + 17.0F, btnY + (btnH - RenderUtil.getSmallFontHeight()) / 2.0F, searchCol, false);
        } finally {
            RenderUtil.popScissor(g);
        }
        if (!searchFilter.isEmpty()) {
            float clearX = searchX + searchW - 14.0F;
            boolean clearHov = mouseX >= clearX && mouseX <= searchX + searchW && mouseY >= btnY && mouseY <= btnY + btnH;
            RenderUtil.drawCenteredSmallText(g, "x", clearX + 7.0F, btnY + (btnH - RenderUtil.getSmallFontHeight()) / 2.0F,
                    clearHov ? accent : ColorUtil.TEXT_MUTED, false);
        }
    }

    private void renderTooltipBar(GuiGraphicsExtractor g, float viewportW, float viewportH, Module hovered) {
        String text = hovered != null
                ? hovered.getName() + "  \u2022  \u00a77" + hovered.getDescription() + "  \u2022  \u00a7fLMB: Toggle \u2022 RMB: Settings \u2022 MMB: Bind"
                : "\u00a77LMB: Toggle \u2022 RMB: Settings \u2022 MMB: Bind \u2022 Scroll over panels";
        float pillW = Math.min(viewportW - 24.0F, RenderUtil.getSmallStringWidth(text) + 20.0F);
        float pillH = 17.0F;
        float pillX = (viewportW - pillW) / 2.0F;
        float pillY = viewportH - pillH - 8.0F;
        int accent = ColorUtil.getAccent();
        RenderUtil.drawDropShadow(g, pillX, pillY, pillW, pillH, 4.0F, 0x50000000);
        RenderUtil.drawRoundedRect(g, pillX, pillY, pillW, pillH, 4.0F, 0xE6141418);
        RenderUtil.drawRoundedOutline(g, pillX, pillY, pillW, pillH, 4.0F, 1.0F,
                hovered != null ? ColorUtil.modifyAlpha(accent, 140) : ColorUtil.BORDER);
        RenderUtil.drawCenteredSmallText(g, text, pillX + pillW / 2.0F, pillY + (pillH - RenderUtil.getSmallFontHeight()) / 2.0F,
                hovered != null ? accent : ColorUtil.TEXT_MUTED, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float smx = (float) (mouseX / guiScale);
        float smy = (float) (mouseY / guiScale);
        if (showProfiles) {
            profilePanel.handleScroll(smx, smy, verticalAmount);
            return true;
        }
        for (int i = frames.size() - 1; i >= 0; i--) {
            if (frames.get(i).handleScroll(smx, smy, verticalAmount, searchFilter)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        float smx = (float) (event.x() / guiScale);
        float smy = (float) (event.y() / guiScale);
        int btn = event.button();
        float width = this.width / guiScale;
        DockLayout dock = computeDock(width);

        float btnY = dock.y + 5.0F;
        float btnH = 18.0F;
        float rightCursor = dock.x + dock.w - 8.0F;

        float hudBtnX = rightCursor - 62.0F;
        if (smx >= hudBtnX && smx <= hudBtnX + 62.0F && smy >= btnY && smy <= btnY + btnH) {
            this.minecraft.gui.setScreen(new HUDEditor());
            return true;
        }
        rightCursor = hudBtnX - 5.0F;

        float profBtnX = rightCursor - 52.0F;
        if (smx >= profBtnX && smx <= profBtnX + 52.0F && smy >= btnY && smy <= btnY + btnH) {
            showProfiles = !showProfiles;
            return true;
        }
        rightCursor = profBtnX - 5.0F;

        float themeX = rightCursor - 78.0F;
        if (smx >= themeX && smx <= themeX + 78.0F && smy >= btnY && smy <= btnY + btnH) {
            String nextPreset = ColorUtil.cycleThemePreset();
            ClickGUIModule mod = getClickGUIModule();
            if (mod != null) mod.setThemePreset(nextPreset);
            return true;
        }
        rightCursor = themeX - 8.0F;

        float trackX = rightCursor - 48.0F;
        float scaleX = trackX - 46.0F;
        if (smx >= trackX - 4.0F && smx <= trackX + 52.0F && smy >= dock.y && smy <= dock.y + dock.h) {
            draggingScale = true;
            float frac = AnimationUtil.clamp((smx - trackX) / 48.0F, 0.0F, 1.0F);
            this.guiScale = 0.75F + (1.5F - 0.75F) * frac;
            return true;
        }

        float verX = dock.x + 10.0F + RenderUtil.getStringWidth("CIGARETTE") + 4.0F;
        float searchX = verX + RenderUtil.getSmallStringWidth("26.3") + 10.0F;
        float searchW = Math.max(70.0F, scaleX - searchX - 10.0F);
        if (smx >= searchX && smx <= searchX + searchW && smy >= btnY && smy <= btnY + btnH) {
            if (!searchFilter.isEmpty() && smx >= searchX + searchW - 14.0F) searchFilter = "";
            searchFocused = true;
            return true;
        } else {
            searchFocused = false;
        }

        if (showProfiles) {
            profilePanel.mouseClicked(smx, smy, btn);
            return true;
        }
        for (int i = frames.size() - 1; i >= 0; i--) {
            CategoryFrame f = frames.get(i);
            if (f.mouseClicked(smx, smy, btn, searchFilter)) {
                if (i != frames.size() - 1) {
                    frames.remove(i);
                    frames.add(f);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        float smx = (float) (event.x() / guiScale);
        float smy = (float) (event.y() / guiScale);
        draggingScale = false;
        if (!showProfiles) {
            for (CategoryFrame f : frames) f.mouseReleased(smx, smy, event.button());
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        boolean frameListening = false;
        for (CategoryFrame f : frames) {
            if (f.isListening()) {
                frameListening = true;
                break;
            }
        }
        if (frameListening) {
            for (CategoryFrame f : frames) f.keyTyped('\0', keyCode);
            return true;
        }

        ClickGUIModule mod = getClickGUIModule();
        if (keyCode == InputConstants.KEY_ESCAPE || (!searchFocused && mod != null && keyCode == mod.getKeyCode())) {
            this.minecraft.gui.setScreen(null);
            return true;
        }
        if (searchFocused) {
            if (keyCode == InputConstants.KEY_BACKSPACE && !searchFilter.isEmpty()) {
                searchFilter = searchFilter.substring(0, searchFilter.length() - 1);
            } else if (keyCode == InputConstants.KEY_RETURN) {
                searchFocused = false;
            }
            return true;
        }
        if (!showProfiles) {
            for (CategoryFrame f : frames) f.keyTyped('\0', keyCode);
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchFocused && event.isAllowedChatCharacter()) {
            String cp = event.codepointAsString();
            if (cp != null && searchFilter.length() < 30) {
                searchFilter += cp;
            }
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private ClickGUIModule getClickGUIModule() {
        return CigaretteModernClient.getInstance() != null && CigaretteModernClient.getInstance().getModuleManager() != null
                ? CigaretteModernClient.getInstance().getModuleManager().getModuleByClass(ClickGUIModule.class) : null;
    }

    public float getGuiScale() { return guiScale; }
    public void setGuiScale(float guiScale) { this.guiScale = AnimationUtil.clamp(guiScale, 0.75F, 1.5F); }
    public List<CategoryFrame> getFrames() { return frames; }

    private static final class DockLayout {
        final float x, y, w, h;
        DockLayout(float x, float y, float w, float h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
        }
    }
}
