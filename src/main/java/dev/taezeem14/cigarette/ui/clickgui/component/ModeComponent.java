package dev.taezeem14.cigarette.ui.clickgui.component;

import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;

public class ModeComponent extends Component {

    private final ModeSetting setting;

    public ModeComponent(ModeSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
    }

    @Override
    public void render(float mouseX, float mouseY, float partialTicks) {
        boolean hovered = isHovered(mouseX, mouseY);
        if (hovered) {
            RenderUtil.drawRect(x, y, width, height, 0x16FFFFFF);
        }

        int textColor = hovered ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED;
        FontUtil.small.drawString(setting.getName(), x + 6.0f, y + (height - FontUtil.small.getHeight()) / 2.0f, textColor);

        String currentMode = setting.getMode();
        int modeWidth = FontUtil.small.getStringWidth(currentMode);
        float badgeWidth = modeWidth + 10.0f;
        float badgeHeight = height - 3.5f;
        float badgeX = x + width - badgeWidth - 6.0f;
        float badgeY = y + (height - badgeHeight) / 2.0f;

        int accent = ColorUtil.getAccent();
        RenderUtil.drawRoundedRect(badgeX, badgeY, badgeWidth, badgeHeight, 3.0f, hovered ? 0xFF262630 : 0xFF1E1E24);
        RenderUtil.drawRoundedOutline(badgeX, badgeY, badgeWidth, badgeHeight, 3.0f, 1.0f,
                hovered ? ColorUtil.modifyAlpha(accent, 180) : ColorUtil.BORDER);

        FontUtil.small.drawCenteredString(currentMode, badgeX + badgeWidth / 2.0f, badgeY + (badgeHeight - FontUtil.small.getHeight()) / 2.0f, accent);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY)) {
            if (mouseButton == 0) {
                setting.cycle();
            } else if (mouseButton == 1) {
                setting.cycleReverse();
            }
        }
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
