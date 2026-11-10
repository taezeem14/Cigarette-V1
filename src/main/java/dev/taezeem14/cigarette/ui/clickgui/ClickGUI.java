package dev.taezeem14.cigarette.ui.clickgui;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.render.ClickGUIModule;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.ui.hud.HUDEditor;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.BlurUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends GuiScreen {

    private static final DecimalFormat SCALE_FORMAT = new DecimalFormat("0.##");

    private final List<CategoryFrame> frames = new ArrayList<>();
    private ProfilePanel profilePanel;
    private boolean showProfiles = false;

    // Search bar state
    private String searchFilter = "";
    private boolean searchFocused = false;

    // GUI scale (0.75x - 1.5x)
    private float guiScale = 1.0f;
    private boolean draggingScale = false;

    // Smooth opening fade animation
    private float fadeProgress = 0.0f;
    private float lastViewportW = -1.0f;
    private float lastGuiScale = -1.0f;

    public ClickGUI() {
        initFrames();
    }

    private void initFrames() {
        frames.clear();

        Category[] categories = {
                Category.COMBAT,
                Category.BLATANT,
                Category.RENDER,
                Category.UTILITY,
                Category.CLIENT,
                Category.WORLD
        };

        for (Category category : categories) {
            List<Module> categoryModules = new ArrayList<>();
            if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
                categoryModules = Cigarette.INSTANCE.getModuleManager().getModulesByCategory(category);
            }
            if (categoryModules.isEmpty()) {
                continue; // Skip empty categories so no blank panels appear
            }
            frames.add(new CategoryFrame(category, categoryModules, 20.0f, 48.0f));
        }

        profilePanel = new ProfilePanel(120.0f, 54.0f);
        lastViewportW = -1.0f;
    }

    private void autoArrangeFrames(float viewportW, float viewportH) {
        if (frames.isEmpty()) return;

        for (CategoryFrame frame : frames) {
            if (frame.hasUserMoved()) {
                return; // Preserve user-dragged positions
            }
        }

        float frameW = 116.0f;
        float gap = 8.0f;
        float startY = 46.0f;

        int maxCols = Math.max(1, (int) ((viewportW - 16.0f + gap) / (frameW + gap)));
        int cols = Math.min(frames.size(), maxCols);
        float totalRowW = cols * frameW + (cols - 1) * gap;
        float startX = Math.max(8.0f, (viewportW - totalRowW) / 2.0f);

        for (int i = 0; i < frames.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            float fx = startX + col * (frameW + gap);
            float fy = startY + row * 190.0f;
            frames.get(i).setWidth(frameW);
            frames.get(i).setX(fx);
            frames.get(i).setY(Math.min(fy, Math.max(startY, viewportH - 60.0f)));
        }

        if (profilePanel != null) {
            profilePanel.setX(Math.max(8.0f, (viewportW - profilePanel.getWidth()) / 2.0f));
            profilePanel.setY(Math.max(46.0f, Math.min(72.0f, (viewportH - profilePanel.getHeight()) / 2.0f)));
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        FontUtil.ensureInitialized();
        fadeProgress = 0.0f;
        Keyboard.enableRepeatEvents(true);

        ClickGUIModule clickGUIModule = getClickGUIModule();
        if (clickGUIModule != null) {
            clickGUIModule.syncThemeFromSetting();
        }

        // Re-align frames only if module list changed
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            int nonEmptyCount = 0;
            for (Category c : Category.values()) {
                if (!Cigarette.INSTANCE.getModuleManager().getModulesByCategory(c).isEmpty()) {
                    nonEmptyCount++;
                }
            }
            boolean mismatch = frames.size() != nonEmptyCount;
            if (!mismatch) {
                for (CategoryFrame frame : frames) {
                    List<Module> currentModules = Cigarette.INSTANCE.getModuleManager().getModulesByCategory(frame.getCategory());
                    if (frame.getButtons().size() != currentModules.size()) {
                        mismatch = true;
                        break;
                    }
                }
            }
            if (mismatch) {
                initFrames();
            }
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        draggingScale = false;
        RenderUtil.clearScissorStack();

        ClickGUIModule clickGUIModule = getClickGUIModule();
        if (clickGUIModule != null && clickGUIModule.isEnabled()) {
            clickGUIModule.setState(false);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        FontUtil.ensureInitialized();
        fadeProgress = Math.min(1.0f, fadeProgress + AnimationUtil.getDeltaTime() * 6.5f);

        ClickGUIModule clickGUIModule = getClickGUIModule();
        if (clickGUIModule != null) {
            clickGUIModule.syncThemeFromSetting();
        }

        ScaledResolution sr = new ScaledResolution(mc);
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        // Optional Gaussian backdrop blur behind dark vignette
        if (clickGUIModule == null || clickGUIModule.isBlurEnabled()) {
            BlurUtil.blurAll(Math.max(1.0f, 6.0f * AnimationUtil.easeOutCubic(fadeProgress)));
        }

        int overlayAlpha = (int) (135 * fadeProgress);
        RenderUtil.drawRect(0, 0, screenW, screenH, (overlayAlpha << 24) | 0x0A0A0E);

        float viewportW = screenW / guiScale;
        float viewportH = screenH / guiScale;
        if (Math.abs(viewportW - lastViewportW) > 1.0f || Math.abs(guiScale - lastGuiScale) > 0.01f) {
            autoArrangeFrames(viewportW, viewportH);
            lastViewportW = viewportW;
            lastGuiScale = guiScale;
        }

        GlStateManager.pushMatrix();
        try {
            GlStateManager.scale(guiScale, guiScale, 1.0f);

            float scaledMouseX = mouseX / guiScale;
            float scaledMouseY = mouseY / guiScale;

            renderTopDock(scaledMouseX, scaledMouseY, viewportW);

            Module hoveredModule = null;
            if (showProfiles) {
                profilePanel.render(scaledMouseX, scaledMouseY, partialTicks, guiScale, viewportW, viewportH);
            } else {
                for (CategoryFrame frame : frames) {
                    frame.render(scaledMouseX, scaledMouseY, partialTicks, searchFilter, guiScale, viewportW, viewportH);
                    if (hoveredModule == null) {
                        hoveredModule = frame.getHoveredModule(scaledMouseX, scaledMouseY, searchFilter);
                    }
                }
            }

            renderTooltipBar(viewportW, viewportH, hoveredModule);
        } finally {
            GlStateManager.popMatrix();
            RenderUtil.clearScissorStack();
        }
    }

    private DockLayout computeDockLayout(float width) {
        float dockH = 28.0f;
        float dockY = 8.0f;
        float dockW = Math.min(width - 16.0f, 680.0f);
        float dockX = (width - dockW) / 2.0f;
        return new DockLayout(dockX, dockY, dockW, dockH);
    }

    private void renderTopDock(float mouseX, float mouseY, float width) {
        DockLayout dock = computeDockLayout(width);
        int accent = ColorUtil.getAccent();

        RenderUtil.drawDropShadow(dock.x, dock.y, dock.w, dock.h, 5.0f, 0x60000000);
        RenderUtil.drawRoundedRect(dock.x, dock.y, dock.w, dock.h, 5.0f, ColorUtil.HEADER);
        RenderUtil.drawRoundedOutline(dock.x, dock.y, dock.w, dock.h, 5.0f, 1.0f, ColorUtil.BORDER);
        RenderUtil.drawRoundedRectTop(dock.x + 1.0f, dock.y, dock.w - 2.0f, 1.5f, 1.5f, ColorUtil.modifyAlpha(accent, 180));

        // Logo
        float logoX = dock.x + 10.0f;
        FontUtil.bold.drawString("CIGARETTE", logoX, dock.y + (dock.h - FontUtil.bold.getHeight()) / 2.0f, ColorUtil.EMBER_ORANGE);
        float verX = logoX + FontUtil.bold.getStringWidth("CIGARETTE") + 4.0f;
        FontUtil.small.drawString("V1", verX, dock.y + (dock.h - FontUtil.small.getHeight()) / 2.0f, ColorUtil.TEXT_MUTED);

        // Right-anchored buttons layout
        float hudBtnW = 62.0f;
        float profBtnW = 52.0f;
        float themeW = 78.0f;
        float trackW = 48.0f;
        float scaleLabelW = 46.0f;
        float rightCursor = dock.x + dock.w - 8.0f;

        // HUD Editor Button
        float hudBtnX = rightCursor - hudBtnW;
        float btnY = dock.y + 5.0f;
        float btnH = 18.0f;
        boolean hudHov = mouseX >= hudBtnX && mouseX <= hudBtnX + hudBtnW && mouseY >= btnY && mouseY <= btnY + btnH;
        RenderUtil.drawRoundedRect(hudBtnX, btnY, hudBtnW, btnH, 3.0f, hudHov ? 0xFF2B2B35 : 0xFF1F1F26);
        RenderUtil.drawRoundedOutline(hudBtnX, btnY, hudBtnW, btnH, 3.0f, 1.0f, ColorUtil.BORDER);
        FontUtil.small.drawCenteredString("HUD Editor", hudBtnX + hudBtnW / 2.0f, btnY + (btnH - FontUtil.small.getHeight()) / 2.0f, ColorUtil.TEXT_PRIMARY);
        rightCursor = hudBtnX - 5.0f;

        // Profiles Button
        float profBtnX = rightCursor - profBtnW;
        boolean profHov = mouseX >= profBtnX && mouseX <= profBtnX + profBtnW && mouseY >= btnY && mouseY <= btnY + btnH;
        RenderUtil.drawRoundedRect(profBtnX, btnY, profBtnW, btnH, 3.0f, showProfiles ? accent : (profHov ? 0xFF2B2B35 : 0xFF1F1F26));
        if (!showProfiles) {
            RenderUtil.drawRoundedOutline(profBtnX, btnY, profBtnW, btnH, 3.0f, 1.0f, ColorUtil.BORDER);
        }
        FontUtil.small.drawCenteredString("Profiles", profBtnX + profBtnW / 2.0f, btnY + (btnH - FontUtil.small.getHeight()) / 2.0f, showProfiles ? 0xFFFFFFFF : ColorUtil.TEXT_PRIMARY);
        rightCursor = profBtnX - 5.0f;

        // Theme Accent Switcher Pill
        float themeX = rightCursor - themeW;
        boolean themeHov = mouseX >= themeX && mouseX <= themeX + themeW && mouseY >= btnY && mouseY <= btnY + btnH;
        RenderUtil.drawRoundedRect(themeX, btnY, themeW, btnH, 3.0f, themeHov ? 0xFF282832 : 0xFF1D1D24);
        RenderUtil.drawRoundedOutline(themeX, btnY, themeW, btnH, 3.0f, 1.0f, themeHov ? accent : ColorUtil.BORDER);
        RenderUtil.drawCircle(themeX + 8.0f, btnY + btnH / 2.0f, 3.2f, accent);
        FontUtil.small.drawString(ColorUtil.getThemePreset(), themeX + 14.5f, btnY + (btnH - FontUtil.small.getHeight()) / 2.0f, ColorUtil.TEXT_PRIMARY);
        rightCursor = themeX - 8.0f;

        // Scale Slider
        float trackX = rightCursor - trackW;
        float scaleX = trackX - scaleLabelW;
        float trackY = dock.y + 12.5f;
        float trackH = 3.0f;

        FontUtil.small.drawString("Scale " + SCALE_FORMAT.format(guiScale) + "x", scaleX, dock.y + (dock.h - FontUtil.small.getHeight()) / 2.0f, ColorUtil.TEXT_MUTED);
        RenderUtil.drawRoundedRect(trackX, trackY, trackW, trackH, 1.5f, 0xFF26262E);

        if (draggingScale) {
            float mouseFrac = AnimationUtil.clamp((mouseX - trackX) / trackW, 0.0f, 1.0f);
            this.guiScale = 0.75f + (1.5f - 0.75f) * mouseFrac;
        }

        float scaleFrac = AnimationUtil.clamp((guiScale - 0.75f) / (1.5f - 0.75f), 0.0f, 1.0f);
        float fillW = trackW * scaleFrac;
        if (fillW > 0.5f) {
            RenderUtil.drawRoundedRect(trackX, trackY, fillW, trackH, 1.5f, accent);
        }
        RenderUtil.drawCircle(trackX + fillW, trackY + trackH / 2.0f, 3.0f, 0xFFF5F5F7);

        // Search input box filling gap between logo and scale slider
        float searchX = verX + FontUtil.small.getStringWidth("V1") + 10.0f;
        float searchW = Math.max(70.0f, scaleX - searchX - 10.0f);
        float searchY = dock.y + 5.0f;
        float searchH = 18.0f;

        RenderUtil.drawRoundedRect(searchX, searchY, searchW, searchH, 3.0f, 0xFF131317);
        RenderUtil.drawRoundedOutline(searchX, searchY, searchW, searchH, 3.0f, 1.0f, searchFocused ? accent : ColorUtil.BORDER);

        String searchDisplay = searchFilter.isEmpty() && !searchFocused
                ? "Search modules..."
                : searchFilter + (searchFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : "");
        int searchCol = searchFilter.isEmpty() && !searchFocused ? 0xFF666670 : ColorUtil.TEXT_PRIMARY;
        RenderUtil.pushScissor(searchX + 2.0f, searchY, searchW - 16.0f, searchH, guiScale);
        try {
            FontUtil.small.drawString(searchDisplay, searchX + 6.0f, searchY + (searchH - FontUtil.small.getHeight()) / 2.0f, searchCol);
        } finally {
            RenderUtil.popScissor();
        }

        // Clear ("x") button inside search box
        if (!searchFilter.isEmpty()) {
            float clearX = searchX + searchW - 14.0f;
            boolean clearHov = mouseX >= clearX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH;
            FontUtil.small.drawCenteredString("x", clearX + 7.0f, searchY + (searchH - FontUtil.small.getHeight()) / 2.0f,
                    clearHov ? accent : ColorUtil.TEXT_MUTED);
        }
    }

    private void renderTooltipBar(float viewportW, float viewportH, Module hoveredModule) {
        String text;
        int accent = ColorUtil.getAccent();
        if (hoveredModule != null) {
            String desc = hoveredModule.getDescription() != null ? hoveredModule.getDescription() : "";
            text = hoveredModule.getName() + "  \u2022  \u00a77" + desc + "  \u2022  \u00a7fLMB: Toggle \u2022 RMB: Settings \u2022 MMB: Bind";
        } else {
            text = "\u00a77LMB: Toggle \u2022 RMB: Settings \u2022 MMB: Bind \u2022 Scroll over panels";
        }

        float textW = FontUtil.small.getStringWidth(text);
        float pillW = Math.min(viewportW - 24.0f, textW + 20.0f);
        float pillH = 17.0f;
        float pillX = (viewportW - pillW) / 2.0f;
        float pillY = viewportH - pillH - 8.0f;

        RenderUtil.drawDropShadow(pillX, pillY, pillW, pillH, 4.0f, 0x50000000);
        RenderUtil.drawRoundedRect(pillX, pillY, pillW, pillH, 4.0f, 0xE6141418);
        RenderUtil.drawRoundedOutline(pillX, pillY, pillW, pillH, 4.0f, 1.0f,
                hoveredModule != null ? ColorUtil.modifyAlpha(accent, 140) : ColorUtil.BORDER);

        FontUtil.small.drawCenteredString(text, pillX + pillW / 2.0f,
                pillY + (pillH - FontUtil.small.getHeight()) / 2.0f,
                hoveredModule != null ? accent : ColorUtil.TEXT_MUTED);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) return;

        int rawMouseX = Mouse.getEventX() * this.width / Math.max(1, this.mc.displayWidth);
        int rawMouseY = this.height - Mouse.getEventY() * this.height / Math.max(1, this.mc.displayHeight) - 1;
        float scaledMouseX = rawMouseX / guiScale;
        float scaledMouseY = rawMouseY / guiScale;

        if (showProfiles) {
            profilePanel.handleScroll(scaledMouseX, scaledMouseY, dWheel);
        } else {
            for (int i = frames.size() - 1; i >= 0; i--) {
                if (frames.get(i).handleScroll(scaledMouseX, scaledMouseY, dWheel, searchFilter)) {
                    break;
                }
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        float scaledMouseX = mouseX / guiScale;
        float scaledMouseY = mouseY / guiScale;
        ScaledResolution sr = new ScaledResolution(mc);
        float width = sr.getScaledWidth() / guiScale;

        DockLayout dock = computeDockLayout(width);
        float btnY = dock.y + 5.0f;
        float btnH = 18.0f;
        float rightCursor = dock.x + dock.w - 8.0f;

        // HUD Editor
        float hudBtnW = 62.0f;
        float hudBtnX = rightCursor - hudBtnW;
        if (scaledMouseX >= hudBtnX && scaledMouseX <= hudBtnX + hudBtnW && scaledMouseY >= btnY && scaledMouseY <= btnY + btnH) {
            mc.displayGuiScreen(new HUDEditor());
            return;
        }
        rightCursor = hudBtnX - 5.0f;

        // Profiles
        float profBtnW = 52.0f;
        float profBtnX = rightCursor - profBtnW;
        if (scaledMouseX >= profBtnX && scaledMouseX <= profBtnX + profBtnW && scaledMouseY >= btnY && scaledMouseY <= btnY + btnH) {
            showProfiles = !showProfiles;
            return;
        }
        rightCursor = profBtnX - 5.0f;

        // Theme Switcher Pill
        float themeW = 78.0f;
        float themeX = rightCursor - themeW;
        if (scaledMouseX >= themeX && scaledMouseX <= themeX + themeW && scaledMouseY >= btnY && scaledMouseY <= btnY + btnH) {
            String nextPreset = ColorUtil.cycleThemePreset();
            ClickGUIModule mod = getClickGUIModule();
            if (mod != null) {
                mod.setThemePreset(nextPreset);
            }
            return;
        }
        rightCursor = themeX - 8.0f;

        // Scale Slider
        float trackW = 48.0f;
        float trackX = rightCursor - trackW;
        float scaleLabelW = 46.0f;
        float scaleX = trackX - scaleLabelW;
        if (scaledMouseX >= trackX - 4.0f && scaledMouseX <= trackX + trackW + 4.0f && scaledMouseY >= dock.y && scaledMouseY <= dock.y + dock.h) {
            draggingScale = true;
            float mouseFrac = AnimationUtil.clamp((scaledMouseX - trackX) / trackW, 0.0f, 1.0f);
            this.guiScale = 0.75f + (1.5f - 0.75f) * mouseFrac;
            return;
        }

        // Search Bar + Clear button
        float verX = dock.x + 10.0f + FontUtil.bold.getStringWidth("CIGARETTE") + 4.0f;
        float searchX = verX + FontUtil.small.getStringWidth("V1") + 10.0f;
        float searchW = Math.max(70.0f, scaleX - searchX - 10.0f);
        if (scaledMouseX >= searchX && scaledMouseX <= searchX + searchW && scaledMouseY >= btnY && scaledMouseY <= btnY + btnH) {
            if (!searchFilter.isEmpty() && scaledMouseX >= searchX + searchW - 14.0f) {
                searchFilter = "";
                searchFocused = true;
            } else {
                searchFocused = true;
            }
            return;
        } else {
            searchFocused = false;
        }

        if (showProfiles) {
            profilePanel.mouseClicked(scaledMouseX, scaledMouseY, mouseButton);
        } else {
            for (int i = frames.size() - 1; i >= 0; i--) {
                CategoryFrame frame = frames.get(i);
                if (frame.mouseClicked(scaledMouseX, scaledMouseY, mouseButton, searchFilter)) {
                    if (i != frames.size() - 1) {
                        frames.remove(i);
                        frames.add(frame);
                    }
                    break;
                }
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        float scaledMouseX = mouseX / guiScale;
        float scaledMouseY = mouseY / guiScale;

        draggingScale = false;

        if (showProfiles) {
            profilePanel.mouseReleased(scaledMouseX, scaledMouseY, state);
        } else {
            for (CategoryFrame frame : frames) {
                frame.mouseReleased(scaledMouseX, scaledMouseY, state);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        boolean frameListening = false;
        for (CategoryFrame frame : frames) {
            if (frame.isListening()) {
                frameListening = true;
                break;
            }
        }

        if (frameListening) {
            for (CategoryFrame frame : frames) {
                frame.keyTyped(typedChar, keyCode);
            }
            return;
        }

        ClickGUIModule clickGUIModule = getClickGUIModule();
        if (keyCode == Keyboard.KEY_ESCAPE || (!searchFocused && clickGUIModule != null && keyCode == clickGUIModule.getKeyCode())) {
            mc.displayGuiScreen(null);
            return;
        }

        if (searchFocused) {
            if (keyCode == Keyboard.KEY_BACK) {
                if (!searchFilter.isEmpty()) {
                    searchFilter = searchFilter.substring(0, searchFilter.length() - 1);
                }
            } else if (keyCode == Keyboard.KEY_RETURN) {
                searchFocused = false;
            } else if (Character.isLetterOrDigit(typedChar) || Character.isWhitespace(typedChar) || typedChar == '_' || typedChar == '-') {
                if (searchFilter.length() < 30) {
                    searchFilter += typedChar;
                }
            }
            return;
        }

        if (showProfiles) {
            profilePanel.keyTyped(typedChar, keyCode);
        } else {
            for (CategoryFrame frame : frames) {
                frame.keyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private ClickGUIModule getClickGUIModule() {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            return Cigarette.INSTANCE.getModuleManager().getModuleByClass(ClickGUIModule.class);
        }
        return null;
    }

    private static final class DockLayout {
        final float x;
        final float y;
        final float w;
        final float h;

        DockLayout(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }
}
