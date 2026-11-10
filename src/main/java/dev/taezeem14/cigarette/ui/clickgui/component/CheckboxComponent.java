package dev.taezeem14.cigarette.ui.clickgui.component;

import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;

public class CheckboxComponent extends Component {

    private final BooleanSetting setting;
    private float animation = 0.0f;

    public CheckboxComponent(BooleanSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
        this.animation = setting.isEnabled() ? 1.0f : 0.0f;
    }

    @Override
    public void render(float mouseX, float mouseY, float partialTicks) {
        boolean enabled = setting.isEnabled();
        float targetAnim = enabled ? 1.0f : 0.0f;
        animation = AnimationUtil.lerp(animation, targetAnim, 16.0f);

        boolean hovered = isHovered(mouseX, mouseY);
        if (hovered) {
            RenderUtil.drawRect(x, y, width, height, 0x16FFFFFF);
        }

        // Setting name
        int textColor = ColorUtil.interpolateColor(ColorUtil.TEXT_MUTED, ColorUtil.TEXT_PRIMARY, Math.max(animation, hovered ? 0.85f : 0.0f));
        FontUtil.small.drawString(setting.getName(), x + 6.0f, y + (height - FontUtil.small.getHeight()) / 2.0f, textColor);

        // Vape V4 sliding toggle switch
        float switchW = 19.0f;
        float switchH = 9.5f;
        float switchX = x + width - switchW - 6.0f;
        float switchY = y + (height - switchH) / 2.0f;

        int trackColor = ColorUtil.interpolateColor(0xFF26262E, ColorUtil.getAccent(), animation);
        RenderUtil.drawRoundedRect(switchX, switchY, switchW, switchH, switchH / 2.0f, trackColor);
        RenderUtil.drawRoundedOutline(switchX, switchY, switchW, switchH, switchH / 2.0f, 1.0f,
                ColorUtil.interpolateColor(0xFF34343F, ColorUtil.modifyAlpha(ColorUtil.getAccent(), 220), animation));

        // Smooth sliding white circular thumb
        float thumbRadius = 3.2f;
        float minThumbX = switchX + switchH / 2.0f;
        float maxThumbX = switchX + switchW - switchH / 2.0f;
        float thumbCX = minThumbX + (maxThumbX - minThumbX) * AnimationUtil.easeOutCubic(animation);
        float thumbCY = switchY + switchH / 2.0f;

        RenderUtil.drawCircle(thumbCX, thumbCY, thumbRadius, 0xFFF5F5F7);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            setting.toggle();
        }
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
