package dev.taezeem14.cigarette.ui.clickgui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.Setting;
import dev.taezeem14.cigarette.setting.impl.*;
import dev.taezeem14.cigarette.ui.clickgui.component.*;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public class ModuleButton {

    private final Module module;
    private final List<Component> components = new ArrayList<>();

    private float x;
    private float y;
    private float width;
    private final float height = 18.0F;

    private boolean expanded = false;
    private boolean binding = false;
    private float toggleAnimation;
    private float expandAnimation = 0.0F;
    private float hoverAnimation = 0.0F;

    public ModuleButton(Module module, float x, float y, float width) {
        this.module = module;
        this.x = x;
        this.y = y;
        this.width = width;
        this.toggleAnimation = module.isEnabled() ? 1.0F : 0.0F;

        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting s) {
                components.add(new CheckboxComponent(s, x, 0, width, 15.0F));
            } else if (setting instanceof NumberSetting s) {
                components.add(new SliderComponent(s, x, 0, width, 17.0F));
            } else if (setting instanceof DoubleSliderSetting s) {
                components.add(new DoubleSliderComponent(s, x, 0, width, 17.0F));
            } else if (setting instanceof ModeSetting s) {
                components.add(new ModeComponent(s, x, 0, width, 16.0F));
            } else if (setting instanceof ColorSetting s) {
                components.add(new ColorPickerComponent(s, x, 0, width, 14.0F));
            } else if (setting instanceof KeybindSetting s) {
                components.add(new KeybindComponent(s, x, 0, width, 15.0F));
            }
        }
    }

    public void render(GuiGraphicsExtractor g, float mouseX, float mouseY, float partialTicks, float guiScale) {
        toggleAnimation = AnimationUtil.lerp(toggleAnimation, module.isEnabled() ? 1.0F : 0.0F, 14.0F);
        expandAnimation = AnimationUtil.lerp(expandAnimation, expanded ? 1.0F : 0.0F, 15.0F);
        boolean hovered = isHovered(mouseX, mouseY);
        hoverAnimation = AnimationUtil.lerp(hoverAnimation, hovered ? 1.0F : 0.0F, 16.0F);

        int accent = ColorUtil.getAccent();
        int activeBg = ColorUtil.interpolateColor(0xFF17171B, ColorUtil.modifyAlpha(accent, 42), toggleAnimation);
        int bg = ColorUtil.interpolateColor(activeBg, 0xFF23232B, hoverAnimation * 0.6F);

        RenderUtil.drawRoundedRect(g, x, y, width, height, 3.0F, bg);
        if (toggleAnimation > 0.05F) {
            RenderUtil.drawRoundedOutline(g, x, y, width, height, 3.0F, 1.0F,
                    ColorUtil.modifyAlpha(accent, (int) (90 * toggleAnimation)));
        }

        if (toggleAnimation > 0.02F) {
            float pillH = (height - 6.0F) * AnimationUtil.easeOutCubic(toggleAnimation);
            float pillY = y + (height - pillH) / 2.0F;
            RenderUtil.drawRoundedRect(g, x + 2.0F, pillY - 0.5F, 3.0F, pillH + 1.0F, 1.5F,
                    ColorUtil.modifyAlpha(accent, (int) (75 * toggleAnimation)));
            RenderUtil.drawRoundedRect(g, x + 2.5F, pillY, 2.0F, pillH, 1.0F,
                    ColorUtil.modifyAlpha(accent, (int) (255 * toggleAnimation)));
        }

        float textX = x + 6.5F + (toggleAnimation * 2.0F);
        int textColor = ColorUtil.interpolateColor(ColorUtil.TEXT_MUTED, ColorUtil.TEXT_PRIMARY,
                Math.max(toggleAnimation, hoverAnimation * 0.85F));
        RenderUtil.drawText(g, module.getName(), textX, y + (height - RenderUtil.getFontHeight()) / 2.0F, textColor, false);

        float rightOffset = components.isEmpty() ? 5.0F : 14.0F;
        if (binding) {
            String bindText = "...";
            RenderUtil.drawSmallText(g, bindText, x + width - RenderUtil.getSmallStringWidth(bindText) - rightOffset,
                    y + (height - RenderUtil.getSmallFontHeight()) / 2.0F, ColorUtil.EMBER_ORANGE, false);
        } else if (module.getKeyCode() > 0) {
            String tag = "[" + KeybindSetting.formatKeyCode(module.getKeyCode()) + "]";
            RenderUtil.drawSmallText(g, tag, x + width - RenderUtil.getSmallStringWidth(tag) - rightOffset,
                    y + (height - RenderUtil.getSmallFontHeight()) / 2.0F, 0x77EBEBEF, false);
        }

        if (!components.isEmpty()) {
            String arrow = expanded ? "v" : ">";
            RenderUtil.drawSmallText(g, arrow, x + width - 9.0F,
                    y + (height - RenderUtil.getSmallFontHeight()) / 2.0F, expanded ? accent : 0x99EBEBEF, false);
        }

        float totalDrawerHeight = getDrawerHeight();
        if (expandAnimation > 0.01F && totalDrawerHeight > 0.0F) {
            float visibleDrawerH = totalDrawerHeight * AnimationUtil.easeOutCubic(expandAnimation);
            float drawerY = y + height + 1.0F;
            RenderUtil.pushScissor(g, x, drawerY, width, visibleDrawerH, guiScale);
            try {
                RenderUtil.drawRoundedRect(g, x + 1.0F, drawerY, width - 2.0F, totalDrawerHeight, 2.5F, 0xFF131317);
                RenderUtil.drawRoundedOutline(g, x + 1.0F, drawerY, width - 2.0F, totalDrawerHeight, 2.5F, 1.0F, ColorUtil.BORDER);

                float compY = drawerY + 2.0F;
                for (Component comp : components) {
                    if (!comp.isVisible()) continue;
                    comp.setX(x + 3.0F);
                    comp.setY(compY);
                    comp.setWidth(width - 6.0F);
                    comp.render(g, mouseX, mouseY, partialTicks);
                    compY += comp.getHeight();
                }
            } finally {
                RenderUtil.popScissor(g);
            }
        }
    }

    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY)) {
            if (mouseButton == 0) {
                module.toggle();
            } else if (mouseButton == 1 && !components.isEmpty()) {
                expanded = !expanded;
            } else if (mouseButton == 2) {
                binding = !binding;
            }
            return;
        }
        if (expanded && expandAnimation > 0.5F) {
            for (Component c : components) {
                if (c.isVisible()) c.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    public void mouseReleased(float mouseX, float mouseY, int mouseButton) {
        if (expanded) {
            for (Component c : components) {
                if (c.isVisible()) c.mouseReleased(mouseX, mouseY, mouseButton);
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (binding) {
            if (keyCode == InputConstants.KEY_ESCAPE || keyCode == InputConstants.KEY_DELETE || keyCode == InputConstants.KEY_BACKSPACE) {
                module.setKeyCode(0);
            } else {
                module.setKeyCode(keyCode);
            }
            binding = false;
            return;
        }
        if (expanded) {
            for (Component c : components) {
                if (c.isVisible()) c.keyTyped(typedChar, keyCode);
            }
        }
    }

    public boolean isListening() {
        if (binding) return true;
        for (Component c : components) {
            if (c instanceof KeybindComponent kc && kc.isListening()) return true;
        }
        return false;
    }

    public float getTotalHeight() {
        float total = height;
        if (expandAnimation > 0.01F) {
            total += getDrawerHeight() * AnimationUtil.easeOutCubic(expandAnimation) + 2.0F;
        }
        return total;
    }

    public float getDrawerHeight() {
        float h = 4.0F;
        for (Component c : components) {
            if (c.isVisible()) h += c.getHeight();
        }
        return h;
    }

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public Module getModule() { return module; }
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public void setWidth(float width) { this.width = width; }
}
