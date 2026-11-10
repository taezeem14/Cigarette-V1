package dev.taezeem14.cigarette.mixin.impl;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.event.impl.AttackEvent;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(EntityPlayer playerIn, Entity targetEntity, CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed() && Cigarette.INSTANCE.getEventBus() != null) {
            AttackEvent event = new AttackEvent(targetEntity);
            Cigarette.INSTANCE.getEventBus().post(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }
}
