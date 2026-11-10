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
            RenderUtil.drawRect(x, y, width, height, 0x16FFFFFF);
        }

        int textColor = hovered || listening ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED;
        FontUtil.small.drawString(setting.getName(), x + 6.0f, y + (height - FontUtil.small.getHeight()) / 2.0f, textColor);

        String keyText = listening ? "..." : setting.getKeyName();
        int keyWidth = FontUtil.small.getStringWidth(keyText);
        float badgeWidth = Math.max(20.0f, keyWidth + 10.0f);
        float badgeHeight = height - 3.5f;
        float badgeX = x + width - badgeWidth - 6.0f;
        float badgeY = y + (height - badgeHeight) / 2.0f;

        int accent = ColorUtil.getAccent();
        RenderUtil.drawRoundedRect(badgeX, badgeY, badgeWidth, badgeHeight, 2.5f,
                listening ? ColorUtil.modifyAlpha(accent, 45) : 0xFF1F1F26);
        RenderUtil.drawRoundedOutline(badgeX, badgeY, badgeWidth, badgeHeight, 2.5f, 1.0f,
                listening ? accent : ColorUtil.BORDER);

        FontUtil.small.drawCenteredString(keyText, badgeX + badgeWidth / 2.0f,
                badgeY + (badgeHeight - FontUtil.small.getHeight()) / 2.0f,
                listening ? accent : ColorUtil.TEXT_PRIMARY);
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
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK) {
                setting.setKeyCode(Keyboard.KEY_NONE);
            } else {
                setting.setKeyCode(keyCode);
            }
            listening = false;
        }
    }

    public boolean isListening() {
        return listening;
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
