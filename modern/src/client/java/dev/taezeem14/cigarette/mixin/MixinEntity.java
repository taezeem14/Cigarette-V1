package dev.taezeem14.cigarette.mixin;

import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.module.impl.combat.HitBoxes;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {

    @Inject(method = "getPickRadius", at = @At("RETURN"), cancellable = true)
    private void onGetPickRadius(CallbackInfoReturnable<Float> cir) {
        if ((Object) this != Minecraft.getInstance().player
                && CigaretteModernClient.getInstance() != null && !CigaretteModernClient.getInstance().isDestructed()) {
            float extra = HitBoxes.getExpansionSize();
            if (extra > 0.0F) {
                cir.setReturnValue(cir.getReturnValue() + extra);
            }
        }
    }
}
