package dev.taezeem14.cigarette.mixin;

import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.module.impl.render.Nametags;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity> {

    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void onShouldShowName(T entity, double squaredDistance, CallbackInfoReturnable<Boolean> cir) {
        if (CigaretteModernClient.getInstance() != null && !CigaretteModernClient.getInstance().isDestructed()
                && Nametags.shouldCancelVanillaNametag(entity)) {
            cir.setReturnValue(false);
        }
    }
}
