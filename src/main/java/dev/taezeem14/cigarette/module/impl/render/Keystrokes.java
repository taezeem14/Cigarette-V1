package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import dev.taezeem14.cigarette.util.player.CPSTracker;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.settings.KeyBinding;

public class Keystrokes extends Module {

    private final BooleanSetting showCps = new BooleanSetting("Show CPS", true);
    private final BooleanSetting showSpace = new BooleanSetting("Show Spacebar", true);
    private final BooleanSetting chroma = new BooleanSetting("Chroma Text", false);

    private float x = 8.0f;
    private float y = 34.0f;
    private final float[] pressAnim = new float[7];

    public Keystrokes() {
        super("Keystrokes", "Displays real-time movement keys, mouse buttons, and live CPS", Category.RENDER);
        addSettings(showCps, showSpace, chroma);
        setState(true);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.gameSettings == null || mc.gameSettings.showDebugInfo) {
            return;
        }

        FontUtil.ensureInitialized();
        int accent = ColorUtil.getAccent();

        // Row 0: W (centered)
        renderKeyTile(0, "W", null, x + 22.0f, y, 20.0f, 20.0f, mc.gameSettings.keyBindForward, accent, 0);

        // Row 1: A, S, D
        renderKeyTile(1, "A", null, x, y + 22.0f, 20.0f, 20.0f, mc.gameSettings.keyBindLeft, accent, 120);
        renderKeyTile(2, "S", null, x + 22.0f, y + 22.0f, 20.0f, 20.0f, mc.gameSettings.keyBindBack, accent, 240);
        renderKeyTile(3, "D", null, x + 44.0f, y + 22.0f, 20.0f, 20.0f, mc.gameSettings.keyBindRight, accent, 360);

        // Row 2: LMB, RMB
        String lmbSub = showCps.isEnabled() ? CPSTracker.getLeftCPS() + " CPS" : null;
        String rmbSub = showCps.isEnabled() ? CPSTracker.getRightCPS() + " CPS" : null;
        renderKeyTile(4, "LMB", lmbSub, x, y + 44.0f, 31.0f, 18.0f, mc.gameSettings.keyBindAttack, accent, 480);
        renderKeyTile(5, "RMB", rmbSub, x + 33.0f, y + 44.0f, 31.0f, 18.0f, mc.gameSettings.keyBindUseItem, accent, 600);

        // Row 3: SPACE
        if (showSpace.isEnabled()) {
            renderSpaceTile(6, x, y + 64.0f, 64.0f, 14.0f, mc.gameSettings.keyBindJump, accent, 720);
        }
    }

    private void renderKeyTile(int animIdx, String label, String subLabel, float kx, float ky, float kw, float kh,
                               KeyBinding binding, int accent, int chromaOffset) {
        boolean pressed = isKeyPressed(binding);
        pressAnim[animIdx] = AnimationUtil.lerp(pressAnim[animIdx], pressed ? 1.0f : 0.0f, 18.0f);
        float anim = pressAnim[animIdx];

        int bgColor = ColorUtil.interpolateColor(0xB4141418, ColorUtil.modifyAlpha(accent, 195), anim);
        int borderColor = ColorUtil.interpolateColor(ColorUtil.modifyAlpha(ColorUtil.BORDER, 190), accent, anim);

        RenderUtil.drawRoundedRect(kx, ky, kw, kh, 3.5f, bgColor);
        RenderUtil.drawRoundedOutline(kx, ky, kw, kh, 3.5f, 1.0f, borderColor);

        int textColor = chroma.isEnabled()
                ? ColorUtil.getRainbow(4, chromaOffset)
                : ColorUtil.interpolateColor(ColorUtil.TEXT_PRIMARY, 0xFFFFFFFF, anim);

        if (subLabel != null) {
            FontUtil.bold.drawCenteredString(label, kx + kw / 2.0f, ky + 2.5f, textColor);
            int subColor = chroma.isEnabled()
                    ? textColor
                    : ColorUtil.interpolateColor(ColorUtil.TEXT_MUTED, 0xFFEFEFFF, anim);
            FontUtil.small.drawCenteredString(subLabel, kx + kw / 2.0f, ky + kh - FontUtil.small.getHeight() - 1.5f, subColor);
        } else {
            FontUtil.bold.drawCenteredString(label, kx + kw / 2.0f, ky + (kh - FontUtil.bold.getHeight()) / 2.0f, textColor);
        }
    }

    private void renderSpaceTile(int animIdx, float kx, float ky, float kw, float kh,
                                 KeyBinding binding, int accent, int chromaOffset) {
        boolean pressed = isKeyPressed(binding);
        pressAnim[animIdx] = AnimationUtil.lerp(pressAnim[animIdx], pressed ? 1.0f : 0.0f, 18.0f);
        float anim = pressAnim[animIdx];

        int bgColor = ColorUtil.interpolateColor(0xB4141418, ColorUtil.modifyAlpha(accent, 195), anim);
        int borderColor = ColorUtil.interpolateColor(ColorUtil.modifyAlpha(ColorUtil.BORDER, 190), accent, anim);

        RenderUtil.drawRoundedRect(kx, ky, kw, kh, 3.5f, bgColor);
        RenderUtil.drawRoundedOutline(kx, ky, kw, kh, 3.5f, 1.0f, borderColor);

        int barColor = chroma.isEnabled()
                ? ColorUtil.getRainbow(4, chromaOffset)
                : ColorUtil.interpolateColor(ColorUtil.TEXT_PRIMARY, 0xFFFFFFFF, anim);

        float barW = 28.0f + 8.0f * anim;
        RenderUtil.drawRoundedRect(kx + (kw - barW) / 2.0f, ky + (kh - 2.0f) / 2.0f, barW, 2.0f, 1.0f, barColor);
    }

    private boolean isKeyPressed(KeyBinding binding) {
        if (binding == null) return false;
        if (binding.isKeyDown()) return true;
        return mc.currentScreen == null && RotationUtil.isPhysicalKeyDown(binding.getKeyCode());
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return 64.0f;
    }

    public float getHeight() {
        return showSpace.isEnabled() ? 78.0f : 62.0f;
    }
}
