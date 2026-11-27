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

    private static final float BUILTIN_ROW_HEIGHT = 15.0F;

    private final Module module;
    private final List<Component> components = new ArrayList<>();

    private float x;
    private float y;
    private float width;
    private final float height = 18.5F;

    private boolean expanded = false;
    private boolean binding = false;
    private float toggleAnimation;
    private float expandAnimation = 0.0F;
    private float hoverAnimation = 0.0F;
    private float showInListAnim = 1.0F;

    public ModuleButton(Module module, float x, float y, float width) {
        this.module = module;
        this.x = x;
        this.y = y;
        this.width = width;
        this.toggleAnimation = module.isEnabled() ? 1.0F : 0.0F;
        this.showInListAnim = module.isHidden() ? 0.0F : 1.0F;

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
        int activeBg = ColorUtil.interpolateColor(0xFF17171C, ColorUtil.modifyAlpha(accent, 44), toggleAnimation);
        int bg = ColorUtil.interpolateColor(activeBg, 0xFF23232C, hoverAnimation * 0.6F);

        RenderUtil.drawRoundedRect(g, x, y, width, height, 3.0F, bg);
        if (toggleAnimation > 0.05F) {
            RenderUtil.drawRoundedOutline(g, x, y, width, height, 3.0F, 1.0F,
                    ColorUtil.modifyAlpha(accent, (int) (95 * toggleAnimation)));
        }

        if (toggleAnimation > 0.02F) {
            float pillH = (height - 6.0F) * AnimationUtil.easeOutCubic(toggleAnimation);
            float pillY = y + (height - pillH) / 2.0F;
            RenderUtil.drawRoundedRect(g, x + 2.0F, pillY - 0.5F, 3.0F, pillH + 1.0F, 1.5F,
                    ColorUtil.modifyAlpha(accent, (int) (75 * toggleAnimation)));
            RenderUtil.drawRoundedRect(g, x + 2.5F, pillY, 2.0F, pillH, 1.0F,
                    ColorUtil.modifyAlpha(accent, (int) (255 * toggleAnimation)));
        }

        float textX = x + 7.0F + (toggleAnimation * 2.0F);
        int textColor = ColorUtil.interpolateColor(ColorUtil.TEXT_MUTED, ColorUtil.TEXT_PRIMARY,
                Math.max(toggleAnimation, hoverAnimation * 0.85F));
        RenderUtil.drawText(g, module.getName(), textX, y + (height - RenderUtil.getFontHeight()) / 2.0F, textColor, false);

        float rightOffset = 15.0F;
        if (binding) {
            String bindText = "[Press key...]";
            RenderUtil.drawSmallText(g, bindText, x + width - RenderUtil.getSmallStringWidth(bindText) - rightOffset,
                    y + (height - RenderUtil.getSmallFontHeight()) / 2.0F, ColorUtil.EMBER_ORANGE, false);
        } else if (module.getKeyCode() > 0) {
            String tag = "[" + KeybindSetting.formatKeyCode(module.getKeyCode()) + "]";
            RenderUtil.drawSmallText(g, tag, x + width - RenderUtil.getSmallStringWidth(tag) - rightOffset,
                    y + (height - RenderUtil.getSmallFontHeight()) / 2.0F, 0x88EBEBEF, false);
        }

        // Three vertical dots icon (always available)
        float dotX = x + width - 8.0F;
        float dotY = y + height / 2.0F;
        int dotCol = expanded ? accent : (hovered ? 0xFFFFFFFF : 0x77EBEBEF);
        RenderUtil.drawCircle(g, dotX, dotY - 3.8F, 1.15F, dotCol);
        RenderUtil.drawCircle(g, dotX, dotY, 1.15F, dotCol);
        RenderUtil.drawCircle(g, dotX, dotY + 3.8F, 1.15F, dotCol);

        float totalDrawerHeight = getDrawerHeight();
        if (expandAnimation > 0.01F && totalDrawerHeight > 0.0F) {
            float visibleDrawerH = totalDrawerHeight * AnimationUtil.easeOutCubic(expandAnimation);
            float drawerY = y + height + 1.0F;
            RenderUtil.pushScissor(g, x, drawerY, width, visibleDrawerH, guiScale);
            try {
                RenderUtil.drawRoundedRect(g, x + 1.0F, drawerY, width - 2.0F, totalDrawerHeight, 2.5F, 0xFF121216);
                RenderUtil.drawRoundedOutline(g, x + 1.0F, drawerY, width - 2.0F, totalDrawerHeight, 2.5F, 1.0F, ColorUtil.BORDER);

                float compY = drawerY + 2.0F;
                boolean hadSettings = false;
                for (Component comp : components) {
                    if (!comp.isVisible()) continue;
                    hadSettings = true;
                    comp.setX(x + 3.0F);
                    comp.setY(compY);
                    comp.setWidth(width - 6.0F);
                    comp.render(g, mouseX, mouseY, partialTicks);
                    compY += comp.getHeight();
                }

                if (hadSettings) {
                    RenderUtil.drawRect(g, x + 6.0F, compY + 1.0F, width - 12.0F, 1.0F, 0xFF202028);
                    compY += 3.0F;
                }

                renderKeybindRow(g, x + 3.0F, compY, width - 6.0F, mouseX, mouseY, accent);
                compY += BUILTIN_ROW_HEIGHT;
                renderArrayListToggleRow(g, x + 3.0F, compY, width - 6.0F, mouseX, mouseY, accent);
            } finally {
                RenderUtil.popScissor(g);
            }
        }
    }

    private void renderKeybindRow(GuiGraphicsExtractor g, float rx, float ry, float rw, float mouseX, float mouseY, int accent) {
        boolean hov = mouseX >= rx && mouseX <= rx + rw && mouseY >= ry && mouseY <= ry + BUILTIN_ROW_HEIGHT;
        if (hov) RenderUtil.drawRect(g, rx, ry, rw, BUILTIN_ROW_HEIGHT, 0x14FFFFFF);

        RenderUtil.drawSmallText(g, "Keybind", rx + 6.0F, ry + (BUILTIN_ROW_HEIGHT - RenderUtil.getSmallFontHeight()) / 2.0F,
                hov || binding ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED, false);

        String keyStr = binding ? "Press key..." : (module.getKeyCode() > 0 ? KeybindSetting.formatKeyCode(module.getKeyCode()) : "NONE");
        float badgeW = RenderUtil.getSmallStringWidth(keyStr) + 10.0F;
        float badgeH = BUILTIN_ROW_HEIGHT - 3.5F;
        float badgeX = rx + rw - badgeW - 6.0F;
        float badgeY = ry + (BUILTIN_ROW_HEIGHT - badgeH) / 2.0F;

        int pillBg = binding ? ColorUtil.modifyAlpha(ColorUtil.EMBER_ORANGE, 55) : (hov ? 0xFF262630 : 0xFF1E1E25);
        int pillBorder = binding ? ColorUtil.EMBER_ORANGE : (hov ? ColorUtil.modifyAlpha(accent, 180) : ColorUtil.BORDER);
        int pillFg = binding ? ColorUtil.EMBER_ORANGE : ("NONE".equals(keyStr) ? ColorUtil.TEXT_MUTED : accent);

        RenderUtil.drawRoundedRect(g, badgeX, badgeY, badgeW, badgeH, 2.5F, pillBg);
        RenderUtil.drawRoundedOutline(g, badgeX, badgeY, badgeW, badgeH, 2.5F, 1.0F, pillBorder);
        RenderUtil.drawCenteredSmallText(g, keyStr, badgeX + badgeW / 2.0F, badgeY + (badgeH - RenderUtil.getSmallFontHeight()) / 2.0F, pillFg, false);
    }

    private void renderArrayListToggleRow(GuiGraphicsExtractor g, float rx, float ry, float rw, float mouseX, float mouseY, int accent) {
        boolean shown = !module.isHidden();
        showInListAnim = AnimationUtil.lerp(showInListAnim, shown ? 1.0F : 0.0F, 16.0F);
        boolean hov = mouseX >= rx && mouseX <= rx + rw && mouseY >= ry && mouseY <= ry + BUILTIN_ROW_HEIGHT;
        if (hov) RenderUtil.drawRect(g, rx, ry, rw, BUILTIN_ROW_HEIGHT, 0x14FFFFFF);

        int textCol = ColorUtil.interpolateColor(ColorUtil.TEXT_MUTED, ColorUtil.TEXT_PRIMARY, Math.max(showInListAnim, hov ? 0.85F : 0.0F));
        RenderUtil.drawSmallText(g, "Show in ArrayList", rx + 6.0F, ry + (BUILTIN_ROW_HEIGHT - RenderUtil.getSmallFontHeight()) / 2.0F, textCol, false);

        float switchW = 19.0F;
        float switchH = 9.5F;
        float switchX = rx + rw - switchW - 6.0F;
        float switchY = ry + (BUILTIN_ROW_HEIGHT - switchH) / 2.0F;

        RenderUtil.drawRoundedRect(g, switchX, switchY, switchW, switchH, switchH / 2.0F, ColorUtil.interpolateColor(0xFF26262E, accent, showInListAnim));
        float minThumb = switchX + switchH / 2.0F;
        float maxThumb = switchX + switchW - switchH / 2.0F;
        float thumbX = minThumb + (maxThumb - minThumb) * AnimationUtil.easeOutCubic(showInListAnim);
        RenderUtil.drawCircle(g, thumbX, switchY + switchH / 2.0F, 3.2F, 0xFFF5F5F7);
    }

    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY)) {
            boolean onDots = (mouseX >= x + width - 18.0F);
            if (mouseButton == 1 || (mouseButton == 0 && onDots)) {
                expanded = !expanded;
            } else if (mouseButton == 0) {
                module.toggle();
            } else if (mouseButton == 2) {
                binding = !binding;
            }
            return;
        }
        if (expanded && expandAnimation > 0.5F) {
            float compY = y + height + 3.0F;
            boolean hadSettings = false;
            for (Component c : components) {
                if (c.isVisible()) {
                    hadSettings = true;
                    c.mouseClicked(mouseX, mouseY, mouseButton);
                    compY += c.getHeight();
                }
            }
            if (hadSettings) compY += 3.0F;

            float rx = x + 3.0F;
            float rw = width - 6.0F;
            if (mouseX >= rx && mouseX <= rx + rw && mouseY >= compY && mouseY <= compY + BUILTIN_ROW_HEIGHT) {
                if (mouseButton == 0) binding = !binding;
                else if (mouseButton == 1) {
                    module.setKeyCode(0);
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
        boolean hadSettings = false;
        for (Component c : components) {
            if (c.isVisible()) {
                hadSettings = true;
                h += c.getHeight();
            }
        }
        if (hadSettings) h += 3.0F;
        h += BUILTIN_ROW_HEIGHT * 2.0F;
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
