package dev.taezeem14.cigarette.ui.clickgui.component;

import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class CheckboxComponent extends Component {

    private final BooleanSetting setting;
    private float toggleAnim;

    public CheckboxComponent(BooleanSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
        this.toggleAnim = setting.isEnabled() ? 1.0F : 0.0F;
    }

    @Override
    public void render(GuiGraphicsExtractor g, float mouseX, float mouseY, float partialTicks) {
        boolean hovered = isHovered(mouseX, mouseY);
        toggleAnim = AnimationUtil.lerp(toggleAnim, setting.isEnabled() ? 1.0F : 0.0F, 16.0F);
        if (hovered) {
            RenderUtil.drawRoundedRect(g, x + 2.0F, y + 1.0F, width - 4.0F, height - 2.0F, 2.0F, 0x16FFFFFF);
        }

        int accent = ColorUtil.getAccent();
        int textColor = setting.isEnabled() ? ColorUtil.TEXT_PRIMARY : (hovered ? 0xFFC4C4CC : ColorUtil.TEXT_MUTED);
        RenderUtil.drawSmallText(g, setting.getName(), x + 6.0F, y + (height - RenderUtil.getSmallFontHeight()) / 2.0F, textColor, false);

        float pillW = 16.0F;
        float pillH = 8.5F;
        float pillX = x + width - pillW - 6.0F;
        float pillY = y + (height - pillH) / 2.0F;

        int trackCol = ColorUtil.interpolateColor(0xFF282830, accent, toggleAnim);
        RenderUtil.drawRoundedRect(g, pillX, pillY, pillW, pillH, pillH / 2.0F, trackCol);
        if (!setting.isEnabled() || toggleAnim < 0.9F) {
            RenderUtil.drawRoundedOutline(g, pillX, pillY, pillW, pillH, pillH / 2.0F, 1.0F,
                    ColorUtil.interpolateColor(ColorUtil.BORDER, ColorUtil.modifyAlpha(accent, 120), toggleAnim));
        }

        float knobR = 3.0F;
        float knobX = pillX + 1.3F + knobR + (pillW - 2.6F - knobR * 2.0F) * AnimationUtil.easeOutCubic(toggleAnim);
        float knobY = pillY + pillH / 2.0F;
        RenderUtil.drawCircle(g, knobX, knobY, knobR, ColorUtil.interpolateColor(0xFFA0A0AA, 0xFFFFFFFF, toggleAnim));
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            setting.toggle();
        }
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int mouseButton) {}

    @Override
    public void keyTyped(char typedChar, int keyCode) {}

    @Override
    public boolean isVisible() { return setting.isVisible(); }
}
