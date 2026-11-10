package dev.taezeem14.cigarette.ui.clickgui.component;

import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
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
        animation += (targetAnim - animation) * 0.2f;

        float boxSize = 10.0f;
        float boxX = x + width - boxSize - 6.0f;
        float boxY = y + (height - boxSize) / 2.0f;

        // Background line hover
        boolean hovered = isHovered(mouseX, mouseY);
        if (hovered) {
            RenderUtil.drawRect(x, y, width, height, 0x1AFFFFFF);
        }

        // Draw setting name
        int textColor = hovered ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED;
        FontUtil.small.drawString(setting.getName(), x + 6.0f, y + (height - FontUtil.small.getHeight()) / 2.0f, textColor);

        // Checkbox box
        RenderUtil.drawRoundedRect(boxX, boxY, boxSize, boxSize, 2.0f, 0xFF222226);
        RenderUtil.drawRoundedOutline(boxX, boxY, boxSize, boxSize, 2.0f, 1.0f, 0xFF333338);

        // Animated checkmark fill
        if (animation > 0.01f) {
            int fillCol = ColorUtil.modifyAlpha(ColorUtil.ACCENT, (int) (255 * animation));
            float inset = (1.0f - animation) * 2.0f;
            RenderUtil.drawRoundedRect(
                    boxX + 2.0f + inset,
                    boxY + 2.0f + inset,
                    (boxSize - 4.0f) * animation,
                    (boxSize - 4.0f) * animation,
                    1.5f,
                    fillCol
            );
        }
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
