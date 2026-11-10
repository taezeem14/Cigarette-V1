package dev.taezeem14.cigarette.ui.clickgui.component;

import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ModeComponent extends Component {

    private final ModeSetting setting;

    public ModeComponent(ModeSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
    }

    @Override
    public void render(GuiGraphicsExtractor g, float mouseX, float mouseY, float partialTicks) {
        boolean hovered = isHovered(mouseX, mouseY);
        if (hovered) RenderUtil.drawRect(g, x, y, width, height, 0x16FFFFFF);

        RenderUtil.drawSmallText(g, setting.getName(), x + 6.0F, y + (height - RenderUtil.getSmallFontHeight()) / 2.0F,
                hovered ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED, false);

        String modeText = setting.getMode();
        float modeWidth = RenderUtil.getSmallStringWidth(modeText);
        float badgeW = Math.min(width * 0.54F, modeWidth + 10.0F);
        float badgeH = height - 3.5F;
        float badgeX = x + width - badgeW - 6.0F;
        float badgeY = y + (height - badgeH) / 2.0F;
        int accent = ColorUtil.getAccent();

        RenderUtil.drawRoundedRect(g, badgeX, badgeY, badgeW, badgeH, 2.5F, hovered ? ColorUtil.modifyAlpha(accent, 45) : 0xFF1F1F26);
        RenderUtil.drawRoundedOutline(g, badgeX, badgeY, badgeW, badgeH, 2.5F, 1.0F, hovered ? accent : ColorUtil.BORDER);
        RenderUtil.drawCenteredSmallText(g, modeText, badgeX + badgeW / 2.0F,
                badgeY + (badgeH - RenderUtil.getSmallFontHeight()) / 2.0F, accent, false);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) setting.cycle();
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int mouseButton) {}

    @Override
    public void keyTyped(char typedChar, int keyCode) {}

    @Override
    public boolean isVisible() { return setting.isVisible(); }
}
