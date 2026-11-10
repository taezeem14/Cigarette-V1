package dev.taezeem14.cigarette.ui.hud;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.module.impl.render.HUD;
import dev.taezeem14.cigarette.module.impl.render.TargetHUD;
import dev.taezeem14.cigarette.ui.clickgui.ClickGUI;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class HUDEditor extends GuiScreen {

    private boolean draggingWatermark = false;
    private boolean draggingArrayList = false;
    private boolean draggingTargetHUD = false;

    private float dragX = 0.0f;
    private float dragY = 0.0f;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        // Subtle dark grid background
        RenderUtil.drawRect(0, 0, screenW, screenH, 0x99101014);

        // Draw helper guide lines
        for (int i = 0; i < screenW; i += 40) {
            RenderUtil.drawRect(i, 0, 1.0f, screenH, 0x08FFFFFF);
        }
        for (int i = 0; i < screenH; i += 40) {
            RenderUtil.drawRect(0, i, screenW, 1.0f, 0x08FFFFFF);
        }

        // Center crosshair
        RenderUtil.drawRect(screenW / 2.0f, 0, 1.0f, screenH, 0x15FFFFFF);
        RenderUtil.drawRect(0, screenH / 2.0f, screenW, 1.0f, 0x15FFFFFF);

        HUD hud = getHUD();
        TargetHUD targetHUD = getTargetHUD();

        // 1. Watermark box
        if (hud != null) {
            float wmX = hud.getWatermarkX();
            float wmY = hud.getWatermarkY();
            float wmW = 110.0f;
            float wmH = 20.0f;

            if (draggingWatermark) {
                hud.setWatermarkX(mouseX - dragX);
                hud.setWatermarkY(mouseY - dragY);
                wmX = hud.getWatermarkX();
                wmY = hud.getWatermarkY();
            }

            RenderUtil.drawRoundedRect(wmX, wmY, wmW, wmH, 3.0f, 0x88141416);
            RenderUtil.drawRoundedOutline(wmX, wmY, wmW, wmH, 3.0f, 1.0f, draggingWatermark ? ColorUtil.ACCENT : 0xFF3882F6);
            FontUtil.bold.drawString("Cigarette V1 §7[Dev]", wmX + 6.0f, wmY + (wmH - FontUtil.bold.getHeight()) / 2.0f, ColorUtil.TEXT_PRIMARY);

            // 2. ArrayList box
            float alX = hud.getArrayListX();
            float alY = hud.getArrayListY();
            float alW = 90.0f;
            float alH = 100.0f;

            if (draggingArrayList) {
                hud.setArrayListX(mouseX - dragX);
                hud.setArrayListY(mouseY - dragY);
                alX = hud.getArrayListX();
                alY = hud.getArrayListY();
            }

            RenderUtil.drawRoundedRect(alX, alY, alW, alH, 3.0f, 0x88141416);
            RenderUtil.drawRoundedOutline(alX, alY, alW, alH, 3.0f, 1.0f, draggingArrayList ? ColorUtil.ACCENT : 0xFF3882F6);
            FontUtil.small.drawString("ArrayList", alX + 6.0f, alY + 4.0f, ColorUtil.ACCENT);
            FontUtil.small.drawString("KillAura", alX + 6.0f, alY + 18.0f, ColorUtil.TEXT_PRIMARY);
            FontUtil.small.drawString("Velocity", alX + 6.0f, alY + 30.0f, ColorUtil.TEXT_PRIMARY);
            FontUtil.small.drawString("Speed", alX + 6.0f, alY + 42.0f, ColorUtil.TEXT_PRIMARY);
            FontUtil.small.drawString("ESP", alX + 6.0f, alY + 54.0f, ColorUtil.TEXT_PRIMARY);
        }

        // 3. TargetHUD box
        if (targetHUD != null) {
            float thX = targetHUD.getX();
            float thY = targetHUD.getY();
            float thW = 140.0f;
            float thH = 42.0f;

            if (draggingTargetHUD) {
                targetHUD.setX(mouseX - dragX);
                targetHUD.setY(mouseY - dragY);
                thX = targetHUD.getX();
                thY = targetHUD.getY();
            }

            RenderUtil.drawRoundedRect(thX, thY, thW, thH, 4.0f, 0x88141416);
            RenderUtil.drawRoundedOutline(thX, thY, thW, thH, 4.0f, 1.0f, draggingTargetHUD ? ColorUtil.ACCENT : 0xFF3882F6);
            FontUtil.bold.drawString("TargetHUD Preview", thX + 6.0f, thY + 4.0f, ColorUtil.TEXT_PRIMARY);
            FontUtil.small.drawString("20.0 HP (100%)", thX + 6.0f, thY + 18.0f, ColorUtil.TEXT_MUTED);
            RenderUtil.drawRoundedRect(thX + 6.0f, thY + 30.0f, thW - 12.0f, 6.0f, 2.0f, 0xFF4CAF50);
        }

        // Instruction header
        float bannerW = 280.0f;
        float bannerH = 26.0f;
        float bannerX = (screenW - bannerW) / 2.0f;
        float bannerY = 12.0f;
        RenderUtil.drawDropShadow(bannerX, bannerY, bannerW, bannerH, 4.0f, 0x66000000);
        RenderUtil.drawRoundedRect(bannerX, bannerY, bannerW, bannerH, 4.0f, ColorUtil.HEADER);
        RenderUtil.drawRoundedOutline(bannerX, bannerY, bannerW, bannerH, 4.0f, 1.0f, 0xFF282830);
        FontUtil.regular.drawCenteredString("HUD Position Editor - Drag elements with Left Click", screenW / 2.0f, bannerY + (bannerH - FontUtil.regular.getHeight()) / 2.0f, ColorUtil.TEXT_PRIMARY);

        // Escape instruction
        FontUtil.small.drawCenteredString("Press ESC or Right Click to Return", screenW / 2.0f, screenH - 20.0f, ColorUtil.TEXT_MUTED);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 1) {
            mc.displayGuiScreen(new ClickGUI());
            return;
        }

        HUD hud = getHUD();
        TargetHUD targetHUD = getTargetHUD();

        if (hud != null) {
            // Watermark click
            float wmX = hud.getWatermarkX();
            float wmY = hud.getWatermarkY();
            if (mouseX >= wmX && mouseX <= wmX + 110.0f && mouseY >= wmY && mouseY <= wmY + 20.0f) {
                draggingWatermark = true;
                dragX = mouseX - wmX;
                dragY = mouseY - wmY;
                return;
            }

            // ArrayList click
            float alX = hud.getArrayListX();
            float alY = hud.getArrayListY();
            if (mouseX >= alX && mouseX <= alX + 90.0f && mouseY >= alY && mouseY <= alY + 100.0f) {
                draggingArrayList = true;
                dragX = mouseX - alX;
                dragY = mouseY - alY;
                return;
            }
        }

        if (targetHUD != null) {
            float thX = targetHUD.getX();
            float thY = targetHUD.getY();
            if (mouseX >= thX && mouseX <= thX + 140.0f && mouseY >= thY && mouseY <= thY + 42.0f) {
                draggingTargetHUD = true;
                dragX = mouseX - thX;
                dragY = mouseY - thY;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingWatermark = false;
        draggingArrayList = false;
        draggingTargetHUD = false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(new ClickGUI());
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private HUD getHUD() {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            return Cigarette.INSTANCE.getModuleManager().getModuleByClass(HUD.class);
        }
        return null;
    }

    private TargetHUD getTargetHUD() {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            return Cigarette.INSTANCE.getModuleManager().getModuleByClass(TargetHUD.class);
        }
        return null;
    }
}
