package dev.taezeem14.cigarette.mixin.impl;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.event.EventState;
import dev.taezeem14.cigarette.event.impl.KeyEvent;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.impl.combat.AutoClicker;
import dev.taezeem14.cigarette.util.player.CPSTracker;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Shadow
    public GuiScreen currentScreen;

    @Shadow
    public int leftClickCounter;

    @Inject(method = "startGame", at = @At("RETURN"))
    private void onStartGame(CallbackInfo ci) {
        if (Cigarette.INSTANCE == null) {
            new Cigarette().init();
        }
    }

    @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
    private void onShutdown(CallbackInfo ci) {
        if (Cigarette.INSTANCE != null) {
            Cigarette.INSTANCE.shutdown();
        }
    }

    @Inject(method = "runGameLoop", at = @At("HEAD"))
    private void onRunGameLoop(CallbackInfo ci) {
        if (Cigarette.INSTANCE == null || !Cigarette.INSTANCE.isDestructed()) {
            AnimationUtil.updateDeltaTime();
        }
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onRunTickPre(CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed() && Cigarette.INSTANCE.getEventBus() != null) {
            Cigarette.INSTANCE.getEventBus().post(new TickEvent(EventState.PRE));
        }
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void onRunTickPost(CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed() && Cigarette.INSTANCE.getEventBus() != null) {
            Cigarette.INSTANCE.getEventBus().post(new TickEvent(EventState.POST));
        }
    }

    @Inject(method = "clickMouse", at = @At("HEAD"))
    private void onClickMouse(CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed()) {
            CPSTracker.recordLeftClick();
            AutoClicker autoClicker = Cigarette.INSTANCE.getModuleManager() != null
                    ? Cigarette.INSTANCE.getModuleManager().getModuleByClass(AutoClicker.class)
                    : null;
            if ((autoClicker != null && autoClicker.isEnabled()) || (this.leftClickCounter > 0 && this.leftClickCounter <= 10)) {
                this.leftClickCounter = 0;
            }
        }
    }

    @Inject(method = "rightClickMouse", at = @At("HEAD"))
    private void onRightClickMouse(CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed()) {
            CPSTracker.recordRightClick();
        }
    }

    @Inject(method = "dispatchKeypresses", at = @At("HEAD"))
    private void onDispatchKeypresses(CallbackInfo ci) {
        if (this.currentScreen != null || Keyboard.isRepeatEvent() || !Keyboard.getEventKeyState()) {
            return;
        }
        int keyCode = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
        if (keyCode > Keyboard.KEY_NONE && Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed()) {
            if (Cigarette.INSTANCE.getEventBus() != null) {
                Cigarette.INSTANCE.getEventBus().post(new KeyEvent(keyCode));
            }
            if (Cigarette.INSTANCE.getModuleManager() != null) {
                Cigarette.INSTANCE.getModuleManager().onKeyPressed(keyCode);
            }
        }
    }
}
