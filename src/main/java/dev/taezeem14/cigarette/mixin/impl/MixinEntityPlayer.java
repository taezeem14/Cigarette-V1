package dev.taezeem14.cigarette.mixin.impl;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.module.impl.blatant.KeepSprint;
import dev.taezeem14.cigarette.module.impl.blatant.KillAura;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer {

    @Redirect(method = "attackTargetEntityWithCurrentItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;setSprinting(Z)V"))
    private void onAttackSetSprinting(EntityPlayer instance, boolean sprinting) {
        if (instance == Minecraft.getMinecraft().thePlayer
                && Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed()
                && (KeepSprint.shouldKeepSprint() || KillAura.shouldKeepSprint())) {
            double multiplier = KeepSprint.shouldKeepSprint() ? KeepSprint.getSlowdownMultiplier() : 1.0D;
            instance.motionX = (instance.motionX / 0.6D) * multiplier;
            instance.motionZ = (instance.motionZ / 0.6D) * multiplier;
            return;
        }
        instance.setSprinting(sprinting);
    }
}
