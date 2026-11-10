package dev.taezeem14.cigarette.ui.clickgui.component;

import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;

import java.text.DecimalFormat;

public class SliderComponent extends Component {

    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

    private final NumberSetting setting;
    private boolean dragging = false;
    private float animatedWidth = -1.0f;
    private float hoverAnimation = 0.0f;

    public SliderComponent(NumberSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
    }

    @Override
    public void render(float mouseX, float mouseY, float partialTicks) {
        boolean hovered = isHovered(mouseX, mouseY) || dragging;
        hoverAnimation = AnimationUtil.lerp(hoverAnimation, hovered ? 1.0f : 0.0f, 16.0f);

        if (hovered) {
            RenderUtil.drawRect(x, y, width, height, 0x16FFFFFF);
        }

        // Slider track dimensions
        float trackX = x + 6.0f;
        float trackY = y + height - 5.5f;
        float trackWidth = width - 12.0f;
        float trackHeight = 3.0f;

        double min = setting.getMin();
        double max = setting.getMax();
        double range = Math.max(0.0001, max - min);

        if (dragging) {
            float mouseFraction = Math.max(0.0f, Math.min(1.0f, (mouseX - trackX) / trackWidth));
            setting.setValue(min + range * mouseFraction);
        }

        double current = setting.getValue();
        float fraction = (float) Math.max(0.0, Math.min(1.0, (current - min) / range));

        float targetWidth = trackWidth * fraction;
        if (animatedWidth < 0.0f) {
            animatedWidth = targetWidth;
        } else {
            animatedWidth = AnimationUtil.lerp(animatedWidth, targetWidth, 20.0f);
        }

        // Title and Value Badge
        String name = setting.getName();
        String valStr = FORMAT.format(setting.getValue());
        int textColor = hovered ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED;
        FontUtil.small.drawString(name, x + 6.0f, y + 2.0f, textColor);

        float valW = FontUtil.small.getStringWidth(valStr) + 6.0f;
        float valX = x + width - valW - 5.0f;
        RenderUtil.drawRoundedRect(valX, y + 1.0f, valW, 8.5f, 2.0f, 0xFF202027);
        FontUtil.small.drawCenteredString(valStr, valX + valW / 2.0f, y + 2.0f, ColorUtil.TEXT_PRIMARY);

        // Background track
        RenderUtil.drawRoundedRect(trackX, trackY, trackWidth, trackHeight, 1.5f, 0xFF26262E);

        // Filled glowing track
        int accent = ColorUtil.getAccent();
        if (animatedWidth > 1.0f) {
            RenderUtil.drawRoundedRect(trackX - 0.5f, trackY - 0.5f, animatedWidth + 1.0f, trackHeight + 1.0f, 2.0f, ColorUtil.modifyAlpha(accent, 55));
            RenderUtil.drawRoundedRect(trackX, trackY, animatedWidth, trackHeight, 1.5f, accent);
        }

        // Hover-enlarged circular thumb with subtle halo
        float thumbCX = trackX + animatedWidth;
        float thumbCY = trackY + trackHeight / 2.0f;
        float haloRadius = 4.0f + 1.8f * hoverAnimation;
        float thumbRadius = 2.6f + 0.8f * hoverAnimation;

        if (hoverAnimation > 0.05f) {
            RenderUtil.drawCircle(thumbCX, thumbCY, haloRadius, ColorUtil.modifyAlpha(accent, (int) (70 * hoverAnimation)));
        }
        RenderUtil.drawCircle(thumbCX, thumbCY, thumbRadius, 0xFFF5F5F7);
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
