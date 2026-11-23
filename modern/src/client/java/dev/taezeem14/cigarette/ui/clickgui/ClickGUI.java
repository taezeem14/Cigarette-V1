package dev.taezeem14.cigarette.ui.clickgui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.config.ConfigManager;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.client.FriendManager;
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
import java.util.*;

/**
 * Authentic Vape V4 dual-card user interface for Minecraft 26.3 Fabric.
 * Compact 2-column layout (Sidebar + Active Category Panel),
 * intelligent screen auto-scaling so it never overflows, unified window dragging,
 * 3-dot drawer toggles, profiles management, and settings controls.
 */
public class ClickGUI extends Screen {

    private static final DecimalFormat SCALE_FORMAT = new DecimalFormat("0.##");

    public static final float SIDEBAR_WIDTH = 124.0F;
    public static final float CONTENT_WIDTH = 176.0F;
    public static final float GAP = 7.0F;
    public static final float TOTAL_WIDTH = SIDEBAR_WIDTH + GAP + CONTENT_WIDTH;
    public static final float TOTAL_HEIGHT = 315.0F;

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

    private String newProfileInput = "";
    private boolean profileInputFocused = false;
    private float profileScrollOffset = 0.0F;
    private float targetProfileScroll = 0.0F;

    private String newFriendInput = "";
    private boolean friendInputFocused = false;

    public ClickGUI() {
        super(Component.literal("Cigarette ClickGUI"));
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
                buttons.add(new ModuleButton(m, 0, 0, CONTENT_WIDTH - 8.0F));
            }
            categoryButtons.put(cat, buttons);
        }
    }

    private float computeEffectiveScale(float screenW, float screenH) {
        float maxFitW = screenW * 0.88F;
        float maxFitH = screenH * 0.88F;
        float autoFitScale = Math.min(1.0F, Math.min(maxFitW / TOTAL_WIDTH, maxFitH / TOTAL_HEIGHT));
        return Math.max(0.55F, autoFitScale * userScale);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        fadeProgress = Math.min(1.0F, fadeProgress + AnimationUtil.getDeltaTime() * 7.5F);

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

        RenderUtil.drawDropShadow(g, x, y, w, h, 6.0F, 0x66000000);
        RenderUtil.drawRoundedRect(g, x, y, w, h, 5.0F, 0xFF141417);
        RenderUtil.drawRoundedOutline(g, x, y, w, h, 5.0F, 1.0F, 0xFF232328);

        float headH = 34.0F;
        RenderUtil.drawRoundedRectTop(g, x, y, w, headH, 5.0F, 0xFF18181D);
        RenderUtil.drawRect(g, x + 1.0F, y + headH - 1.0F, w - 2.0F, 1.0F, 0xFF222228);

        float logoX = x + 10.0F;
        RenderUtil.drawText(g, "VAPE", logoX, y + (headH - RenderUtil.getFontHeight()) / 2.0F, 0xFFFFFFFF, true);

        float v4X = logoX + RenderUtil.getStringWidth("VAPE") + 4.0F;
        float v4Y = y + (headH - 12.0F) / 2.0F;
        RenderUtil.drawRoundedRect(g, v4X, v4Y, 17.0F, 12.0F, 2.0F, 0xFF0D251A);
        RenderUtil.drawRoundedOutline(g, v4X, v4Y, 17.0F, 12.0F, 2.0F, 1.0F, 0xFF10B981);
        RenderUtil.drawSmallText(g, "v4", v4X + 3.0F, v4Y + (12.0F - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFF10B981, false);

        float gearX = x + w - 20.0F;
        float gearY = y + (headH - 11.0F) / 2.0F;
        boolean gearHovered = mouseX >= gearX - 3.0F && mouseX <= gearX + 14.0F && mouseY >= gearY - 3.0F && mouseY <= gearY + 14.0F;
        int gearColor = (activeView == ViewType.SETTINGS) ? accent : (gearHovered ? 0xFFFFFFFF : 0xFF727280);
        RenderUtil.drawFaIcon(g, "gear", gearX, gearY, 10.0F, gearColor);

        float itemY = y + headH + 4.0F;
        float itemH = 24.0F;

        for (Category cat : Category.values()) {
            boolean active = (activeView == ViewType.CATEGORY && activeCategory == cat);
            boolean hovered = mouseX >= x + 3.0F && mouseX <= x + w - 3.0F && mouseY >= itemY && mouseY <= itemY + itemH;

            int itemBg = active ? 0xFF1D2028 : (hovered ? 0xFF1A1C24 : 0);
            if (itemBg != 0) RenderUtil.drawRoundedRect(g, x + 3.0F, itemY, w - 6.0F, itemH, 3.5F, itemBg);
            if (active) RenderUtil.drawRoundedRect(g, x + 4.0F, itemY + 3.5F, 2.5F, itemH - 7.0F, 1.25F, accent);

            int iconCol = active ? accent : (hovered ? 0xFFCCCCCC : 0xFF727280);
            RenderUtil.drawFaIcon(g, cat.name(), x + 10.0F, itemY + (itemH - 10.0F) / 2.0F, 10.0F, iconCol);

            int textCol = active ? accent : (hovered ? 0xFFFFFFFF : 0xFF9E9EA8);
            RenderUtil.drawText(g, cat.getDisplayName(), x + 25.0F, itemY + (itemH - RenderUtil.getFontHeight()) / 2.0F, textCol, active);

            int chevCol = active ? accent : (hovered ? 0xFF8E8E9A : 0xFF4A4A55);
            RenderUtil.drawFaIcon(g, "chevron_right", x + w - 14.0F, itemY + (itemH - 8.0F) / 2.0F, 7.5F, chevCol);

            itemY += itemH + 1.0F;
        }

        itemY += 3.0F;
        RenderUtil.drawSmallText(g, "MISC", x + 10.0F, itemY, 0xFF585864, false);
        itemY += 13.0F;

        String[] miscItems = {"Friends", "Profiles", "HUD Editor"};
        for (String misc : miscItems) {
            boolean active = ("Profiles".equals(misc) && activeView == ViewType.PROFILES) || ("Friends".equals(misc) && activeView == ViewType.FRIENDS);
            boolean hovered = mouseX >= x + 3.0F && mouseX <= x + w - 3.0F && mouseY >= itemY && mouseY <= itemY + itemH;

            int itemBg = active ? 0xFF1D2028 : (hovered ? 0xFF1A1C24 : 0);
            if (itemBg != 0) RenderUtil.drawRoundedRect(g, x + 3.0F, itemY, w - 6.0F, itemH, 3.5F, itemBg);
            if (active) RenderUtil.drawRoundedRect(g, x + 4.0F, itemY + 3.5F, 2.5F, itemH - 7.0F, 1.25F, accent);

            int iconCol = active ? accent : (hovered ? 0xFFCCCCCC : 0xFF727280);
            String iconKey = "Friends".equals(misc) ? "friends" : ("Profiles".equals(misc) ? "profiles" : "hud");
            RenderUtil.drawFaIcon(g, iconKey, x + 10.0F, itemY + (itemH - 10.0F) / 2.0F, 10.0F, iconCol);

            int textCol = active ? accent : (hovered ? 0xFFFFFFFF : 0xFF9E9EA8);
            RenderUtil.drawText(g, misc, x + 25.0F, itemY + (itemH - RenderUtil.getFontHeight()) / 2.0F, textCol, active);

            if ("Profiles".equals(misc)) {
                ConfigManager cm = CigaretteModernClient.getInstance() != null ? CigaretteModernClient.getInstance().getConfigManager() : null;
                String activeProf = (cm != null && cm.getActiveProfile() != null) ? cm.getActiveProfile().getName() : "default";
                float badgeW = RenderUtil.getSmallStringWidth(activeProf) + 6.0F;
                float badgeX = x + w - badgeW - 16.0F;
                float badgeY = itemY + (itemH - 10.0F) / 2.0F;
                RenderUtil.drawRoundedRect(g, badgeX, badgeY, badgeW, 10.5F, 2.0F, 0xFF1F2027);
                RenderUtil.drawSmallText(g, activeProf, badgeX + 3.0F, badgeY + (10.5F - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFF7E7E8C, false);
            }

            int chevCol = active ? accent : (hovered ? 0xFF8E8E9A : 0xFF4A4A55);
            RenderUtil.drawFaIcon(g, "chevron_right", x + w - 14.0F, itemY + (itemH - 8.0F) / 2.0F, 7.5F, chevCol);

            itemY += itemH + 1.0F;
        }

        float footY = y + h - 24.0F;
        RenderUtil.drawRect(g, x + 1.0F, footY, w - 2.0F, 1.0F, 0xFF1F1F24);
        RenderUtil.drawFaIcon(g, "user", x + 10.0F, footY + 6.5F, 10.0F, 0xFF7A7A88);

        String scaleTag = SCALE_FORMAT.format(userScale) + "x";
        RenderUtil.drawSmallText(g, scaleTag, x + 40.0F, footY + (24.0F - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFF656572, false);

        float palX = x + w - 16.0F;
        float palY = footY + 12.0F;
        boolean palHovered = mouseX >= palX - 5.0F && mouseX <= palX + 5.0F && mouseY >= palY - 5.0F && mouseY <= palY + 5.0F;
        RenderUtil.drawCircle(g, palX, palY, palHovered ? 4.5F : 3.5F, accent);
        RenderUtil.drawCircleOutline(g, palX, palY, palHovered ? 5.5F : 4.5F, 1.0F, 0xFF353540);
    }

    private void renderContentPanel(GuiGraphicsExtractor g, float mouseX, float mouseY, float partialTicks, float effectiveScale, int accent) {
        float x = guiX + SIDEBAR_WIDTH + GAP;
        float y = guiY;
        float w = CONTENT_WIDTH;
        float h = TOTAL_HEIGHT;

        RenderUtil.drawDropShadow(g, x, y, w, h, 6.0F, 0x66000000);
        RenderUtil.drawRoundedRect(g, x, y, w, h, 5.0F, 0xFF141417);
        RenderUtil.drawRoundedOutline(g, x, y, w, h, 5.0F, 1.0F, 0xFF232328);

        float headH = 34.0F;
        RenderUtil.drawRoundedRectTop(g, x, y, w, headH, 5.0F, 0xFF18181D);
        RenderUtil.drawRect(g, x + 1.0F, y + headH - 1.0F, w - 2.0F, 1.0F, 0xFF222228);

        String title = (activeView == ViewType.CATEGORY) ? activeCategory.getDisplayName() : (activeView == ViewType.PROFILES ? "Profiles" : (activeView == ViewType.SETTINGS ? "Settings" : "Friends"));
        String iconKey = (activeView == ViewType.CATEGORY) ? activeCategory.name() : (activeView == ViewType.PROFILES ? "profiles" : (activeView == ViewType.SETTINGS ? "gear" : "friends"));

        RenderUtil.drawFaIcon(g, iconKey, x + 10.0F, y + (headH - 10.0F) / 2.0F, 10.0F, accent);
        RenderUtil.drawText(g, title, x + 25.0F, y + (headH - RenderUtil.getFontHeight()) / 2.0F, 0xFFFFFFFF, true);

        float chevX = x + w - 18.0F;
        float chevY = y + (headH - 8.0F) / 2.0F;
        RenderUtil.drawFaIcon(g, "chevron_up", chevX, chevY, 8.0F, 0xFF7A7A88);

        float bodyY = y + headH + 4.0F;
        float bodyH = h - headH - 8.0F;

        if (activeView == ViewType.CATEGORY) {
            renderCategoryModules(g, x, bodyY, w, bodyH, mouseX, mouseY, partialTicks, effectiveScale, accent);
        } else if (activeView == ViewType.PROFILES) {
            renderProfilesView(g, x, bodyY, w, bodyH, mouseX, mouseY, accent);
        } else if (activeView == ViewType.SETTINGS) {
            renderSettingsView(g, x, bodyY, w, bodyH, mouseX, mouseY, accent);
        } else if (activeView == ViewType.FRIENDS) {
            renderFriendsView(g, x, bodyY, w, bodyH, mouseX, mouseY, accent);
        }
    }

    private void renderCategoryModules(GuiGraphicsExtractor g, float panelX, float bodyY, float panelW, float bodyH,
                                       float mouseX, float mouseY, float partialTicks, float effectiveScale, int accent) {
        List<ModuleButton> buttons = categoryButtons.get(activeCategory);
        if (buttons == null) return;

        float totalH = 4.0F;
        for (ModuleButton btn : buttons) totalH += btn.getTotalHeight() + 2.0F;

        float maxScroll = Math.max(0.0F, totalH - bodyH);
        targetScrollOffset = AnimationUtil.clamp(targetScrollOffset, 0.0F, maxScroll);
        scrollOffset = AnimationUtil.lerp(scrollOffset, targetScrollOffset, 16.0F);

        RenderUtil.pushScissor(g, panelX, bodyY, panelW, bodyH, effectiveScale);
        try {
            float btnY = bodyY + 2.0F - scrollOffset;
            float btnW = panelW - 10.0F;

            for (ModuleButton btn : buttons) {
                btn.setX(panelX + 5.0F);
                btn.setY(btnY);
                btn.setWidth(btnW);
                btn.render(g, mouseX, mouseY, partialTicks, effectiveScale);
                btnY += btn.getTotalHeight() + 2.0F;
            }

            if (maxScroll > 0.5F) {
                float trackX = panelX + panelW - 4.5F;
                float trackH = bodyH - 4.0F;
                RenderUtil.drawRoundedRect(g, trackX, bodyY + 2.0F, 2.0F, trackH, 1.0F, 0xFF1E1E24);
                float thumbH = Math.max(20.0F, trackH * (bodyH / totalH));
                float thumbY = bodyY + 2.0F + (trackH - thumbH) * (scrollOffset / maxScroll);
                RenderUtil.drawRoundedRect(g, trackX, thumbY, 2.0F, thumbH, 1.0F, ColorUtil.modifyAlpha(accent, 190));
            }
        } finally {
            RenderUtil.popScissor(g);
        }
    }

    private void renderProfilesView(GuiGraphicsExtractor g, float x, float y, float w, float h, float mouseX, float mouseY, int accent) {
        ConfigManager cm = CigaretteModernClient.getInstance() != null ? CigaretteModernClient.getInstance().getConfigManager() : null;
        String activeName = (cm != null && cm.getActiveProfile() != null) ? cm.getActiveProfile().getName() : "default";

        RenderUtil.drawSmallText(g, "Active: \u00a7a" + activeName, x + 8.0F, y + 2.0F, 0xFFCCCCCC, false);

        float inY = y + 15.0F;
        float inW = w - 52.0F;
        float inH = 17.0F;
        RenderUtil.drawRoundedRect(g, x + 8.0F, inY, inW, inH, 3.0F, 0xFF19191E);
        RenderUtil.drawRoundedOutline(g, x + 8.0F, inY, inW, inH, 3.0F, 1.0F, profileInputFocused ? accent : 0xFF2A2A32);

        String txt = newProfileInput.isEmpty() && !profileInputFocused ? "New profile..." : newProfileInput + (profileInputFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : "");
        RenderUtil.drawSmallText(g, txt, x + 12.0F, inY + (inH - RenderUtil.getSmallFontHeight()) / 2.0F, newProfileInput.isEmpty() && !profileInputFocused ? 0xFF60606B : 0xFFFFFFFF, false);

        float btnX = x + 8.0F + inW + 3.0F;
        float btnW = 33.0F;
        boolean crHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= inY && mouseY <= inY + inH;
        RenderUtil.drawRoundedRect(g, btnX, inY, btnW, inH, 3.0F, crHovered ? accent : 0xFF22232B);
        RenderUtil.drawCenteredSmallText(g, "Add", btnX + btnW / 2.0F, inY + (inH - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFFFFFFFF, false);

        float saveY = inY + inH + 5.0F;
        float saveW = w - 16.0F;
        boolean saveHovered = mouseX >= x + 8.0F && mouseX <= x + 8.0F + saveW && mouseY >= saveY && mouseY <= saveY + 16.0F;
        RenderUtil.drawRoundedRect(g, x + 8.0F, saveY, saveW, 16.0F, 3.0F, saveHovered ? 0xFF10B981 : 0xFF17241F);
        RenderUtil.drawRoundedOutline(g, x + 8.0F, saveY, saveW, 16.0F, 3.0F, 1.0F, 0xFF10B981);
        RenderUtil.drawCenteredSmallText(g, "Save Current Profile", x + 8.0F + saveW / 2.0F, saveY + (16.0F - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFFFFFFFF, false);

        float listY = saveY + 20.0F;
        float listH = h - (listY - y) - 4.0F;
        RenderUtil.drawRoundedRect(g, x + 8.0F, listY, w - 16.0F, listH, 3.0F, 0xFF16161B);
        RenderUtil.drawRoundedOutline(g, x + 8.0F, listY, w - 16.0F, listH, 3.0F, 1.0F, 0xFF222228);

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
                    RenderUtil.drawRoundedRect(g, x + 10.0F, curY, w - 20.0F, itemH, 2.5F, hov ? 0xFF22222A : (isCur ? 0xFF1A1C25 : 0xFF19191E));

                    int nameCol = isCur ? accent : 0xFFD0D0D8;
                    RenderUtil.drawSmallText(g, p, x + 14.0F, curY + (itemH - RenderUtil.getSmallFontHeight()) / 2.0F, nameCol, false);

                    float lBtnX = x + w - 38.0F;
                    boolean lHov = mouseX >= lBtnX && mouseX <= lBtnX + 24.0F && mouseY >= curY + 2.5F && mouseY <= curY + 17.5F;
                    RenderUtil.drawRoundedRect(g, lBtnX, curY + 2.5F, 24.0F, 15.0F, 2.0F, lHov ? accent : 0xFF252630);
                    RenderUtil.drawCenteredSmallText(g, "Load", lBtnX + 12.0F, curY + 2.5F + (15.0F - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFFFFFFFF, false);

                    curY += itemH + 2.0F;
                }
            } finally {
                RenderUtil.popScissor(g);
            }
        }
    }

    private void renderSettingsView(GuiGraphicsExtractor g, float x, float y, float w, float h, float mouseX, float mouseY, int accent) {
        RenderUtil.drawText(g, "ClickGUI Settings", x + 10.0F, y + 4.0F, 0xFFFFFFFF, true);

        RenderUtil.drawSmallText(g, "Theme Preset", x + 10.0F, y + 20.0F, 0xFF858592, false);
        String[] presets = {"Electric Blue", "Ember Orange", "Amethyst", "Emerald", "Rainbow"};
        float pillX = x + 10.0F;
        float pillY = y + 32.0F;
        float pillH = 15.0F;

        for (String preset : presets) {
            float pillW = RenderUtil.getSmallStringWidth(preset) + 8.0F;
            if (pillX + pillW > x + w - 10.0F) {
                pillX = x + 10.0F;
                pillY += pillH + 4.0F;
            }
            boolean isCur = preset.equalsIgnoreCase(ColorUtil.getThemePreset());
            boolean hov = mouseX >= pillX && mouseX <= pillX + pillW && mouseY >= pillY && mouseY <= pillY + pillH;
            RenderUtil.drawRoundedRect(g, pillX, pillY, pillW, pillH, 3.0F, isCur ? accent : (hov ? 0xFF262630 : 0xFF1C1C22));
            RenderUtil.drawRoundedOutline(g, pillX, pillY, pillW, pillH, 3.0F, 1.0F, isCur ? accent : 0xFF2B2B35);
            RenderUtil.drawSmallText(g, preset, pillX + 4.0F, pillY + (pillH - RenderUtil.getSmallFontHeight()) / 2.0F, isCur ? 0xFFFFFFFF : 0xFFB0B0BC, false);
            pillX += pillW + 4.0F;
        }

        float scaleSecY = pillY + pillH + 14.0F;
        RenderUtil.drawSmallText(g, "GUI Scale: \u00a7f" + SCALE_FORMAT.format(userScale) + "x", x + 10.0F, scaleSecY, 0xFF858592, false);
        float trackX = x + 10.0F;
        float trackY = scaleSecY + 12.0F;
        float trackW = w - 20.0F;
        float trackH = 4.0F;

        RenderUtil.drawRoundedRect(g, trackX, trackY, trackW, trackH, 2.0F, 0xFF23232C);
        float frac = AnimationUtil.clamp((userScale - 0.70F) / (1.25F - 0.70F), 0.0F, 1.0F);
        float fillW = trackW * frac;
        if (fillW > 1.0F) RenderUtil.drawRoundedRect(g, trackX, trackY, fillW, trackH, 2.0F, accent);
        RenderUtil.drawCircle(g, trackX + fillW, trackY + trackH / 2.0F, 3.5F, 0xFFFFFFFF);

        if (draggingScaleSlider) {
            float mouseFrac = AnimationUtil.clamp((mouseX - trackX) / trackW, 0.0F, 1.0F);
            this.userScale = 0.70F + (1.25F - 0.70F) * mouseFrac;
        }

        float rBtnY = trackY + 22.0F;
        float rBtnW = w - 20.0F;
        float rBtnH = 17.0F;
        boolean rHov = mouseX >= x + 10.0F && mouseX <= x + 10.0F + rBtnW && mouseY >= rBtnY && mouseY <= rBtnY + rBtnH;
        RenderUtil.drawRoundedRect(g, x + 10.0F, rBtnY, rBtnW, rBtnH, 3.0F, rHov ? 0xFF282834 : 0xFF1C1C23);
        RenderUtil.drawRoundedOutline(g, x + 10.0F, rBtnY, rBtnW, rBtnH, 3.0F, 1.0F, 0xFF2E2E38);
        RenderUtil.drawCenteredSmallText(g, "Reset Window Position", x + 10.0F + rBtnW / 2.0F, rBtnY + (rBtnH - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFFB0B0BC, false);
    }

    private void renderFriendsView(GuiGraphicsExtractor g, float x, float y, float w, float h, float mouseX, float mouseY, int accent) {
        RenderUtil.drawText(g, "Friends Manager", x + 10.0F, y + 4.0F, 0xFFFFFFFF, true);

        float inY = y + 16.0F;
        float inW = w - 52.0F;
        float inH = 17.0F;
        RenderUtil.drawRoundedRect(g, x + 8.0F, inY, inW, inH, 3.0F, 0xFF19191E);
        RenderUtil.drawRoundedOutline(g, x + 8.0F, inY, inW, inH, 3.0F, 1.0F, friendInputFocused ? accent : 0xFF2A2A32);

        String txt = newFriendInput.isEmpty() && !friendInputFocused ? "Username..." : newFriendInput + (friendInputFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : "");
        RenderUtil.drawSmallText(g, txt, x + 12.0F, inY + (inH - RenderUtil.getSmallFontHeight()) / 2.0F, newFriendInput.isEmpty() && !friendInputFocused ? 0xFF60606B : 0xFFFFFFFF, false);

        float btnX = x + 8.0F + inW + 3.0F;
        float btnW = 33.0F;
        boolean crHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= inY && mouseY <= inY + inH;
        RenderUtil.drawRoundedRect(g, btnX, inY, btnW, inH, 3.0F, crHovered ? accent : 0xFF22232B);
        RenderUtil.drawCenteredSmallText(g, "Add", btnX + btnW / 2.0F, inY + (inH - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFFFFFFFF, false);

        float listY = inY + inH + 8.0F;
        float listH = h - (listY - y) - 4.0F;
        RenderUtil.drawRoundedRect(g, x + 8.0F, listY, w - 16.0F, listH, 3.0F, 0xFF16161B);
        RenderUtil.drawRoundedOutline(g, x + 8.0F, listY, w - 16.0F, listH, 3.0F, 1.0F, 0xFF222228);

        List<String> friends = new ArrayList<>(FriendManager.getFriends());
        if (friends.isEmpty()) {
            RenderUtil.drawCenteredSmallText(g, "No friends added yet.", x + 8.0F + (w - 16.0F) / 2.0F, listY + listH / 2.0F - 4.0F, 0xFF60606D, false);
        } else {
            float itemY = listY + 4.0F;
            for (String f : friends) {
                RenderUtil.drawRoundedRect(g, x + 10.0F, itemY, w - 20.0F, 18.0F, 2.5F, 0xFF191920);
                RenderUtil.drawSmallText(g, f, x + 14.0F, itemY + (18.0F - RenderUtil.getSmallFontHeight()) / 2.0F, 0xFFD8D8E0, false);

                float delX = x + w - 32.0F;
                boolean dHov = mouseX >= delX && mouseX <= delX + 18.0F && mouseY >= itemY + 2.0F && mouseY <= itemY + 16.0F;
                RenderUtil.drawRoundedRect(g, delX, itemY + 2.0F, 18.0F, 14.0F, 2.0F, dHov ? 0xFFC62828 : 0xFF2B1D1D);
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
        if (sX >= guiX && sX <= guiX + SIDEBAR_WIDTH && sY >= guiY && sY <= guiY + headH) {
            float gearX = guiX + SIDEBAR_WIDTH - 20.0F;
            if (sX >= gearX - 4.0F && sX <= gearX + 16.0F) {
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

        float rightX = guiX + SIDEBAR_WIDTH + GAP;
        if (sX >= rightX && sX <= rightX + CONTENT_WIDTH && sY >= guiY && sY <= guiY + headH) {
            if (mouseButton == 0) {
                draggingWindow = true;
                dragOffsetX = sX - guiX;
                dragOffsetY = sY - guiY;
            }
            return true;
        }

        float itemY = guiY + headH + 4.0F;
        float itemH = 24.0F;
        for (Category cat : Category.values()) {
            if (sX >= guiX + 3.0F && sX <= guiX + SIDEBAR_WIDTH - 3.0F && sY >= itemY && sY <= itemY + itemH) {
                activeView = ViewType.CATEGORY;
                activeCategory = cat;
                targetScrollOffset = 0.0F;
                scrollOffset = 0.0F;
                return true;
            }
            itemY += itemH + 1.0F;
        }

        itemY += 16.0F;
        String[] miscItems = {"Friends", "Profiles", "HUD Editor"};
        for (String misc : miscItems) {
            if (sX >= guiX + 3.0F && sX <= guiX + SIDEBAR_WIDTH - 3.0F && sY >= itemY && sY <= itemY + itemH) {
                if ("Friends".equals(misc)) activeView = ViewType.FRIENDS;
                else if ("Profiles".equals(misc)) {
                    activeView = ViewType.PROFILES;
                    targetProfileScroll = 0.0F;
                } else if ("HUD Editor".equals(misc)) {
                    this.minecraft.gui.setScreen(new HUDEditor());
                }
                return true;
            }
            itemY += itemH + 1.0F;
        }

        float footY = guiY + TOTAL_HEIGHT - 24.0F;
        float palX = guiX + SIDEBAR_WIDTH - 16.0F;
        float palY = footY + 12.0F;
        if (sX >= palX - 6.0F && sX <= palX + 6.0F && sY >= palY - 6.0F && sY <= palY + 6.0F) {
            String next = ColorUtil.cycleThemePreset();
            ClickGUIModule mod = getClickGUIModule();
            if (mod != null) mod.setThemePreset(next);
            return true;
        }

        float bodyY = guiY + headH + 4.0F;
        float bodyH = TOTAL_HEIGHT - headH - 8.0F;

        if (activeView == ViewType.CATEGORY) {
            List<ModuleButton> buttons = categoryButtons.get(activeCategory);
            if (buttons != null && sX >= rightX && sX <= rightX + CONTENT_WIDTH && sY >= bodyY && sY <= bodyY + bodyH) {
                for (ModuleButton btn : buttons) {
                    btn.mouseClicked(sX, sY, mouseButton);
                }
                return true;
            }
        } else if (activeView == ViewType.PROFILES) {
            handleProfilesClick(rightX, bodyY, CONTENT_WIDTH, sX, sY, mouseButton);
            return true;
        } else if (activeView == ViewType.SETTINGS) {
            handleSettingsClick(rightX, bodyY, CONTENT_WIDTH, sX, sY, mouseButton);
            return true;
        } else if (activeView == ViewType.FRIENDS) {
            handleFriendsClick(rightX, bodyY, CONTENT_WIDTH, sX, sY, mouseButton);
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void handleProfilesClick(float x, float y, float w, float sX, float sY, int mouseButton) {
        ConfigManager cm = CigaretteModernClient.getInstance() != null ? CigaretteModernClient.getInstance().getConfigManager() : null;
        float inY = y + 15.0F;
        float inW = w - 52.0F;
        float inH = 17.0F;

        if (sX >= x + 8.0F && sX <= x + 8.0F + inW && sY >= inY && sY <= inY + inH) {
            profileInputFocused = true;
            return;
        } else profileInputFocused = false;

        float btnX = x + 8.0F + inW + 3.0F;
        float btnW = 33.0F;
        if (sX >= btnX && sX <= btnX + btnW && sY >= inY && sY <= inY + inH) {
            if (cm != null && !newProfileInput.trim().isEmpty()) {
                cm.save(newProfileInput.trim());
                newProfileInput = "";
            }
            return;
        }

        float saveY = inY + inH + 5.0F;
        float saveW = w - 16.0F;
        if (sX >= x + 8.0F && sX <= x + 8.0F + saveW && sY >= saveY && sY <= saveY + 16.0F) {
            if (cm != null && cm.getActiveProfile() != null) cm.save(cm.getActiveProfile().getName());
            return;
        }

        float listY = saveY + 20.0F;
        if (cm != null && sY >= listY) {
            List<String> list = cm.listProfiles();
            float itemH = 20.0F;
            float curY = listY + 3.0F - profileScrollOffset;
            for (String p : list) {
                float lBtnX = x + w - 38.0F;
                if (sX >= lBtnX && sX <= lBtnX + 24.0F && sY >= curY + 2.5F && sY <= curY + 17.5F) {
                    cm.load(p);
                    return;
                }
                curY += itemH + 2.0F;
            }
        }
    }

    private void handleSettingsClick(float x, float y, float w, float sX, float sY, int mouseButton) {
        String[] presets = {"Electric Blue", "Ember Orange", "Amethyst", "Emerald", "Rainbow"};
        float pillX = x + 10.0F;
        float pillY = y + 32.0F;
        float pillH = 15.0F;

        for (String preset : presets) {
            float pillW = RenderUtil.getSmallStringWidth(preset) + 8.0F;
            if (pillX + pillW > x + w - 10.0F) {
                pillX = x + 10.0F;
                pillY += pillH + 4.0F;
            }
            if (sX >= pillX && sX <= pillX + pillW && sY >= pillY && sY <= pillY + pillH) {
                ClickGUIModule cgm = getClickGUIModule();
                if (cgm != null) cgm.setThemePreset(preset);
                return;
            }
            pillX += pillW + 4.0F;
        }

        float scaleSecY = pillY + pillH + 14.0F;
        float trackX = x + 10.0F;
        float trackY = scaleSecY + 12.0F;
        float trackW = w - 20.0F;
        float trackH = 4.0F;
        if (sX >= trackX - 4.0F && sX <= trackX + trackW + 4.0F && sY >= trackY - 4.0F && sY <= trackY + trackH + 4.0F) {
            draggingScaleSlider = true;
            float mouseFrac = AnimationUtil.clamp((sX - trackX) / trackW, 0.0F, 1.0F);
            this.userScale = 0.70F + (1.25F - 0.70F) * mouseFrac;
            return;
        }

        float rBtnY = trackY + 22.0F;
        float rBtnW = w - 20.0F;
        float rBtnH = 17.0F;
        if (sX >= x + 10.0F && sX <= x + 10.0F + rBtnW && sY >= rBtnY && sY <= rBtnY + rBtnH) {
            float effectiveScale = computeEffectiveScale((float) this.width, (float) this.height);
            float vW = (float) this.width / effectiveScale;
            float vH = (float) this.height / effectiveScale;
            guiX = (vW - TOTAL_WIDTH) / 2.0F;
            guiY = (vH - TOTAL_HEIGHT) / 2.0F;
            userScale = 1.0F;
        }
    }

    private void handleFriendsClick(float x, float y, float w, float sX, float sY, int mouseButton) {
        float inY = y + 16.0F;
        float inW = w - 52.0F;
        float inH = 17.0F;

        if (sX >= x + 8.0F && sX <= x + 8.0F + inW && sY >= inY && sY <= inY + inH) {
            friendInputFocused = true;
            return;
        } else friendInputFocused = false;

        float btnX = x + 8.0F + inW + 3.0F;
        float btnW = 33.0F;
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
            List<ModuleButton> buttons = categoryButtons.get(activeCategory);
            if (buttons != null) {
                for (ModuleButton btn : buttons) btn.mouseReleased(sX, sY, event.button());
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();

        if (activeView == ViewType.CATEGORY) {
            List<ModuleButton> buttons = categoryButtons.get(activeCategory);
            if (buttons != null) {
                for (ModuleButton btn : buttons) {
                    if (btn.isListening()) {
                        btn.keyTyped('\0', keyCode);
                        return true;
                    }
                }
            }
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
                || (mod != null && keyCode == mod.getKeyCode())) {
            this.minecraft.gui.setScreen(null);
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
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
