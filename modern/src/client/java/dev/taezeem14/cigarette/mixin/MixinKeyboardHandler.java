package dev.taezeem14.cigarette.mixin;

import dev.taezeem14.cigarette.CigaretteModernClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onKeyPress(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (action != 1 || this.minecraft.gui.screen() != null || event == null || event.key() <= 0) {
            return;
        }
        CigaretteModernClient inst = CigaretteModernClient.getInstance();
        if (inst == null || inst.isDestructed()) {
            return;
        }
        if (inst.getEventBus() != null) {
            inst.getEventBus().post(new dev.taezeem14.cigarette.event.impl.KeyEvent(event.key()));
        }
        if (inst.getModuleManager() != null) {
            inst.getModuleManager().onKeyPressed(event.key());
        }
    }
}
