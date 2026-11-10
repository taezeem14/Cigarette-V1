package dev.taezeem14.cigarette.mixin;

import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.event.impl.AttackEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Player player, Entity targetEntity, CallbackInfo ci) {
        if (CigaretteModernClient.getInstance() != null && !CigaretteModernClient.getInstance().isDestructed()
                && player == Minecraft.getInstance().player) {
            AttackEvent event = new AttackEvent(targetEntity);
            CigaretteModernClient.getInstance().getEventBus().post(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }
}
