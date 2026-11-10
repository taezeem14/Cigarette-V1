package dev.taezeem14.cigarette.ui.clickgui.component;

import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.awt.Color;

public class ColorPickerComponent extends Component {

    private final ColorSetting setting;
    private boolean expanded = false;
    private boolean draggingSB = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;
    private float hue;
    private float saturation;
    private float brightness;
    private float alpha;

    public ColorPickerComponent(ColorSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
        syncHSBFromSetting();
    }

    private void syncHSBFromSetting() {
        int color = setting.getValue();
        float[] hsb = Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        this.alpha = ((color >> 24) & 0xFF) / 255.0F;
    }

    @Override
    public float getHeight() {
        return expanded ? height + 72.0F : height;
    }

    @Override
    public void render(GuiGraphicsExtractor g, float mouseX, float mouseY, float partialTicks) {
        RenderUtil.drawSmallText(g, setting.getName(), x + 6.0F, y + (height - RenderUtil.getSmallFontHeight()) / 2.0F,
                isHoveredHeader(mouseX, mouseY) ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED, false);

        float swatchW = 16.0F;
        float swatchH = height - 4.0F;
        float swatchX = x + width - swatchW - 6.0F;
        float swatchY = y + 2.0F;
        RenderUtil.drawRoundedRect(g, swatchX, swatchY, swatchW, swatchH, 2.5F, setting.getRGB());
        RenderUtil.drawRoundedOutline(g, swatchX, swatchY, swatchW, swatchH, 2.5F, 1.0F, ColorUtil.BORDER);

        if (!expanded) return;

        float padX = x + 6.0F;
        float pickerY = y + height + 2.0F;
        float pickerW = width - 12.0F;
        float sbH = 44.0F;

        if (draggingSB) {
            saturation = Math.max(0.0F, Math.min(1.0F, (mouseX - padX) / pickerW));
            brightness = Math.max(0.0F, Math.min(1.0F, 1.0F - (mouseY - pickerY) / sbH));
            updateSetting();
        }

        float hueY = pickerY + sbH + 4.0F;
        float hueH = 7.0F;
        if (draggingHue) {
            hue = Math.max(0.0F, Math.min(1.0F, (mouseX - padX) / pickerW));
            updateSetting();
        }

        float alphaY = hueY + hueH + 4.0F;
        float alphaH = 7.0F;
        if (draggingAlpha) {
            alpha = Math.max(0.0F, Math.min(1.0F, (mouseX - padX) / pickerW));
            updateSetting();
        }

        int pureHue = 0xFF000000 | (Color.HSBtoRGB(hue, 1.0F, 1.0F) & 0x00FFFFFF);
        RenderUtil.drawHorizontalGradient(g, padX, pickerY, pickerW, sbH, 0xFFFFFFFF, pureHue);
        RenderUtil.drawGradientRect(g, padX, pickerY, pickerW, sbH, 0x00000000, 0xFF000000);
        RenderUtil.drawOutline(g, padX, pickerY, pickerW, sbH, 1.0F, ColorUtil.BORDER);

        float indX = padX + saturation * pickerW;
        float indY = pickerY + (1.0F - brightness) * sbH;
        RenderUtil.drawCircle(g, indX, indY, 3.0F, 0xFFFFFFFF);

        int segments = 18;
        float segW = pickerW / segments;
        for (int i = 0; i < segments; i++) {
            int c1 = 0xFF000000 | (Color.HSBtoRGB(i / (float) segments, 1.0F, 1.0F) & 0x00FFFFFF);
            int c2 = 0xFF000000 | (Color.HSBtoRGB((i + 1) / (float) segments, 1.0F, 1.0F) & 0x00FFFFFF);
            RenderUtil.drawHorizontalGradient(g, padX + i * segW, hueY, segW + 0.5F, hueH, c1, c2);
        }
        RenderUtil.drawCircle(g, padX + hue * pickerW, hueY + hueH / 2.0F, 3.5F, 0xFFFFFFFF);

        RenderUtil.drawHorizontalGradient(g, padX, alphaY, pickerW, alphaH, 0xFF202026, setting.getRGB() | 0xFF000000);
        RenderUtil.drawCircle(g, padX + alpha * pickerW, alphaY + alphaH / 2.0F, 3.5F, 0xFFFFFFFF);

        float rbY = alphaY + alphaH + 4.0F;
        RenderUtil.drawSmallText(g, "Rainbow", padX, rbY, setting.isRainbow() ? ColorUtil.getAccent() : ColorUtil.TEXT_MUTED, false);
    }

    private void updateSetting() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness) & 0x00FFFFFF;
        int a = ((int) (alpha * 255.0F)) << 24;
        setting.setValue(a | rgb);
        setting.setRainbow(false);
    }

    private boolean isHoveredHeader(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (isHoveredHeader(mouseX, mouseY) && (mouseButton == 0 || mouseButton == 1)) {
            expanded = !expanded;
            return;
        }
        if (!expanded || mouseButton != 0) return;

        float padX = x + 6.0F;
        float pickerY = y + height + 2.0F;
        float pickerW = width - 12.0F;
        float sbH = 44.0F;
        float hueY = pickerY + sbH + 4.0F;
        float hueH = 7.0F;
        float alphaY = hueY + hueH + 4.0F;
        float alphaH = 7.0F;
        float rbY = alphaY + alphaH + 3.0F;

        if (mouseX >= padX && mouseX <= padX + pickerW && mouseY >= pickerY && mouseY <= pickerY + sbH) draggingSB = true;
        else if (mouseX >= padX && mouseX <= padX + pickerW && mouseY >= hueY && mouseY <= hueY + hueH) draggingHue = true;
        else if (mouseX >= padX && mouseX <= padX + pickerW && mouseY >= alphaY && mouseY <= alphaY + alphaH) draggingAlpha = true;
        else if (mouseX >= padX && mouseX <= padX + pickerW && mouseY >= rbY && mouseY <= rbY + 12.0F) setting.setRainbow(!setting.isRainbow());
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int mouseButton) {
        draggingSB = false;
        draggingHue = false;
        draggingAlpha = false;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {}

    @Override
    public boolean isVisible() { return setting.isVisible(); }
}
