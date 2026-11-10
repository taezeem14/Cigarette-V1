package dev.taezeem14.cigarette.ui.clickgui;

import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public class CategoryFrame {

    private static final float MAX_CONTENT_HEIGHT = 260.0F;

    private final Category category;
    private final List<ModuleButton> buttons = new ArrayList<>();

    private float x;
    private float y;
    private float width = 122.0F;
    private final float headerHeight = 23.0F;

    private boolean collapsed = false;
    private boolean dragging = false;
    private boolean userMoved = false;
    private float dragX = 0.0F;
    private float dragY = 0.0F;
    private float collapseAnimation = 1.0F;
    private float scrollOffset = 0.0F;
    private float targetScrollOffset = 0.0F;

    public CategoryFrame(Category category, List<Module> modules, float x, float y) {
        this.category = category;
        this.x = x;
        this.y = y;
        for (Module m : modules) {
            buttons.add(new ModuleButton(m, x + 3.0F, 0, width - 6.0F));
        }
    }

    public void render(GuiGraphicsExtractor g, float mouseX, float mouseY, float partialTicks, String filter, float guiScale, float viewportW, float viewportH) {
        if (dragging) {
            this.x = AnimationUtil.clamp(mouseX - dragX, 4.0F, Math.max(4.0F, viewportW - width - 4.0F));
            this.y = AnimationUtil.clamp(mouseY - dragY, 40.0F, Math.max(40.0F, viewportH - headerHeight - 4.0F));
        } else if (viewportW > 0.0F && viewportH > 0.0F) {
            this.x = AnimationUtil.clamp(this.x, 4.0F, Math.max(4.0F, viewportW - width - 4.0F));
            this.y = AnimationUtil.clamp(this.y, 40.0F, Math.max(40.0F, viewportH - headerHeight - 4.0F));
        }

        collapseAnimation = AnimationUtil.lerp(collapseAnimation, collapsed ? 0.0F : 1.0F, 15.0F);
        float totalContentH = calculateContentHeight(filter);
        float maxScroll = Math.max(0.0F, totalContentH - MAX_CONTENT_HEIGHT);
        targetScrollOffset = AnimationUtil.clamp(targetScrollOffset, 0.0F, maxScroll);
        scrollOffset = AnimationUtil.lerp(scrollOffset, targetScrollOffset, 18.0F);

        float cappedH = Math.min(MAX_CONTENT_HEIGHT, totalContentH);
        float visibleContentH = cappedH * AnimationUtil.easeOutCubic(collapseAnimation);
        float frameH = headerHeight + (collapseAnimation > 0.01F ? visibleContentH + 4.0F : 0.0F);
        int accent = ColorUtil.getAccent();

        RenderUtil.drawDropShadow(g, x, y, width, frameH, 5.0F, 0x60000000);
        RenderUtil.drawRoundedRect(g, x, y, width, frameH, 4.0F, ColorUtil.BACKGROUND);
        RenderUtil.drawRoundedOutline(g, x, y, width, frameH, 4.0F, 1.0F, ColorUtil.BORDER);

        if (collapseAnimation > 0.05F) {
            RenderUtil.drawRoundedRectTop(g, x, y, width, headerHeight, 4.0F, ColorUtil.HEADER);
        } else {
            RenderUtil.drawRoundedRect(g, x, y, width, headerHeight, 4.0F, ColorUtil.HEADER);
        }
        RenderUtil.drawRect(g, x + 1.0F, y + headerHeight - 2.0F, width - 2.0F, 1.0F, ColorUtil.modifyAlpha(accent, 45));
        RenderUtil.drawRect(g, x + 1.0F, y + headerHeight - 1.0F, width - 2.0F, 1.0F, ColorUtil.modifyAlpha(accent, 180));

        RenderUtil.drawFaIcon(g, category.name(), x + 7.0F, y + (headerHeight - 9.0F) / 2.0F, 9.0F, accent);
        RenderUtil.drawText(g, category.getDisplayName(), x + 19.0F, y + (headerHeight - RenderUtil.getFontHeight()) / 2.0F, ColorUtil.TEXT_PRIMARY, false);

        int enabledCount = 0;
        for (ModuleButton btn : buttons) {
            if (btn.getModule().isEnabled()) enabledCount++;
        }
        String countText = enabledCount + "/" + buttons.size();
        float countW = RenderUtil.getSmallStringWidth(countText) + 6.0F;
        float countH = 10.5F;
        String indicator = collapsed ? "+" : "-";
        float indW = RenderUtil.getStringWidth(indicator);
        float indX = x + width - indW - 7.0F;
        float countX = indX - countW - 4.0F;
        float countY = y + (headerHeight - countH) / 2.0F;

        RenderUtil.drawRoundedRect(g, countX, countY, countW, countH, 2.5F,
                enabledCount > 0 ? ColorUtil.modifyAlpha(accent, 40) : 0xFF22222A);
        RenderUtil.drawCenteredSmallText(g, countText, countX + countW / 2.0F,
                countY + (countH - RenderUtil.getSmallFontHeight()) / 2.0F,
                enabledCount > 0 ? accent : ColorUtil.TEXT_MUTED, false);
        RenderUtil.drawText(g, indicator, indX, y + (headerHeight - RenderUtil.getFontHeight()) / 2.0F, ColorUtil.TEXT_MUTED, false);

        if (collapseAnimation > 0.01F && totalContentH > 0.0F) {
            float clipH = visibleContentH + 3.0F;
            boolean hasScrollbar = totalContentH > MAX_CONTENT_HEIGHT + 1.0F;
            float btnW = hasScrollbar ? width - 9.0F : width - 6.0F;

            RenderUtil.pushScissor(g, x, y + headerHeight, width, clipH, guiScale);
            try {
                float btnY = y + headerHeight + 3.0F - scrollOffset;
                for (ModuleButton btn : buttons) {
                    if (matchesFilter(btn.getModule(), filter)) {
                        btn.setX(x + 3.0F);
                        btn.setY(btnY);
                        btn.setWidth(btnW);
                        btn.render(g, mouseX, mouseY, partialTicks, guiScale);
                        btnY += btn.getTotalHeight() + 2.0F;
                    }
                }
                if (hasScrollbar && collapseAnimation > 0.5F) {
                    float trackX = x + width - 4.0F;
                    float trackY = y + headerHeight + 3.0F;
                    float trackH = visibleContentH - 3.0F;
                    RenderUtil.drawRoundedRect(g, trackX, trackY, 2.0F, trackH, 1.0F, 0xFF202027);
                    float thumbH = Math.max(18.0F, trackH * (cappedH / totalContentH));
                    float scrollRatio = maxScroll > 0.0F ? (scrollOffset / maxScroll) : 0.0F;
                    RenderUtil.drawRoundedRect(g, trackX, trackY + (trackH - thumbH) * scrollRatio, 2.0F, thumbH, 1.0F, ColorUtil.modifyAlpha(accent, 190));
                }
            } finally {
                RenderUtil.popScissor(g);
            }
        }
    }

    public boolean mouseClicked(float mouseX, float mouseY, int mouseButton, String filter) {
        if (isHeaderHovered(mouseX, mouseY)) {
            if (mouseButton == 0) {
                dragging = true;
                userMoved = true;
                dragX = mouseX - x;
                dragY = mouseY - y;
            } else if (mouseButton == 1) {
                collapsed = !collapsed;
            }
            return true;
        }
        if (!collapsed && collapseAnimation > 0.5F && isContentHovered(mouseX, mouseY, filter)) {
            for (ModuleButton btn : buttons) {
                if (matchesFilter(btn.getModule(), filter)) {
                    btn.mouseClicked(mouseX, mouseY, mouseButton);
                }
            }
            return true;
        }
        return false;
    }

    public void mouseReleased(float mouseX, float mouseY, int mouseButton) {
        dragging = false;
        if (!collapsed) {
            for (ModuleButton btn : buttons) btn.mouseReleased(mouseX, mouseY, mouseButton);
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (!collapsed) {
            for (ModuleButton btn : buttons) btn.keyTyped(typedChar, keyCode);
        }
    }

    public boolean isListening() {
        for (ModuleButton btn : buttons) {
            if (btn.isListening()) return true;
        }
        return false;
    }

    public boolean handleScroll(float mouseX, float mouseY, double scrollDeltaY, String filter) {
        if (collapsed || collapseAnimation < 0.5F || scrollDeltaY == 0.0) return false;
        if (!isContentHovered(mouseX, mouseY, filter)) return false;
        float maxScroll = Math.max(0.0F, calculateContentHeight(filter) - MAX_CONTENT_HEIGHT);
        if (maxScroll <= 0.0F) return false;
        targetScrollOffset = AnimationUtil.clamp(targetScrollOffset + (scrollDeltaY > 0.0 ? -20.0F : 20.0F), 0.0F, maxScroll);
        return true;
    }

    public Module getHoveredModule(float mouseX, float mouseY, String filter) {
        if (collapsed || collapseAnimation < 0.5F || !isContentHovered(mouseX, mouseY, filter)) return null;
        for (ModuleButton btn : buttons) {
            if (matchesFilter(btn.getModule(), filter) && btn.isHovered(mouseX, mouseY)) return btn.getModule();
        }
        return null;
    }

    private boolean isContentHovered(float mouseX, float mouseY, String filter) {
        float visibleH = Math.min(MAX_CONTENT_HEIGHT, calculateContentHeight(filter)) + 4.0F;
        return mouseX >= x && mouseX <= x + width && mouseY >= y + headerHeight && mouseY <= y + headerHeight + visibleH;
    }

    private float calculateContentHeight(String filter) {
        float h = 2.0F;
        for (ModuleButton btn : buttons) {
            if (matchesFilter(btn.getModule(), filter)) h += btn.getTotalHeight() + 2.0F;
        }
        return h;
    }

    private boolean matchesFilter(Module m, String filter) {
        if (filter == null || filter.trim().isEmpty()) return true;
        String q = filter.toLowerCase().trim();
        return m.getName().toLowerCase().contains(q)
                || (m.getDescription() != null && m.getDescription().toLowerCase().contains(q));
    }

    public boolean isHeaderHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight;
    }

    public Category getCategory() { return category; }
    public List<ModuleButton> getButtons() { return buttons; }
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    public boolean isCollapsed() { return collapsed; }
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        this.collapseAnimation = collapsed ? 0.0F : 1.0F;
    }
    public boolean hasUserMoved() { return userMoved; }
    public void setUserMoved(boolean userMoved) { this.userMoved = userMoved; }
}
