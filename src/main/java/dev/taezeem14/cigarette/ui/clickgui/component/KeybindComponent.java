package dev.taezeem14.cigarette.ui.clickgui.component;

import dev.taezeem14.cigarette.setting.impl.KeybindSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import org.lwjgl.input.Keyboard;

public class KeybindComponent extends Component {

    private final KeybindSetting setting;
    private boolean listening = false;

    public KeybindComponent(KeybindSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
    }

    @Override
    public void render(float mouseX, float mouseY, float partialTicks) {
        boolean hovered = isHovered(mouseX, mouseY);
        if (hovered) {
            RenderUtil.drawRect(x, y, width, height, 0x1AFFFFFF);
        }

        int textColor = hovered ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED;
        FontUtil.small.drawString(setting.getName(), x + 6.0f, y + (height - FontUtil.small.getHeight()) / 2.0f, textColor);

        String keyText = listening ? "[ ... ]" : "[" + setting.getKeyName() + "]";
        int keyWidth = FontUtil.small.getStringWidth(keyText);
        float badgeWidth = keyWidth + 8.0f;
        float badgeHeight = height - 4.0f;
        float badgeX = x + width - badgeWidth - 6.0f;
        float badgeY = y + 2.0f;

        RenderUtil.drawRoundedRect(badgeX, badgeY, badgeWidth, badgeHeight, 3.0f, 0xFF222226);
        RenderUtil.drawRoundedOutline(badgeX, badgeY, badgeWidth, badgeHeight, 3.0f, 1.0f, listening ? ColorUtil.ACCENT : 0xFF333338);

        FontUtil.small.drawString(keyText, badgeX + 4.0f, badgeY + (badgeHeight - FontUtil.small.getHeight()) / 2.0f, listening ? ColorUtil.ACCENT : ColorUtil.TEXT_PRIMARY);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            listening = !listening;
        } else if (listening) {
            listening = false;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (listening) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE) {
                setting.setKeyCode(Keyboard.KEY_NONE);
            } else {
                setting.setKeyCode(keyCode);
            }
            listening = false;
        }
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
