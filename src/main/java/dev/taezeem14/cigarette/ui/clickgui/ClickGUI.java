package dev.taezeem14.cigarette.ui.clickgui;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.render.ClickGUIModule;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.ui.hud.HUDEditor;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

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
    private long lastTime = 0L;

    public ClickGUI() {
        initFrames();
    }

    private void initFrames() {
        frames.clear();
        float startX = 30.0f;
        float startY = 50.0f;
        float spacing = 118.0f;
        int index = 0;

        Category[] categories = {
                Category.COMBAT,
                Category.BLATANT,
                Category.RENDER,
                Category.UTILITY,
                Category.WORLD
        };

        for (Category category : categories) {
            List<Module> categoryModules = new ArrayList<>();
            if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
                categoryModules = Cigarette.INSTANCE.getModuleManager().getModulesByCategory(category);
            }
            frames.add(new CategoryFrame(category, categoryModules, startX + index * spacing, startY));
            index++;
        }

        profilePanel = new ProfilePanel(150.0f, 60.0f);
    }

    @Override
    public void initGui() {
        super.initGui();
        fadeProgress = 0.0f;
        lastTime = System.currentTimeMillis();
        Keyboard.enableRepeatEvents(true);

        // Re-align frames with current module manager
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            for (CategoryFrame frame : frames) {
                List<Module> currentModules = Cigarette.INSTANCE.getModuleManager().getModulesByCategory(frame.getCategory());
                if (frame.getButtons().size() != currentModules.size()) {
                    initFrames();
                    break;
                }
            }
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        ClickGUIModule clickGUIModule = Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null
                ? Cigarette.INSTANCE.getModuleManager().getModuleByClass(ClickGUIModule.class)
                : null;
        if (clickGUIModule != null && clickGUIModule.isEnabled()) {
            clickGUIModule.setState(false);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        float delta = (now - lastTime) / 1000.0f;
        lastTime = now;
        fadeProgress = Math.min(1.0f, fadeProgress + delta * 5.0f);

        ScaledResolution sr = new ScaledResolution(mc);
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        // Dark dim overlay with fade
        int overlayAlpha = (int) (120 * fadeProgress);
        RenderUtil.drawRect(0, 0, screenW, screenH, (overlayAlpha << 24));

        // Scale transform for user GUI scale setting
        GL11.glPushMatrix();
        GlStateManager.scale(guiScale, guiScale, 1.0f);

        float scaledMouseX = mouseX / guiScale;
        float scaledMouseY = mouseY / guiScale;

        // Render Top Dock / Search Bar
        renderTopDock(scaledMouseX, scaledMouseY, screenW / guiScale);

        // Render Category Frames or Profile Panel
        if (showProfiles) {
            profilePanel.render(scaledMouseX, scaledMouseY, partialTicks);
        } else {
            for (CategoryFrame frame : frames) {
                frame.render(scaledMouseX, scaledMouseY, partialTicks, searchFilter);
            }
        }

        GL11.glPopMatrix();
    }

    private void renderTopDock(float mouseX, float mouseY, float width) {
        float dockH = 28.0f;
        float dockY = 8.0f;
        float dockW = Math.min(width - 40.0f, 620.0f);
        float dockX = (width - dockW) / 2.0f;

        // Dock background
        RenderUtil.drawDropShadow(dockX, dockY, dockW, dockH, 4.0f, 0x55000000);
        RenderUtil.drawRoundedRect(dockX, dockY, dockW, dockH, 4.0f, ColorUtil.HEADER);
        RenderUtil.drawRoundedOutline(dockX, dockY, dockW, dockH, 4.0f, 1.0f, 0xFF282830);

        // Logo / Title
        FontUtil.bold.drawString("CIGARETTE", dockX + 10.0f, dockY + (dockH - FontUtil.bold.getHeight()) / 2.0f, ColorUtil.ACCENT);
        FontUtil.small.drawString("v1.0", dockX + 10.0f + FontUtil.bold.getStringWidth("CIGARETTE") + 4.0f, dockY + 7.0f, ColorUtil.TEXT_MUTED);

        // Live Search Input Box
        float searchX = dockX + 110.0f;
        float searchY = dockY + 5.0f;
        float searchW = 160.0f;
        float searchH = 18.0f;

        RenderUtil.drawRoundedRect(searchX, searchY, searchW, searchH, 3.0f, 0xFF141417);
        RenderUtil.drawRoundedOutline(searchX, searchY, searchW, searchH, 3.0f, 1.0f, searchFocused ? ColorUtil.ACCENT : 0xFF25252C);

        String searchDisplay = searchFilter.isEmpty() && !searchFocused ? "Search modules..." : searchFilter + (searchFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : "");
        int searchCol = searchFilter.isEmpty() && !searchFocused ? 0xFF666670 : ColorUtil.TEXT_PRIMARY;
        FontUtil.small.drawString(searchDisplay, searchX + 6.0f, searchY + (searchH - FontUtil.small.getHeight()) / 2.0f, searchCol);

        // Scale Slider
        float scaleX = searchX + searchW + 16.0f;
        float scaleY = dockY + 6.0f;
        float scaleW = 85.0f;
        float scaleH = 16.0f;

        FontUtil.small.drawString("Scale: " + SCALE_FORMAT.format(guiScale) + "x", scaleX, scaleY + 3.0f, ColorUtil.TEXT_MUTED);

        float trackX = scaleX + 60.0f;
        float trackY = scaleY + 6.0f;
        float trackW = 55.0f;
        float trackH = 3.0f;

        RenderUtil.drawRoundedRect(trackX, trackY, trackW, trackH, 1.5f, 0xFF25252C);
        float scaleFrac = (guiScale - 0.75f) / (1.5f - 0.75f);
        scaleFrac = Math.max(0.0f, Math.min(1.0f, scaleFrac));

        if (draggingScale) {
            float mouseFrac = (mouseX - trackX) / trackW;
            mouseFrac = Math.max(0.0f, Math.min(1.0f, mouseFrac));
            this.guiScale = 0.75f + (1.5f - 0.75f) * mouseFrac;
        }

        float fillW = trackW * scaleFrac;
        if (fillW > 0.0f) {
            RenderUtil.drawRoundedRect(trackX, trackY, fillW, trackH, 1.5f, ColorUtil.ACCENT);
        }
        RenderUtil.drawRoundedRect(trackX + fillW - 2.5f, trackY - 1.5f, 5.0f, 6.0f, 2.5f, 0xFFEBEBEF);

        // Profiles Tab Button
        float profBtnX = trackX + trackW + 16.0f;
        float profBtnY = dockY + 5.0f;
        float profBtnW = 60.0f;
        float profBtnH = 18.0f;
        boolean profHov = mouseX >= profBtnX && mouseX <= profBtnX + profBtnW && mouseY >= profBtnY && mouseY <= profBtnY + profBtnH;

        RenderUtil.drawRoundedRect(profBtnX, profBtnY, profBtnW, profBtnH, 3.0f, showProfiles ? ColorUtil.ACCENT : (profHov ? 0xFF2A2A33 : 0xFF202026));
        FontUtil.small.drawCenteredString("Profiles", profBtnX + profBtnW / 2.0f, profBtnY + (profBtnH - FontUtil.small.getHeight()) / 2.0f, showProfiles ? 0xFFFFFFFF : ColorUtil.TEXT_PRIMARY);

        // HUD Editor Button
        float hudBtnX = profBtnX + profBtnW + 6.0f;
        float hudBtnY = dockY + 5.0f;
        float hudBtnW = 68.0f;
        float hudBtnH = 18.0f;
        boolean hudHov = mouseX >= hudBtnX && mouseX <= hudBtnX + hudBtnW && mouseY >= hudBtnY && mouseY <= hudBtnY + hudBtnH;

        RenderUtil.drawRoundedRect(hudBtnX, hudBtnY, hudBtnW, hudBtnH, 3.0f, hudHov ? 0xFF2A2A33 : 0xFF202026);
        FontUtil.small.drawCenteredString("HUD Editor", hudBtnX + hudBtnW / 2.0f, hudBtnY + (hudBtnH - FontUtil.small.getHeight()) / 2.0f, ColorUtil.TEXT_PRIMARY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        float scaledMouseX = mouseX / guiScale;
        float scaledMouseY = mouseY / guiScale;
        ScaledResolution sr = new ScaledResolution(mc);
        float width = sr.getScaledWidth() / guiScale;

        float dockH = 28.0f;
        float dockY = 8.0f;
        float dockW = Math.min(width - 40.0f, 620.0f);
        float dockX = (width - dockW) / 2.0f;

        // Search bar click
        float searchX = dockX + 110.0f;
        float searchY = dockY + 5.0f;
        float searchW = 160.0f;
        float searchH = 18.0f;

        if (scaledMouseX >= searchX && scaledMouseX <= searchX + searchW && scaledMouseY >= searchY && scaledMouseY <= searchY + searchH) {
            searchFocused = true;
            return;
        } else {
            searchFocused = false;
        }

        // Scale slider click
        float scaleX = searchX + searchW + 16.0f;
        float scaleY = dockY + 6.0f;
        float trackX = scaleX + 60.0f;
        float trackW = 55.0f;

        if (scaledMouseX >= trackX - 4.0f && scaledMouseX <= trackX + trackW + 4.0f && scaledMouseY >= scaleY && scaledMouseY <= scaleY + 16.0f) {
            draggingScale = true;
            return;
        }

        // Profiles tab click
        float profBtnX = trackX + trackW + 16.0f;
        float profBtnY = dockY + 5.0f;
        float profBtnW = 60.0f;
        float profBtnH = 18.0f;

        if (scaledMouseX >= profBtnX && scaledMouseX <= profBtnX + profBtnW && scaledMouseY >= profBtnY && scaledMouseY <= profBtnY + profBtnH) {
            showProfiles = !showProfiles;
            return;
        }

        // HUD Editor click
        float hudBtnX = profBtnX + profBtnW + 6.0f;
        float hudBtnY = dockY + 5.0f;
        float hudBtnW = 68.0f;
        float hudBtnH = 18.0f;

        if (scaledMouseX >= hudBtnX && scaledMouseX <= hudBtnX + hudBtnW && scaledMouseY >= hudBtnY && scaledMouseY <= hudBtnY + hudBtnH) {
            mc.displayGuiScreen(new HUDEditor());
            return;
        }

        if (showProfiles) {
            profilePanel.mouseClicked(scaledMouseX, scaledMouseY, mouseButton);
        } else {
            for (CategoryFrame frame : frames) {
                frame.mouseClicked(scaledMouseX, scaledMouseY, mouseButton, searchFilter);
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        float scaledMouseX = mouseX / guiScale;
        float scaledMouseY = mouseY / guiScale;

        draggingScale = false;

        if (!showProfiles) {
            for (CategoryFrame frame : frames) {
                frame.mouseReleased(scaledMouseX, scaledMouseY, state);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
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
}
