package dev.taezeem14.cigarette.ui.clickgui.component;

import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;

import java.awt.Color;

public class ColorPickerComponent extends Component {

    private final ColorSetting setting;
    private boolean expanded = false;
    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float brightness = 1.0f;
    private boolean draggingPicker = false;
    private boolean draggingHue = false;

    public ColorPickerComponent(ColorSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;

        int rgb = setting.getValue();
        float[] hsb = Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    @Override
    public void render(float mouseX, float mouseY, float partialTicks) {
        boolean headerHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 14.0f;
        if (headerHovered) {
            RenderUtil.drawRect(x, y, width, 14.0f, 0x16FFFFFF);
        }

        int textColor = headerHovered || expanded ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED;
        FontUtil.small.drawString(setting.getName(), x + 6.0f, y + (14.0f - FontUtil.small.getHeight()) / 2.0f, textColor);

        // Color swatch pill
        float badgeWidth = 18.0f;
        float badgeHeight = 9.0f;
        float badgeX = x + width - badgeWidth - 6.0f;
        float badgeY = y + (14.0f - badgeHeight) / 2.0f;

        RenderUtil.drawRoundedRect(badgeX, badgeY, badgeWidth, badgeHeight, 2.5f, setting.getRGB());
        RenderUtil.drawRoundedOutline(badgeX, badgeY, badgeWidth, badgeHeight, 2.5f, 1.0f, expanded ? ColorUtil.getAccent() : 0xFF3A3A44);

        if (expanded) {
            float pickerY = y + 16.0f;
            float pickerWidth = width - 12.0f;
            float pickerX = x + 6.0f;
            float panelH = 68.0f;

            RenderUtil.drawRoundedRect(pickerX, pickerY, pickerWidth, panelH, 3.0f, ColorUtil.SURFACE);
            RenderUtil.drawRoundedOutline(pickerX, pickerY, pickerWidth, panelH, 3.0f, 1.0f, ColorUtil.BORDER);

            // Saturation / Brightness box
            float satBoxW = pickerWidth - 15.0f;
            float satBoxH = 42.0f;
            float satBoxX = pickerX + 3.0f;
            float satBoxY = pickerY + 3.0f;

            int pureHue = Color.HSBtoRGB(hue, 1.0f, 1.0f);
            RenderUtil.drawGradientRect(satBoxX, satBoxY, satBoxW, satBoxH, 0xFFFFFFFF, pureHue, true);
            RenderUtil.drawGradientRect(satBoxX, satBoxY, satBoxW, satBoxH, 0x00000000, 0xFF000000, false);
            RenderUtil.drawOutline(satBoxX, satBoxY, satBoxW, satBoxH, 1.0f, 0xFF2C2C34);

            if (draggingPicker) {
                saturation = Math.max(0.0f, Math.min(1.0f, (mouseX - satBoxX) / satBoxW));
                brightness = Math.max(0.0f, Math.min(1.0f, 1.0f - (mouseY - satBoxY) / satBoxH));
                updateColor();
            }

            float selX = satBoxX + saturation * satBoxW;
            float selY = satBoxY + (1.0f - brightness) * satBoxH;
            RenderUtil.drawCircle(selX, selY, 2.6f, 0xFF101014);
            RenderUtil.drawCircle(selX, selY, 1.8f, 0xFFFFFFFF);

            // Smooth Hue Bar using gradient segments
            float hueX = pickerX + pickerWidth - 9.0f;
            float hueY = pickerY + 3.0f;
            float hueW = 6.0f;
            float hueH = 42.0f;
            int[] spectrum = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
            float segH = hueH / (spectrum.length - 1);
            for (int i = 0; i < spectrum.length - 1; i++) {
                RenderUtil.drawGradientRect(hueX, hueY + i * segH, hueW, segH + 0.5f, spectrum[i], spectrum[i + 1], false);
            }
            RenderUtil.drawOutline(hueX, hueY, hueW, hueH, 1.0f, 0xFF2C2C34);

            if (draggingHue) {
                hue = Math.max(0.0f, Math.min(1.0f, (mouseY - hueY) / hueH));
                updateColor();
            }

            float hueCurY = hueY + hue * hueH;
            RenderUtil.drawRoundedRect(hueX - 1.0f, hueCurY - 1.5f, hueW + 2.0f, 3.0f, 1.0f, 0xFFFFFFFF);

            // Rainbow Pill Toggle & Hex Badge
            float bottomY = pickerY + 49.0f;
            float pillW = 46.0f;
            float pillH = 14.0f;
            int accent = ColorUtil.getAccent();
            boolean rainbowOn = setting.isRainbow();

            RenderUtil.drawRoundedRect(pickerX + 3.0f, bottomY, pillW, pillH, 3.0f,
                    rainbowOn ? ColorUtil.modifyAlpha(accent, 55) : 0xFF202027);
            RenderUtil.drawRoundedOutline(pickerX + 3.0f, bottomY, pillW, pillH, 3.0f, 1.0f,
                    rainbowOn ? accent : ColorUtil.BORDER);
            FontUtil.small.drawCenteredString("Rainbow", pickerX + 3.0f + pillW / 2.0f,
                    bottomY + (pillH - FontUtil.small.getHeight()) / 2.0f,
                    rainbowOn ? accent : ColorUtil.TEXT_MUTED);

            String hexStr = String.format("#%06X", (0xFFFFFF & setting.getRGB()));
            float hexW = FontUtil.small.getStringWidth(hexStr) + 6.0f;
            float hexX = pickerX + pickerWidth - hexW - 3.0f;
            RenderUtil.drawRoundedRect(hexX, bottomY, hexW, pillH, 3.0f, 0xFF141418);
            FontUtil.small.drawCenteredString(hexStr, hexX + hexW / 2.0f,
                    bottomY + (pillH - FontUtil.small.getHeight()) / 2.0f, ColorUtil.TEXT_PRIMARY);

            this.height = 14.0f + 72.0f;
        } else {
            this.height = 14.0f;
        }
    }

    private void updateColor() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        int alpha = setting.getAlpha();
        setting.setValue((alpha << 24) | (rgb & 0x00FFFFFF));
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (mouseButton == 0 || mouseButton == 1) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 14.0f) {
                expanded = !expanded;
                return;
            }
        }

        if (mouseButton == 0 && expanded) {
            float pickerY = y + 16.0f;
            float pickerWidth = width - 12.0f;
            float pickerX = x + 6.0f;

            float satBoxW = pickerWidth - 15.0f;
            float satBoxH = 42.0f;
            float satBoxX = pickerX + 3.0f;
            float satBoxY = pickerY + 3.0f;

            if (mouseX >= satBoxX && mouseX <= satBoxX + satBoxW && mouseY >= satBoxY && mouseY <= satBoxY + satBoxH) {
                draggingPicker = true;
                saturation = Math.max(0.0f, Math.min(1.0f, (mouseX - satBoxX) / satBoxW));
                brightness = Math.max(0.0f, Math.min(1.0f, 1.0f - (mouseY - satBoxY) / satBoxH));
                updateColor();
                return;
            }

            float hueX = pickerX + pickerWidth - 9.0f;
            float hueY = pickerY + 3.0f;
            float hueW = 6.0f;
            float hueH = 42.0f;

            if (mouseX >= hueX - 1.0f && mouseX <= hueX + hueW + 1.0f && mouseY >= hueY && mouseY <= hueY + hueH) {
                draggingHue = true;
                hue = Math.max(0.0f, Math.min(1.0f, (mouseY - hueY) / hueH));
                updateColor();
                return;
            }

            float bottomY = pickerY + 49.0f;
            if (mouseY >= bottomY && mouseY <= bottomY + 14.0f && mouseX >= pickerX + 3.0f && mouseX <= pickerX + 49.0f) {
                setting.setRainbow(!setting.isRainbow());
            }
        }
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int mouseButton) {
        this.draggingPicker = false;
        this.draggingHue = false;
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
