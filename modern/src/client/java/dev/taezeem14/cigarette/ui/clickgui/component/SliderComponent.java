package dev.taezeem14.cigarette.ui.clickgui.component;

import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.text.DecimalFormat;

public class SliderComponent extends Component {

    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");
    private final NumberSetting setting;
    private boolean dragging = false;
    private float animatedFill = 0.0F;

    public SliderComponent(NumberSetting setting, float x, float y, float width, float height) {
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

        if (dragging) {
            float diff = AnimationUtil.clamp((mouseX - sliderX) / sliderWidth, 0.0F, 1.0F);
            setting.setValue(setting.getMin() + (setting.getMax() - setting.getMin()) * diff);
        }

        double range = Math.max(0.0001, setting.getMax() - setting.getMin());
        float targetPercent = (float) AnimationUtil.clamp((float) ((setting.getValue() - setting.getMin()) / range), 0.0F, 1.0F);
        animatedFill = AnimationUtil.lerp(animatedFill, targetPercent, 18.0F);

        int accent = ColorUtil.getAccent();
        RenderUtil.drawSmallText(g, setting.getName(), sliderX, y + 2.0F, hovered || dragging ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED, false);
        String valStr = FORMAT.format(setting.getValue());
        RenderUtil.drawSmallText(g, valStr, sliderX + sliderWidth - RenderUtil.getSmallStringWidth(valStr), y + 2.0F,
                hovered || dragging ? accent : ColorUtil.TEXT_PRIMARY, false);

        RenderUtil.drawRoundedRect(g, sliderX, trackY, sliderWidth, trackHeight, 1.5F, 0xFF25252D);
        float fillW = sliderWidth * animatedFill;
        if (fillW > 0.5F) {
            RenderUtil.drawRoundedRect(g, sliderX, trackY, fillW, trackHeight, 1.5F, accent);
        }
        float thumbX = sliderX + fillW;
        float thumbY = trackY + trackHeight / 2.0F;
        RenderUtil.drawCircle(g, thumbX, thumbY, dragging ? 3.5F : (hovered ? 3.0F : 2.5F), 0xFFF5F5F7);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) dragging = true;
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int mouseButton) { dragging = false; }

    @Override
    public void keyTyped(char typedChar, int keyCode) {}

    @Override
    public boolean isVisible() { return setting.isVisible(); }
}
