package dev.taezeem14.cigarette.ui.clickgui.component;

import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;

import java.text.DecimalFormat;

public class DoubleSliderComponent extends Component {

    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

    private final DoubleSliderSetting setting;
    private int draggingThumb = 0; // 0 = none, 1 = min, 2 = max
    private float animatedMinX = -1.0f;
    private float animatedMaxX = -1.0f;
    private float hoverAnimation = 0.0f;

    public DoubleSliderComponent(DoubleSliderSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
    }

    @Override
    public void render(float mouseX, float mouseY, float partialTicks) {
        boolean hovered = isHovered(mouseX, mouseY) || draggingThumb != 0;
        hoverAnimation = AnimationUtil.lerp(hoverAnimation, hovered ? 1.0f : 0.0f, 16.0f);

        if (hovered) {
            RenderUtil.drawRect(x, y, width, height, 0x16FFFFFF);
        }

        // Title and Value Badge
        String name = setting.getName();
        String valStr = FORMAT.format(setting.getValueMin()) + " - " + FORMAT.format(setting.getValueMax());

        int textColor = hovered ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED;
        FontUtil.small.drawString(name, x + 6.0f, y + 2.0f, textColor);

        float valW = FontUtil.small.getStringWidth(valStr) + 6.0f;
        float valX = x + width - valW - 5.0f;
        RenderUtil.drawRoundedRect(valX, y + 1.0f, valW, 8.5f, 2.0f, 0xFF202027);
        FontUtil.small.drawCenteredString(valStr, valX + valW / 2.0f, y + 2.0f, ColorUtil.TEXT_PRIMARY);

        // Track
        float trackX = x + 6.0f;
        float trackY = y + height - 5.5f;
        float trackWidth = width - 12.0f;
        float trackHeight = 3.0f;

        RenderUtil.drawRoundedRect(trackX, trackY, trackWidth, trackHeight, 1.5f, 0xFF26262E);

        double min = setting.getMin();
        double max = setting.getMax();
        double range = Math.max(0.0001, max - min);

        float minFraction = (float) Math.max(0.0, Math.min(1.0, (setting.getValueMin() - min) / range));
        float maxFraction = (float) Math.max(0.0, Math.min(1.0, (setting.getValueMax() - min) / range));

        if (draggingThumb == 1) {
            float mouseFraction = Math.max(0.0f, Math.min(maxFraction, (mouseX - trackX) / trackWidth));
            setting.setValueMin(min + range * mouseFraction);
            minFraction = (float) Math.max(0.0, Math.min(1.0, (setting.getValueMin() - min) / range));
        } else if (draggingThumb == 2) {
            float mouseFraction = Math.max(minFraction, Math.min(1.0f, (mouseX - trackX) / trackWidth));
            setting.setValueMax(min + range * mouseFraction);
            maxFraction = (float) Math.max(0.0, Math.min(1.0, (setting.getValueMax() - min) / range));
        }

        float targetMinX = trackWidth * minFraction;
        float targetMaxX = trackWidth * maxFraction;

        if (animatedMinX < 0.0f || animatedMaxX < 0.0f) {
            animatedMinX = targetMinX;
            animatedMaxX = targetMaxX;
        } else {
            animatedMinX = AnimationUtil.lerp(animatedMinX, targetMinX, 20.0f);
            animatedMaxX = AnimationUtil.lerp(animatedMaxX, targetMaxX, 20.0f);
        }

        float fillStartX = trackX + animatedMinX;
        float fillEndX = trackX + animatedMaxX;
        float fillWidth = Math.max(0.0f, fillEndX - fillStartX);
        int accent = ColorUtil.getAccent();

        if (fillWidth > 0.5f) {
            RenderUtil.drawRoundedRect(fillStartX - 0.5f, trackY - 0.5f, fillWidth + 1.0f, trackHeight + 1.0f, 2.0f, ColorUtil.modifyAlpha(accent, 55));
            RenderUtil.drawRoundedRect(fillStartX, trackY, fillWidth, trackHeight, 1.5f, accent);
        }

        float thumbCY = trackY + trackHeight / 2.0f;
        float haloRadius = 3.8f + 1.6f * hoverAnimation;
        float thumbRadius = 2.5f + 0.7f * hoverAnimation;

        if (hoverAnimation > 0.05f) {
            RenderUtil.drawCircle(fillStartX, thumbCY, haloRadius, ColorUtil.modifyAlpha(accent, (int) (65 * hoverAnimation)));
            RenderUtil.drawCircle(fillEndX, thumbCY, haloRadius, ColorUtil.modifyAlpha(accent, (int) (65 * hoverAnimation)));
        }
        RenderUtil.drawCircle(fillStartX, thumbCY, thumbRadius, 0xFFF5F5F7);
        RenderUtil.drawCircle(fillEndX, thumbCY, thumbRadius, 0xFFF5F5F7);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            float trackX = x + 6.0f;
            float trackWidth = width - 12.0f;

            double min = setting.getMin();
            double max = setting.getMax();
            double range = Math.max(0.0001, max - min);

            float minPos = trackX + trackWidth * (float) ((setting.getValueMin() - min) / range);
            float maxPos = trackX + trackWidth * (float) ((setting.getValueMax() - min) / range);

            float distToMin = Math.abs(mouseX - minPos);
            float distToMax = Math.abs(mouseX - maxPos);

            if (distToMin < distToMax || (distToMin == distToMax && mouseX < minPos)) {
                draggingThumb = 1;
            } else {
                draggingThumb = 2;
            }
        }
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int mouseButton) {
        this.draggingThumb = 0;
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
