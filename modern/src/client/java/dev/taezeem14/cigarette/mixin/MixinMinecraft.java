package dev.taezeem14.cigarette.mixin;

import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.module.impl.combat.AutoClicker;
import dev.taezeem14.cigarette.module.impl.utility.FastPlace;
import dev.taezeem14.cigarette.util.player.CPSTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Shadow protected int missTime;
    @Shadow private int rightClickDelay;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        if (CigaretteModernClient.getInstance() != null && !CigaretteModernClient.getInstance().isDestructed()) {
            this.rightClickDelay = FastPlace.modifyRightClickDelay(this.rightClickDelay);
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (CigaretteModernClient.getInstance() != null && !CigaretteModernClient.getInstance().isDestructed()) {
            CPSTracker.recordLeftClick();
            AutoClicker ac = AutoClicker.getInstance();
            if (ac != null && ac.isEnabled()) {
                this.missTime = 0;
            }
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void onStartUseItem(CallbackInfo ci) {
        if (CigaretteModernClient.getInstance() != null && !CigaretteModernClient.getInstance().isDestructed()) {
            CPSTracker.recordRightClick();
        }
    }

    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void onStartUseItemReturn(CallbackInfo ci) {
        if (CigaretteModernClient.getInstance() != null && !CigaretteModernClient.getInstance().isDestructed()) {
            this.rightClickDelay = FastPlace.modifyRightClickDelay(this.rightClickDelay);
        }
    }
}
