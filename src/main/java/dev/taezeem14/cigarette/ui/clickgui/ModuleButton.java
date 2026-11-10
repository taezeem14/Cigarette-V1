package dev.taezeem14.cigarette.ui.clickgui;

import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.Setting;
import dev.taezeem14.cigarette.setting.impl.*;
import dev.taezeem14.cigarette.ui.clickgui.component.*;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public class ModuleButton {

    private final Module module;
    private final List<Component> components = new ArrayList<>();

    private float x;
    private float y;
    private float width;
    private float height = 18.0f;

    private boolean expanded = false;
    private boolean binding = false;
    private float toggleAnimation = 0.0f;
    private float expandAnimation = 0.0f;

    public ModuleButton(Module module, float x, float y, float width) {
        this.module = module;
        this.x = x;
        this.y = y;
        this.width = width;
        this.toggleAnimation = module.isEnabled() ? 1.0f : 0.0f;

        // Initialize components for all module settings
        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting) {
                components.add(new CheckboxComponent((BooleanSetting) setting, x, 0, width, 14.0f));
            } else if (setting instanceof NumberSetting) {
                components.add(new SliderComponent((NumberSetting) setting, x, 0, width, 16.0f));
            } else if (setting instanceof DoubleSliderSetting) {
                components.add(new DoubleSliderComponent((DoubleSliderSetting) setting, x, 0, width, 16.0f));
            } else if (setting instanceof ModeSetting) {
                components.add(new ModeComponent((ModeSetting) setting, x, 0, width, 15.0f));
            } else if (setting instanceof ColorSetting) {
                components.add(new ColorPickerComponent((ColorSetting) setting, x, 0, width, 14.0f));
            } else if (setting instanceof KeybindSetting) {
                components.add(new KeybindComponent((KeybindSetting) setting, x, 0, width, 14.0f));
            }
        }
    }

    public void render(float mouseX, float mouseY, float partialTicks) {
        // Smooth toggle animation
        float targetToggle = module.isEnabled() ? 1.0f : 0.0f;
        toggleAnimation += (targetToggle - toggleAnimation) * 0.2f;

        // Smooth expand animation
        float targetExpand = expanded ? 1.0f : 0.0f;
        expandAnimation += (targetExpand - expandAnimation) * 0.2f;

        boolean hovered = isHovered(mouseX, mouseY);

        // Background color calculation
        int baseBg = 0xFF17171A;
        int activeBg = ColorUtil.modifyAlpha(ColorUtil.ACCENT, 200);
        int currentBg = ColorUtil.interpolateColor(baseBg, activeBg, toggleAnimation);

        if (hovered) {
            currentBg = ColorUtil.interpolateColor(currentBg, 0xFF2A2A32, 0.3f);
        }

        RenderUtil.drawRoundedRect(x, y, width, height, 2.5f, currentBg);

        // Module Name
        int textColor = ColorUtil.interpolateColor(ColorUtil.TEXT_MUTED, ColorUtil.TEXT_PRIMARY, Math.max(toggleAnimation, hovered ? 0.7f : 0.0f));
        FontUtil.regular.drawString(module.getName(), x + 6.0f, y + (height - FontUtil.regular.getHeight()) / 2.0f, textColor);

        // Binding state or Keybind indicator
        if (binding) {
            String bindText = "[ ... ]";
            FontUtil.small.drawString(bindText, x + width - FontUtil.small.getStringWidth(bindText) - 16.0f, y + (height - FontUtil.small.getHeight()) / 2.0f, ColorUtil.EMBER_ORANGE);
        } else if (module.getKeyCode() != Keyboard.KEY_NONE) {
            String keyName = "[" + Keyboard.getKeyName(module.getKeyCode()) + "]";
            FontUtil.small.drawString(keyName, x + width - FontUtil.small.getStringWidth(keyName) - 16.0f, y + (height - FontUtil.small.getHeight()) / 2.0f, 0x88FFFFFF);
        }

        // Expand chevron / indicator
        if (!components.isEmpty()) {
            String arrow = expanded ? "v" : ">";
            FontUtil.small.drawString(arrow, x + width - 10.0f, y + (height - FontUtil.small.getHeight()) / 2.0f, 0x99EBEBEF);
        }

        // Draw expanded setting drawer
        float totalDrawerHeight = getDrawerHeight();
        if (expandAnimation > 0.01f && totalDrawerHeight > 0) {
            float visibleDrawerHeight = totalDrawerHeight * expandAnimation;
            float drawerY = y + height;

            RenderUtil.startScissor(x, drawerY, width, visibleDrawerHeight);

            // Drawer background
            RenderUtil.drawRoundedRect(x + 2.0f, drawerY, width - 4.0f, totalDrawerHeight, 2.0f, 0xFF141417);
            RenderUtil.drawRoundedOutline(x + 2.0f, drawerY, width - 4.0f, totalDrawerHeight, 2.0f, 1.0f, 0xFF222228);

            float compY = drawerY + 2.0f;
            for (Component component : components) {
                if (!component.isVisible()) continue;

                component.setX(x + 4.0f);
                component.setY(compY);
                component.setWidth(width - 8.0f);
                component.render(mouseX, mouseY, partialTicks);

                compY += component.getHeight();
            }

            RenderUtil.stopScissor();
        }
    }

    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY)) {
            if (mouseButton == 0) {
                // Left click toggles module
                module.toggle();
            } else if (mouseButton == 1) {
                // Right click expands drawer
                if (!components.isEmpty()) {
                    expanded = !expanded;
                }
            } else if (mouseButton == 2) {
                // Middle click binds key
                binding = !binding;
            }
            return;
        }

        if (expanded && expandAnimation > 0.5f) {
            for (Component component : components) {
                if (component.isVisible()) {
                    component.mouseClicked(mouseX, mouseY, mouseButton);
                }
            }
        }
    }

    public void mouseReleased(float mouseX, float mouseY, int mouseButton) {
        if (expanded) {
            for (Component component : components) {
                if (component.isVisible()) {
                    component.mouseReleased(mouseX, mouseY, mouseButton);
                }
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (binding) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE) {
                module.setKeyCode(Keyboard.KEY_NONE);
            } else {
                module.setKeyCode(keyCode);
            }
            binding = false;
            return;
        }

        if (expanded) {
            for (Component component : components) {
                if (component.isVisible()) {
                    component.keyTyped(typedChar, keyCode);
                }
            }
        }
    }

    public float getTotalHeight() {
        float total = height;
        if (expandAnimation > 0.01f) {
            total += getDrawerHeight() * expandAnimation + 2.0f;
        }
        return total;
    }

    public float getDrawerHeight() {
        float h = 4.0f;
        for (Component component : components) {
            if (component.isVisible()) {
                h += component.getHeight();
            }
        }
        return h;
    }

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public Module getModule() {
        return module;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
