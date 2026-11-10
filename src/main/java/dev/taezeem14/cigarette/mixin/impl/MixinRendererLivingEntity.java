package dev.taezeem14.cigarette.mixin.impl;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.module.Module;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity<T extends EntityLivingBase> {

    @Inject(method = "doRender*", at = @At("HEAD"))
    private void injectChamsPre(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            Module chams = Cigarette.INSTANCE.getModuleManager().getModuleByName("Chams");
            if (chams != null && chams.isEnabled()) {
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                GL11.glPolygonOffset(1.0F, -1000000.0F);
            }
        }
    }

    @Inject(method = "doRender*", at = @At("RETURN"))
    private void injectChamsPost(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            Module chams = Cigarette.INSTANCE.getModuleManager().getModuleByName("Chams");
            if (chams != null && chams.isEnabled()) {
                GL11.glPolygonOffset(1.0F, 1000000.0F);
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            }
        }
    }

    @Inject(method = "canRenderName*", at = @At("HEAD"), cancellable = true)
    private void onCanRenderName(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            Module nametags = Cigarette.INSTANCE.getModuleManager().getModuleByName("Nametags");
            if (nametags != null && nametags.isEnabled()) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "renderName*", at = @At("HEAD"), cancellable = true)
    private void onRenderName(T entity, double x, double y, double z, CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            Module nametags = Cigarette.INSTANCE.getModuleManager().getModuleByName("Nametags");
            if (nametags != null && nametags.isEnabled()) {
                ci.cancel();
            }
        }
    }
}
