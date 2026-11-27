package dev.taezeem14.cigarette.ui.clickgui;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.config.ConfigManager;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.client.FriendManager;
import dev.taezeem14.cigarette.module.impl.render.ClickGUIModule;
import dev.taezeem14.cigarette.setting.Setting;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
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
 * Cigarette V1 signature dual-card ClickGUI.
 * Features Cigarette V1 branding, live cross-category search bar, category module counters,
 * viewport auto-fit scaling, unified window dragging, 3-dot drawers with laptop-friendly keybinds,
 * 1.9+ multi-version combat cooldown quick-toggle, profiles, friends, and hover descriptions.
 */
public class ClickGUI extends GuiScreen {

    private static final DecimalFormat SCALE_FORMAT = new DecimalFormat("0.##");

    public static final float SIDEBAR_WIDTH = 136.0f;
    public static final float CONTENT_WIDTH = 212.0f;
    public static final float GAP = 7.0f;
    public static final float TOTAL_WIDTH = SIDEBAR_WIDTH + GAP + CONTENT_WIDTH; // 355px
    public static final float TOTAL_HEIGHT = 336.0f;

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

    // Global Search state
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

    // Hovered module for bottom tooltip bar
    private Module hoveredModule = null;

    public ClickGUI() {
        initCategoryButtons();
    }

    private void initCategoryButtons() {
        categoryButtons.clear();
        for (Category cat : Category.values()) {
            List<Module> mods = new ArrayList<>();
            if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
                mods = Cigarette.INSTANCE.getModuleManager().getModulesByCategory(cat);
            }
            List<ModuleButton> buttons = new ArrayList<>();
            for (Module m : mods) {
                buttons.add(new ModuleButton(m, 0, 0, CONTENT_WIDTH - 10.0f));
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

        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            boolean mismatch = false;
            for (Category cat : Category.values()) {
                List<Module> current = Cigarette.INSTANCE.getModuleManager().getModulesByCategory(cat);
                List<ModuleButton> cached = categoryButtons.get(cat);
                if (cached == null || current.size() != cached.size()) {
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
        searchFocused = false;
        profileInputFocused = false;
        friendInputFocused = false;
        RenderUtil.clearScissorStack();

        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getConfigManager() != null
                && Cigarette.INSTANCE.getConfigManager().getActiveProfile() != null) {
            Cigarette.INSTANCE.getConfigManager().save(Cigarette.INSTANCE.getConfigManager().getActiveProfile().getName());
        }

        ClickGUIModule clickGUIModule = getClickGUIModule();
        if (clickGUIModule != null && clickGUIModule.isEnabled()) {
            clickGUIModule.setState(false);
        }
    }

    private float computeEffectiveScale(float screenW, float screenH) {
        float maxFitW = screenW * 0.86f;
        float maxFitH = screenH * 0.86f;
        float autoFitScale = Math.min(1.0f, Math.min(maxFitW / TOTAL_WIDTH, maxFitH / TOTAL_HEIGHT));
        return Math.max(0.50f, autoFitScale * userScale);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        FontUtil.ensureInitialized();
        fadeProgress = Math.min(1.0f, fadeProgress + AnimationUtil.getDeltaTime() * 7.5f);
        hoveredModule = null;

        ClickGUIModule clickGUIModule = getClickGUIModule();
        if (clickGUIModule != null) {
            clickGUIModule.syncThemeFromSetting();
        }

        ScaledResolution sr = new ScaledResolution(mc);
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        if (clickGUIModule == null || clickGUIModule.isBlurEnabled()) {
            BlurUtil.blurAll(Math.max(1.0f, 6.0f * AnimationUtil.easeOutCubic(fadeProgress)));
        }

        int overlayAlpha = (int) (135 * fadeProgress);
        RenderUtil.drawRect(0, 0, screenW, screenH, (overlayAlpha << 24) | 0x0A0A0E);

        float effectiveScale = computeEffectiveScale(screenW, screenH);
        float viewportW = screenW / effectiveScale;
        float viewportH = screenH / effectiveScale;

        if (guiX < 0 || guiY < 0) {
            guiX = Math.max(8.0f, (viewportW - TOTAL_WIDTH) / 2.0f);
            guiY = Math.max(8.0f, (viewportH - TOTAL_HEIGHT) / 2.0f);
        }

        float scaledMouseX = mouseX / effectiveScale;
        float scaledMouseY = mouseY / effectiveScale;

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

            renderSidebar(scaledMouseX, scaledMouseY, accent);
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

        RenderUtil.drawDropShadow(x, y, w, h, 6.0f, 0x70000000);
        RenderUtil.drawRoundedRect(x, y, w, h, 5.5f, 0xFF141418);
        RenderUtil.drawRoundedOutline(x, y, w, h, 5.5f, 1.0f, 0xFF25252E);

        // Header: Cigarette V1 Logo + Badge + Settings Gear
        float headH = 34.0f;
        RenderUtil.drawRoundedRectTop(x, y, w, headH, 5.5f, 0xFF191920);
        RenderUtil.drawRoundedRectTop(x + 1.0f, y, w - 2.0f, 1.5f, 1.5f, ColorUtil.modifyAlpha(accent, 190));
        RenderUtil.drawRect(x + 1.0f, y + headH - 1.0f, w - 2.0f, 1.0f, 0xFF23232B);

        RenderUtil.drawFaIcon("logo", x + 8.0f, y + (headH - 11.0f) / 2.0f, 11.0f, accent);
        float brandX = x + 22.0f;
        FontUtil.bold.drawString("CIGARETTE", brandX, y + (headH - FontUtil.bold.getHeight()) / 2.0f, 0xFFFFFFFF);

        float v1X = brandX + FontUtil.bold.getStringWidth("CIGARETTE") + 4.0f;
        float v1Y = y + (headH - 11.5f) / 2.0f;
        RenderUtil.drawRoundedRect(v1X, v1Y, 16.0f, 11.5f, 2.5f, ColorUtil.modifyAlpha(accent, 38));
        RenderUtil.drawRoundedOutline(v1X, v1Y, 16.0f, 11.5f, 2.5f, 1.0f, accent);
        FontUtil.small.drawCenteredString("v1", v1X + 8.0f, v1Y + (11.5f - FontUtil.small.getHeight()) / 2.0f, accent);

        // Settings gear icon
        float gearX = x + w - 18.5f;
        float gearY = y + (headH - 10.0f) / 2.0f;
        boolean gearHovered = mouseX >= gearX - 3.0f && mouseX <= gearX + 13.0f && mouseY >= gearY - 3.0f && mouseY <= gearY + 13.0f;
        int gearColor = (activeView == ViewType.SETTINGS) ? accent : (gearHovered ? 0xFFFFFFFF : 0xFF787887);
        RenderUtil.drawFaIcon("gear", gearX, gearY, 10.0f, gearColor);

        // Category Items List
        float itemY = y + headH + 5.0f;
        float itemH = 24.0f;

        for (Category cat : Category.values()) {
            boolean active = (activeView == ViewType.CATEGORY && activeCategory == cat && searchFilter.trim().isEmpty());
            boolean hovered = mouseX >= x + 3.0f && mouseX <= x + w - 3.0f && mouseY >= itemY && mouseY <= itemY + itemH;

            int itemBg = active ? 0xFF1E212B : (hovered ? 0xFF1A1C25 : 0x00000000);
            if (itemBg != 0) {
                RenderUtil.drawRoundedRect(x + 3.5f, itemY, w - 7.0f, itemH, 3.5f, itemBg);
            }
            if (active) {
                RenderUtil.drawRoundedRect(x + 4.5f, itemY + 3.5f, 2.5f, itemH - 7.0f, 1.25f, accent);
            }

            int iconCol = active ? accent : (hovered ? 0xFFD4D4DE : 0xFF757584);
            RenderUtil.drawFaIcon(cat.name(), x + 10.5f, itemY + (itemH - 10.0f) / 2.0f, 10.0f, iconCol);

            int textCol = active ? 0xFFFFFFFF : (hovered ? 0xFFEAEAF2 : 0xFFA0A0AC);
            if (active) {
                FontUtil.bold.drawString(cat.getDisplayName(), x + 25.5f, itemY + (itemH - FontUtil.bold.getHeight()) / 2.0f, textCol);
            } else {
                FontUtil.regular.drawString(cat.getDisplayName(), x + 25.5f, itemY + (itemH - FontUtil.regular.getHeight()) / 2.0f, textCol);
            }

            // Enabled / Total counter badge
            List<ModuleButton> catBtns = categoryButtons.get(cat);
            int total = catBtns != null ? catBtns.size() : 0;
            int enabledCount = 0;
            if (catBtns != null) {
                for (ModuleButton mb : catBtns) {
                    if (mb.getModule().isEnabled()) enabledCount++;
                }
            }
            String countStr = enabledCount > 0 ? (enabledCount + "/" + total) : String.valueOf(total);
            float cW = FontUtil.small.getStringWidth(countStr) + 6.0f;
            float cX = x + w - cW - 15.0f;
            float cY = itemY + (itemH - 10.5f) / 2.0f;
            RenderUtil.drawRoundedRect(cX, cY, cW, 10.5f, 2.0f, enabledCount > 0 ? ColorUtil.modifyAlpha(accent, 36) : 0xFF1D1D24);
            FontUtil.small.drawCenteredString(countStr, cX + cW / 2.0f, cY + (10.5f - FontUtil.small.getHeight()) / 2.0f,
                    enabledCount > 0 ? accent : 0xFF6B6B78);

            int chevCol = active ? accent : (hovered ? 0xFF8E8E9A : 0xFF4A4A55);
            RenderUtil.drawFaIcon("chevron_right", x + w - 12.5f, itemY + (itemH - 7.5f) / 2.0f, 7.5f, chevCol);

            itemY += itemH + 1.5f;
        }

        // Section: "CLIENT & TOOLS"
        itemY += 4.0f;
        FontUtil.small.drawString("CLIENT & TOOLS", x + 10.0f, itemY, 0xFF5A5A68);
        itemY += 12.5f;

        String[] miscItems = {"Friends", "Profiles", "HUD Editor"};
        for (String misc : miscItems) {
            boolean active = ("Profiles".equals(misc) && activeView == ViewType.PROFILES)
                    || ("Friends".equals(misc) && activeView == ViewType.FRIENDS);
            boolean hovered = mouseX >= x + 3.0f && mouseX <= x + w - 3.0f && mouseY >= itemY && mouseY <= itemY + itemH;

            int itemBg = active ? 0xFF1E212B : (hovered ? 0xFF1A1C25 : 0x00000000);
            if (itemBg != 0) {
                RenderUtil.drawRoundedRect(x + 3.5f, itemY, w - 7.0f, itemH, 3.5f, itemBg);
            }
            if (active) {
                RenderUtil.drawRoundedRect(x + 4.5f, itemY + 3.5f, 2.5f, itemH - 7.0f, 1.25f, accent);
            }

            int iconCol = active ? accent : (hovered ? 0xFFD4D4DE : 0xFF757584);
            String iconKey = "Friends".equals(misc) ? "friends" : ("Profiles".equals(misc) ? "profiles" : "hud");
            RenderUtil.drawFaIcon(iconKey, x + 10.5f, itemY + (itemH - 10.0f) / 2.0f, 10.0f, iconCol);

            int textCol = active ? 0xFFFFFFFF : (hovered ? 0xFFEAEAF2 : 0xFFA0A0AC);
            if (active) {
                FontUtil.bold.drawString(misc, x + 25.5f, itemY + (itemH - FontUtil.bold.getHeight()) / 2.0f, textCol);
            } else {
                FontUtil.regular.drawString(misc, x + 25.5f, itemY + (itemH - FontUtil.regular.getHeight()) / 2.0f, textCol);
            }

            if ("Profiles".equals(misc)) {
                ConfigManager cm = Cigarette.INSTANCE != null ? Cigarette.INSTANCE.getConfigManager() : null;
                String activeProf = (cm != null && cm.getActiveProfile() != null) ? cm.getActiveProfile().getName() : "default";
                if (activeProf.length() > 8) activeProf = activeProf.substring(0, 8);
                float badgeW = FontUtil.small.getStringWidth(activeProf) + 6.0f;
                float badgeX = x + w - badgeW - 15.0f;
                float badgeY = itemY + (itemH - 10.5f) / 2.0f;
                RenderUtil.drawRoundedRect(badgeX, badgeY, badgeW, 10.5f, 2.0f, 0xFF1F2028);
                FontUtil.small.drawString(activeProf, badgeX + 3.0f, badgeY + (10.5f - FontUtil.small.getHeight()) / 2.0f, 0xFF828292);
            }

            int chevCol = active ? accent : (hovered ? 0xFF8E8E9A : 0xFF4A4A55);
            RenderUtil.drawFaIcon("chevron_right", x + w - 12.5f, itemY + (itemH - 7.5f) / 2.0f, 7.5f, chevCol);

            itemY += itemH + 1.5f;
        }

        // Sidebar Footer
        float footY = y + h - 24.0f;
        RenderUtil.drawRect(x + 1.0f, footY, w - 2.0f, 1.0f, 0xFF202028);

        RenderUtil.drawFaIcon("user", x + 9.5f, footY + 6.5f, 10.0f, accent);
        String playerName = (mc.getSession() != null && mc.getSession().getUsername() != null)
                ? mc.getSession().getUsername() : "Player";
        if (playerName.length() > 11) playerName = playerName.substring(0, 11);
        FontUtil.small.drawString(playerName, x + 23.0f, footY + (24.0f - FontUtil.small.getHeight()) / 2.0f, 0xFFB8B8C6);

        // Scale tag + Theme cycler circle
        String scaleTag = SCALE_FORMAT.format(userScale) + "x";
        FontUtil.small.drawString(scaleTag, x + w - 42.0f, footY + (24.0f - FontUtil.small.getHeight()) / 2.0f, 0xFF6E6E7C);

        float palX = x + w - 14.0f;
        float palY = footY + 12.0f;
        boolean palHovered = mouseX >= palX - 6.0f && mouseX <= palX + 6.0f && mouseY >= palY - 6.0f && mouseY <= palY + 6.0f;
        RenderUtil.drawCircle(palX, palY, palHovered ? 4.5f : 3.8f, accent);
        RenderUtil.drawCircleOutline(palX, palY, palHovered ? 5.8f : 5.0f, 1.0f, 0xFF3A3A48);
    }

    private void renderContentPanel(float mouseX, float mouseY, float partialTicks, float effectiveScale, int accent) {
        float x = guiX + SIDEBAR_WIDTH + GAP;
        float y = guiY;
        float w = CONTENT_WIDTH;
        float h = TOTAL_HEIGHT;

        RenderUtil.drawDropShadow(x, y, w, h, 6.0f, 0x70000000);
        RenderUtil.drawRoundedRect(x, y, w, h, 5.5f, 0xFF141418);
        RenderUtil.drawRoundedOutline(x, y, w, h, 5.5f, 1.0f, 0xFF25252E);

        // Header
        float headH = 34.0f;
        RenderUtil.drawRoundedRectTop(x, y, w, headH, 5.5f, 0xFF191920);
        RenderUtil.drawRoundedRectTop(x + 1.0f, y, w - 2.0f, 1.5f, 1.5f, ColorUtil.modifyAlpha(accent, 190));
        RenderUtil.drawRect(x + 1.0f, y + headH - 1.0f, w - 2.0f, 1.0f, 0xFF23232B);

        boolean searching = !searchFilter.trim().isEmpty();
        String title = "Combat";
        String iconKey = "combat";
        if (searching && activeView == ViewType.CATEGORY) {
            title = "Search";
            iconKey = "search";
        } else if (activeView == ViewType.CATEGORY) {
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
        FontUtil.bold.drawString(title, x + 24.0f, y + (headH - FontUtil.bold.getHeight()) / 2.0f, 0xFFFFFFFF);

        // Interactive Global Search Pill on right side of Header
        float searchW = 96.0f;
        float searchH = 17.5f;
        float searchX = x + w - searchW - 8.0f;
        float searchY = y + (headH - searchH) / 2.0f;
        boolean searchHov = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH;

        RenderUtil.drawRoundedRect(searchX, searchY, searchW, searchH, 3.5f, 0xFF121216);
        RenderUtil.drawRoundedOutline(searchX, searchY, searchW, searchH, 3.5f, 1.0f,
                searchFocused ? accent : (searchHov ? 0xFF383846 : 0xFF272732));
        RenderUtil.drawFaIcon("search", searchX + 5.0f, searchY + (searchH - 8.5f) / 2.0f, 8.5f,
                searchFocused ? accent : 0xFF6E6E7C);

        String sDisp = searchFilter.isEmpty() && !searchFocused
                ? "Search all..."
                : searchFilter + (searchFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : "");
        FontUtil.small.drawString(sDisp, searchX + 17.0f, searchY + (searchH - FontUtil.small.getHeight()) / 2.0f,
                searchFilter.isEmpty() && !searchFocused ? 0xFF626270 : 0xFFEFEFF6);

        if (!searchFilter.isEmpty()) {
            FontUtil.small.drawString("x", searchX + searchW - 9.5f, searchY + (searchH - FontUtil.small.getHeight()) / 2.0f, 0xFF9A9AA8);
        }

        // Body Content Views (reserving 20px at bottom for tooltip status bar)
        float footerH = 20.0f;
        float bodyY = y + headH + 4.0f;
        float bodyH = h - headH - footerH - 6.0f;

        if (activeView == ViewType.CATEGORY) {
            renderCategoryModules(x, bodyY, w, bodyH, mouseX, mouseY, partialTicks, effectiveScale, accent);
        } else if (activeView == ViewType.PROFILES) {
            renderProfilesView(x, bodyY, w, bodyH, mouseX, mouseY, accent);
        } else if (activeView == ViewType.SETTINGS) {
            renderSettingsView(x, bodyY, w, bodyH, mouseX, mouseY, accent);
        } else if (activeView == ViewType.FRIENDS) {
            renderFriendsView(x, bodyY, w, bodyH, mouseX, mouseY, accent);
        }

        // Bottom Tooltip / Description Status Bar
        float statusY = y + h - footerH;
        RenderUtil.drawRect(x + 1.0f, statusY, w - 2.0f, 1.0f, 0xFF202028);
        String tip = hoveredModule != null
                ? "\u00a7f" + hoveredModule.getName() + ": \u00a77" + hoveredModule.getDescription()
                : "Click: Toggle \u2022 \u22ee / R-Click: Settings & Keybind";
        tip = trimSmallText(tip, w - 14.0f);
        FontUtil.small.drawString(tip, x + 7.0f, statusY + (footerH - FontUtil.small.getHeight()) / 2.0f + 0.5f, 0xFF828292);
    }

    private String trimSmallText(String text, float maxW) {
        if (text == null || FontUtil.small.getStringWidth(text) <= maxW) return text;
        String cur = text;
        while (cur.length() > 4 && FontUtil.small.getStringWidth(cur + "..") > maxW) {
            cur = cur.substring(0, cur.length() - 1);
        }
        return cur + "..";
    }

    private List<ModuleButton> getVisibleButtons() {
        String q = searchFilter == null ? "" : searchFilter.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            List<ModuleButton> list = categoryButtons.get(activeCategory);
            return list != null ? list : Collections.emptyList();
        }
        List<ModuleButton> matches = new ArrayList<>();
        for (List<ModuleButton> group : categoryButtons.values()) {
            for (ModuleButton btn : group) {
                Module m = btn.getModule();
                if (m.getName().toLowerCase(Locale.ROOT).contains(q)
                        || m.getDescription().toLowerCase(Locale.ROOT).contains(q)
                        || m.getCategory().getDisplayName().toLowerCase(Locale.ROOT).contains(q)) {
                    matches.add(btn);
                }
            }
        }
        return matches;
    }

    private void renderCategoryModules(float panelX, float bodyY, float panelW, float bodyH,
                                       float mouseX, float mouseY, float partialTicks, float effectiveScale, int accent) {
        List<ModuleButton> buttons = getVisibleButtons();

        float totalH = 4.0f;
        for (ModuleButton btn : buttons) {
            totalH += btn.getTotalHeight() + 2.5f;
        }

        float maxScroll = Math.max(0.0f, totalH - bodyH);
        targetScrollOffset = AnimationUtil.clamp(targetScrollOffset, 0.0f, maxScroll);
        scrollOffset = AnimationUtil.lerp(scrollOffset, targetScrollOffset, 16.0f);

        RenderUtil.pushScissor(panelX, bodyY, panelW, bodyH, effectiveScale);
        try {
            if (buttons.isEmpty()) {
                FontUtil.small.drawCenteredString("No matching modules found.", panelX + panelW / 2.0f, bodyY + bodyH / 2.0f - 4.0f, 0xFF666675);
                return;
            }

            float btnY = bodyY + 2.0f - scrollOffset;
            float btnW = panelW - 11.0f;

            for (ModuleButton btn : buttons) {
                btn.setX(panelX + 5.0f);
                btn.setY(btnY);
                btn.setWidth(btnW);
                btn.render(mouseX, mouseY, partialTicks, effectiveScale);

                if (mouseY >= bodyY && mouseY <= bodyY + bodyH && btn.isHovered(mouseX, mouseY)) {
                    hoveredModule = btn.getModule();
                }
                btnY += btn.getTotalHeight() + 2.5f;
            }

            if (maxScroll > 0.5f) {
                float trackX = panelX + panelW - 4.0f;
                float trackH = bodyH - 4.0f;
                RenderUtil.drawRoundedRect(trackX, bodyY + 2.0f, 2.0f, trackH, 1.0f, 0xFF1E1E26);

                float thumbH = Math.max(20.0f, trackH * (bodyH / totalH));
                float thumbY = bodyY + 2.0f + (trackH - thumbH) * (scrollOffset / maxScroll);
                RenderUtil.drawRoundedRect(trackX, thumbY, 2.0f, thumbH, 1.0f, ColorUtil.modifyAlpha(accent, 200));
            }
        } finally {
            RenderUtil.popScissor();
        }
    }

    private void renderProfilesView(float x, float y, float w, float h, float mouseX, float mouseY, int accent) {
        ConfigManager cm = Cigarette.INSTANCE != null ? Cigarette.INSTANCE.getConfigManager() : null;
        String activeName = (cm != null && cm.getActiveProfile() != null) ? cm.getActiveProfile().getName() : "default";

        FontUtil.small.drawString("Active Profile: \u00a7a" + activeName, x + 8.0f, y + 2.0f, 0xFFCCCCCC);

        float inY = y + 15.0f;
        float inW = w - 54.0f;
        float inH = 17.5f;
        RenderUtil.drawRoundedRect(x + 8.0f, inY, inW, inH, 3.0f, 0xFF191920);
        RenderUtil.drawRoundedOutline(x + 8.0f, inY, inW, inH, 3.0f, 1.0f, profileInputFocused ? accent : 0xFF2B2B35);

        String txt = newProfileInput.isEmpty() && !profileInputFocused
                ? "New profile name..."
                : newProfileInput + (profileInputFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : "");
        FontUtil.small.drawString(txt, x + 12.0f, inY + (inH - FontUtil.small.getHeight()) / 2.0f,
                newProfileInput.isEmpty() && !profileInputFocused ? 0xFF626270 : 0xFFFFFFFF);

        float btnX = x + 8.0f + inW + 3.0f;
        float btnW = 35.0f;
        boolean crHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= inY && mouseY <= inY + inH;
        RenderUtil.drawRoundedRect(btnX, inY, btnW, inH, 3.0f, crHovered ? accent : 0xFF23242E);
        FontUtil.small.drawCenteredString("Create", btnX + btnW / 2.0f, inY + (inH - FontUtil.small.getHeight()) / 2.0f, 0xFFFFFFFF);

        float saveY = inY + inH + 5.0f;
        float saveW = w - 16.0f;
        boolean saveHovered = mouseX >= x + 8.0f && mouseX <= x + 8.0f + saveW && mouseY >= saveY && mouseY <= saveY + 16.5f;
        RenderUtil.drawRoundedRect(x + 8.0f, saveY, saveW, 16.5f, 3.0f, saveHovered ? 0xFF10B981 : 0xFF172520);
        RenderUtil.drawRoundedOutline(x + 8.0f, saveY, saveW, 16.5f, 3.0f, 1.0f, 0xFF10B981);
        FontUtil.small.drawCenteredString("Save Current Profile", x + 8.0f + saveW / 2.0f, saveY + (16.5f - FontUtil.small.getHeight()) / 2.0f, 0xFFFFFFFF);

        float listY = saveY + 21.0f;
        float listH = h - (listY - y) - 4.0f;
        RenderUtil.drawRoundedRect(x + 8.0f, listY, w - 16.0f, listH, 3.0f, 0xFF16161C);
        RenderUtil.drawRoundedOutline(x + 8.0f, listY, w - 16.0f, listH, 3.0f, 1.0f, 0xFF23232C);

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
                    RenderUtil.drawRoundedRect(x + 10.0f, curY, w - 20.0f, itemH, 2.5f, hov ? 0xFF22222C : (isCur ? 0xFF1A1D28 : 0xFF191920));

                    int nameCol = isCur ? accent : 0xFFD4D4DE;
                    FontUtil.small.drawString(p, x + 14.0f, curY + (itemH - FontUtil.small.getHeight()) / 2.0f, nameCol);

                    boolean canDel = !"default".equalsIgnoreCase(p);
                    float lBtnX = x + w - (canDel ? 57.0f : 38.0f);
                    boolean lHov = mouseX >= lBtnX && mouseX <= lBtnX + 26.0f && mouseY >= curY + 2.5f && mouseY <= curY + 17.5f;
                    RenderUtil.drawRoundedRect(lBtnX, curY + 2.5f, 26.0f, 15.0f, 2.0f, lHov ? accent : 0xFF252632);
                    FontUtil.small.drawCenteredString("Load", lBtnX + 13.0f, curY + 2.5f + (15.0f - FontUtil.small.getHeight()) / 2.0f, 0xFFFFFFFF);

                    if (canDel) {
                        float dBtnX = x + w - 28.0f;
                        boolean dHov = mouseX >= dBtnX && mouseX <= dBtnX + 16.0f && mouseY >= curY + 2.5f && mouseY <= curY + 17.5f;
                        RenderUtil.drawRoundedRect(dBtnX, curY + 2.5f, 16.0f, 15.0f, 2.0f, dHov ? 0xFFC62828 : 0xFF2C1D1F);
                        FontUtil.small.drawCenteredString("X", dBtnX + 8.0f, curY + 2.5f + (15.0f - FontUtil.small.getHeight()) / 2.0f, 0xFFEF9A9A);
                    }

                    curY += itemH + 2.0f;
                }
            } finally {
                RenderUtil.popScissor();
            }
        }
    }

    private void renderSettingsView(float x, float y, float w, float h, float mouseX, float mouseY, int accent) {
        FontUtil.bold.drawString("Cigarette V1 Preferences", x + 10.0f, y + 3.0f, 0xFFFFFFFF);

        // 1. Theme Presets (7 presets)
        FontUtil.small.drawString("Theme Accent Preset", x + 10.0f, y + 18.0f, 0xFF8A8A98);
        String[] presets = ColorUtil.THEME_PRESETS;
        float pillX = x + 10.0f;
        float pillY = y + 29.0f;
        float pillH = 14.5f;

        for (String preset : presets) {
            float pillW = FontUtil.small.getStringWidth(preset) + 8.0f;
            if (pillX + pillW > x + w - 10.0f) {
                pillX = x + 10.0f;
                pillY += pillH + 3.5f;
            }
            boolean isCur = preset.equalsIgnoreCase(ColorUtil.getThemePreset());
            boolean hov = mouseX >= pillX && mouseX <= pillX + pillW && mouseY >= pillY && mouseY <= pillY + pillH;
            RenderUtil.drawRoundedRect(pillX, pillY, pillW, pillH, 3.0f, isCur ? accent : (hov ? 0xFF262632 : 0xFF1C1C24));
            RenderUtil.drawRoundedOutline(pillX, pillY, pillW, pillH, 3.0f, 1.0f, isCur ? accent : 0xFF2B2B36);
            FontUtil.small.drawString(preset, pillX + 4.0f, pillY + (pillH - FontUtil.small.getHeight()) / 2.0f, isCur ? 0xFFFFFFFF : 0xFFB5B5C2);
            pillX += pillW + 4.0f;
        }

        // 2. GUI Scale Slider
        float scaleSecY = pillY + pillH + 11.0f;
        FontUtil.small.drawString("GUI Scale: \u00a7f" + SCALE_FORMAT.format(userScale) + "x", x + 10.0f, scaleSecY, 0xFF8A8A98);
        float trackX = x + 10.0f;
        float trackY = scaleSecY + 11.5f;
        float trackW = w - 20.0f;
        float trackH = 4.0f;

        RenderUtil.drawRoundedRect(trackX, trackY, trackW, trackH, 2.0f, 0xFF23232D);
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
        float blurY = trackY + 13.0f;
        ClickGUIModule cgm = getClickGUIModule();
        boolean blurOn = cgm == null || cgm.isBlurEnabled();
        FontUtil.small.drawString("Gaussian Blur Backdrop", x + 10.0f, blurY + 2.0f, 0xFFD4D4DE);

        float switchW = 24.0f;
        float switchH = 12.0f;
        float switchX = x + w - switchW - 10.0f;
        RenderUtil.drawRoundedRect(switchX, blurY, switchW, switchH, 6.0f, blurOn ? accent : 0xFF23232D);
        RenderUtil.drawCircle(blurOn ? (switchX + switchW - 6.0f) : (switchX + 6.0f), blurY + switchH / 2.0f, 4.0f, 0xFFFFFFFF);

        // 4. Multi-Version 1.9+ Combat Cooldown Toggle (ViaForge / 1.9 - 26.x servers)
        float protoY = blurY + 18.0f;
        boolean modernCooldown = isModernCooldownActive();
        FontUtil.small.drawString("1.9+ Server Cooldown (ViaForge)", x + 10.0f, protoY + 2.0f, 0xFFD4D4DE);
        RenderUtil.drawRoundedRect(switchX, protoY, switchW, switchH, 6.0f, modernCooldown ? 0xFF10B981 : 0xFF23232D);
        RenderUtil.drawCircle(modernCooldown ? (switchX + switchW - 6.0f) : (switchX + 6.0f), protoY + switchH / 2.0f, 4.0f, 0xFFFFFFFF);

        // 5. Center Window Button
        float rBtnY = protoY + 20.0f;
        float rBtnW = w - 20.0f;
        float rBtnH = 17.0f;
        boolean rHov = mouseX >= x + 10.0f && mouseX <= x + 10.0f + rBtnW && mouseY >= rBtnY && mouseY <= rBtnY + rBtnH;
        RenderUtil.drawRoundedRect(x + 10.0f, rBtnY, rBtnW, rBtnH, 3.0f, rHov ? 0xFF282835 : 0xFF1C1C24);
        RenderUtil.drawRoundedOutline(x + 10.0f, rBtnY, rBtnW, rBtnH, 3.0f, 1.0f, 0xFF2E2E3A);
        FontUtil.small.drawCenteredString("Reset Window Position & Scale", x + 10.0f + rBtnW / 2.0f, rBtnY + (rBtnH - FontUtil.small.getHeight()) / 2.0f, 0xFFB8B8C6);
    }

    private boolean isModernCooldownActive() {
        if (Cigarette.INSTANCE == null || Cigarette.INSTANCE.getModuleManager() == null) return false;
        Module ka = Cigarette.INSTANCE.getModuleManager().getModuleByName("KillAura");
        if (ka != null) {
            for (Setting<?> s : ka.getSettings()) {
                if ("1.9+ Cooldown".equalsIgnoreCase(s.getName()) && s instanceof BooleanSetting) {
                    return ((BooleanSetting) s).isEnabled();
                }
            }
        }
        return false;
    }

    private void toggleModernCooldown() {
        if (Cigarette.INSTANCE == null || Cigarette.INSTANCE.getModuleManager() == null) return;
        boolean next = !isModernCooldownActive();
        for (String modName : new String[]{"KillAura", "AutoClicker"}) {
            Module m = Cigarette.INSTANCE.getModuleManager().getModuleByName(modName);
            if (m != null) {
                for (Setting<?> s : m.getSettings()) {
                    if ("1.9+ Cooldown".equalsIgnoreCase(s.getName()) && s instanceof BooleanSetting) {
                        ((BooleanSetting) s).setEnabled(next);
                    }
                }
            }
        }
    }

    private void renderFriendsView(float x, float y, float w, float h, float mouseX, float mouseY, int accent) {
        FontUtil.bold.drawString("Friends Protection", x + 10.0f, y + 3.0f, 0xFFFFFFFF);

        float inY = y + 16.0f;
        float inW = w - 54.0f;
        float inH = 17.5f;
        RenderUtil.drawRoundedRect(x + 8.0f, inY, inW, inH, 3.0f, 0xFF191920);
        RenderUtil.drawRoundedOutline(x + 8.0f, inY, inW, inH, 3.0f, 1.0f, friendInputFocused ? accent : 0xFF2A2A34);

        String txt = newFriendInput.isEmpty() && !friendInputFocused
                ? "Player username..."
                : newFriendInput + (friendInputFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : "");
        FontUtil.small.drawString(txt, x + 12.0f, inY + (inH - FontUtil.small.getHeight()) / 2.0f,
                newFriendInput.isEmpty() && !friendInputFocused ? 0xFF626270 : 0xFFFFFFFF);

        float btnX = x + 8.0f + inW + 3.0f;
        float btnW = 35.0f;
        boolean crHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= inY && mouseY <= inY + inH;
        RenderUtil.drawRoundedRect(btnX, inY, btnW, inH, 3.0f, crHovered ? accent : 0xFF23242E);
        FontUtil.small.drawCenteredString("Add", btnX + btnW / 2.0f, inY + (inH - FontUtil.small.getHeight()) / 2.0f, 0xFFFFFFFF);

        float listY = inY + inH + 8.0f;
        float listH = h - (listY - y) - 4.0f;
        RenderUtil.drawRoundedRect(x + 8.0f, listY, w - 16.0f, listH, 3.0f, 0xFF16161C);
        RenderUtil.drawRoundedOutline(x + 8.0f, listY, w - 16.0f, listH, 3.0f, 1.0f, 0xFF23232C);

        List<String> friends = new ArrayList<>(FriendManager.getFriends());
        if (friends.isEmpty()) {
            FontUtil.small.drawCenteredString("No friends added yet.", x + 8.0f + (w - 16.0f) / 2.0f, listY + listH / 2.0f - 4.0f, 0xFF626270);
        } else {
            float itemY = listY + 4.0f;
            for (String f : friends) {
                RenderUtil.drawRoundedRect(x + 10.0f, itemY, w - 20.0f, 18.0f, 2.5f, 0xFF191922);
                FontUtil.small.drawString(f, x + 14.0f, itemY + (18.0f - FontUtil.small.getHeight()) / 2.0f, 0xFFD8D8E4);

                float delX = x + w - 32.0f;
                boolean dHov = mouseX >= delX && mouseX <= delX + 18.0f && mouseY >= itemY + 2.0f && mouseY <= itemY + 16.0f;
                RenderUtil.drawRoundedRect(delX, itemY + 2.0f, 18.0f, 14.0f, 2.0f, dHov ? 0xFFC62828 : 0xFF2B1D1F);
                FontUtil.small.drawCenteredString("X", delX + 9.0f, itemY + 2.0f + (14.0f - FontUtil.small.getHeight()) / 2.0f, 0xFFEF9A9A);

                itemY += 21.0f;
            }
        }
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

        float headH = 34.0f;
        float rightX = guiX + SIDEBAR_WIDTH + GAP;

        // Check Search Pill click in Right Content Header FIRST
        float searchW = 96.0f;
        float searchH = 17.5f;
        float searchX = rightX + CONTENT_WIDTH - searchW - 8.0f;
        float searchY = guiY + (headH - searchH) / 2.0f;
        if (sX >= searchX && sX <= searchX + searchW && sY >= searchY && sY <= searchY + searchH) {
            if (!searchFilter.isEmpty() && sX >= searchX + searchW - 14.0f) {
                searchFilter = "";
            }
            searchFocused = true;
            profileInputFocused = false;
            friendInputFocused = false;
            activeView = ViewType.CATEGORY;
            targetScrollOffset = 0.0f;
            return;
        } else {
            searchFocused = false;
        }

        // 1. Sidebar Header click (Drag window or open settings)
        if (sX >= guiX && sX <= guiX + SIDEBAR_WIDTH && sY >= guiY && sY <= guiY + headH) {
            float gearX = guiX + SIDEBAR_WIDTH - 18.5f;
            if (sX >= gearX - 4.0f && sX <= gearX + 15.0f) {
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
        if (sX >= rightX && sX <= rightX + CONTENT_WIDTH && sY >= guiY && sY <= guiY + headH) {
            if (mouseButton == 0) {
                draggingWindow = true;
                dragOffsetX = sX - guiX;
                dragOffsetY = sY - guiY;
            }
            return;
        }

        // 3. Sidebar Category selection
        float itemY = guiY + headH + 5.0f;
        float itemH = 24.0f;
        for (Category cat : Category.values()) {
            if (sX >= guiX + 3.0f && sX <= guiX + SIDEBAR_WIDTH - 3.0f && sY >= itemY && sY <= itemY + itemH) {
                activeView = ViewType.CATEGORY;
                activeCategory = cat;
                searchFilter = "";
                targetScrollOffset = 0.0f;
                scrollOffset = 0.0f;
                return;
            }
            itemY += itemH + 1.5f;
        }

        // 4. Sidebar CLIENT & TOOLS selection
        itemY += 16.5f;
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
            itemY += itemH + 1.5f;
        }

        // 5. Sidebar footer palette click (cycle theme)
        float footY = guiY + TOTAL_HEIGHT - 24.0f;
        float palX = guiX + SIDEBAR_WIDTH - 14.0f;
        float palY = footY + 12.0f;
        if (sX >= palX - 7.0f && sX <= palX + 7.0f && sY >= palY - 7.0f && sY <= palY + 7.0f) {
            String nextPreset = ColorUtil.cycleThemePreset();
            ClickGUIModule mod = getClickGUIModule();
            if (mod != null) mod.setThemePreset(nextPreset);
            return;
        }

        // 6. Right Content interactions
        float footerH = 20.0f;
        float bodyY = guiY + headH + 4.0f;
        float bodyH = TOTAL_HEIGHT - headH - footerH - 6.0f;

        if (activeView == ViewType.CATEGORY) {
            List<ModuleButton> buttons = getVisibleButtons();
            if (sX >= rightX && sX <= rightX + CONTENT_WIDTH && sY >= bodyY && sY <= bodyY + bodyH) {
                for (ModuleButton btn : buttons) {
                    btn.mouseClicked(sX, sY, mouseButton);
                }
            }
        } else if (activeView == ViewType.PROFILES) {
            handleProfilesClick(rightX, bodyY, CONTENT_WIDTH, sX, sY);
        } else if (activeView == ViewType.SETTINGS) {
            handleSettingsClick(rightX, bodyY, CONTENT_WIDTH, sX, sY);
        } else if (activeView == ViewType.FRIENDS) {
            handleFriendsClick(rightX, bodyY, CONTENT_WIDTH, sX, sY);
        }
    }

    private void handleProfilesClick(float x, float y, float w, float sX, float sY) {
        ConfigManager cm = Cigarette.INSTANCE != null ? Cigarette.INSTANCE.getConfigManager() : null;
        float inY = y + 15.0f;
        float inW = w - 54.0f;
        float inH = 17.5f;

        if (sX >= x + 8.0f && sX <= x + 8.0f + inW && sY >= inY && sY <= inY + inH) {
            profileInputFocused = true;
            return;
        } else {
            profileInputFocused = false;
        }

        float btnX = x + 8.0f + inW + 3.0f;
        float btnW = 35.0f;
        if (sX >= btnX && sX <= btnX + btnW && sY >= inY && sY <= inY + inH) {
            if (cm != null && !newProfileInput.trim().isEmpty()) {
                cm.save(newProfileInput.trim());
                newProfileInput = "";
            }
            return;
        }

        float saveY = inY + inH + 5.0f;
        float saveW = w - 16.0f;
        if (sX >= x + 8.0f && sX <= x + 8.0f + saveW && sY >= saveY && sY <= saveY + 16.5f) {
            if (cm != null && cm.getActiveProfile() != null) {
                cm.save(cm.getActiveProfile().getName());
            }
            return;
        }

        float listY = saveY + 21.0f;
        if (cm != null && sY >= listY) {
            List<String> list = cm.listProfiles();
            float itemH = 20.0f;
            float curY = listY + 3.0f - profileScrollOffset;
            for (String p : list) {
                boolean canDel = !"default".equalsIgnoreCase(p);
                float lBtnX = x + w - (canDel ? 57.0f : 38.0f);
                if (sX >= lBtnX && sX <= lBtnX + 26.0f && sY >= curY + 2.5f && sY <= curY + 17.5f) {
                    cm.load(p);
                    return;
                }
                if (canDel) {
                    float dBtnX = x + w - 28.0f;
                    if (sX >= dBtnX && sX <= dBtnX + 16.0f && sY >= curY + 2.5f && sY <= curY + 17.5f) {
                        cm.delete(p);
                        return;
                    }
                }
                curY += itemH + 2.0f;
            }
        }
    }

    private void handleSettingsClick(float x, float y, float w, float sX, float sY) {
        String[] presets = ColorUtil.THEME_PRESETS;
        float pillX = x + 10.0f;
        float pillY = y + 29.0f;
        float pillH = 14.5f;

        for (String preset : presets) {
            float pillW = FontUtil.small.getStringWidth(preset) + 8.0f;
            if (pillX + pillW > x + w - 10.0f) {
                pillX = x + 10.0f;
                pillY += pillH + 3.5f;
            }
            if (sX >= pillX && sX <= pillX + pillW && sY >= pillY && sY <= pillY + pillH) {
                ColorUtil.setThemePreset(preset);
                ClickGUIModule mod = getClickGUIModule();
                if (mod != null) mod.setThemePreset(preset);
                return;
            }
            pillX += pillW + 4.0f;
        }

        float scaleSecY = pillY + pillH + 11.0f;
        float trackX = x + 10.0f;
        float trackY = scaleSecY + 11.5f;
        float trackW = w - 20.0f;
        float trackH = 4.0f;
        if (sX >= trackX - 4.0f && sX <= trackX + trackW + 4.0f && sY >= trackY - 4.0f && sY <= trackY + trackH + 4.0f) {
            draggingScaleSlider = true;
            float mouseFrac = AnimationUtil.clamp((sX - trackX) / trackW, 0.0f, 1.0f);
            this.userScale = 0.70f + (1.25f - 0.70f) * mouseFrac;
            return;
        }

        float blurY = trackY + 13.0f;
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

        float protoY = blurY + 18.0f;
        if (sX >= switchX - 2.0f && sX <= switchX + switchW + 2.0f && sY >= protoY - 2.0f && sY <= protoY + switchH + 2.0f) {
            toggleModernCooldown();
            return;
        }

        float rBtnY = protoY + 20.0f;
        float rBtnW = w - 20.0f;
        float rBtnH = 17.0f;
        if (sX >= x + 10.0f && sX <= x + 10.0f + rBtnW && sY >= rBtnY && sY <= rBtnY + rBtnH) {
            ScaledResolution sr = new ScaledResolution(mc);
            userScale = 1.0f;
            float effectiveScale = computeEffectiveScale(sr.getScaledWidth(), sr.getScaledHeight());
            float vW = sr.getScaledWidth() / effectiveScale;
            float vH = sr.getScaledHeight() / effectiveScale;
            guiX = (vW - TOTAL_WIDTH) / 2.0f;
            guiY = (vH - TOTAL_HEIGHT) / 2.0f;
        }
    }

    private void handleFriendsClick(float x, float y, float w, float sX, float sY) {
        float inY = y + 16.0f;
        float inW = w - 54.0f;
        float inH = 17.5f;

        if (sX >= x + 8.0f && sX <= x + 8.0f + inW && sY >= inY && sY <= inY + inH) {
            friendInputFocused = true;
            return;
        } else {
            friendInputFocused = false;
        }

        float btnX = x + 8.0f + inW + 3.0f;
        float btnW = 35.0f;
        if (sX >= btnX && sX <= btnX + btnW && sY >= inY && sY <= inY + inH) {
            if (!newFriendInput.trim().isEmpty()) {
                FriendManager.addFriend(newFriendInput.trim());
                newFriendInput = "";
            }
            return;
        }

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
            for (ModuleButton btn : getVisibleButtons()) {
                btn.mouseReleased(sX, sY, state);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (activeView == ViewType.CATEGORY) {
            for (ModuleButton btn : getVisibleButtons()) {
                if (btn.isListening()) {
                    btn.keyTyped(typedChar, keyCode);
                    return;
                }
            }
        }

        if (searchFocused) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                if (!searchFilter.isEmpty()) {
                    searchFilter = "";
                } else {
                    searchFocused = false;
                }
            } else if (keyCode == Keyboard.KEY_RETURN) {
                searchFocused = false;
            } else if (keyCode == Keyboard.KEY_BACK && !searchFilter.isEmpty()) {
                searchFilter = searchFilter.substring(0, searchFilter.length() - 1);
                targetScrollOffset = 0.0f;
            } else if (Character.isLetterOrDigit(typedChar) || typedChar == ' ' || typedChar == '_' || typedChar == '-') {
                if (searchFilter.length() < 22) {
                    searchFilter += typedChar;
                    targetScrollOffset = 0.0f;
                }
            }
            return;
        }

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

        ClickGUIModule mod = getClickGUIModule();
        int closeKey = (mod != null && mod.getKeyCode() > 0) ? mod.getKeyCode() : Keyboard.KEY_RSHIFT;
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == closeKey || keyCode == Keyboard.KEY_RSHIFT || keyCode == Keyboard.KEY_INSERT) {
            mc.displayGuiScreen(null);
            return;
        }

        if (activeView == ViewType.CATEGORY) {
            for (ModuleButton btn : getVisibleButtons()) {
                btn.keyTyped(typedChar, keyCode);
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
