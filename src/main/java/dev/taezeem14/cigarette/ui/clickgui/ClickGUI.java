package dev.taezeem14.cigarette.ui.clickgui;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.config.ConfigManager;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.client.FriendManager;
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
import java.util.*;

/**
 * Authentic Vape V4 dual-card user interface.
 * Features a compact 2-column layout (Sidebar + Active Category Panel),
 * intelligent screen auto-scaling so it never overflows, unified window dragging,
 * 3-dot drawer toggles, profiles management, and settings controls.
 */
public class ClickGUI extends GuiScreen {

    private static final DecimalFormat SCALE_FORMAT = new DecimalFormat("0.##");

    public static final float SIDEBAR_WIDTH = 124.0f;
    public static final float CONTENT_WIDTH = 176.0f;
    public static final float GAP = 7.0f;
    public static final float TOTAL_WIDTH = SIDEBAR_WIDTH + GAP + CONTENT_WIDTH; // 307px
    public static final float TOTAL_HEIGHT = 315.0f;

    public enum ViewType {
        CATEGORY,
        PROFILES,
        SETTINGS,
        FRIENDS
    }

    private ViewType activeView = ViewType.CATEGORY;
    private Category activeCategory = Category.COMBAT;

    // Window position
    private static float guiX = -1.0f;
    private static float guiY = -1.0f;
    private boolean draggingWindow = false;
    private float dragOffsetX = 0.0f;
    private float dragOffsetY = 0.0f;

    // GUI scaling
    private float userScale = 1.0f;
    private boolean draggingScaleSlider = false;

    // Animations
    private float fadeProgress = 0.0f;

    // Module list per category
    private final Map<Category, List<ModuleButton>> categoryButtons = new LinkedHashMap<>();

    // Scroll state for content panel
    private float scrollOffset = 0.0f;
    private float targetScrollOffset = 0.0f;

    // Search state
    private String searchFilter = "";
    private boolean searchFocused = false;

    // Profile creation state
    private String newProfileInput = "";
    private boolean profileInputFocused = false;
    private float profileScrollOffset = 0.0f;
    private float targetProfileScroll = 0.0f;

    // Friend input state
    private String newFriendInput = "";
    private boolean friendInputFocused = false;

    public ClickGUI() {
        initCategoryButtons();
    }

    private void initCategoryButtons() {
        categoryButtons.clear();
        Category[] categories = {
                Category.COMBAT,
                Category.BLATANT,
                Category.RENDER,
                Category.UTILITY,
                Category.WORLD,
                Category.CLIENT
        };

        for (Category cat : categories) {
            List<Module> mods = new ArrayList<>();
            if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
                mods = Cigarette.INSTANCE.getModuleManager().getModulesByCategory(cat);
            }
            List<ModuleButton> buttons = new ArrayList<>();
            for (Module m : mods) {
                buttons.add(new ModuleButton(m, 0, 0, CONTENT_WIDTH - 8.0f));
            }
            categoryButtons.put(cat, buttons);
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

        // Verify module list integrity
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            boolean mismatch = false;
            for (Map.Entry<Category, List<ModuleButton>> entry : categoryButtons.entrySet()) {
                List<Module> current = Cigarette.INSTANCE.getModuleManager().getModulesByCategory(entry.getKey());
                if (current.size() != entry.getValue().size()) {
                    mismatch = true;
                    break;
                }
            }
            if (mismatch) {
                initCategoryButtons();
            }
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        draggingWindow = false;
        draggingScaleSlider = false;
        RenderUtil.clearScissorStack();

        ClickGUIModule clickGUIModule = getClickGUIModule();
        if (clickGUIModule != null && clickGUIModule.isEnabled()) {
            clickGUIModule.setState(false);
        }
    }

    private float computeEffectiveScale(float screenW, float screenH) {
        float maxFitW = screenW * 0.88f;
        float maxFitH = screenH * 0.88f;
        float autoFitScale = Math.min(1.0f, Math.min(maxFitW / TOTAL_WIDTH, maxFitH / TOTAL_HEIGHT));
        return Math.max(0.55f, autoFitScale * userScale);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        FontUtil.ensureInitialized();
        fadeProgress = Math.min(1.0f, fadeProgress + AnimationUtil.getDeltaTime() * 7.5f);

        ClickGUIModule clickGUIModule = getClickGUIModule();
        if (clickGUIModule != null) {
            clickGUIModule.syncThemeFromSetting();
        }

        ScaledResolution sr = new ScaledResolution(mc);
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        // Optional Gaussian backdrop blur
        if (clickGUIModule == null || clickGUIModule.isBlurEnabled()) {
            BlurUtil.blurAll(Math.max(1.0f, 6.0f * AnimationUtil.easeOutCubic(fadeProgress)));
        }

        int overlayAlpha = (int) (135 * fadeProgress);
        RenderUtil.drawRect(0, 0, screenW, screenH, (overlayAlpha << 24) | 0x0A0A0E);

        float effectiveScale = computeEffectiveScale(screenW, screenH);
        float viewportW = screenW / effectiveScale;
        float viewportH = screenH / effectiveScale;

        // Default center position
        if (guiX < 0 || guiY < 0) {
            guiX = Math.max(8.0f, (viewportW - TOTAL_WIDTH) / 2.0f);
            guiY = Math.max(8.0f, (viewportH - TOTAL_HEIGHT) / 2.0f);
        }

        float scaledMouseX = mouseX / effectiveScale;
        float scaledMouseY = mouseY / effectiveScale;

        // Window dragging
        if (draggingWindow) {
            guiX = AnimationUtil.clamp(scaledMouseX - dragOffsetX, 4.0f, Math.max(4.0f, viewportW - TOTAL_WIDTH - 4.0f));
            guiY = AnimationUtil.clamp(scaledMouseY - dragOffsetY, 4.0f, Math.max(4.0f, viewportH - TOTAL_HEIGHT - 4.0f));
        } else {
            guiX = AnimationUtil.clamp(guiX, 4.0f, Math.max(4.0f, viewportW - TOTAL_WIDTH - 4.0f));
            guiY = AnimationUtil.clamp(guiY, 4.0f, Math.max(4.0f, viewportH - TOTAL_HEIGHT - 4.0f));
        }

        GlStateManager.pushMatrix();
        try {
            GlStateManager.scale(effectiveScale, effectiveScale, 1.0f);

            int accent = ColorUtil.getAccent();

            // 1. Render Left Sidebar
            renderSidebar(scaledMouseX, scaledMouseY, accent);

            // 2. Render Right Content Panel
            renderContentPanel(scaledMouseX, scaledMouseY, partialTicks, effectiveScale, accent);

        } finally {
            GlStateManager.popMatrix();
            RenderUtil.clearScissorStack();
        }
    }

    private void renderSidebar(float mouseX, float mouseY, int accent) {
        float x = guiX;
        float y = guiY;
        float w = SIDEBAR_WIDTH;
        float h = TOTAL_HEIGHT;

        // Drop shadow & background card
        RenderUtil.drawDropShadow(x, y, w, h, 6.0f, 0x66000000);
        RenderUtil.drawRoundedRect(x, y, w, h, 5.0f, 0xFF141417);
        RenderUtil.drawRoundedOutline(x, y, w, h, 5.0f, 1.0f, 0xFF232328);

        // Header: Logo "VAPE v4" + Settings gear
        float headH = 34.0f;
        RenderUtil.drawRoundedRectTop(x, y, w, headH, 5.0f, 0xFF18181D);
        RenderUtil.drawRect(x + 1.0f, y + headH - 1.0f, w - 2.0f, 1.0f, 0xFF222228);

        // VAPE text
        float logoX = x + 10.0f;
        FontUtil.bold.drawString("VAPE", logoX, y + (headH - FontUtil.bold.getHeight()) / 2.0f, 0xFFFFFFFF);

        // v4 badge (green outline box)
        float v4X = logoX + FontUtil.bold.getStringWidth("VAPE") + 4.0f;
        float v4Y = y + (headH - 12.0f) / 2.0f;
        RenderUtil.drawRoundedRect(v4X, v4Y, 17.0f, 12.0f, 2.0f, 0xFF0D251A);
        RenderUtil.drawRoundedOutline(v4X, v4Y, 17.0f, 12.0f, 2.0f, 1.0f, 0xFF10B981);
        FontUtil.small.drawString("v4", v4X + 3.0f, v4Y + (12.0f - FontUtil.small.getHeight()) / 2.0f, 0xFF10B981);

        // Settings gear icon
        float gearX = x + w - 20.0f;
        float gearY = y + (headH - 11.0f) / 2.0f;
        boolean gearHovered = mouseX >= gearX - 3.0f && mouseX <= gearX + 14.0f && mouseY >= gearY - 3.0f && mouseY <= gearY + 14.0f;
        int gearColor = (activeView == ViewType.SETTINGS) ? accent : (gearHovered ? 0xFFFFFFFF : 0xFF727280);
        RenderUtil.drawFaIcon("gear", gearX, gearY, 10.0f, gearColor);

        // Category Items List
        float itemY = y + headH + 4.0f;
        float itemH = 24.0f;
        Category[] cats = Category.values();

        for (Category cat : cats) {
            boolean active = (activeView == ViewType.CATEGORY && activeCategory == cat);
            boolean hovered = mouseX >= x + 3.0f && mouseX <= x + w - 3.0f && mouseY >= itemY && mouseY <= itemY + itemH;

            int itemBg = active ? 0xFF1D2028 : (hovered ? 0xFF1A1C24 : 0x00000000);
            if (itemBg != 0) {
                RenderUtil.drawRoundedRect(x + 3.0f, itemY, w - 6.0f, itemH, 3.5f, itemBg);
            }

            // Left vertical accent indicator bar
            if (active) {
                RenderUtil.drawRoundedRect(x + 4.0f, itemY + 3.5f, 2.5f, itemH - 7.0f, 1.25f, accent);
            }

            // Category Icon
            int iconCol = active ? accent : (hovered ? 0xFFCCCCCC : 0xFF727280);
            RenderUtil.drawFaIcon(cat.name(), x + 10.0f, itemY + (itemH - 10.0f) / 2.0f, 10.0f, iconCol);

            // Category Title
            int textCol = active ? accent : (hovered ? 0xFFFFFFFF : 0xFF9E9EA8);
            if (active) {
                FontUtil.bold.drawString(cat.getDisplayName(), x + 25.0f, itemY + (itemH - FontUtil.bold.getHeight()) / 2.0f, textCol);
            } else {
                FontUtil.regular.drawString(cat.getDisplayName(), x + 25.0f, itemY + (itemH - FontUtil.regular.getHeight()) / 2.0f, textCol);
            }

            // Right chevron ">"
            int chevCol = active ? accent : (hovered ? 0xFF8E8E9A : 0xFF4A4A55);
            RenderUtil.drawFaIcon("chevron_right", x + w - 14.0f, itemY + (itemH - 8.0f) / 2.0f, 7.5f, chevCol);

            itemY += itemH + 1.0f;
        }

        // Section: "MISC"
        itemY += 3.0f;
        FontUtil.small.drawString("MISC", x + 10.0f, itemY, 0xFF585864);
        itemY += 13.0f;

        // Misc items: Friends, Profiles, HUD Editor
        String[] miscItems = {"Friends", "Profiles", "HUD Editor"};
        for (String misc : miscItems) {
            boolean active = false;
            if ("Profiles".equals(misc) && activeView == ViewType.PROFILES) active = true;
            if ("Friends".equals(misc) && activeView == ViewType.FRIENDS) active = true;

            boolean hovered = mouseX >= x + 3.0f && mouseX <= x + w - 3.0f && mouseY >= itemY && mouseY <= itemY + itemH;
            int itemBg = active ? 0xFF1D2028 : (hovered ? 0xFF1A1C24 : 0x00000000);
            if (itemBg != 0) {
                RenderUtil.drawRoundedRect(x + 3.0f, itemY, w - 6.0f, itemH, 3.5f, itemBg);
            }

            if (active) {
                RenderUtil.drawRoundedRect(x + 4.0f, itemY + 3.5f, 2.5f, itemH - 7.0f, 1.25f, accent);
            }

            int iconCol = active ? accent : (hovered ? 0xFFCCCCCC : 0xFF727280);
            String iconKey = "Friends".equals(misc) ? "friends" : ("Profiles".equals(misc) ? "profiles" : "hud");
            RenderUtil.drawFaIcon(iconKey, x + 10.0f, itemY + (itemH - 10.0f) / 2.0f, 10.0f, iconCol);

            int textCol = active ? accent : (hovered ? 0xFFFFFFFF : 0xFF9E9EA8);
            if (active) {
                FontUtil.bold.drawString(misc, x + 25.0f, itemY + (itemH - FontUtil.bold.getHeight()) / 2.0f, textCol);
            } else {
                FontUtil.regular.drawString(misc, x + 25.0f, itemY + (itemH - FontUtil.regular.getHeight()) / 2.0f, textCol);
            }

            // "default" badge for Profiles
            if ("Profiles".equals(misc)) {
                ConfigManager cm = Cigarette.INSTANCE != null ? Cigarette.INSTANCE.getConfigManager() : null;
                String activeProf = (cm != null && cm.getActiveProfile() != null) ? cm.getActiveProfile().getName() : "default";
                float badgeW = FontUtil.small.getStringWidth(activeProf) + 6.0f;
                float badgeX = x + w - badgeW - 16.0f;
                float badgeY = itemY + (itemH - 10.0f) / 2.0f;
                RenderUtil.drawRoundedRect(badgeX, badgeY, badgeW, 10.5f, 2.0f, 0xFF1F2027);
                FontUtil.small.drawString(activeProf, badgeX + 3.0f, badgeY + (10.5f - FontUtil.small.getHeight()) / 2.0f, 0xFF7E7E8C);
            }

            int chevCol = active ? accent : (hovered ? 0xFF8E8E9A : 0xFF4A4A55);
            RenderUtil.drawFaIcon("chevron_right", x + w - 14.0f, itemY + (itemH - 8.0f) / 2.0f, 7.5f, chevCol);

            itemY += itemH + 1.0f;
        }

        // Sidebar Footer
        float footY = y + h - 24.0f;
        RenderUtil.drawRect(x + 1.0f, footY, w - 2.0f, 1.0f, 0xFF1F1F24);

        // User avatar icon
        RenderUtil.drawFaIcon("user", x + 10.0f, footY + 6.5f, 10.0f, 0xFF7A7A88);

        // Scale tag
        String scaleTag = SCALE_FORMAT.format(userScale) + "x";
        FontUtil.small.drawString(scaleTag, x + 40.0f, footY + (24.0f - FontUtil.small.getHeight()) / 2.0f, 0xFF656572);

        // Palette / Theme cycler circle
        float palX = x + w - 16.0f;
        float palY = footY + 12.0f;
        boolean palHovered = mouseX >= palX - 5.0f && mouseX <= palX + 5.0f && mouseY >= palY - 5.0f && mouseY <= palY + 5.0f;
        RenderUtil.drawCircle(palX, palY, palHovered ? 4.5f : 3.5f, accent);
        RenderUtil.drawCircleOutline(palX, palY, palHovered ? 5.5f : 4.5f, 1.0f, 0xFF353540);
    }

    private void renderContentPanel(float mouseX, float mouseY, float partialTicks, float effectiveScale, int accent) {
        float x = guiX + SIDEBAR_WIDTH + GAP;
        float y = guiY;
        float w = CONTENT_WIDTH;
        float h = TOTAL_HEIGHT;

        // Drop shadow & background card
        RenderUtil.drawDropShadow(x, y, w, h, 6.0f, 0x66000000);
        RenderUtil.drawRoundedRect(x, y, w, h, 5.0f, 0xFF141417);
        RenderUtil.drawRoundedOutline(x, y, w, h, 5.0f, 1.0f, 0xFF232328);

        // Header
        float headH = 34.0f;
        RenderUtil.drawRoundedRectTop(x, y, w, headH, 5.0f, 0xFF18181D);
        RenderUtil.drawRect(x + 1.0f, y + headH - 1.0f, w - 2.0f, 1.0f, 0xFF222228);

        // Header Title & Icon
        String title = "Combat";
        String iconKey = "combat";
        if (activeView == ViewType.CATEGORY) {
            title = activeCategory.getDisplayName();
            iconKey = activeCategory.name();
        } else if (activeView == ViewType.PROFILES) {
            title = "Profiles";
            iconKey = "profiles";
        } else if (activeView == ViewType.SETTINGS) {
            title = "Settings";
            iconKey = "gear";
        } else if (activeView == ViewType.FRIENDS) {
            title = "Friends";
            iconKey = "friends";
        }

        RenderUtil.drawFaIcon(iconKey, x + 10.0f, y + (headH - 10.0f) / 2.0f, 10.0f, accent);
        FontUtil.bold.drawString(title, x + 25.0f, y + (headH - FontUtil.bold.getHeight()) / 2.0f, 0xFFFFFFFF);

        // Search or chevron on top right
        float chevX = x + w - 18.0f;
        float chevY = y + (headH - 8.0f) / 2.0f;
        RenderUtil.drawFaIcon("chevron_up", chevX, chevY, 8.0f, 0xFF7A7A88);

        // Body Content Views
        float bodyY = y + headH + 4.0f;
        float bodyH = h - headH - 8.0f;

        if (activeView == ViewType.CATEGORY) {
            renderCategoryModules(x, bodyY, w, bodyH, mouseX, mouseY, partialTicks, effectiveScale, accent);
        } else if (activeView == ViewType.PROFILES) {
            renderProfilesView(x, bodyY, w, bodyH, mouseX, mouseY, accent);
        } else if (activeView == ViewType.SETTINGS) {
            renderSettingsView(x, bodyY, w, bodyH, mouseX, mouseY, accent);
        } else if (activeView == ViewType.FRIENDS) {
            renderFriendsView(x, bodyY, w, bodyH, mouseX, mouseY, accent);
        }
    }

    private void renderCategoryModules(float panelX, float bodyY, float panelW, float bodyH,
                                       float mouseX, float mouseY, float partialTicks, float effectiveScale, int accent) {
        List<ModuleButton> buttons = categoryButtons.get(activeCategory);
        if (buttons == null) return;

        // Calculate total content height
        float totalH = 4.0f;
        for (ModuleButton btn : buttons) {
            if (matchesFilter(btn.getModule())) {
                totalH += btn.getTotalHeight() + 2.0f;
            }
        }

        float maxScroll = Math.max(0.0f, totalH - bodyH);
        targetScrollOffset = AnimationUtil.clamp(targetScrollOffset, 0.0f, maxScroll);
        scrollOffset = AnimationUtil.lerp(scrollOffset, targetScrollOffset, 16.0f);

        RenderUtil.pushScissor(panelX, bodyY, panelW, bodyH, effectiveScale);
        try {
            float btnY = bodyY + 2.0f - scrollOffset;
            float btnW = panelW - 10.0f;

            for (ModuleButton btn : buttons) {
                if (matchesFilter(btn.getModule())) {
                    btn.setX(panelX + 5.0f);
                    btn.setY(btnY);
                    btn.setWidth(btnW);
                    btn.render(mouseX, mouseY, partialTicks, effectiveScale);
                    btnY += btn.getTotalHeight() + 2.0f;
                }
            }

            // Scrollbar
            if (maxScroll > 0.5f) {
                float trackX = panelX + panelW - 4.5f;
                float trackH = bodyH - 4.0f;
                RenderUtil.drawRoundedRect(trackX, bodyY + 2.0f, 2.0f, trackH, 1.0f, 0xFF1E1E24);

                float thumbH = Math.max(20.0f, trackH * (bodyH / totalH));
                float thumbY = bodyY + 2.0f + (trackH - thumbH) * (scrollOffset / maxScroll);
                RenderUtil.drawRoundedRect(trackX, thumbY, 2.0f, thumbH, 1.0f, ColorUtil.modifyAlpha(accent, 190));
            }
        } finally {
            RenderUtil.popScissor();
        }
    }

    private void renderProfilesView(float x, float y, float w, float h, float mouseX, float mouseY, int accent) {
        ConfigManager cm = Cigarette.INSTANCE != null ? Cigarette.INSTANCE.getConfigManager() : null;
        String activeName = (cm != null && cm.getActiveProfile() != null) ? cm.getActiveProfile().getName() : "default";

        // Active profile display
        FontUtil.small.drawString("Active: \u00a7a" + activeName, x + 8.0f, y + 2.0f, 0xFFCCCCCC);

        // Input box to create profile
        float inY = y + 15.0f;
        float inW = w - 52.0f;
        float inH = 17.0f;
        RenderUtil.drawRoundedRect(x + 8.0f, inY, inW, inH, 3.0f, 0xFF19191E);
        RenderUtil.drawRoundedOutline(x + 8.0f, inY, inW, inH, 3.0f, 1.0f, profileInputFocused ? accent : 0xFF2A2A32);

        String txt = newProfileInput.isEmpty() && !profileInputFocused ? "New profile..." : newProfileInput + (profileInputFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : "");
        FontUtil.small.drawString(txt, x + 12.0f, inY + (inH - FontUtil.small.getHeight()) / 2.0f, newProfileInput.isEmpty() && !profileInputFocused ? 0xFF60606B : 0xFFFFFFFF);

        // Create Button
        float btnX = x + 8.0f + inW + 3.0f;
        float btnW = 33.0f;
        boolean crHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= inY && mouseY <= inY + inH;
        RenderUtil.drawRoundedRect(btnX, inY, btnW, inH, 3.0f, crHovered ? accent : 0xFF22232B);
        FontUtil.small.drawCenteredString("Add", btnX + btnW / 2.0f, inY + (inH - FontUtil.small.getHeight()) / 2.0f, 0xFFFFFFFF);

        // Save active button
        float saveY = inY + inH + 5.0f;
        float saveW = w - 16.0f;
        boolean saveHovered = mouseX >= x + 8.0f && mouseX <= x + 8.0f + saveW && mouseY >= saveY && mouseY <= saveY + 16.0f;
        RenderUtil.drawRoundedRect(x + 8.0f, saveY, saveW, 16.0f, 3.0f, saveHovered ? 0xFF10B981 : 0xFF17241F);
        RenderUtil.drawRoundedOutline(x + 8.0f, saveY, saveW, 16.0f, 3.0f, 1.0f, 0xFF10B981);
        FontUtil.small.drawCenteredString("Save Current Profile", x + 8.0f + saveW / 2.0f, saveY + (16.0f - FontUtil.small.getHeight()) / 2.0f, 0xFFFFFFFF);

        // Scrollable profile list
        float listY = saveY + 20.0f;
        float listH = h - (listY - y) - 4.0f;
        RenderUtil.drawRoundedRect(x + 8.0f, listY, w - 16.0f, listH, 3.0f, 0xFF16161B);
        RenderUtil.drawRoundedOutline(x + 8.0f, listY, w - 16.0f, listH, 3.0f, 1.0f, 0xFF222228);

        if (cm != null) {
            List<String> list = cm.listProfiles();
            float itemH = 20.0f;
            float totalListH = list.size() * (itemH + 2.0f);
            float maxScr = Math.max(0.0f, totalListH - listH);
            targetProfileScroll = AnimationUtil.clamp(targetProfileScroll, 0.0f, maxScr);
            profileScrollOffset = AnimationUtil.lerp(profileScrollOffset, targetProfileScroll, 16.0f);

            RenderUtil.pushScissor(x + 8.0f, listY, w - 16.0f, listH);
            try {
                float curY = listY + 3.0f - profileScrollOffset;
                for (String p : list) {
                    boolean isCur = p.equalsIgnoreCase(activeName);
                    boolean hov = mouseX >= x + 10.0f && mouseX <= x + w - 10.0f && mouseY >= curY && mouseY <= curY + itemH;
                    RenderUtil.drawRoundedRect(x + 10.0f, curY, w - 20.0f, itemH, 2.5f, hov ? 0xFF22222A : (isCur ? 0xFF1A1C25 : 0xFF19191E));

                    int nameCol = isCur ? accent : 0xFFD0D0D8;
                    FontUtil.small.drawString(p, x + 14.0f, curY + (itemH - FontUtil.small.getHeight()) / 2.0f, nameCol);

                    // Load button
                    float lBtnX = x + w - 38.0f;
                    boolean lHov = mouseX >= lBtnX && mouseX <= lBtnX + 24.0f && mouseY >= curY + 2.5f && mouseY <= curY + 17.5f;
                    RenderUtil.drawRoundedRect(lBtnX, curY + 2.5f, 24.0f, 15.0f, 2.0f, lHov ? accent : 0xFF252630);
                    FontUtil.small.drawCenteredString("Load", lBtnX + 12.0f, curY + 2.5f + (15.0f - FontUtil.small.getHeight()) / 2.0f, 0xFFFFFFFF);

                    curY += itemH + 2.0f;
                }
            } finally {
                RenderUtil.popScissor();
            }
        }
    }

    private void renderSettingsView(float x, float y, float w, float h, float mouseX, float mouseY, int accent) {
        FontUtil.bold.drawString("ClickGUI Settings", x + 10.0f, y + 4.0f, 0xFFFFFFFF);

        // 1. Theme Presets
        FontUtil.small.drawString("Theme Preset", x + 10.0f, y + 20.0f, 0xFF858592);
        String[] presets = {"Electric Blue", "Ember Orange", "Amethyst", "Emerald", "Rainbow"};
        float pillX = x + 10.0f;
        float pillY = y + 32.0f;
        float pillH = 15.0f;

        for (String preset : presets) {
            float pillW = FontUtil.small.getStringWidth(preset) + 8.0f;
            if (pillX + pillW > x + w - 10.0f) {
                pillX = x + 10.0f;
                pillY += pillH + 4.0f;
            }
            boolean isCur = preset.equalsIgnoreCase(ColorUtil.getThemePreset());
            boolean hov = mouseX >= pillX && mouseX <= pillX + pillW && mouseY >= pillY && mouseY <= pillY + pillH;
            RenderUtil.drawRoundedRect(pillX, pillY, pillW, pillH, 3.0f, isCur ? accent : (hov ? 0xFF262630 : 0xFF1C1C22));
            RenderUtil.drawRoundedOutline(pillX, pillY, pillW, pillH, 3.0f, 1.0f, isCur ? accent : 0xFF2B2B35);
            FontUtil.small.drawString(preset, pillX + 4.0f, pillY + (pillH - FontUtil.small.getHeight()) / 2.0f, isCur ? 0xFFFFFFFF : 0xFFB0B0BC);
            pillX += pillW + 4.0f;
        }

        // 2. GUI Scale Slider
        float scaleSecY = pillY + pillH + 14.0f;
        FontUtil.small.drawString("GUI Scale: \u00a7f" + SCALE_FORMAT.format(userScale) + "x", x + 10.0f, scaleSecY, 0xFF858592);
        float trackX = x + 10.0f;
        float trackY = scaleSecY + 12.0f;
        float trackW = w - 20.0f;
        float trackH = 4.0f;

        RenderUtil.drawRoundedRect(trackX, trackY, trackW, trackH, 2.0f, 0xFF23232C);
        float frac = AnimationUtil.clamp((userScale - 0.70f) / (1.25f - 0.70f), 0.0f, 1.0f);
        float fillW = trackW * frac;
        if (fillW > 1.0f) {
            RenderUtil.drawRoundedRect(trackX, trackY, fillW, trackH, 2.0f, accent);
        }
        RenderUtil.drawCircle(trackX + fillW, trackY + trackH / 2.0f, 3.5f, 0xFFFFFFFF);

        if (draggingScaleSlider) {
            float mouseFrac = AnimationUtil.clamp((mouseX - trackX) / trackW, 0.0f, 1.0f);
            this.userScale = 0.70f + (1.25f - 0.70f) * mouseFrac;
        }

        // 3. Blur Backdrop Toggle
        float blurY = trackY + 16.0f;
        ClickGUIModule cgm = getClickGUIModule();
        boolean blurOn = cgm == null || cgm.isBlurEnabled();
        FontUtil.small.drawString("Gaussian Blur Backdrop", x + 10.0f, blurY + 2.0f, 0xFFD0D0D8);

        float switchW = 24.0f;
        float switchH = 12.0f;
        float switchX = x + w - switchW - 10.0f;
        RenderUtil.drawRoundedRect(switchX, blurY, switchW, switchH, 6.0f, blurOn ? accent : 0xFF22222A);
        float knobX = blurOn ? (switchX + switchW - 6.0f) : (switchX + 6.0f);
        RenderUtil.drawCircle(knobX, blurY + switchH / 2.0f, 4.0f, 0xFFFFFFFF);

        // 4. Center Window Button
        float rBtnY = blurY + 22.0f;
        float rBtnW = w - 20.0f;
        float rBtnH = 17.0f;
        boolean rHov = mouseX >= x + 10.0f && mouseX <= x + 10.0f + rBtnW && mouseY >= rBtnY && mouseY <= rBtnY + rBtnH;
        RenderUtil.drawRoundedRect(x + 10.0f, rBtnY, rBtnW, rBtnH, 3.0f, rHov ? 0xFF282834 : 0xFF1C1C23);
        RenderUtil.drawRoundedOutline(x + 10.0f, rBtnY, rBtnW, rBtnH, 3.0f, 1.0f, 0xFF2E2E38);
        FontUtil.small.drawCenteredString("Reset Window Position", x + 10.0f + rBtnW / 2.0f, rBtnY + (rBtnH - FontUtil.small.getHeight()) / 2.0f, 0xFFB0B0BC);
    }

    private void renderFriendsView(float x, float y, float w, float h, float mouseX, float mouseY, int accent) {
        FontUtil.bold.drawString("Friends Manager", x + 10.0f, y + 4.0f, 0xFFFFFFFF);

        // Add friend input
        float inY = y + 16.0f;
        float inW = w - 52.0f;
        float inH = 17.0f;
        RenderUtil.drawRoundedRect(x + 8.0f, inY, inW, inH, 3.0f, 0xFF19191E);
        RenderUtil.drawRoundedOutline(x + 8.0f, inY, inW, inH, 3.0f, 1.0f, friendInputFocused ? accent : 0xFF2A2A32);

        String txt = newFriendInput.isEmpty() && !friendInputFocused ? "Username..." : newFriendInput + (friendInputFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : "");
        FontUtil.small.drawString(txt, x + 12.0f, inY + (inH - FontUtil.small.getHeight()) / 2.0f, newFriendInput.isEmpty() && !friendInputFocused ? 0xFF60606B : 0xFFFFFFFF);

        float btnX = x + 8.0f + inW + 3.0f;
        float btnW = 33.0f;
        boolean crHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= inY && mouseY <= inY + inH;
        RenderUtil.drawRoundedRect(btnX, inY, btnW, inH, 3.0f, crHovered ? accent : 0xFF22232B);
        FontUtil.small.drawCenteredString("Add", btnX + btnW / 2.0f, inY + (inH - FontUtil.small.getHeight()) / 2.0f, 0xFFFFFFFF);

        // Friends list box
        float listY = inY + inH + 8.0f;
        float listH = h - (listY - y) - 4.0f;
        RenderUtil.drawRoundedRect(x + 8.0f, listY, w - 16.0f, listH, 3.0f, 0xFF16161B);
        RenderUtil.drawRoundedOutline(x + 8.0f, listY, w - 16.0f, listH, 3.0f, 1.0f, 0xFF222228);

        List<String> friends = new ArrayList<>(FriendManager.getFriends());
        if (friends.isEmpty()) {
            FontUtil.small.drawCenteredString("No friends added yet.", x + 8.0f + (w - 16.0f) / 2.0f, listY + listH / 2.0f - 4.0f, 0xFF60606D);
        } else {
            float itemY = listY + 4.0f;
            for (String f : friends) {
                RenderUtil.drawRoundedRect(x + 10.0f, itemY, w - 20.0f, 18.0f, 2.5f, 0xFF191920);
                FontUtil.small.drawString(f, x + 14.0f, itemY + (18.0f - FontUtil.small.getHeight()) / 2.0f, 0xFFD8D8E0);

                // Delete button
                float delX = x + w - 32.0f;
                boolean dHov = mouseX >= delX && mouseX <= delX + 18.0f && mouseY >= itemY + 2.0f && mouseY <= itemY + 16.0f;
                RenderUtil.drawRoundedRect(delX, itemY + 2.0f, 18.0f, 14.0f, 2.0f, dHov ? 0xFFC62828 : 0xFF2B1D1D);
                FontUtil.small.drawCenteredString("X", delX + 9.0f, itemY + 2.0f + (14.0f - FontUtil.small.getHeight()) / 2.0f, 0xFFEF9A9A);

                itemY += 21.0f;
            }
        }
    }

    private boolean matchesFilter(Module module) {
        if (searchFilter == null || searchFilter.trim().isEmpty()) {
            return true;
        }
        return module.getName().toLowerCase().contains(searchFilter.trim().toLowerCase());
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) return;

        float scrollDelta = (dWheel > 0 ? -24.0f : 24.0f);

        if (activeView == ViewType.CATEGORY) {
            targetScrollOffset += scrollDelta;
        } else if (activeView == ViewType.PROFILES) {
            targetProfileScroll += scrollDelta;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        ScaledResolution sr = new ScaledResolution(mc);
        float effectiveScale = computeEffectiveScale(sr.getScaledWidth(), sr.getScaledHeight());
        float sX = mouseX / effectiveScale;
        float sY = mouseY / effectiveScale;

        // 1. Sidebar Header click (Drag window or open settings)
        float headH = 34.0f;
        if (sX >= guiX && sX <= guiX + SIDEBAR_WIDTH && sY >= guiY && sY <= guiY + headH) {
            float gearX = guiX + SIDEBAR_WIDTH - 20.0f;
            if (sX >= gearX - 4.0f && sX <= gearX + 16.0f) {
                activeView = (activeView == ViewType.SETTINGS) ? ViewType.CATEGORY : ViewType.SETTINGS;
                return;
            }
            if (mouseButton == 0) {
                draggingWindow = true;
                dragOffsetX = sX - guiX;
                dragOffsetY = sY - guiY;
            }
            return;
        }

        // 2. Right panel header click (Drag window)
        float rightX = guiX + SIDEBAR_WIDTH + GAP;
        if (sX >= rightX && sX <= rightX + CONTENT_WIDTH && sY >= guiY && sY <= guiY + headH) {
            if (mouseButton == 0) {
                draggingWindow = true;
                dragOffsetX = sX - guiX;
                dragOffsetY = sY - guiY;
            }
            return;
        }

        // 3. Sidebar Category selection
        float itemY = guiY + headH + 4.0f;
        float itemH = 24.0f;
        for (Category cat : Category.values()) {
            if (sX >= guiX + 3.0f && sX <= guiX + SIDEBAR_WIDTH - 3.0f && sY >= itemY && sY <= itemY + itemH) {
                activeView = ViewType.CATEGORY;
                activeCategory = cat;
                targetScrollOffset = 0.0f;
                scrollOffset = 0.0f;
                return;
            }
            itemY += itemH + 1.0f;
        }

        // 4. Sidebar MISC selection
        itemY += 16.0f; // skip "MISC" text
        String[] miscItems = {"Friends", "Profiles", "HUD Editor"};
        for (String misc : miscItems) {
            if (sX >= guiX + 3.0f && sX <= guiX + SIDEBAR_WIDTH - 3.0f && sY >= itemY && sY <= itemY + itemH) {
                if ("Friends".equals(misc)) {
                    activeView = ViewType.FRIENDS;
                } else if ("Profiles".equals(misc)) {
                    activeView = ViewType.PROFILES;
                    targetProfileScroll = 0.0f;
                } else if ("HUD Editor".equals(misc)) {
                    mc.displayGuiScreen(new HUDEditor());
                }
                return;
            }
            itemY += itemH + 1.0f;
        }

        // 5. Sidebar footer palette click (cycle theme)
        float footY = guiY + TOTAL_HEIGHT - 24.0f;
        float palX = guiX + SIDEBAR_WIDTH - 16.0f;
        float palY = footY + 12.0f;
        if (sX >= palX - 6.0f && sX <= palX + 6.0f && sY >= palY - 6.0f && sY <= palY + 6.0f) {
            String nextPreset = ColorUtil.cycleThemePreset();
            ClickGUIModule mod = getClickGUIModule();
            if (mod != null) mod.setThemePreset(nextPreset);
            return;
        }

        // 6. Right Content interactions
        float bodyY = guiY + headH + 4.0f;
        float bodyH = TOTAL_HEIGHT - headH - 8.0f;

        if (activeView == ViewType.CATEGORY) {
            List<ModuleButton> buttons = categoryButtons.get(activeCategory);
            if (buttons != null && sX >= rightX && sX <= rightX + CONTENT_WIDTH && sY >= bodyY && sY <= bodyY + bodyH) {
                for (ModuleButton btn : buttons) {
                    if (matchesFilter(btn.getModule())) {
                        btn.mouseClicked(sX, sY, mouseButton);
                    }
                }
            }
        } else if (activeView == ViewType.PROFILES) {
            handleProfilesClick(rightX, bodyY, CONTENT_WIDTH, sX, sY, mouseButton);
        } else if (activeView == ViewType.SETTINGS) {
            handleSettingsClick(rightX, bodyY, CONTENT_WIDTH, sX, sY, mouseButton);
        } else if (activeView == ViewType.FRIENDS) {
            handleFriendsClick(rightX, bodyY, CONTENT_WIDTH, sX, sY, mouseButton);
        }
    }

    private void handleProfilesClick(float x, float y, float w, float sX, float sY, int mouseButton) {
        ConfigManager cm = Cigarette.INSTANCE != null ? Cigarette.INSTANCE.getConfigManager() : null;
        float inY = y + 15.0f;
        float inW = w - 52.0f;
        float inH = 17.0f;

        // Click text input
        if (sX >= x + 8.0f && sX <= x + 8.0f + inW && sY >= inY && sY <= inY + inH) {
            profileInputFocused = true;
            return;
        } else {
            profileInputFocused = false;
        }

        // Click "Add" profile
        float btnX = x + 8.0f + inW + 3.0f;
        float btnW = 33.0f;
        if (sX >= btnX && sX <= btnX + btnW && sY >= inY && sY <= inY + inH) {
            if (cm != null && !newProfileInput.trim().isEmpty()) {
                cm.save(newProfileInput.trim());
                newProfileInput = "";
            }
            return;
        }

        // Click "Save Active"
        float saveY = inY + inH + 5.0f;
        float saveW = w - 16.0f;
        if (sX >= x + 8.0f && sX <= x + 8.0f + saveW && sY >= saveY && sY <= saveY + 16.0f) {
            if (cm != null && cm.getActiveProfile() != null) {
                cm.save(cm.getActiveProfile().getName());
            }
            return;
        }

        // List items Load clicks
        float listY = saveY + 20.0f;
        if (cm != null && sY >= listY) {
            List<String> list = cm.listProfiles();
            float itemH = 20.0f;
            float curY = listY + 3.0f - profileScrollOffset;
            for (String p : list) {
                float lBtnX = x + w - 38.0f;
                if (sX >= lBtnX && sX <= lBtnX + 24.0f && sY >= curY + 2.5f && sY <= curY + 17.5f) {
                    cm.load(p);
                    return;
                }
                curY += itemH + 2.0f;
            }
        }
    }

    private void handleSettingsClick(float x, float y, float w, float sX, float sY, int mouseButton) {
        // Theme preset pills
        String[] presets = {"Electric Blue", "Ember Orange", "Amethyst", "Emerald", "Rainbow"};
        float pillX = x + 10.0f;
        float pillY = y + 32.0f;
        float pillH = 15.0f;

        for (String preset : presets) {
            float pillW = FontUtil.small.getStringWidth(preset) + 8.0f;
            if (pillX + pillW > x + w - 10.0f) {
                pillX = x + 10.0f;
                pillY += pillH + 4.0f;
            }
            if (sX >= pillX && sX <= pillX + pillW && sY >= pillY && sY <= pillY + pillH) {
                ClickGUIModule cgm = getClickGUIModule();
                if (cgm != null) cgm.setThemePreset(preset);
                return;
            }
            pillX += pillW + 4.0f;
        }

        // Scale slider
        float scaleSecY = pillY + pillH + 14.0f;
        float trackX = x + 10.0f;
        float trackY = scaleSecY + 12.0f;
        float trackW = w - 20.0f;
        float trackH = 4.0f;
        if (sX >= trackX - 4.0f && sX <= trackX + trackW + 4.0f && sY >= trackY - 4.0f && sY <= trackY + trackH + 4.0f) {
            draggingScaleSlider = true;
            float mouseFrac = AnimationUtil.clamp((sX - trackX) / trackW, 0.0f, 1.0f);
            this.userScale = 0.70f + (1.25f - 0.70f) * mouseFrac;
            return;
        }

        // Blur backdrop toggle
        float blurY = trackY + 16.0f;
        float switchW = 24.0f;
        float switchH = 12.0f;
        float switchX = x + w - switchW - 10.0f;
        if (sX >= switchX - 2.0f && sX <= switchX + switchW + 2.0f && sY >= blurY - 2.0f && sY <= blurY + switchH + 2.0f) {
            ClickGUIModule cgm = getClickGUIModule();
            if (cgm != null && cgm.getBlurBackdrop() != null) {
                cgm.getBlurBackdrop().toggle();
            }
            return;
        }

        // Reset position button
        float rBtnY = blurY + 22.0f;
        float rBtnW = w - 20.0f;
        float rBtnH = 17.0f;
        if (sX >= x + 10.0f && sX <= x + 10.0f + rBtnW && sY >= rBtnY && sY <= rBtnY + rBtnH) {
            ScaledResolution sr = new ScaledResolution(mc);
            float effectiveScale = computeEffectiveScale(sr.getScaledWidth(), sr.getScaledHeight());
            float vW = sr.getScaledWidth() / effectiveScale;
            float vH = sr.getScaledHeight() / effectiveScale;
            guiX = (vW - TOTAL_WIDTH) / 2.0f;
            guiY = (vH - TOTAL_HEIGHT) / 2.0f;
            userScale = 1.0f;
        }
    }

    private void handleFriendsClick(float x, float y, float w, float sX, float sY, int mouseButton) {
        float inY = y + 16.0f;
        float inW = w - 52.0f;
        float inH = 17.0f;

        if (sX >= x + 8.0f && sX <= x + 8.0f + inW && sY >= inY && sY <= inY + inH) {
            friendInputFocused = true;
            return;
        } else {
            friendInputFocused = false;
        }

        float btnX = x + 8.0f + inW + 3.0f;
        float btnW = 33.0f;
        if (sX >= btnX && sX <= btnX + btnW && sY >= inY && sY <= inY + inH) {
            if (!newFriendInput.trim().isEmpty()) {
                FriendManager.addFriend(newFriendInput.trim());
                newFriendInput = "";
            }
            return;
        }

        // Remove friend clicks
        float listY = inY + inH + 8.0f;
        List<String> friends = new ArrayList<>(FriendManager.getFriends());
        float itemY = listY + 4.0f;
        for (String f : friends) {
            float delX = x + w - 32.0f;
            if (sX >= delX && sX <= delX + 18.0f && sY >= itemY + 2.0f && sY <= itemY + 16.0f) {
                FriendManager.removeFriend(f);
                return;
            }
            itemY += 21.0f;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingWindow = false;
        draggingScaleSlider = false;

        ScaledResolution sr = new ScaledResolution(mc);
        float effectiveScale = computeEffectiveScale(sr.getScaledWidth(), sr.getScaledHeight());
        float sX = mouseX / effectiveScale;
        float sY = mouseY / effectiveScale;

        if (activeView == ViewType.CATEGORY) {
            List<ModuleButton> buttons = categoryButtons.get(activeCategory);
            if (buttons != null) {
                for (ModuleButton btn : buttons) {
                    btn.mouseReleased(sX, sY, state);
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Check if any module is binding a key
        if (activeView == ViewType.CATEGORY) {
            List<ModuleButton> buttons = categoryButtons.get(activeCategory);
            if (buttons != null) {
                for (ModuleButton btn : buttons) {
                    if (btn.isListening()) {
                        btn.keyTyped(typedChar, keyCode);
                        return;
                    }
                }
            }
        }

        // Profile input typing
        if (profileInputFocused) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                if (keyCode == Keyboard.KEY_RETURN && Cigarette.INSTANCE != null && !newProfileInput.trim().isEmpty()) {
                    Cigarette.INSTANCE.getConfigManager().save(newProfileInput.trim());
                    newProfileInput = "";
                }
                profileInputFocused = false;
            } else if (keyCode == Keyboard.KEY_BACK && !newProfileInput.isEmpty()) {
                newProfileInput = newProfileInput.substring(0, newProfileInput.length() - 1);
            } else if (Character.isLetterOrDigit(typedChar) || typedChar == '_' || typedChar == '-') {
                if (newProfileInput.length() < 24) newProfileInput += typedChar;
            }
            return;
        }

        // Friend input typing
        if (friendInputFocused) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                if (keyCode == Keyboard.KEY_RETURN && !newFriendInput.trim().isEmpty()) {
                    FriendManager.addFriend(newFriendInput.trim());
                    newFriendInput = "";
                }
                friendInputFocused = false;
            } else if (keyCode == Keyboard.KEY_BACK && !newFriendInput.isEmpty()) {
                newFriendInput = newFriendInput.substring(0, newFriendInput.length() - 1);
            } else if (Character.isLetterOrDigit(typedChar) || typedChar == '_') {
                if (newFriendInput.length() < 16) newFriendInput += typedChar;
            }
            return;
        }

        // Universal close keys: Escape, Right Shift, Insert
        ClickGUIModule clickGUIModule = getClickGUIModule();
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RSHIFT || keyCode == Keyboard.KEY_INSERT
                || (clickGUIModule != null && keyCode == clickGUIModule.getKeyCode())) {
            mc.displayGuiScreen(null);
            return;
        }

        // Pass key down to buttons
        if (activeView == ViewType.CATEGORY) {
            List<ModuleButton> buttons = categoryButtons.get(activeCategory);
            if (buttons != null) {
                for (ModuleButton btn : buttons) {
                    btn.keyTyped(typedChar, keyCode);
                }
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

    public float getGuiScale() {
        return userScale;
    }

    public void setGuiScale(float guiScale) {
        this.userScale = AnimationUtil.clamp(guiScale, 0.70f, 1.25f);
    }
}
