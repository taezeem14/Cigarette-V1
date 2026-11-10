package dev.taezeem14.cigarette.mixin;

import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.module.impl.render.Fullbright;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Inject(method = "hasEffect", at = @At("HEAD"), cancellable = true)
    private void onHasEffect(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == Minecraft.getInstance().player
                && CigaretteModernClient.getInstance() != null && !CigaretteModernClient.getInstance().isDestructed()
                && Fullbright.shouldApplyFullbright()
                && effect != null && effect.equals(MobEffects.NIGHT_VISION)) {
            cir.setReturnValue(true);
        }
    }
}
