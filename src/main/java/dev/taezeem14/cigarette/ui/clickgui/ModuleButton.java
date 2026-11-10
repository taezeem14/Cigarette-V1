package dev.taezeem14.cigarette.ui.clickgui;

import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.Setting;
import dev.taezeem14.cigarette.setting.impl.*;
import dev.taezeem14.cigarette.ui.clickgui.component.*;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
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
    private float hoverAnimation = 0.0f;

    public ModuleButton(Module module, float x, float y, float width) {
        this.module = module;
        this.x = x;
        this.y = y;
        this.width = width;
        this.toggleAnimation = module.isEnabled() ? 1.0f : 0.0f;

        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting) {
                components.add(new CheckboxComponent((BooleanSetting) setting, x, 0, width, 15.0f));
            } else if (setting instanceof NumberSetting) {
                components.add(new SliderComponent((NumberSetting) setting, x, 0, width, 17.0f));
            } else if (setting instanceof DoubleSliderSetting) {
                components.add(new DoubleSliderComponent((DoubleSliderSetting) setting, x, 0, width, 17.0f));
            } else if (setting instanceof ModeSetting) {
                components.add(new ModeComponent((ModeSetting) setting, x, 0, width, 16.0f));
            } else if (setting instanceof ColorSetting) {
                components.add(new ColorPickerComponent((ColorSetting) setting, x, 0, width, 14.0f));
            } else if (setting instanceof KeybindSetting) {
                components.add(new KeybindComponent((KeybindSetting) setting, x, 0, width, 15.0f));
            }
        }
    }

    public void render(float mouseX, float mouseY, float partialTicks) {
        render(mouseX, mouseY, partialTicks, 1.0f);
    }

    public void render(float mouseX, float mouseY, float partialTicks, float guiScale) {
        float targetToggle = module.isEnabled() ? 1.0f : 0.0f;
        toggleAnimation = AnimationUtil.lerp(toggleAnimation, targetToggle, 14.0f);

        float targetExpand = expanded ? 1.0f : 0.0f;
        expandAnimation = AnimationUtil.lerp(expandAnimation, targetExpand, 15.0f);

        boolean hovered = isHovered(mouseX, mouseY);
        hoverAnimation = AnimationUtil.lerp(hoverAnimation, hovered ? 1.0f : 0.0f, 16.0f);

        int accent = ColorUtil.getAccent();
        int baseBg = 0xFF17171B;
        int activeBg = ColorUtil.interpolateColor(baseBg, ColorUtil.modifyAlpha(accent, 42), toggleAnimation);
        int currentBg = ColorUtil.interpolateColor(activeBg, 0xFF23232B, hoverAnimation * 0.6f);

        RenderUtil.drawRoundedRect(x, y, width, height, 3.0f, currentBg);
        if (toggleAnimation > 0.05f) {
            RenderUtil.drawRoundedOutline(x, y, width, height, 3.0f, 1.0f,
                    ColorUtil.modifyAlpha(accent, (int) (90 * toggleAnimation)));
        }

        // Left glowing 2px vertical accent pill on enabled modules
        if (toggleAnimation > 0.02f) {
            float pillH = (height - 6.0f) * AnimationUtil.easeOutCubic(toggleAnimation);
            float pillY = y + (height - pillH) / 2.0f;
            RenderUtil.drawRoundedRect(x + 2.0f, pillY - 0.5f, 3.0f, pillH + 1.0f, 1.5f,
                    ColorUtil.modifyAlpha(accent, (int) (75 * toggleAnimation)));
            RenderUtil.drawRoundedRect(x + 2.5f, pillY, 2.0f, pillH, 1.0f,
                    ColorUtil.modifyAlpha(accent, (int) (255 * toggleAnimation)));
        }

        // Module Name (bright & bold when enabled)
        float textX = x + 6.5f + (toggleAnimation * 2.0f);
        int textColor = ColorUtil.interpolateColor(ColorUtil.TEXT_MUTED, ColorUtil.TEXT_PRIMARY,
                Math.max(toggleAnimation, hoverAnimation * 0.85f));
        if (toggleAnimation > 0.5f) {
            FontUtil.bold.drawString(module.getName(), textX, y + (height - FontUtil.bold.getHeight()) / 2.0f, textColor);
        } else {
            FontUtil.regular.drawString(module.getName(), textX, y + (height - FontUtil.regular.getHeight()) / 2.0f, textColor);
        }

        // Binding state or Keybind indicator
        float rightOffset = components.isEmpty() ? 5.0f : 14.0f;
        if (binding) {
            String bindText = "...";
            FontUtil.small.drawString(bindText, x + width - FontUtil.small.getStringWidth(bindText) - rightOffset,
                    y + (height - FontUtil.small.getHeight()) / 2.0f, ColorUtil.EMBER_ORANGE);
        } else if (module.getKeyCode() > Keyboard.KEY_NONE && module.getKeyCode() < 256) {
            String keyName = Keyboard.getKeyName(module.getKeyCode());
            if (keyName != null && !keyName.isEmpty()) {
                String tag = "[" + keyName + "]";
                FontUtil.small.drawString(tag, x + width - FontUtil.small.getStringWidth(tag) - rightOffset,
                        y + (height - FontUtil.small.getHeight()) / 2.0f, 0x77EBEBEF);
            }
        }

        // Expand chevron indicator
        if (!components.isEmpty()) {
            String arrow = expanded ? "v" : ">";
            FontUtil.small.drawString(arrow, x + width - 9.0f,
                    y + (height - FontUtil.small.getHeight()) / 2.0f,
                    expanded ? accent : 0x99EBEBEF);
        }

        // Expanded settings drawer with scale-aware nested scissor clipping
        float totalDrawerHeight = getDrawerHeight();
        if (expandAnimation > 0.01f && totalDrawerHeight > 0.0f) {
            float visibleDrawerHeight = totalDrawerHeight * AnimationUtil.easeOutCubic(expandAnimation);
            float drawerY = y + height + 1.0f;

            RenderUtil.pushScissor(x, drawerY, width, visibleDrawerHeight, guiScale);
            try {
                RenderUtil.drawRoundedRect(x + 1.0f, drawerY, width - 2.0f, totalDrawerHeight, 2.5f, 0xFF131317);
                RenderUtil.drawRoundedOutline(x + 1.0f, drawerY, width - 2.0f, totalDrawerHeight, 2.5f, 1.0f, ColorUtil.BORDER);

                float compY = drawerY + 2.0f;
                for (Component component : components) {
                    if (!component.isVisible()) continue;

                    component.setX(x + 3.0f);
                    component.setY(compY);
                    component.setWidth(width - 6.0f);
                    component.render(mouseX, mouseY, partialTicks);

                    compY += component.getHeight();
                }
            } finally {
                RenderUtil.popScissor();
            }
        }
    }

    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY)) {
            if (mouseButton == 0) {
                module.toggle();
            } else if (mouseButton == 1) {
                if (!components.isEmpty()) {
                    expanded = !expanded;
                }
            } else if (mouseButton == 2) {
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
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK) {
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
            total += getDrawerHeight() * AnimationUtil.easeOutCubic(expandAnimation) + 2.0f;
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

    public boolean isExpanded() {
        return expanded;
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
