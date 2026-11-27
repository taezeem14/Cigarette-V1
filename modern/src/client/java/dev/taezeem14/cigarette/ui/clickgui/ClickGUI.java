package dev.taezeem14.cigarette.ui.clickgui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.config.ConfigManager;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.client.FriendManager;
import dev.taezeem14.cigarette.module.impl.render.ClickGUIModule;
import dev.taezeem14.cigarette.setting.Setting;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
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
import java.util.*;

/**
 * Cigarette V1 signature dual-card ClickGUI for Minecraft 26.3 Fabric.
 * Features Cigarette V1 branding, live cross-category search bar, category module counters,
 * viewport auto-fit scaling, unified window dragging, 3-dot drawers with laptop-friendly keybinds,
 * 1.9+ multi-version combat cooldown quick-toggle, profiles, friends, and hover descriptions.
 */
public class ClickGUI extends Screen {

    private static final DecimalFormat SCALE_FORMAT = new DecimalFormat("0.##");

    public static final float SIDEBAR_WIDTH = 136.0F;
    public static final float CONTENT_WIDTH = 212.0F;
    public static final float GAP = 7.0F;
    public static final float TOTAL_WIDTH = SIDEBAR_WIDTH + GAP + CONTENT_WIDTH; // 355px
    public static final float TOTAL_HEIGHT = 336.0F;

    public enum ViewType {
        CATEGORY,
        PROFILES,
        SETTINGS,
        FRIENDS
    }

    private ViewType activeView = ViewType.CATEGORY;
    private Category activeCategory = Category.COMBAT;

    private static float guiX = -1.0F;
    private static float guiY = -1.0F;
    private boolean draggingWindow = false;
    private float dragOffsetX = 0.0F;
    private float dragOffsetY = 0.0F;

    private float userScale = 1.0F;
    private boolean draggingScaleSlider = false;
    private float fadeProgress = 0.0F;

    private final Map<Category, List<ModuleButton>> categoryButtons = new LinkedHashMap<>();

    private float scrollOffset = 0.0F;
    private float targetScrollOffset = 0.0F;

    private String searchFilter = "";
    private boolean searchFocused = false;

    private String newProfileInput = "";
    private boolean profileInputFocused = false;
    private float profileScrollOffset = 0.0F;
    private float targetProfileScroll = 0.0F;

    private String newFriendInput = "";
    private boolean friendInputFocused = false;

    private Module hoveredModule = null;

    public ClickGUI() {
        super(Component.literal("Cigarette V1 ClickGUI"));
        initCategoryButtons();
    }

    private void initCategoryButtons() {
        categoryButtons.clear();
        for (Category cat : Category.values()) {
            List<Module> mods = CigaretteModernClient.getInstance() != null && CigaretteModernClient.getInstance().getModuleManager() != null
                    ? CigaretteModernClient.getInstance().getModuleManager().getModulesByCategory(cat)
                    : new ArrayList<>();
            List<ModuleButton> buttons = new ArrayList<>();
            for (Module m : mods) {
                buttons.add(new ModuleButton(m, 0, 0, CONTENT_WIDTH - 10.0F));
            }
            categoryButtons.put(cat, buttons);
        }
    }

    private void ensureCategoryButtonsSynced() {
        if (CigaretteModernClient.getInstance() == null || CigaretteModernClient.getInstance().getModuleManager() == null) {
            return;
        }
        boolean mismatch = false;
        for (Category cat : Category.values()) {
            List<Module> current = CigaretteModernClient.getInstance().getModuleManager().getModulesByCategory(cat);
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

    private float computeEffectiveScale(float screenW, float screenH) {
        float maxFitW = screenW * 0.86F;
        float maxFitH = screenH * 0.86F;
        float autoFitScale = Math.min(1.0F, Math.min(maxFitW / TOTAL_WIDTH, maxFitH / TOTAL_HEIGHT));
        return Math.max(0.50F, autoFitScale * userScale);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        ensureCategoryButtonsSynced();
        fadeProgress = Math.min(1.0F, fadeProgress + AnimationUtil.getDeltaTime() * 7.5F);
        hoveredModule = null;

        ClickGUIModule clickGUIModule = getClickGUIModule();
        if (clickGUIModule != null) {
            clickGUIModule.syncThemeFromSetting();
        }

        float screenW = (float) this.width;
        float screenH = (float) this.height;

        int overlayAlpha = (int) (135 * fadeProgress);
        RenderUtil.drawRect(g, 0, 0, screenW, screenH, (overlayAlpha << 24) | 0x0A0A0E);

        float effectiveScale = computeEffectiveScale(screenW, screenH);
        float viewportW = screenW / effectiveScale;
        float viewportH = screenH / effectiveScale;

        if (guiX < 0 || guiY < 0) {
            guiX = Math.max(8.0F, (viewportW - TOTAL_WIDTH) / 2.0F);
            guiY = Math.max(8.0F, (viewportH - TOTAL_HEIGHT) / 2.0F);
        }

        float smx = (float) mouseX / effectiveScale;
        float smy = (float) mouseY / effectiveScale;

        if (draggingWindow) {
            guiX = AnimationUtil.clamp(smx - dragOffsetX, 4.0F, Math.max(4.0F, viewportW - TOTAL_WIDTH - 4.0F));
            guiY = AnimationUtil.clamp(smy - dragOffsetY, 4.0F, Math.max(4.0F, viewportH - TOTAL_HEIGHT - 4.0F));
        } else {
            guiX = AnimationUtil.clamp(guiX, 4.0F, Math.max(4.0F, viewportW - TOTAL_WIDTH - 4.0F));
            guiY = AnimationUtil.clamp(guiY, 4.0F, Math.max(4.0F, viewportH - TOTAL_HEIGHT - 4.0F));
        }

        g.pose().pushMatrix();
        try {
            g.pose().scale(effectiveScale, effectiveScale);
            int accent = ColorUtil.getAccent();

            renderSidebar(g, smx, smy, accent);
            renderContentPanel(g, smx, smy, partialTicks, effectiveScale, accent);
        } finally {
            g.pose().popMatrix();
            RenderUtil.clearScissorStack();
        }
    }

    private void renderSidebar(GuiGraphicsExtractor g, float mouseX, float mouseY, int accent) {
        float x = guiX;
        float y = guiY;
        float w = SIDEBAR_WIDTH;
        float h = TOTAL_HEIGHT;

        RenderUtil.drawDropShadow(g, x, y, w, h, 6.0F, 0x70000000);
        RenderUtil.drawRoundedRect(g, x, y, w, h, 5.5F, 0xFF141418);
        RenderUtil.drawRoundedOutline(g, x, y, w, h, 5.5F, 1.0F, 0xFF25252E);

        float headH = 34.0F;
        RenderUtil.drawRoundedRectTop(g, x, y, w, headH, 5.5F, 0xFF191920);
        RenderUtil.drawRoundedRectTop(g, x + 1.0F, y, w - 2.0F, 1.5F, 1.5F, ColorUtil.modifyAlpha(accent, 190));
        RenderUtil.drawRect(g, x + 1.0F, y + headH - 1.0F, w - 2.0F, 1.0F, 0xFF23232B);

        RenderUtil.drawFaIcon(g, "logo", x + 8.0F, y + (headH - 11.0F) / 2.0F, 11.0F, accent);
        float brandX = x + 22.0F;
        RenderUtil.drawText(g, "CIGARETTE", brandX, y + (headH - RenderUtil.getFontHeight()) / 2.0F, 0xFFFFFFFF, true);

        float v1X = brandX + RenderUtil.getStringWidth("CIGARETTE") + 4.0F;
        float v1Y = y + (headH - 11.5F) / 2.0F;
        RenderUtil.drawRoundedRect(g, v1X, v1Y, 16.0F, 11.5F, 2.5F, ColorUtil.modifyAlpha(accent, 38));
        RenderUtil.drawRoundedOutline(g, v1X, v1Y, 16.0F, 11.5F, 2.5F, 1.0F, accent);
        RenderUtil.drawCenteredSmallText(g, "v1", v1X + 8.0F, v1Y + (11.5F - RenderUtil.getSmallFontHeight()) / 2.0F, accent, false);

        float gearX = x + w - 18.5F;
        float gearY = y + (headH - 10.0F) / 2.0F;
        boolean gearHovered = mouseX >= gearX - 3.0F && mouseX <= gearX + 13.0F && mouseY >= gearY - 3.0F && mouseY <= gearY + 13.0F;
        int gearColor = (activeView == ViewType.SETTINGS) ? accent : (gearHovered ? 0xFFFFFFFF : 0xFF787887);
        RenderUtil.drawFaIcon(g, "gear", gearX, gearY, 10.0F, gearColor);

        float itemY = y + headH + 5.0F;
        float itemH = 24.0F;

        for (Category cat : Category.values()) {
            boolean active = (activeView == ViewType.CATEGORY && activeCategory == cat && searchFilter.trim().isEmpty());
            boolean hovered = mouseX >= x + 3.0F && mouseX <= x + w - 3.0F && mouseY >= itemY && mouseY <= itemY + itemH;

            int itemBg = active ? 0xFF1E212B : (hovered ? 0xFF1A1C25 : 0);
            if (itemBg != 0) RenderUtil.drawRoundedRect(g, x + 3.5F, itemY, w - 7.0F, itemH, 3.5F, itemBg);
            if (active) RenderUtil.drawRoundedRect(g, x + 4.5F, itemY + 3.5F, 2.5F, itemH - 7.0F, 1.25F, accent);

            int iconCol = active ? accent : (hovered ? 0xFFD4D4DE : 0xFF757584);
            RenderUtil.drawFaIcon(g, cat.name(), x + 10.5F, itemY + (itemH - 10.0F) / 2.0F, 10.0F, iconCol);

            int textCol = active ? 0xFFFFFFFF : (hovered ? 0xFFEAEAF2 : 0xFFA0A0AC);
            RenderUtil.drawText(g, cat.getDisplayName(), x + 25.5F, itemY + (itemH - RenderUtil.getFontHeight()) / 2.0F, textCol, active);

            List<ModuleButton> catBtns = categoryButtons.get(cat);
            int total = catBtns != null ? catBtns.size() : 0;
            int enabledCount = 0;
            if (catBtns != null) {
                for (ModuleButton mb : catBtns) {
                    if (mb.getModule().isEnabled()) enabledCount++;
                }
            }
            String countStr = enabledCount > 0 ? (enabledCount + "/" + total) : String.valueOf(total);
            float cW = RenderUtil.getSmallStringWidth(countStr) + 6.0F;
            float cX = x + w - cW - 15.0F;
            float cY = itemY + (itemH - 10.5F) / 2.0F;
            RenderUtil.drawRoundedRect(g, cX, cY, cW, 10.5F, 2.0F, enabledCount > 0 ? ColorUtil.modifyAlpha(accent, 36) : 0xFF1D1D24);
            RenderUtil.drawCenteredSmallText(g, countStr, cX + cW / 2.0F, cY + (10.5F - RenderUtil.getSmallFontHeight()) / 2.0F,
                    enabledCount > 0 ? accent : 0xFF6B6B78, false);

            int chevCol = active ? accent : (hovered ? 0xFF8E8E9A : 0xFF4A4A55);
            RenderUtil.drawFaIcon(g, "chevron_right", x + w - 12.5F, itemY + (itemH - 7.5F) / 2.0F, 7.5F, chevCol);

            itemY += itemH + 1.5F;
        }

        itemY += 4.0F;
        RenderUtil.drawSmallText(g, "CLIENT & TOOLS", x + 10.0F, itemY, 0xFF5A5A68, false);
        itemY += 12.5F;

        String[] miscItems = {"Friends", "Profiles", "HUD Editor"};
        for (String misc : miscItems) {
            boolean active = ("Profiles".equals(misc) && activeView == ViewType.PROFILES)
                    || ("Friends".equals(misc) && activeView == ViewType.FRIENDS);
            boolean hovered = mouseX >= x + 3.0F && mouseX <= x + w - 3.0F && mouseY >= itemY && mouseY <= itemY + itemH;

            int itemBg = active ? 0xFF1E212B : (hovered ? 0xFF1A1C25 : 0);
            if (itemBg != 0) RenderUtil.drawRoundedRect(g, x + 3.5F, itemY, w - 7.0F, itemH, 3.5F, itemBg);
            if (active) RenderUtil.drawRoundedRect(g, x + 4.5F, itemY + 3.5F, 2.5F, itemH - 7.0F, 1.25F, accent);

            int iconCol = active ? accent : (hovered ? 0xFFD4D4DE : 0xFF757584);
            String iconKey = "Friends".equals(misc) ? "friends" : ("Profiles".equals(misc) ? "profiles" : "hud");
            RenderUtil.drawFaIcon(g, iconKey, x + 10.5F, itemY + (itemH - 10.0F) / 2.0F, 10.0F, iconCol);

            int textCol = active ? 0xFFFFFFFF : (hovered ? 0xFFEAEAF2 : 0xFFA0A0AC);
            RenderUtil.drawText(g, misc, x + 25.5F, itemY + (itemH - RenderUtil.getFontHeight()) / 2.0F, textCol, active);

            if ("Profiles".equals(misc)) {
                ConfigManager cm = CigaretteModernClient.getInstance() != null ? CigaretteModernClient.getInstance().getConfigManager() : null;
                String activeProf = (cm != null && cm.getActiveProfile() != null) ? cm.getActiveProfile().getName() : "default";
                if (activeProf.length() > 8) activeProf = activeProf.substring(0, 8);
                float badgeW = RenderUtil.getSmallStringWidth(activeProf) + 6.0F;
                float badgeX = x + w - badgeW - 15.0F;
                float badgeY = itemY + (itemH - 10.5F) / 2.0F;
                RenderUtil.drawRoundedRect(g, badgeX, badgeY, badgeW, 10.5F, 2.0F, 0xFF1F2028);
                RenderUtil.drawSmallText(g, activeProf, badgeX + 3.0F, badgeY + (10.5F - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFF828292, false);
            }

            int chevCol = active ? accent : (hovered ? 0xFF8E8E9A : 0xFF4A4A55);
            RenderUtil.drawFaIcon(g, "chevron_right", x + w - 12.5F, itemY + (itemH - 7.5F) / 2.0F, 7.5F, chevCol);

            itemY += itemH + 1.5F;
        }

        float footY = y + h - 24.0F;
        RenderUtil.drawRect(g, x + 1.0F, footY, w - 2.0F, 1.0F, 0xFF202028);

        RenderUtil.drawFaIcon(g, "user", x + 9.5F, footY + 6.5F, 10.0F, accent);
        String playerName = (this.minecraft != null && this.minecraft.player != null)
                ? this.minecraft.player.getName().getString() : "Player";
        if (playerName.length() > 11) playerName = playerName.substring(0, 11);
        RenderUtil.drawSmallText(g, playerName, x + 23.0F, footY + (24.0F - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFFB8B8C6, false);

        String scaleTag = SCALE_FORMAT.format(userScale) + "x";
        RenderUtil.drawSmallText(g, scaleTag, x + w - 42.0F, footY + (24.0F - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFF6E6E7C, false);

        float palX = x + w - 14.0F;
        float palY = footY + 12.0F;
        boolean palHovered = mouseX >= palX - 6.0F && mouseX <= palX + 6.0F && mouseY >= palY - 6.0F && mouseY <= palY + 6.0F;
        RenderUtil.drawCircle(g, palX, palY, palHovered ? 4.5F : 3.8F, accent);
        RenderUtil.drawCircleOutline(g, palX, palY, palHovered ? 5.8F : 5.0F, 1.0F, 0xFF3A3A48);
    }

    private void renderContentPanel(GuiGraphicsExtractor g, float mouseX, float mouseY, float partialTicks, float effectiveScale, int accent) {
        float x = guiX + SIDEBAR_WIDTH + GAP;
        float y = guiY;
        float w = CONTENT_WIDTH;
        float h = TOTAL_HEIGHT;

        RenderUtil.drawDropShadow(g, x, y, w, h, 6.0F, 0x70000000);
        RenderUtil.drawRoundedRect(g, x, y, w, h, 5.5F, 0xFF141418);
        RenderUtil.drawRoundedOutline(g, x, y, w, h, 5.5F, 1.0F, 0xFF25252E);

        float headH = 34.0F;
        RenderUtil.drawRoundedRectTop(g, x, y, w, headH, 5.5F, 0xFF191920);
        RenderUtil.drawRoundedRectTop(g, x + 1.0F, y, w - 2.0F, 1.5F, 1.5F, ColorUtil.modifyAlpha(accent, 190));
        RenderUtil.drawRect(g, x + 1.0F, y + headH - 1.0F, w - 2.0F, 1.0F, 0xFF23232B);

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

        RenderUtil.drawFaIcon(g, iconKey, x + 10.0F, y + (headH - 10.0F) / 2.0F, 10.0F, accent);
        RenderUtil.drawText(g, title, x + 24.0F, y + (headH - RenderUtil.getFontHeight()) / 2.0F, 0xFFFFFFFF, true);

        float searchW = 96.0F;
        float searchH = 17.5F;
        float searchX = x + w - searchW - 8.0F;
        float searchY = y + (headH - searchH) / 2.0F;
        boolean searchHov = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH;

        RenderUtil.drawRoundedRect(g, searchX, searchY, searchW, searchH, 3.5F, 0xFF121216);
        RenderUtil.drawRoundedOutline(g, searchX, searchY, searchW, searchH, 3.5F, 1.0F,
                searchFocused ? accent : (searchHov ? 0xFF383846 : 0xFF272732));
        RenderUtil.drawFaIcon(g, "search", searchX + 5.0F, searchY + (searchH - 8.5F) / 2.0F, 8.5F,
                searchFocused ? accent : 0xFF6E6E7C);

        String sDisp = searchFilter.isEmpty() && !searchFocused
                ? "Search all..."
                : searchFilter + (searchFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : "");
        RenderUtil.drawSmallText(g, sDisp, searchX + 17.0F, searchY + (searchH - RenderUtil.getSmallFontHeight()) / 2.0F,
                searchFilter.isEmpty() && !searchFocused ? 0xFF626270 : 0xFFEFEFF6, false);

        if (!searchFilter.isEmpty()) {
            RenderUtil.drawSmallText(g, "x", searchX + searchW - 9.5F, searchY + (searchH - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFF9A9AA8, false);
        }

        float footerH = 20.0F;
        float bodyY = y + headH + 4.0F;
        float bodyH = h - headH - footerH - 6.0F;

        if (activeView == ViewType.CATEGORY) {
            renderCategoryModules(g, x, bodyY, w, bodyH, mouseX, mouseY, partialTicks, effectiveScale, accent);
        } else if (activeView == ViewType.PROFILES) {
            renderProfilesView(g, x, bodyY, w, bodyH, mouseX, mouseY, accent);
        } else if (activeView == ViewType.SETTINGS) {
            renderSettingsView(g, x, bodyY, w, bodyH, mouseX, mouseY, accent);
        } else if (activeView == ViewType.FRIENDS) {
            renderFriendsView(g, x, bodyY, w, bodyH, mouseX, mouseY, accent);
        }

        float statusY = y + h - footerH;
        RenderUtil.drawRect(g, x + 1.0F, statusY, w - 2.0F, 1.0F, 0xFF202028);
        String tip = hoveredModule != null
                ? "\u00a7f" + hoveredModule.getName() + ": \u00a77" + hoveredModule.getDescription()
                : "Click: Toggle \u2022 \u22ee / R-Click: Settings & Keybind";
        tip = trimSmallText(tip, w - 14.0F);
        RenderUtil.drawSmallText(g, tip, x + 7.0F, statusY + (footerH - RenderUtil.getSmallFontHeight()) / 2.0F + 0.5F, 0xFF828292, false);
    }

    private String trimSmallText(String text, float maxW) {
        if (text == null || RenderUtil.getSmallStringWidth(text) <= maxW) return text;
        String cur = text;
        while (cur.length() > 4 && RenderUtil.getSmallStringWidth(cur + "..") > maxW) {
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

    private void renderCategoryModules(GuiGraphicsExtractor g, float panelX, float bodyY, float panelW, float bodyH,
                                       float mouseX, float mouseY, float partialTicks, float effectiveScale, int accent) {
        List<ModuleButton> buttons = getVisibleButtons();

        float totalH = 4.0F;
        for (ModuleButton btn : buttons) totalH += btn.getTotalHeight() + 2.5F;

        float maxScroll = Math.max(0.0F, totalH - bodyH);
        targetScrollOffset = AnimationUtil.clamp(targetScrollOffset, 0.0F, maxScroll);
        scrollOffset = AnimationUtil.lerp(scrollOffset, targetScrollOffset, 16.0F);

        RenderUtil.pushScissor(g, panelX, bodyY, panelW, bodyH, effectiveScale);
        try {
            if (buttons.isEmpty()) {
                RenderUtil.drawCenteredSmallText(g, "No matching modules found.", panelX + panelW / 2.0F, bodyY + bodyH / 2.0F - 4.0F, 0xFF666675, false);
                return;
            }

            float btnY = bodyY + 2.0F - scrollOffset;
            float btnW = panelW - 11.0F;

            for (ModuleButton btn : buttons) {
                btn.setX(panelX + 5.0F);
                btn.setY(btnY);
                btn.setWidth(btnW);
                btn.render(g, mouseX, mouseY, partialTicks, effectiveScale);

                if (mouseY >= bodyY && mouseY <= bodyY + bodyH && btn.isHovered(mouseX, mouseY)) {
                    hoveredModule = btn.getModule();
                }
                btnY += btn.getTotalHeight() + 2.5F;
            }

            if (maxScroll > 0.5F) {
                float trackX = panelX + panelW - 4.0F;
                float trackH = bodyH - 4.0F;
                RenderUtil.drawRoundedRect(g, trackX, bodyY + 2.0F, 2.0F, trackH, 1.0F, 0xFF1E1E26);
                float thumbH = Math.max(20.0F, trackH * (bodyH / totalH));
                float thumbY = bodyY + 2.0F + (trackH - thumbH) * (scrollOffset / maxScroll);
                RenderUtil.drawRoundedRect(g, trackX, thumbY, 2.0F, thumbH, 1.0F, ColorUtil.modifyAlpha(accent, 200));
            }
        } finally {
            RenderUtil.popScissor(g);
        }
    }

    private void renderProfilesView(GuiGraphicsExtractor g, float x, float y, float w, float h, float mouseX, float mouseY, int accent) {
        ConfigManager cm = CigaretteModernClient.getInstance() != null ? CigaretteModernClient.getInstance().getConfigManager() : null;
        String activeName = (cm != null && cm.getActiveProfile() != null) ? cm.getActiveProfile().getName() : "default";

        RenderUtil.drawSmallText(g, "Active Profile: \u00a7a" + activeName, x + 8.0F, y + 2.0F, 0xFFCCCCCC, false);

        float inY = y + 15.0F;
        float inW = w - 54.0F;
        float inH = 17.5F;
        RenderUtil.drawRoundedRect(g, x + 8.0F, inY, inW, inH, 3.0F, 0xFF191920);
        RenderUtil.drawRoundedOutline(g, x + 8.0F, inY, inW, inH, 3.0F, 1.0F, profileInputFocused ? accent : 0xFF2B2B35);

        String txt = newProfileInput.isEmpty() && !profileInputFocused
                ? "New profile name..."
                : newProfileInput + (profileInputFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : "");
        RenderUtil.drawSmallText(g, txt, x + 12.0F, inY + (inH - RenderUtil.getSmallFontHeight()) / 2.0F,
                newProfileInput.isEmpty() && !profileInputFocused ? 0xFF626270 : 0xFFFFFFFF, false);

        float btnX = x + 8.0F + inW + 3.0F;
        float btnW = 35.0F;
        boolean crHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= inY && mouseY <= inY + inH;
        RenderUtil.drawRoundedRect(g, btnX, inY, btnW, inH, 3.0F, crHovered ? accent : 0xFF23242E);
        RenderUtil.drawCenteredSmallText(g, "Create", btnX + btnW / 2.0F, inY + (inH - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFFFFFFFF, false);

        float saveY = inY + inH + 5.0F;
        float saveW = w - 16.0F;
        boolean saveHovered = mouseX >= x + 8.0F && mouseX <= x + 8.0F + saveW && mouseY >= saveY && mouseY <= saveY + 16.5F;
        RenderUtil.drawRoundedRect(g, x + 8.0F, saveY, saveW, 16.5F, 3.0F, saveHovered ? 0xFF10B981 : 0xFF172520);
        RenderUtil.drawRoundedOutline(g, x + 8.0F, saveY, saveW, 16.5F, 3.0F, 1.0F, 0xFF10B981);
        RenderUtil.drawCenteredSmallText(g, "Save Current Profile", x + 8.0F + saveW / 2.0F, saveY + (16.5F - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFFFFFFFF, false);

        float listY = saveY + 21.0F;
        float listH = h - (listY - y) - 4.0F;
        RenderUtil.drawRoundedRect(g, x + 8.0F, listY, w - 16.0F, listH, 3.0F, 0xFF16161C);
        RenderUtil.drawRoundedOutline(g, x + 8.0F, listY, w - 16.0F, listH, 3.0F, 1.0F, 0xFF23232C);

        if (cm != null) {
            List<String> list = cm.listProfiles();
            float itemH = 20.0F;
            float totalListH = list.size() * (itemH + 2.0F);
            float maxScr = Math.max(0.0F, totalListH - listH);
            targetProfileScroll = AnimationUtil.clamp(targetProfileScroll, 0.0F, maxScr);
            profileScrollOffset = AnimationUtil.lerp(profileScrollOffset, targetProfileScroll, 16.0F);

            RenderUtil.pushScissor(g, x + 8.0F, listY, w - 16.0F, listH);
            try {
                float curY = listY + 3.0F - profileScrollOffset;
                for (String p : list) {
                    boolean isCur = p.equalsIgnoreCase(activeName);
                    boolean hov = mouseX >= x + 10.0F && mouseX <= x + w - 10.0F && mouseY >= curY && mouseY <= curY + itemH;
                    RenderUtil.drawRoundedRect(g, x + 10.0F, curY, w - 20.0F, itemH, 2.5F, hov ? 0xFF22222C : (isCur ? 0xFF1A1D28 : 0xFF191920));

                    int nameCol = isCur ? accent : 0xFFD4D4DE;
                    RenderUtil.drawSmallText(g, p, x + 14.0F, curY + (itemH - RenderUtil.getSmallFontHeight()) / 2.0F, nameCol, false);

                    boolean canDel = !"default".equalsIgnoreCase(p);
                    float lBtnX = x + w - (canDel ? 57.0F : 38.0F);
                    boolean lHov = mouseX >= lBtnX && mouseX <= lBtnX + 26.0F && mouseY >= curY + 2.5F && mouseY <= curY + 17.5F;
                    RenderUtil.drawRoundedRect(g, lBtnX, curY + 2.5F, 26.0F, 15.0F, 2.0F, lHov ? accent : 0xFF252632);
                    RenderUtil.drawCenteredSmallText(g, "Load", lBtnX + 13.0F, curY + 2.5F + (15.0F - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFFFFFFFF, false);

                    if (canDel) {
                        float dBtnX = x + w - 28.0F;
                        boolean dHov = mouseX >= dBtnX && mouseX <= dBtnX + 16.0F && mouseY >= curY + 2.5F && mouseY <= curY + 17.5F;
                        RenderUtil.drawRoundedRect(g, dBtnX, curY + 2.5F, 16.0F, 15.0F, 2.0F, dHov ? 0xFFC62828 : 0xFF2C1D1F);
                        RenderUtil.drawCenteredSmallText(g, "X", dBtnX + 8.0F, curY + 2.5F + (15.0F - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFFEF9A9A, false);
                    }

                    curY += itemH + 2.0F;
                }
            } finally {
                RenderUtil.popScissor(g);
            }
        }
    }

    private void renderSettingsView(GuiGraphicsExtractor g, float x, float y, float w, float h, float mouseX, float mouseY, int accent) {
        RenderUtil.drawText(g, "Cigarette V1 Preferences", x + 10.0F, y + 3.0F, 0xFFFFFFFF, true);

        RenderUtil.drawSmallText(g, "Theme Accent Preset", x + 10.0F, y + 18.0F, 0xFF8A8A98, false);
        String[] presets = ColorUtil.THEME_PRESETS;
        float pillX = x + 10.0F;
        float pillY = y + 29.0F;
        float pillH = 14.5F;

        for (String preset : presets) {
            float pillW = RenderUtil.getSmallStringWidth(preset) + 8.0F;
            if (pillX + pillW > x + w - 10.0F) {
                pillX = x + 10.0F;
                pillY += pillH + 3.5F;
            }
            boolean isCur = preset.equalsIgnoreCase(ColorUtil.getThemePreset());
            boolean hov = mouseX >= pillX && mouseX <= pillX + pillW && mouseY >= pillY && mouseY <= pillY + pillH;
            RenderUtil.drawRoundedRect(g, pillX, pillY, pillW, pillH, 3.0F, isCur ? accent : (hov ? 0xFF262632 : 0xFF1C1C24));
            RenderUtil.drawRoundedOutline(g, pillX, pillY, pillW, pillH, 3.0F, 1.0F, isCur ? accent : 0xFF2B2B36);
            RenderUtil.drawSmallText(g, preset, pillX + 4.0F, pillY + (pillH - RenderUtil.getSmallFontHeight()) / 2.0F, isCur ? 0xFFFFFFFF : 0xFFB5B5C2, false);
            pillX += pillW + 4.0F;
        }

        float scaleSecY = pillY + pillH + 11.0F;
        RenderUtil.drawSmallText(g, "GUI Scale: \u00a7f" + SCALE_FORMAT.format(userScale) + "x", x + 10.0F, scaleSecY, 0xFF8A8A98, false);
        float trackX = x + 10.0F;
        float trackY = scaleSecY + 11.5F;
        float trackW = w - 20.0F;
        float trackH = 4.0F;

        RenderUtil.drawRoundedRect(g, trackX, trackY, trackW, trackH, 2.0F, 0xFF23232D);
        float frac = AnimationUtil.clamp((userScale - 0.70F) / (1.25F - 0.70F), 0.0F, 1.0F);
        float fillW = trackW * frac;
        if (fillW > 1.0F) RenderUtil.drawRoundedRect(g, trackX, trackY, fillW, trackH, 2.0F, accent);
        RenderUtil.drawCircle(g, trackX + fillW, trackY + trackH / 2.0F, 3.5F, 0xFFFFFFFF);

        if (draggingScaleSlider) {
            float mouseFrac = AnimationUtil.clamp((mouseX - trackX) / trackW, 0.0F, 1.0F);
            this.userScale = 0.70F + (1.25F - 0.70F) * mouseFrac;
        }

        float blurY = trackY + 13.0F;
        ClickGUIModule cgm = getClickGUIModule();
        boolean blurOn = cgm == null || cgm.isBlurEnabled();
        RenderUtil.drawSmallText(g, "Gaussian Blur Backdrop", x + 10.0F, blurY + 2.0F, 0xFFD4D4DE, false);

        float switchW = 24.0F;
        float switchH = 12.0F;
        float switchX = x + w - switchW - 10.0F;
        RenderUtil.drawRoundedRect(g, switchX, blurY, switchW, switchH, 6.0F, blurOn ? accent : 0xFF23232D);
        RenderUtil.drawCircle(g, blurOn ? (switchX + switchW - 6.0F) : (switchX + 6.0F), blurY + switchH / 2.0F, 4.0F, 0xFFFFFFFF);

        float protoY = blurY + 18.0F;
        boolean modernCooldown = isModernCooldownActive();
        RenderUtil.drawSmallText(g, "1.9+ Server Cooldown", x + 10.0F, protoY + 2.0F, 0xFFD4D4DE, false);
        RenderUtil.drawRoundedRect(g, switchX, protoY, switchW, switchH, 6.0F, modernCooldown ? 0xFF10B981 : 0xFF23232D);
        RenderUtil.drawCircle(g, modernCooldown ? (switchX + switchW - 6.0F) : (switchX + 6.0F), protoY + switchH / 2.0F, 4.0F, 0xFFFFFFFF);

        float rBtnY = protoY + 20.0F;
        float rBtnW = w - 20.0F;
        float rBtnH = 17.0F;
        boolean rHov = mouseX >= x + 10.0F && mouseX <= x + 10.0F + rBtnW && mouseY >= rBtnY && mouseY <= rBtnY + rBtnH;
        RenderUtil.drawRoundedRect(g, x + 10.0F, rBtnY, rBtnW, rBtnH, 3.0F, rHov ? 0xFF282835 : 0xFF1C1C24);
        RenderUtil.drawRoundedOutline(g, x + 10.0F, rBtnY, rBtnW, rBtnH, 3.0F, 1.0F, 0xFF2E2E3A);
        RenderUtil.drawCenteredSmallText(g, "Reset Window Position & Scale", x + 10.0F + rBtnW / 2.0F, rBtnY + (rBtnH - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFFB8B8C6, false);
    }

    private boolean isModernCooldownActive() {
        if (CigaretteModernClient.getInstance() == null || CigaretteModernClient.getInstance().getModuleManager() == null) return false;
        Module ka = CigaretteModernClient.getInstance().getModuleManager().getModuleByName("KillAura");
        if (ka != null) {
            for (Setting<?> s : ka.getSettings()) {
                if ("1.9+ Cooldown".equalsIgnoreCase(s.getName()) && s instanceof BooleanSetting bs) {
                    return bs.isEnabled();
                }
            }
        }
        return false;
    }

    private void toggleModernCooldown() {
        if (CigaretteModernClient.getInstance() == null || CigaretteModernClient.getInstance().getModuleManager() == null) return;
        boolean next = !isModernCooldownActive();
        for (String modName : new String[]{"KillAura", "AutoClicker"}) {
            Module m = CigaretteModernClient.getInstance().getModuleManager().getModuleByName(modName);
            if (m != null) {
                for (Setting<?> s : m.getSettings()) {
                    if ("1.9+ Cooldown".equalsIgnoreCase(s.getName()) && s instanceof BooleanSetting bs) {
                        bs.setValue(next);
                    }
                }
            }
        }
    }

    private void renderFriendsView(GuiGraphicsExtractor g, float x, float y, float w, float h, float mouseX, float mouseY, int accent) {
        RenderUtil.drawText(g, "Friends Protection", x + 10.0F, y + 3.0F, 0xFFFFFFFF, true);

        float inY = y + 16.0F;
        float inW = w - 54.0F;
        float inH = 17.5F;
        RenderUtil.drawRoundedRect(g, x + 8.0F, inY, inW, inH, 3.0F, 0xFF191920);
        RenderUtil.drawRoundedOutline(g, x + 8.0F, inY, inW, inH, 3.0F, 1.0F, friendInputFocused ? accent : 0xFF2A2A34);

        String txt = newFriendInput.isEmpty() && !friendInputFocused
                ? "Player username..."
                : newFriendInput + (friendInputFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : "");
        RenderUtil.drawSmallText(g, txt, x + 12.0F, inY + (inH - RenderUtil.getSmallFontHeight()) / 2.0F,
                newFriendInput.isEmpty() && !friendInputFocused ? 0xFF626270 : 0xFFFFFFFF, false);

        float btnX = x + 8.0F + inW + 3.0F;
        float btnW = 35.0F;
        boolean crHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= inY && mouseY <= inY + inH;
        RenderUtil.drawRoundedRect(g, btnX, inY, btnW, inH, 3.0F, crHovered ? accent : 0xFF23242E);
        RenderUtil.drawCenteredSmallText(g, "Add", btnX + btnW / 2.0F, inY + (inH - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFFFFFFFF, false);

        float listY = inY + inH + 8.0F;
        float listH = h - (listY - y) - 4.0F;
        RenderUtil.drawRoundedRect(g, x + 8.0F, listY, w - 16.0F, listH, 3.0F, 0xFF16161C);
        RenderUtil.drawRoundedOutline(g, x + 8.0F, listY, w - 16.0F, listH, 3.0F, 1.0F, 0xFF23232C);

        List<String> friends = new ArrayList<>(FriendManager.getFriends());
        if (friends.isEmpty()) {
            RenderUtil.drawCenteredSmallText(g, "No friends added yet.", x + 8.0F + (w - 16.0F) / 2.0F, listY + listH / 2.0F - 4.0F, 0xFF626270, false);
        } else {
            float itemY = listY + 4.0F;
            for (String f : friends) {
                RenderUtil.drawRoundedRect(g, x + 10.0F, itemY, w - 20.0F, 18.0F, 2.5F, 0xFF191922);
                RenderUtil.drawSmallText(g, f, x + 14.0F, itemY + (18.0F - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFFD8D8E4, false);

                float delX = x + w - 32.0F;
                boolean dHov = mouseX >= delX && mouseX <= delX + 18.0F && mouseY >= itemY + 2.0F && mouseY <= itemY + 16.0F;
                RenderUtil.drawRoundedRect(g, delX, itemY + 2.0F, 18.0F, 14.0F, 2.0F, dHov ? 0xFFC62828 : 0xFF2B1D1F);
                RenderUtil.drawCenteredSmallText(g, "X", delX + 9.0F, itemY + 2.0F + (14.0F - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFFEF9A9A, false);

                itemY += 21.0F;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float scrollDelta = (float) (verticalAmount > 0 ? -24.0F : 24.0F);
        if (activeView == ViewType.CATEGORY) {
            targetScrollOffset += scrollDelta;
            return true;
        } else if (activeView == ViewType.PROFILES) {
            targetProfileScroll += scrollDelta;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        float screenW = (float) this.width;
        float screenH = (float) this.height;
        float effectiveScale = computeEffectiveScale(screenW, screenH);
        float sX = (float) (event.x() / effectiveScale);
        float sY = (float) (event.y() / effectiveScale);
        int mouseButton = event.button();

        float headH = 34.0F;
        float rightX = guiX + SIDEBAR_WIDTH + GAP;

        float searchW = 96.0F;
        float searchH = 17.5F;
        float searchX = rightX + CONTENT_WIDTH - searchW - 8.0F;
        float searchY = guiY + (headH - searchH) / 2.0F;
        if (sX >= searchX && sX <= searchX + searchW && sY >= searchY && sY <= searchY + searchH) {
            if (!searchFilter.isEmpty() && sX >= searchX + searchW - 14.0F) {
                searchFilter = "";
            }
            searchFocused = true;
            profileInputFocused = false;
            friendInputFocused = false;
            activeView = ViewType.CATEGORY;
            targetScrollOffset = 0.0F;
            return true;
        } else {
            searchFocused = false;
        }

        if (sX >= guiX && sX <= guiX + SIDEBAR_WIDTH && sY >= guiY && sY <= guiY + headH) {
            float gearX = guiX + SIDEBAR_WIDTH - 18.5F;
            if (sX >= gearX - 4.0F && sX <= gearX + 15.0F) {
                activeView = (activeView == ViewType.SETTINGS) ? ViewType.CATEGORY : ViewType.SETTINGS;
                return true;
            }
            if (mouseButton == 0) {
                draggingWindow = true;
                dragOffsetX = sX - guiX;
                dragOffsetY = sY - guiY;
            }
            return true;
        }

        if (sX >= rightX && sX <= rightX + CONTENT_WIDTH && sY >= guiY && sY <= guiY + headH) {
            if (mouseButton == 0) {
                draggingWindow = true;
                dragOffsetX = sX - guiX;
                dragOffsetY = sY - guiY;
            }
            return true;
        }

        float itemY = guiY + headH + 5.0F;
        float itemH = 24.0F;
        for (Category cat : Category.values()) {
            if (sX >= guiX + 3.0F && sX <= guiX + SIDEBAR_WIDTH - 3.0F && sY >= itemY && sY <= itemY + itemH) {
                activeView = ViewType.CATEGORY;
                activeCategory = cat;
                searchFilter = "";
                targetScrollOffset = 0.0F;
                scrollOffset = 0.0F;
                return true;
            }
            itemY += itemH + 1.5F;
        }

        itemY += 16.5F;
        String[] miscItems = {"Friends", "Profiles", "HUD Editor"};
        for (String misc : miscItems) {
            if (sX >= guiX + 3.0F && sX <= guiX + SIDEBAR_WIDTH - 3.0F && sY >= itemY && sY <= itemY + itemH) {
                if ("Friends".equals(misc)) {
                    activeView = ViewType.FRIENDS;
                } else if ("Profiles".equals(misc)) {
                    activeView = ViewType.PROFILES;
                    targetProfileScroll = 0.0F;
                } else if ("HUD Editor".equals(misc)) {
                    this.minecraft.gui.setScreen(new HUDEditor());
                }
                return true;
            }
            itemY += itemH + 1.5F;
        }

        float footY = guiY + TOTAL_HEIGHT - 24.0F;
        float palX = guiX + SIDEBAR_WIDTH - 14.0F;
        float palY = footY + 12.0F;
        if (sX >= palX - 7.0F && sX <= palX + 7.0F && sY >= palY - 7.0F && sY <= palY + 7.0F) {
            String next = ColorUtil.cycleThemePreset();
            ClickGUIModule mod = getClickGUIModule();
            if (mod != null) mod.setThemePreset(next);
            return true;
        }

        float footerH = 20.0F;
        float bodyY = guiY + headH + 4.0F;
        float bodyH = TOTAL_HEIGHT - headH - footerH - 6.0F;

        if (activeView == ViewType.CATEGORY) {
            List<ModuleButton> buttons = getVisibleButtons();
            if (sX >= rightX && sX <= rightX + CONTENT_WIDTH && sY >= bodyY && sY <= bodyY + bodyH) {
                for (ModuleButton btn : buttons) {
                    btn.mouseClicked(sX, sY, mouseButton);
                }
                return true;
            }
        } else if (activeView == ViewType.PROFILES) {
            handleProfilesClick(rightX, bodyY, CONTENT_WIDTH, sX, sY);
            return true;
        } else if (activeView == ViewType.SETTINGS) {
            handleSettingsClick(rightX, bodyY, CONTENT_WIDTH, sX, sY);
            return true;
        } else if (activeView == ViewType.FRIENDS) {
            handleFriendsClick(rightX, bodyY, CONTENT_WIDTH, sX, sY);
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void handleProfilesClick(float x, float y, float w, float sX, float sY) {
        ConfigManager cm = CigaretteModernClient.getInstance() != null ? CigaretteModernClient.getInstance().getConfigManager() : null;
        float inY = y + 15.0F;
        float inW = w - 54.0F;
        float inH = 17.5F;

        if (sX >= x + 8.0F && sX <= x + 8.0F + inW && sY >= inY && sY <= inY + inH) {
            profileInputFocused = true;
            return;
        } else {
            profileInputFocused = false;
        }

        float btnX = x + 8.0F + inW + 3.0F;
        float btnW = 35.0F;
        if (sX >= btnX && sX <= btnX + btnW && sY >= inY && sY <= inY + inH) {
            if (cm != null && !newProfileInput.trim().isEmpty()) {
                cm.save(newProfileInput.trim());
                newProfileInput = "";
            }
            return;
        }

        float saveY = inY + inH + 5.0F;
        float saveW = w - 16.0F;
        if (sX >= x + 8.0F && sX <= x + 8.0F + saveW && sY >= saveY && sY <= saveY + 16.5F) {
            if (cm != null && cm.getActiveProfile() != null) cm.save(cm.getActiveProfile().getName());
            return;
        }

        float listY = saveY + 21.0F;
        if (cm != null && sY >= listY) {
            List<String> list = cm.listProfiles();
            float itemH = 20.0F;
            float curY = listY + 3.0F - profileScrollOffset;
            for (String p : list) {
                boolean canDel = !"default".equalsIgnoreCase(p);
                float lBtnX = x + w - (canDel ? 57.0F : 38.0F);
                if (sX >= lBtnX && sX <= lBtnX + 26.0F && sY >= curY + 2.5F && sY <= curY + 17.5F) {
                    cm.load(p);
                    return;
                }
                if (canDel) {
                    float dBtnX = x + w - 28.0F;
                    if (sX >= dBtnX && sX <= dBtnX + 16.0F && sY >= curY + 2.5F && sY <= curY + 17.5F) {
                        cm.delete(p);
                        return;
                    }
                }
                curY += itemH + 2.0F;
            }
        }
    }

    private void handleSettingsClick(float x, float y, float w, float sX, float sY) {
        String[] presets = ColorUtil.THEME_PRESETS;
        float pillX = x + 10.0F;
        float pillY = y + 29.0F;
        float pillH = 14.5F;

        for (String preset : presets) {
            float pillW = RenderUtil.getSmallStringWidth(preset) + 8.0F;
            if (pillX + pillW > x + w - 10.0F) {
                pillX = x + 10.0F;
                pillY += pillH + 3.5F;
            }
            if (sX >= pillX && sX <= pillX + pillW && sY >= pillY && sY <= pillY + pillH) {
                ColorUtil.setThemePreset(preset);
                ClickGUIModule cgm = getClickGUIModule();
                if (cgm != null) cgm.setThemePreset(preset);
                return;
            }
            pillX += pillW + 4.0F;
        }

        float scaleSecY = pillY + pillH + 11.0F;
        float trackX = x + 10.0F;
        float trackY = scaleSecY + 11.5F;
        float trackW = w - 20.0F;
        float trackH = 4.0F;
        if (sX >= trackX - 4.0F && sX <= trackX + trackW + 4.0F && sY >= trackY - 4.0F && sY <= trackY + trackH + 4.0F) {
            draggingScaleSlider = true;
            float mouseFrac = AnimationUtil.clamp((sX - trackX) / trackW, 0.0F, 1.0F);
            this.userScale = 0.70F + (1.25F - 0.70F) * mouseFrac;
            return;
        }

        float blurY = trackY + 13.0F;
        float switchW = 24.0F;
        float switchH = 12.0F;
        float switchX = x + w - switchW - 10.0F;
        if (sX >= switchX - 2.0F && sX <= switchX + switchW + 2.0F && sY >= blurY - 2.0F && sY <= blurY + switchH + 2.0F) {
            ClickGUIModule cgm = getClickGUIModule();
            if (cgm != null && cgm.getBlurBackdrop() != null) {
                cgm.getBlurBackdrop().toggle();
            }
            return;
        }

        float protoY = blurY + 18.0F;
        if (sX >= switchX - 2.0F && sX <= switchX + switchW + 2.0F && sY >= protoY - 2.0F && sY <= protoY + switchH + 2.0F) {
            toggleModernCooldown();
            return;
        }

        float rBtnY = protoY + 20.0F;
        float rBtnW = w - 20.0F;
        float rBtnH = 17.0F;
        if (sX >= x + 10.0F && sX <= x + 10.0F + rBtnW && sY >= rBtnY && sY <= rBtnY + rBtnH) {
            userScale = 1.0F;
            float effectiveScale = computeEffectiveScale((float) this.width, (float) this.height);
            float vW = (float) this.width / effectiveScale;
            float vH = (float) this.height / effectiveScale;
            guiX = (vW - TOTAL_WIDTH) / 2.0F;
            guiY = (vH - TOTAL_HEIGHT) / 2.0F;
        }
    }

    private void handleFriendsClick(float x, float y, float w, float sX, float sY) {
        float inY = y + 16.0F;
        float inW = w - 54.0F;
        float inH = 17.5F;

        if (sX >= x + 8.0F && sX <= x + 8.0F + inW && sY >= inY && sY <= inY + inH) {
            friendInputFocused = true;
            return;
        } else {
            friendInputFocused = false;
        }

        float btnX = x + 8.0F + inW + 3.0F;
        float btnW = 35.0F;
        if (sX >= btnX && sX <= btnX + btnW && sY >= inY && sY <= inY + inH) {
            if (!newFriendInput.trim().isEmpty()) {
                FriendManager.addFriend(newFriendInput.trim());
                newFriendInput = "";
            }
            return;
        }

        float listY = inY + inH + 8.0F;
        List<String> friends = new ArrayList<>(FriendManager.getFriends());
        float itemY = listY + 4.0F;
        for (String f : friends) {
            float delX = x + w - 32.0F;
            if (sX >= delX && sX <= delX + 18.0F && sY >= itemY + 2.0F && sY <= itemY + 16.0F) {
                FriendManager.removeFriend(f);
                return;
            }
            itemY += 21.0F;
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingWindow = false;
        draggingScaleSlider = false;
        float effectiveScale = computeEffectiveScale((float) this.width, (float) this.height);
        float sX = (float) (event.x() / effectiveScale);
        float sY = (float) (event.y() / effectiveScale);

        if (activeView == ViewType.CATEGORY) {
            for (ModuleButton btn : getVisibleButtons()) {
                btn.mouseReleased(sX, sY, event.button());
            }
        }
        return super.mouseReleased(event);
    }

    private void closeGui() {
        draggingWindow = false;
        draggingScaleSlider = false;
        searchFocused = false;
        profileInputFocused = false;
        friendInputFocused = false;
        if (CigaretteModernClient.getInstance() != null && CigaretteModernClient.getInstance().getConfigManager() != null
                && CigaretteModernClient.getInstance().getConfigManager().getActiveProfile() != null) {
            CigaretteModernClient.getInstance().getConfigManager().save(
                    CigaretteModernClient.getInstance().getConfigManager().getActiveProfile().getName());
        }
        ClickGUIModule mod = getClickGUIModule();
        if (mod != null && mod.isEnabled()) {
            mod.setState(false);
        }
        if (this.minecraft != null && this.minecraft.gui != null) {
            this.minecraft.gui.setScreen(null);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();

        if (activeView == ViewType.CATEGORY) {
            for (ModuleButton btn : getVisibleButtons()) {
                if (btn.isListening()) {
                    btn.keyTyped('\0', keyCode);
                    return true;
                }
            }
        }

        if (searchFocused) {
            if (keyCode == InputConstants.KEY_ESCAPE) {
                if (!searchFilter.isEmpty()) {
                    searchFilter = "";
                } else {
                    searchFocused = false;
                }
            } else if (keyCode == InputConstants.KEY_RETURN) {
                searchFocused = false;
            } else if (keyCode == InputConstants.KEY_BACKSPACE && !searchFilter.isEmpty()) {
                searchFilter = searchFilter.substring(0, searchFilter.length() - 1);
                targetScrollOffset = 0.0F;
            }
            return true;
        }

        if (profileInputFocused) {
            if (keyCode == InputConstants.KEY_ESCAPE || keyCode == InputConstants.KEY_RETURN) {
                if (keyCode == InputConstants.KEY_RETURN && CigaretteModernClient.getInstance() != null && !newProfileInput.trim().isEmpty()) {
                    CigaretteModernClient.getInstance().getConfigManager().save(newProfileInput.trim());
                    newProfileInput = "";
                }
                profileInputFocused = false;
            } else if (keyCode == InputConstants.KEY_BACKSPACE && !newProfileInput.isEmpty()) {
                newProfileInput = newProfileInput.substring(0, newProfileInput.length() - 1);
            }
            return true;
        }

        if (friendInputFocused) {
            if (keyCode == InputConstants.KEY_ESCAPE || keyCode == InputConstants.KEY_RETURN) {
                if (keyCode == InputConstants.KEY_RETURN && !newFriendInput.trim().isEmpty()) {
                    FriendManager.addFriend(newFriendInput.trim());
                    newFriendInput = "";
                }
                friendInputFocused = false;
            } else if (keyCode == InputConstants.KEY_BACKSPACE && !newFriendInput.isEmpty()) {
                newFriendInput = newFriendInput.substring(0, newFriendInput.length() - 1);
            }
            return true;
        }

        ClickGUIModule mod = getClickGUIModule();
        if (keyCode == InputConstants.KEY_ESCAPE || keyCode == InputConstants.KEY_RSHIFT || keyCode == InputConstants.KEY_INSERT
                || (mod != null && mod.getKeyCode() > 0 && keyCode == mod.getKeyCode())) {
            closeGui();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchFocused && event.isAllowedChatCharacter()) {
            String cp = event.codepointAsString();
            if (cp != null && searchFilter.length() < 22) {
                searchFilter += cp;
                targetScrollOffset = 0.0F;
            }
            return true;
        }
        if (profileInputFocused && event.isAllowedChatCharacter()) {
            String cp = event.codepointAsString();
            if (cp != null && newProfileInput.length() < 24) newProfileInput += cp;
            return true;
        }
        if (friendInputFocused && event.isAllowedChatCharacter()) {
            String cp = event.codepointAsString();
            if (cp != null && newFriendInput.length() < 16) newFriendInput += cp;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private ClickGUIModule getClickGUIModule() {
        if (CigaretteModernClient.getInstance() != null && CigaretteModernClient.getInstance().getModuleManager() != null) {
            return CigaretteModernClient.getInstance().getModuleManager().getModuleByClass(ClickGUIModule.class);
        }
        return null;
    }

    public float getGuiScale() {
        return userScale;
    }

    public void setGuiScale(float guiScale) {
        this.userScale = AnimationUtil.clamp(guiScale, 0.70F, 1.25F);
    }
}
