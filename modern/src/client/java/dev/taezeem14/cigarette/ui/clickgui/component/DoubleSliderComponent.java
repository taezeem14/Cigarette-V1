package dev.taezeem14.cigarette.ui.clickgui.component;

import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.text.DecimalFormat;

public class DoubleSliderComponent extends Component {

    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");
    private final DoubleSliderSetting setting;
    private int dragMode = 0;
    private float animatedMinFrac = 0.0F;
    private float animatedMaxFrac = 1.0F;

    public DoubleSliderComponent(DoubleSliderSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
    }

    @Override
    public void render(GuiGraphicsExtractor g, float mouseX, float mouseY, float partialTicks) {
        boolean hovered = isHovered(mouseX, mouseY);
        float sliderX = x + 6.0F;
        float sliderWidth = width - 12.0F;
        float trackY = y + height - 5.0F;
        float trackHeight = 3.0F;

        if (dragMode != 0) {
            float diff = AnimationUtil.clamp((mouseX - sliderX) / sliderWidth, 0.0F, 1.0F);
            double valueAtCursor = setting.getMinLimit() + (setting.getMaxLimit() - setting.getMinLimit()) * diff;
            if (dragMode == 1) setting.setValueMin(valueAtCursor);
            else setting.setValueMax(valueAtCursor);
        }

        double range = Math.max(0.0001, setting.getMaxLimit() - setting.getMinLimit());
        float targetMinFrac = AnimationUtil.clamp((float) ((setting.getValueMin() - setting.getMinLimit()) / range), 0.0F, 1.0F);
        float targetMaxFrac = AnimationUtil.clamp((float) ((setting.getValueMax() - setting.getMinLimit()) / range), targetMinFrac, 1.0F);
        animatedMinFrac = AnimationUtil.lerp(animatedMinFrac, targetMinFrac, 18.0F);
        animatedMaxFrac = AnimationUtil.lerp(animatedMaxFrac, targetMaxFrac, 18.0F);

        int accent = ColorUtil.getAccent();
        RenderUtil.drawSmallText(g, setting.getName(), sliderX, y + 2.0F, hovered || dragMode != 0 ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED, false);
        String rangeText = FORMAT.format(setting.getValueMin()) + " - " + FORMAT.format(setting.getValueMax());
        RenderUtil.drawSmallText(g, rangeText, sliderX + sliderWidth - RenderUtil.getSmallStringWidth(rangeText), y + 2.0F,
                hovered || dragMode != 0 ? accent : ColorUtil.TEXT_PRIMARY, false);

        RenderUtil.drawRoundedRect(g, sliderX, trackY, sliderWidth, trackHeight, 1.5F, 0xFF25252D);
        float minPos = sliderX + sliderWidth * animatedMinFrac;
        float maxPos = sliderX + sliderWidth * animatedMaxFrac;
        float spanW = Math.max(2.0F, maxPos - minPos);

        RenderUtil.drawRoundedRect(g, minPos, trackY, spanW, trackHeight, 1.5F, accent);
        float thumbY = trackY + trackHeight / 2.0F;
        RenderUtil.drawCircle(g, minPos, thumbY, dragMode == 1 ? 3.5F : (hovered ? 3.0F : 2.5F), 0xFFF5F5F7);
        RenderUtil.drawCircle(g, maxPos, thumbY, dragMode == 2 ? 3.5F : (hovered ? 3.0F : 2.5F), 0xFFF5F5F7);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            float sliderX = x + 6.0F;
            float sliderWidth = width - 12.0F;
            double range = Math.max(0.0001, setting.getMaxLimit() - setting.getMinLimit());
            float minPos = sliderX + sliderWidth * (float) ((setting.getValueMin() - setting.getMinLimit()) / range);
            float maxPos = sliderX + sliderWidth * (float) ((setting.getValueMax() - setting.getMinLimit()) / range);
            dragMode = Math.abs(mouseX - minPos) <= Math.abs(mouseX - maxPos) ? 1 : 2;
        }
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int mouseButton) { dragMode = 0; }

    @Override
    public void keyTyped(char typedChar, int keyCode) {}

    @Override
    public boolean isVisible() { return setting.isVisible(); }
}
