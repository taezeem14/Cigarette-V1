package dev.taezeem14.cigarette.mixin.impl;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.module.impl.render.Chams;
import dev.taezeem14.cigarette.module.impl.render.Nametags;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity<T extends EntityLivingBase> {

    @Inject(method = "doRender*", at = @At("HEAD"))
    private void injectChamsPre(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed() && Chams.shouldChams(entity)) {
            Chams.preRenderCallback(entity);
        }
    }

    @Inject(method = "doRender*", at = @At("RETURN"))
    private void injectChamsPost(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed() && Chams.shouldChams(entity)) {
            Chams.postRenderCallback(entity);
        }
    }

    @Inject(method = "canRenderName*", at = @At("HEAD"), cancellable = true)
    private void onCanRenderName(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed() && Nametags.shouldCancelVanillaNametag(entity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "renderName*", at = @At("HEAD"), cancellable = true)
    private void onRenderName(T entity, double x, double y, double z, CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && !Cigarette.INSTANCE.isDestructed() && Nametags.shouldCancelVanillaNametag(entity)) {
            ci.cancel();
        }
    }
}
