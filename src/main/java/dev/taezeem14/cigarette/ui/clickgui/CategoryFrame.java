package dev.taezeem14.cigarette.ui.clickgui;

import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;

import java.util.ArrayList;
import java.util.List;

public class CategoryFrame {

    private static final float MAX_CONTENT_HEIGHT = 260.0f;

    private final Category category;
    private final List<ModuleButton> buttons = new ArrayList<>();

    private float x;
    private float y;
    private float width = 116.0f;
    private float headerHeight = 23.0f;

    private boolean collapsed = false;
    private boolean dragging = false;
    private boolean userMoved = false;
    private float dragX = 0.0f;
    private float dragY = 0.0f;
    private float collapseAnimation = 1.0f;
    private float scrollOffset = 0.0f;
    private float targetScrollOffset = 0.0f;

    public CategoryFrame(Category category, List<Module> modules, float x, float y) {
        this.category = category;
        this.x = x;
        this.y = y;

        for (Module module : modules) {
            buttons.add(new ModuleButton(module, x + 3.0f, 0, width - 6.0f));
        }
    }

    public void render(float mouseX, float mouseY, float partialTicks, String filter) {
        render(mouseX, mouseY, partialTicks, filter, 1.0f, 10000.0f, 10000.0f);
    }

    public void render(float mouseX, float mouseY, float partialTicks, String filter, float guiScale, float viewportW, float viewportH) {
        if (dragging) {
            this.x = AnimationUtil.clamp(mouseX - dragX, 4.0f, Math.max(4.0f, viewportW - width - 4.0f));
            this.y = AnimationUtil.clamp(mouseY - dragY, 40.0f, Math.max(40.0f, viewportH - headerHeight - 4.0f));
        } else if (viewportW > 0.0f && viewportH > 0.0f) {
            this.x = AnimationUtil.clamp(this.x, 4.0f, Math.max(4.0f, viewportW - width - 4.0f));
            this.y = AnimationUtil.clamp(this.y, 40.0f, Math.max(40.0f, viewportH - headerHeight - 4.0f));
        }

        float targetAnim = collapsed ? 0.0f : 1.0f;
        collapseAnimation = AnimationUtil.lerp(collapseAnimation, targetAnim, 15.0f);

        float totalContentHeight = calculateContentHeight(filter);
        float maxScroll = Math.max(0.0f, totalContentHeight - MAX_CONTENT_HEIGHT);
        targetScrollOffset = AnimationUtil.clamp(targetScrollOffset, 0.0f, maxScroll);
        scrollOffset = AnimationUtil.lerp(scrollOffset, targetScrollOffset, 18.0f);

        float cappedContentHeight = Math.min(MAX_CONTENT_HEIGHT, totalContentHeight);
        float visibleContentHeight = cappedContentHeight * AnimationUtil.easeOutCubic(collapseAnimation);
        float frameHeight = headerHeight + (collapseAnimation > 0.01f ? visibleContentHeight + 4.0f : 0.0f);
        int accent = ColorUtil.getAccent();

        // Frame drop shadow & background
        RenderUtil.drawDropShadow(x, y, width, frameHeight, 5.0f, 0x60000000);
        RenderUtil.drawRoundedRect(x, y, width, frameHeight, 4.0f, ColorUtil.BACKGROUND);
        RenderUtil.drawRoundedOutline(x, y, width, frameHeight, 4.0f, 1.0f, ColorUtil.BORDER);

        // Header with rounded top corners & glowing accent underline
        if (collapseAnimation > 0.05f) {
            RenderUtil.drawRoundedRectTop(x, y, width, headerHeight, 4.0f, ColorUtil.HEADER);
        } else {
            RenderUtil.drawRoundedRect(x, y, width, headerHeight, 4.0f, ColorUtil.HEADER);
        }
        RenderUtil.drawRect(x + 1.0f, y + headerHeight - 2.0f, width - 2.0f, 1.0f, ColorUtil.modifyAlpha(accent, 45));
        RenderUtil.drawRect(x + 1.0f, y + headerHeight - 1.0f, width - 2.0f, 1.0f, ColorUtil.modifyAlpha(accent, 180));

        // Category Title
        FontUtil.bold.drawString(category.getDisplayName(), x + 8.0f, y + (headerHeight - FontUtil.bold.getHeight()) / 2.0f, ColorUtil.TEXT_PRIMARY);

        // Enabled count badge (e.g. 2/9) + collapse indicator
        int enabledCount = 0;
        for (ModuleButton btn : buttons) {
            if (btn.getModule().isEnabled()) {
                enabledCount++;
            }
        }
        String countText = enabledCount + "/" + buttons.size();
        float countW = FontUtil.small.getStringWidth(countText) + 6.0f;
        float countH = 10.5f;
        String indicator = collapsed ? "+" : "-";
        float indW = FontUtil.regular.getStringWidth(indicator);
        float indX = x + width - indW - 7.0f;
        float countX = indX - countW - 4.0f;
        float countY = y + (headerHeight - countH) / 2.0f;

        RenderUtil.drawRoundedRect(countX, countY, countW, countH, 2.5f,
                enabledCount > 0 ? ColorUtil.modifyAlpha(accent, 40) : 0xFF22222A);
        FontUtil.small.drawCenteredString(countText, countX + countW / 2.0f,
                countY + (countH - FontUtil.small.getHeight()) / 2.0f,
                enabledCount > 0 ? accent : ColorUtil.TEXT_MUTED);

        FontUtil.regular.drawString(indicator, indX, y + (headerHeight - FontUtil.regular.getHeight()) / 2.0f, ColorUtil.TEXT_MUTED);

        // Scrollable Module Buttons clipped by scale-aware scissor stack
        if (collapseAnimation > 0.01f && totalContentHeight > 0.0f) {
            float clipH = visibleContentHeight + 3.0f;
            boolean hasScrollbar = totalContentHeight > MAX_CONTENT_HEIGHT + 1.0f;
            float buttonWidth = hasScrollbar ? width - 9.0f : width - 6.0f;

            RenderUtil.pushScissor(x, y + headerHeight, width, clipH, guiScale);
            try {
                float btnY = y + headerHeight + 3.0f - scrollOffset;
                for (ModuleButton btn : buttons) {
                    if (matchesFilter(btn.getModule(), filter)) {
                        btn.setX(x + 3.0f);
                        btn.setY(btnY);
                        btn.setWidth(buttonWidth);
                        btn.render(mouseX, mouseY, partialTicks, guiScale);
                        btnY += btn.getTotalHeight() + 2.0f;
                    }
                }

                // Subtle scrollbar
                if (hasScrollbar && collapseAnimation > 0.5f) {
                    float trackX = x + width - 4.0f;
                    float trackY = y + headerHeight + 3.0f;
                    float trackH = visibleContentHeight - 3.0f;
                    RenderUtil.drawRoundedRect(trackX, trackY, 2.0f, trackH, 1.0f, 0xFF202027);

                    float thumbH = Math.max(18.0f, trackH * (cappedContentHeight / totalContentHeight));
                    float scrollRatio = maxScroll > 0.0f ? (scrollOffset / maxScroll) : 0.0f;
                    float thumbY = trackY + (trackH - thumbH) * scrollRatio;
                    RenderUtil.drawRoundedRect(trackX, thumbY, 2.0f, thumbH, 1.0f, ColorUtil.modifyAlpha(accent, 190));
                }
            } finally {
                RenderUtil.popScissor();
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

        if (!collapsed && collapseAnimation > 0.5f && isContentHovered(mouseX, mouseY, filter)) {
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
        this.dragging = false;
        if (!collapsed) {
            for (ModuleButton btn : buttons) {
                btn.mouseReleased(mouseX, mouseY, mouseButton);
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (!collapsed) {
            for (ModuleButton btn : buttons) {
                btn.keyTyped(typedChar, keyCode);
            }
        }
    }

    public boolean handleScroll(float mouseX, float mouseY, int dWheel, String filter) {
        if (collapsed || collapseAnimation < 0.5f || dWheel == 0) {
            return false;
        }
        if (!isContentHovered(mouseX, mouseY, filter)) {
            return false;
        }
        float totalContentHeight = calculateContentHeight(filter);
        float maxScroll = Math.max(0.0f, totalContentHeight - MAX_CONTENT_HEIGHT);
        if (maxScroll <= 0.0f) {
            return false;
        }
        float step = (dWheel > 0 ? -20.0f : 20.0f);
        targetScrollOffset = AnimationUtil.clamp(targetScrollOffset + step, 0.0f, maxScroll);
        return true;
    }

    public Module getHoveredModule(float mouseX, float mouseY, String filter) {
        if (collapsed || collapseAnimation < 0.5f || !isContentHovered(mouseX, mouseY, filter)) {
            return null;
        }
        for (ModuleButton btn : buttons) {
            if (matchesFilter(btn.getModule(), filter) && btn.isHovered(mouseX, mouseY)) {
                return btn.getModule();
            }
        }
        return null;
    }

    private boolean isContentHovered(float mouseX, float mouseY, String filter) {
        float visibleH = Math.min(MAX_CONTENT_HEIGHT, calculateContentHeight(filter)) + 4.0f;
        return mouseX >= x && mouseX <= x + width && mouseY >= y + headerHeight && mouseY <= y + headerHeight + visibleH;
    }

    private float calculateContentHeight(String filter) {
        float h = 2.0f;
        for (ModuleButton btn : buttons) {
            if (matchesFilter(btn.getModule(), filter)) {
                h += btn.getTotalHeight() + 2.0f;
            }
        }
        return h;
    }

    private boolean matchesFilter(Module module, String filter) {
        if (filter == null || filter.trim().isEmpty()) return true;
        String q = filter.toLowerCase().trim();
        return module.getName().toLowerCase().contains(q)
                || (module.getDescription() != null && module.getDescription().toLowerCase().contains(q));
    }

    public boolean isHeaderHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight;
    }

    public boolean hasUserMoved() {
        return userMoved;
    }

    public Category getCategory() {
        return category;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public List<ModuleButton> getButtons() {
        return buttons;
    }
}
