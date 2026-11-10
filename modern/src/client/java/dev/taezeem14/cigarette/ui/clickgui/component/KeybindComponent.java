package dev.taezeem14.cigarette.ui.clickgui.component;

import com.mojang.blaze3d.platform.InputConstants;
import dev.taezeem14.cigarette.setting.impl.KeybindSetting;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class KeybindComponent extends Component {

    private final KeybindSetting setting;
    private boolean listening = false;

    public KeybindComponent(KeybindSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
    }

    @Override
    public void render(GuiGraphicsExtractor g, float mouseX, float mouseY, float partialTicks) {
        boolean hovered = isHovered(mouseX, mouseY);
        if (hovered) RenderUtil.drawRect(g, x, y, width, height, 0x16FFFFFF);

        int textColor = hovered || listening ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED;
        RenderUtil.drawSmallText(g, setting.getName(), x + 6.0F, y + (height - RenderUtil.getSmallFontHeight()) / 2.0F, textColor, false);

        String keyText = listening ? "..." : setting.getKeyName();
        float keyW = RenderUtil.getSmallStringWidth(keyText);
        float badgeW = Math.max(20.0F, keyW + 10.0F);
        float badgeH = height - 3.5F;
        float badgeX = x + width - badgeW - 6.0F;
        float badgeY = y + (height - badgeH) / 2.0F;
        int accent = ColorUtil.getAccent();

        RenderUtil.drawRoundedRect(g, badgeX, badgeY, badgeW, badgeH, 2.5F,
                listening ? ColorUtil.modifyAlpha(accent, 45) : 0xFF1F1F26);
        RenderUtil.drawRoundedOutline(g, badgeX, badgeY, badgeW, badgeH, 2.5F, 1.0F,
                listening ? accent : ColorUtil.BORDER);
        RenderUtil.drawCenteredSmallText(g, keyText, badgeX + badgeW / 2.0F,
                badgeY + (badgeH - RenderUtil.getSmallFontHeight()) / 2.0F,
                listening ? accent : ColorUtil.TEXT_PRIMARY, false);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) listening = !listening;
        else if (listening) listening = false;
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int mouseButton) {}

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (listening) {
            if (keyCode == InputConstants.KEY_ESCAPE || keyCode == InputConstants.KEY_DELETE || keyCode == InputConstants.KEY_BACKSPACE) {
                setting.setKeyCode(0);
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
    public boolean isVisible() { return setting.isVisible(); }
}
