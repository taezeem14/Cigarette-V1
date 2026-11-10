package dev.taezeem14.cigarette.ui.clickgui.component;

import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;

import java.text.DecimalFormat;

public class SliderComponent extends Component {

    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

    private final NumberSetting setting;
    private boolean dragging = false;
    private float animatedWidth = 0.0f;

    public SliderComponent(NumberSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
    }

    @Override
    public void render(float mouseX, float mouseY, float partialTicks) {
        boolean hovered = isHovered(mouseX, mouseY);
        if (hovered) {
            RenderUtil.drawRect(x, y, width, height, 0x1AFFFFFF);
        }

        // Title and Value
        String name = setting.getName();
        String valStr = FORMAT.format(setting.getValue());

        int textColor = hovered ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED;
        FontUtil.small.drawString(name, x + 6.0f, y + 2.0f, textColor);
        FontUtil.small.drawString(valStr, x + width - FontUtil.small.getStringWidth(valStr) - 6.0f, y + 2.0f, ColorUtil.TEXT_PRIMARY);

        // Slider track
        float trackX = x + 6.0f;
        float trackY = y + height - 6.0f;
        float trackWidth = width - 12.0f;
        float trackHeight = 3.0f;

        RenderUtil.drawRoundedRect(trackX, trackY, trackWidth, trackHeight, 1.5f, 0xFF222226);

        // Calculate fraction
        double min = setting.getMin();
        double max = setting.getMax();
        double current = setting.getValue();
        float fraction = (float) ((current - min) / (max - min));
        fraction = Math.max(0.0f, Math.min(1.0f, fraction));

        if (dragging) {
            float mouseFraction = (mouseX - trackX) / trackWidth;
            mouseFraction = Math.max(0.0f, Math.min(1.0f, mouseFraction));
            double newValue = min + (max - min) * mouseFraction;
            setting.setValue(newValue);
        }

        float targetWidth = trackWidth * fraction;
        animatedWidth += (targetWidth - animatedWidth) * 0.25f;

        // Filled slider
        if (animatedWidth > 1.0f) {
            RenderUtil.drawRoundedRect(trackX, trackY, animatedWidth, trackHeight, 1.5f, ColorUtil.ACCENT);
        }

        // Thumb circle
        RenderUtil.drawRoundedRect(trackX + animatedWidth - 2.5f, trackY - 1.5f, 6.0f, 6.0f, 3.0f, 0xFFEBEBEF);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            this.dragging = true;
        }
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int mouseButton) {
        this.dragging = false;
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
