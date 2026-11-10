package dev.taezeem14.cigarette.mixin;

import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.module.impl.blatant.KillAura;
import dev.taezeem14.cigarette.module.impl.combat.SilentAim;
import dev.taezeem14.cigarette.util.player.MoveUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInput extends ClientInput {

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTickReturn(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || CigaretteModernClient.getInstance() == null || CigaretteModernClient.getInstance().isDestructed()) {
            return;
        }
        float[] kaRots = KillAura.getActiveRotations();
        if (kaRots != null) {
            this.moveVector = MoveUtil.fixMovementVector(this.moveVector, mc.player.getYRot(), kaRots[0], false);
            return;
        }
        float[] silentRots = SilentAim.getActiveRotations();
        if (silentRots != null && SilentAim.isMovementFixEnabled()) {
            this.moveVector = MoveUtil.fixMovementVector(this.moveVector, mc.player.getYRot(), silentRots[0], SilentAim.isStrictMovementFix());
        }
    }
}
