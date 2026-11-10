package dev.taezeem14.cigarette.ui.clickgui.component;

import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;

import java.text.DecimalFormat;

public class DoubleSliderComponent extends Component {

    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

    private final DoubleSliderSetting setting;
    private int draggingThumb = 0; // 0 = none, 1 = min, 2 = max

    public DoubleSliderComponent(DoubleSliderSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
    }

    @Override
    public void render(float mouseX, float mouseY, float partialTicks) {
        boolean hovered = isHovered(mouseX, mouseY);
        if (hovered) {
            RenderUtil.drawRect(x, y, width, height, 0x1AFFFFFF);
        }

        // Title and Value String
        String name = setting.getName();
        String valStr = FORMAT.format(setting.getValueMin()) + " - " + FORMAT.format(setting.getValueMax());

        int textColor = hovered ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED;
        FontUtil.small.drawString(name, x + 6.0f, y + 2.0f, textColor);
        FontUtil.small.drawString(valStr, x + width - FontUtil.small.getStringWidth(valStr) - 6.0f, y + 2.0f, ColorUtil.TEXT_PRIMARY);

        // Track
        float trackX = x + 6.0f;
        float trackY = y + height - 6.0f;
        float trackWidth = width - 12.0f;
        float trackHeight = 3.0f;

        RenderUtil.drawRoundedRect(trackX, trackY, trackWidth, trackHeight, 1.5f, 0xFF222226);

        double min = setting.getMin();
        double max = setting.getMax();
        double range = max - min;

        float minFraction = (float) ((setting.getValueMin() - min) / range);
        float maxFraction = (float) ((setting.getValueMax() - min) / range);

        minFraction = Math.max(0.0f, Math.min(1.0f, minFraction));
        maxFraction = Math.max(0.0f, Math.min(1.0f, maxFraction));

        if (draggingThumb == 1) {
            float mouseFraction = (mouseX - trackX) / trackWidth;
            mouseFraction = Math.max(0.0f, Math.min(maxFraction, mouseFraction));
            double newMin = min + range * mouseFraction;
            setting.setValueMin(newMin);
        } else if (draggingThumb == 2) {
            float mouseFraction = (mouseX - trackX) / trackWidth;
            mouseFraction = Math.max(minFraction, Math.min(1.0f, mouseFraction));
            double newMax = min + range * mouseFraction;
            setting.setValueMax(newMax);
        }

        // Active range line
        float fillStartX = trackX + trackWidth * minFraction;
        float fillEndX = trackX + trackWidth * maxFraction;
        float fillWidth = Math.max(0.0f, fillEndX - fillStartX);

        if (fillWidth > 0.0f) {
            RenderUtil.drawRoundedRect(fillStartX, trackY, fillWidth, trackHeight, 1.5f, ColorUtil.ACCENT);
        }

        // Two thumbs
        RenderUtil.drawRoundedRect(fillStartX - 2.5f, trackY - 1.5f, 5.0f, 6.0f, 2.5f, 0xFFEBEBEF);
        RenderUtil.drawRoundedRect(fillEndX - 2.5f, trackY - 1.5f, 5.0f, 6.0f, 2.5f, 0xFFEBEBEF);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            float trackX = x + 6.0f;
            float trackWidth = width - 12.0f;

            double min = setting.getMin();
            double max = setting.getMax();
            double range = max - min;

            float minPos = trackX + trackWidth * (float) ((setting.getValueMin() - min) / range);
            float maxPos = trackX + trackWidth * (float) ((setting.getValueMax() - min) / range);

            float distToMin = Math.abs(mouseX - minPos);
            float distToMax = Math.abs(mouseX - maxPos);

            if (distToMin <= distToMax) {
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
