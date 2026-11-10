package dev.taezeem14.cigarette.mixin;

import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.module.impl.blatant.KeepSprint;
import dev.taezeem14.cigarette.module.impl.blatant.KillAura;
import dev.taezeem14.cigarette.module.impl.combat.Reach;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class MixinPlayer {

    @Inject(method = "entityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void onEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
        if ((Object) this == Minecraft.getInstance().player
                && CigaretteModernClient.getInstance() != null && !CigaretteModernClient.getInstance().isDestructed()) {
            cir.setReturnValue(Reach.getReachDistance(cir.getReturnValue()));
        }
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"))
    private void redirectAttackSetSprinting(Player player, boolean sprinting) {
        if (CigaretteModernClient.getInstance() != null && !CigaretteModernClient.getInstance().isDestructed()
                && player == Minecraft.getInstance().player
                && (KeepSprint.shouldKeepSprint() || KillAura.shouldKeepSprint())) {
            double mult = KeepSprint.shouldKeepSprint() ? KeepSprint.getVelocityMultiplier() : 1.0;
            if (mult != 0.6) {
                double rescale = mult / 0.6;
                Vec3 delta = player.getDeltaMovement();
                player.setDeltaMovement(delta.x * rescale, delta.y, delta.z * rescale);
            }
            player.setSprinting(true);
            return;
        }
        player.setSprinting(sprinting);
    }
}
