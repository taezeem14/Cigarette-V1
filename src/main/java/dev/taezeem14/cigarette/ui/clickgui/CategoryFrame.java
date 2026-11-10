package dev.taezeem14.cigarette.ui.clickgui;

import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;

import java.util.ArrayList;
import java.util.List;

public class CategoryFrame {

    private final Category category;
    private final List<ModuleButton> buttons = new ArrayList<>();

    private float x;
    private float y;
    private float width = 110.0f;
    private float headerHeight = 22.0f;

    private boolean collapsed = false;
    private boolean dragging = false;
    private float dragX = 0.0f;
    private float dragY = 0.0f;
    private float collapseAnimation = 1.0f;

    public CategoryFrame(Category category, List<Module> modules, float x, float y) {
        this.category = category;
        this.x = x;
        this.y = y;

        for (Module module : modules) {
            buttons.add(new ModuleButton(module, x + 3.0f, 0, width - 6.0f));
        }
    }

    public void render(float mouseX, float mouseY, float partialTicks, String filter) {
        // Drag handling
        if (dragging) {
            this.x = mouseX - dragX;
            this.y = mouseY - dragY;
        }

        // Collapse animation
        float targetAnim = collapsed ? 0.0f : 1.0f;
        collapseAnimation += (targetAnim - collapseAnimation) * 0.2f;

        float totalContentHeight = calculateContentHeight(filter);
        float visibleContentHeight = totalContentHeight * collapseAnimation;
        float frameHeight = headerHeight + (collapseAnimation > 0.01f ? visibleContentHeight + 4.0f : 0.0f);

        // Frame drop shadow & background
        RenderUtil.drawDropShadow(x, y, width, frameHeight, 4.0f, 0x55000000);
        RenderUtil.drawRoundedRect(x, y, width, frameHeight, 4.0f, ColorUtil.BACKGROUND);
        RenderUtil.drawRoundedOutline(x, y, width, frameHeight, 4.0f, 1.0f, 0xFF222228);

        // Header
        RenderUtil.drawRoundedRect(x, y, width, headerHeight, 4.0f, ColorUtil.HEADER);
        RenderUtil.drawRect(x, y + headerHeight - 1.0f, width, 1.0f, 0xFF26262D);

        // Category Title
        FontUtil.bold.drawString(category.getDisplayName(), x + 8.0f, y + (headerHeight - FontUtil.bold.getHeight()) / 2.0f, ColorUtil.TEXT_PRIMARY);

        // Module count / collapse icon
        String indicator = collapsed ? "+" : "-";
        FontUtil.regular.drawString(indicator, x + width - FontUtil.regular.getStringWidth(indicator) - 8.0f, y + (headerHeight - FontUtil.regular.getHeight()) / 2.0f, ColorUtil.TEXT_MUTED);

        // Render Buttons
        if (collapseAnimation > 0.01f && totalContentHeight > 0.0f) {
            RenderUtil.startScissor(x, y + headerHeight, width, visibleContentHeight + 4.0f);

            float btnY = y + headerHeight + 3.0f;
            for (ModuleButton btn : buttons) {
                if (matchesFilter(btn.getModule(), filter)) {
                    btn.setX(x + 3.0f);
                    btn.setY(btnY);
                    btn.setWidth(width - 6.0f);
                    btn.render(mouseX, mouseY, partialTicks);
                    btnY += btn.getTotalHeight() + 2.0f;
                }
            }

            RenderUtil.stopScissor();
        }
    }

    public void mouseClicked(float mouseX, float mouseY, int mouseButton, String filter) {
        if (isHeaderHovered(mouseX, mouseY)) {
            if (mouseButton == 0) {
                dragging = true;
                dragX = mouseX - x;
                dragY = mouseY - y;
            } else if (mouseButton == 1) {
                collapsed = !collapsed;
            }
            return;
        }

        if (!collapsed && collapseAnimation > 0.5f) {
            for (ModuleButton btn : buttons) {
                if (matchesFilter(btn.getModule(), filter)) {
                    btn.mouseClicked(mouseX, mouseY, mouseButton);
                }
            }
        }
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

    private float calculateContentHeight(String filter) {
        float h = 0.0f;
        for (ModuleButton btn : buttons) {
            if (matchesFilter(btn.getModule(), filter)) {
                h += btn.getTotalHeight() + 2.0f;
            }
        }
        return h;
    }

    private boolean matchesFilter(Module module, String filter) {
        if (filter == null || filter.trim().isEmpty()) return true;
        return module.getName().toLowerCase().contains(filter.toLowerCase().trim());
    }

    public boolean isHeaderHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight;
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
