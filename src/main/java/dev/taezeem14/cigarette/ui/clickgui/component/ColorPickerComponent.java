package dev.taezeem14.cigarette.ui.clickgui.component;

import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import org.lwjgl.opengl.GL11;

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
        boolean hovered = isHovered(mouseX, mouseY);
        if (hovered) {
            RenderUtil.drawRect(x, y, width, 14.0f, 0x1AFFFFFF);
        }

        // Draw setting name
        int textColor = hovered ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED;
        FontUtil.small.drawString(setting.getName(), x + 6.0f, y + (14.0f - FontUtil.small.getHeight()) / 2.0f, textColor);

        // Preview badge
        float badgeWidth = 16.0f;
        float badgeHeight = 9.0f;
        float badgeX = x + width - badgeWidth - 6.0f;
        float badgeY = y + (14.0f - badgeHeight) / 2.0f;

        RenderUtil.drawRoundedRect(badgeX, badgeY, badgeWidth, badgeHeight, 2.0f, setting.getRGB());
        RenderUtil.drawRoundedOutline(badgeX, badgeY, badgeWidth, badgeHeight, 2.0f, 1.0f, 0xFF333338);

        // If expanded, render palette
        if (expanded) {
            float pickerY = y + 16.0f;
            float pickerHeight = 50.0f;
            float pickerWidth = width - 12.0f;
            float pickerX = x + 6.0f;

            // Background of picker area
            RenderUtil.drawRoundedRect(pickerX, pickerY, pickerWidth, 68.0f, 3.0f, 0xFF18181C);
            RenderUtil.drawRoundedOutline(pickerX, pickerY, pickerWidth, 68.0f, 3.0f, 1.0f, 0xFF2A2A30);

            // Saturation/Brightness area
            float satBoxW = pickerWidth - 14.0f;
            float satBoxH = 40.0f;
            float satBoxX = pickerX + 3.0f;
            float satBoxY = pickerY + 3.0f;

            int pureHue = Color.HSBtoRGB(hue, 1.0f, 1.0f);
            RenderUtil.drawGradientRect(satBoxX, satBoxY, satBoxW, satBoxH, 0xFFFFFFFF, pureHue, true);
            RenderUtil.drawGradientRect(satBoxX, satBoxY, satBoxW, satBoxH, 0x00000000, 0xFF000000, false);

            if (draggingPicker) {
                saturation = Math.max(0.0f, Math.min(1.0f, (mouseX - satBoxX) / satBoxW));
                brightness = Math.max(0.0f, Math.min(1.0f, 1.0f - (mouseY - satBoxY) / satBoxH));
                updateColor();
            }

            // Selector circle
            float selX = satBoxX + saturation * satBoxW;
            float selY = satBoxY + (1.0f - brightness) * satBoxH;
            RenderUtil.drawRoundedRect(selX - 2.0f, selY - 2.0f, 4.0f, 4.0f, 2.0f, 0xFFEBEBEF);

            // Hue bar
            float hueX = pickerX + pickerWidth - 8.0f;
            float hueY = pickerY + 3.0f;
            float hueW = 5.0f;
            float hueH = 40.0f;

            for (int i = 0; i < (int) hueH; i++) {
                float h = (float) i / hueH;
                RenderUtil.drawRect(hueX, hueY + i, hueW, 1.0f, Color.HSBtoRGB(h, 1.0f, 1.0f));
            }

            if (draggingHue) {
                hue = Math.max(0.0f, Math.min(1.0f, (mouseY - hueY) / hueH));
                updateColor();
            }

            // Hue cursor
            float hueCurY = hueY + hue * hueH;
            RenderUtil.drawRect(hueX - 1.0f, hueCurY - 1.0f, hueW + 2.0f, 2.0f, 0xFFFFFFFF);

            // Rainbow Toggle & Hex String
            float bottomY = pickerY + 46.0f;
            String rainbowText = "Rainbow: " + (setting.isRainbow() ? "ON" : "OFF");
            int rCol = setting.isRainbow() ? ColorUtil.ACCENT : ColorUtil.TEXT_MUTED;
            FontUtil.small.drawString(rainbowText, pickerX + 4.0f, bottomY + 3.0f, rCol);

            String hexStr = String.format("#%06X", (0xFFFFFF & setting.getValue()));
            FontUtil.small.drawString(hexStr, pickerX + pickerWidth - FontUtil.small.getStringWidth(hexStr) - 4.0f, bottomY + 3.0f, ColorUtil.TEXT_PRIMARY);

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
        if (mouseButton == 0) {
            float badgeWidth = 16.0f;
            float badgeHeight = 9.0f;
            float badgeX = x + width - badgeWidth - 6.0f;
            float badgeY = y + (14.0f - badgeHeight) / 2.0f;

            if (mouseX >= badgeX && mouseX <= badgeX + badgeWidth && mouseY >= badgeY && mouseY <= badgeY + badgeHeight) {
                expanded = !expanded;
                return;
            }

            if (expanded) {
                float pickerY = y + 16.0f;
                float pickerWidth = width - 12.0f;
                float pickerX = x + 6.0f;

                float satBoxW = pickerWidth - 14.0f;
                float satBoxH = 40.0f;
                float satBoxX = pickerX + 3.0f;
                float satBoxY = pickerY + 3.0f;

                if (mouseX >= satBoxX && mouseX <= satBoxX + satBoxW && mouseY >= satBoxY && mouseY <= satBoxY + satBoxH) {
                    draggingPicker = true;
                    return;
                }

                float hueX = pickerX + pickerWidth - 8.0f;
                float hueY = pickerY + 3.0f;
                float hueW = 5.0f;
                float hueH = 40.0f;

                if (mouseX >= hueX && mouseX <= hueX + hueW && mouseY >= hueY && mouseY <= hueY + hueH) {
                    draggingHue = true;
                    return;
                }

                float bottomY = pickerY + 46.0f;
                if (mouseY >= bottomY && mouseY <= bottomY + 16.0f && mouseX >= pickerX && mouseX <= pickerX + 70.0f) {
                    setting.setRainbow(!setting.isRainbow());
                }
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
