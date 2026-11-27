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

    private static final float BUILTIN_ROW_HEIGHT = 15.0f;

    private final Module module;
    private final List<Component> components = new ArrayList<>();

    private float x;
    private float y;
    private float width;
    private float height = 18.5f;

    private boolean expanded = false;
    private boolean binding = false;
    private float toggleAnimation = 0.0f;
    private float expandAnimation = 0.0f;
    private float hoverAnimation = 0.0f;
    private float showInListAnim = 1.0f;

    public ModuleButton(Module module, float x, float y, float width) {
        this.module = module;
        this.x = x;
        this.y = y;
        this.width = width;
        this.toggleAnimation = module.isEnabled() ? 1.0f : 0.0f;
        this.showInListAnim = module.isHidden() ? 0.0f : 1.0f;

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
        int baseBg = 0xFF17171C;
        int activeBg = ColorUtil.interpolateColor(baseBg, ColorUtil.modifyAlpha(accent, 44), toggleAnimation);
        int currentBg = ColorUtil.interpolateColor(activeBg, 0xFF23232C, hoverAnimation * 0.6f);

        RenderUtil.drawRoundedRect(x, y, width, height, 3.0f, currentBg);
        if (toggleAnimation > 0.05f) {
            RenderUtil.drawRoundedOutline(x, y, width, height, 3.0f, 1.0f,
                    ColorUtil.modifyAlpha(accent, (int) (95 * toggleAnimation)));
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
        float textX = x + 7.0f + (toggleAnimation * 2.0f);
        int textColor = ColorUtil.interpolateColor(ColorUtil.TEXT_MUTED, ColorUtil.TEXT_PRIMARY,
                Math.max(toggleAnimation, hoverAnimation * 0.85f));
        if (toggleAnimation > 0.5f) {
            FontUtil.bold.drawString(module.getName(), textX, y + (height - FontUtil.bold.getHeight()) / 2.0f, textColor);
        } else {
            FontUtil.regular.drawString(module.getName(), textX, y + (height - FontUtil.regular.getHeight()) / 2.0f, textColor);
        }

        // Binding state or Keybind indicator pill
        float rightOffset = 15.0f;
        if (binding) {
            String bindText = "[Press key...]";
            FontUtil.small.drawString(bindText, x + width - FontUtil.small.getStringWidth(bindText) - rightOffset,
                    y + (height - FontUtil.small.getHeight()) / 2.0f, ColorUtil.EMBER_ORANGE);
        } else if (module.getKeyCode() > Keyboard.KEY_NONE && module.getKeyCode() < 256) {
            String keyName = Keyboard.getKeyName(module.getKeyCode());
            if (keyName != null && !keyName.isEmpty()) {
                String tag = "[" + keyName + "]";
                FontUtil.small.drawString(tag, x + width - FontUtil.small.getStringWidth(tag) - rightOffset,
                        y + (height - FontUtil.small.getHeight()) / 2.0f, 0x88EBEBEF);
            }
        }

        // Three vertical dots icon (always available — opens settings, keybind & ArrayList visibility)
        float dotX = x + width - 8.0f;
        float dotY = y + height / 2.0f;
        int dotCol = expanded ? accent : (hovered ? 0xFFFFFFFF : 0x77EBEBEF);
        RenderUtil.drawCircle(dotX, dotY - 3.8f, 1.15f, dotCol);
        RenderUtil.drawCircle(dotX, dotY, 1.15f, dotCol);
        RenderUtil.drawCircle(dotX, dotY + 3.8f, 1.15f, dotCol);

        // Expanded settings drawer with scale-aware nested scissor clipping
        float totalDrawerHeight = getDrawerHeight();
        if (expandAnimation > 0.01f && totalDrawerHeight > 0.0f) {
            float visibleDrawerHeight = totalDrawerHeight * AnimationUtil.easeOutCubic(expandAnimation);
            float drawerY = y + height + 1.0f;

            RenderUtil.pushScissor(x, drawerY, width, visibleDrawerHeight, guiScale);
            try {
                RenderUtil.drawRoundedRect(x + 1.0f, drawerY, width - 2.0f, totalDrawerHeight, 2.5f, 0xFF121216);
                RenderUtil.drawRoundedOutline(x + 1.0f, drawerY, width - 2.0f, totalDrawerHeight, 2.5f, 1.0f, ColorUtil.BORDER);

                float compY = drawerY + 2.0f;
                boolean hadSettings = false;
                for (Component component : components) {
                    if (!component.isVisible()) continue;
                    hadSettings = true;

                    component.setX(x + 3.0f);
                    component.setY(compY);
                    component.setWidth(width - 6.0f);
                    component.render(mouseX, mouseY, partialTicks);

                    compY += component.getHeight();
                }

                if (hadSettings) {
                    RenderUtil.drawRect(x + 6.0f, compY + 1.0f, width - 12.0f, 1.0f, 0xFF202028);
                    compY += 3.0f;
                }

                // Built-in Row 1: Keybind capture (left-click friendly for laptops!)
                renderKeybindRow(x + 3.0f, compY, width - 6.0f, mouseX, mouseY, accent);
                compY += BUILTIN_ROW_HEIGHT;

                // Built-in Row 2: Show in HUD ArrayList toggle
                renderArrayListToggleRow(x + 3.0f, compY, width - 6.0f, mouseX, mouseY, accent);
            } finally {
                RenderUtil.popScissor();
            }
        }
    }

    private void renderKeybindRow(float rx, float ry, float rw, float mouseX, float mouseY, int accent) {
        boolean hov = mouseX >= rx && mouseX <= rx + rw && mouseY >= ry && mouseY <= ry + BUILTIN_ROW_HEIGHT;
        if (hov) {
            RenderUtil.drawRect(rx, ry, rw, BUILTIN_ROW_HEIGHT, 0x14FFFFFF);
        }
        FontUtil.small.drawString("Keybind", rx + 6.0f, ry + (BUILTIN_ROW_HEIGHT - FontUtil.small.getHeight()) / 2.0f,
                hov || binding ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED);

        String keyStr = "NONE";
        if (binding) {
            keyStr = "Press key...";
        } else if (module.getKeyCode() > Keyboard.KEY_NONE && module.getKeyCode() < 256) {
            String kn = Keyboard.getKeyName(module.getKeyCode());
            if (kn != null && !kn.isEmpty()) keyStr = kn;
        }
        float badgeW = FontUtil.small.getStringWidth(keyStr) + 10.0f;
        float badgeH = BUILTIN_ROW_HEIGHT - 3.5f;
        float badgeX = rx + rw - badgeW - 6.0f;
        float badgeY = ry + (BUILTIN_ROW_HEIGHT - badgeH) / 2.0f;

        int pillBg = binding ? ColorUtil.modifyAlpha(ColorUtil.EMBER_ORANGE, 55) : (hov ? 0xFF262630 : 0xFF1E1E25);
        int pillBorder = binding ? ColorUtil.EMBER_ORANGE : (hov ? ColorUtil.modifyAlpha(accent, 180) : ColorUtil.BORDER);
        int pillFg = binding ? ColorUtil.EMBER_ORANGE : ("NONE".equals(keyStr) ? ColorUtil.TEXT_MUTED : accent);

        RenderUtil.drawRoundedRect(badgeX, badgeY, badgeW, badgeH, 2.5f, pillBg);
        RenderUtil.drawRoundedOutline(badgeX, badgeY, badgeW, badgeH, 2.5f, 1.0f, pillBorder);
        FontUtil.small.drawCenteredString(keyStr, badgeX + badgeW / 2.0f, badgeY + (badgeH - FontUtil.small.getHeight()) / 2.0f, pillFg);
    }

    private void renderArrayListToggleRow(float rx, float ry, float rw, float mouseX, float mouseY, int accent) {
        boolean shown = !module.isHidden();
        showInListAnim = AnimationUtil.lerp(showInListAnim, shown ? 1.0f : 0.0f, 16.0f);
        boolean hov = mouseX >= rx && mouseX <= rx + rw && mouseY >= ry && mouseY <= ry + BUILTIN_ROW_HEIGHT;
        if (hov) {
            RenderUtil.drawRect(rx, ry, rw, BUILTIN_ROW_HEIGHT, 0x14FFFFFF);
        }

        int textColor = ColorUtil.interpolateColor(ColorUtil.TEXT_MUTED, ColorUtil.TEXT_PRIMARY, Math.max(showInListAnim, hov ? 0.85f : 0.0f));
        FontUtil.small.drawString("Show in ArrayList", rx + 6.0f, ry + (BUILTIN_ROW_HEIGHT - FontUtil.small.getHeight()) / 2.0f, textColor);

        float switchW = 19.0f;
        float switchH = 9.5f;
        float switchX = rx + rw - switchW - 6.0f;
        float switchY = ry + (BUILTIN_ROW_HEIGHT - switchH) / 2.0f;

        int trackCol = ColorUtil.interpolateColor(0xFF26262E, accent, showInListAnim);
        RenderUtil.drawRoundedRect(switchX, switchY, switchW, switchH, switchH / 2.0f, trackCol);
        float minThumb = switchX + switchH / 2.0f;
        float maxThumb = switchX + switchW - switchH / 2.0f;
        float thumbX = minThumb + (maxThumb - minThumb) * AnimationUtil.easeOutCubic(showInListAnim);
        RenderUtil.drawCircle(thumbX, switchY + switchH / 2.0f, 3.2f, 0xFFF5F5F7);
    }

    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY)) {
            boolean onDots = (mouseX >= x + width - 18.0f);
            if (mouseButton == 1 || (mouseButton == 0 && onDots)) {
                expanded = !expanded;
            } else if (mouseButton == 0) {
                module.toggle();
            } else if (mouseButton == 2) {
                binding = !binding;
            }
            return;
        }

        if (expanded && expandAnimation > 0.5f) {
            float compY = y + height + 3.0f;
            boolean hadSettings = false;
            for (Component component : components) {
                if (component.isVisible()) {
                    hadSettings = true;
                    component.mouseClicked(mouseX, mouseY, mouseButton);
                    compY += component.getHeight();
                }
            }
            if (hadSettings) {
                compY += 3.0f;
            }

            float rx = x + 3.0f;
            float rw = width - 6.0f;
            if (mouseX >= rx && mouseX <= rx + rw && mouseY >= compY && mouseY <= compY + BUILTIN_ROW_HEIGHT) {
                if (mouseButton == 0) {
                    binding = !binding;
                } else if (mouseButton == 1) {
                    module.setKeyCode(Keyboard.KEY_NONE);
                    binding = false;
                }
                return;
            }
            compY += BUILTIN_ROW_HEIGHT;
            if (mouseX >= rx && mouseX <= rx + rw && mouseY >= compY && mouseY <= compY + BUILTIN_ROW_HEIGHT && mouseButton == 0) {
                module.setHidden(!module.isHidden());
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
        boolean hadSettings = false;
        for (Component component : components) {
            if (component.isVisible()) {
                hadSettings = true;
                h += component.getHeight();
            }
        }
        if (hadSettings) {
            h += 3.0f;
        }
        h += BUILTIN_ROW_HEIGHT * 2.0f;
        return h;
    }

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isListening() {
        if (binding) {
            return true;
        }
        for (Component component : components) {
            if (component instanceof KeybindComponent && ((KeybindComponent) component).isListening()) {
                return true;
            }
        }
        return false;
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
