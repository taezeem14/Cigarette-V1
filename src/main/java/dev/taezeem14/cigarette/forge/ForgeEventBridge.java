package dev.taezeem14.cigarette.forge;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.event.impl.KeyEvent;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.util.render.FrustumUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public class ForgeEventBridge {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static long lastKeyTime = 0L;
    private static int lastKeyHandled = -1;
    public static volatile long lastMixinRender2DTime = 0L;
    public static volatile long lastMixinRender3DTime = 0L;

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (mc.currentScreen != null || !Keyboard.getEventKeyState() || Keyboard.isRepeatEvent()) {
            return;
        }

        int keyCode = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
        if (keyCode <= Keyboard.KEY_NONE) return;

        long now = System.currentTimeMillis();
        if (keyCode == lastKeyHandled && (now - lastKeyTime) < 60L) {
            return; // Already processed by MixinMinecraft
        }

        lastKeyHandled = keyCode;
        lastKeyTime = now;

        if (Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed()) {
            if (Cigarette.INSTANCE.getEventBus() != null) {
                Cigarette.INSTANCE.getEventBus().post(new KeyEvent(keyCode));
            }
            if (Cigarette.INSTANCE.getModuleManager() != null) {
                Cigarette.INSTANCE.getModuleManager().onKeyPressed(keyCode);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL && event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }

        long now = System.currentTimeMillis();
        // If MixinGuiIngame rendered within the last 15ms, skip to avoid double draw
        if ((now - lastMixinRender2DTime) < 15L) {
            return;
        }

        if (Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed() && Cigarette.INSTANCE.getEventBus() != null) {
            ScaledResolution sr = event.resolution;
            if (sr == null && mc.displayWidth > 0 && mc.displayHeight > 0) {
                sr = new ScaledResolution(mc);
            }
            if (sr != null) {
                Cigarette.INSTANCE.getEventBus().post(new Render2DEvent(event.partialTicks, sr));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        long now = System.currentTimeMillis();
        // If MixinEntityRenderer rendered within the last 15ms, skip to avoid double draw
        if ((now - lastMixinRender3DTime) < 15L) {
            return;
        }

        if (Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed()) {
            FrustumUtil.updateFrustum();
            if (Cigarette.INSTANCE.getEventBus() != null) {
                Cigarette.INSTANCE.getEventBus().post(new Render3DEvent(event.partialTicks));
            }
        }
    }
}
