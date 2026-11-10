package dev.taezeem14.cigarette.mixin.impl;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.event.EventState;
import dev.taezeem14.cigarette.event.impl.KeyEvent;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

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

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onRunTickPre(CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getEventBus() != null) {
            Cigarette.INSTANCE.getEventBus().post(new TickEvent(EventState.PRE));
        }
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void onRunTickPost(CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getEventBus() != null) {
            Cigarette.INSTANCE.getEventBus().post(new TickEvent(EventState.POST));
        }
    }

    @Inject(method = "clickMouse", at = @At("HEAD"))
    private void onClickMouse(CallbackInfo ci) {
    }

    @Inject(method = "rightClickMouse", at = @At("HEAD"))
    private void onRightClickMouse(CallbackInfo ci) {
    }

    @Inject(method = "dispatchKeypresses", at = @At("HEAD"))
    private void onDispatchKeypresses(CallbackInfo ci) {
        int keyCode = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
        if (Keyboard.getEventKeyState() && keyCode > Keyboard.KEY_NONE) {
            if (Cigarette.INSTANCE != null) {
                if (Cigarette.INSTANCE.getEventBus() != null) {
                    Cigarette.INSTANCE.getEventBus().post(new KeyEvent(keyCode));
                }
                if (Cigarette.INSTANCE.getModuleManager() != null) {
                    Cigarette.INSTANCE.getModuleManager().onKeyPressed(keyCode);
                }
            }
        }
    }
}
