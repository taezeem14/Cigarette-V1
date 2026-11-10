package dev.taezeem14.cigarette.mixin;

import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.module.impl.blatant.Timer;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DeltaTracker.Timer.class)
public class MixinDeltaTrackerTimer {

    @Redirect(method = "advanceGameTime", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/floats/FloatUnaryOperator;apply(F)F"))
    private float redirectTargetMspt(FloatUnaryOperator instance, float msPerTick) {
        float baseMspt = instance.apply(msPerTick);
        if (CigaretteModernClient.getInstance() != null && !CigaretteModernClient.getInstance().isDestructed()) {
            float mult = Timer.getActiveTimerSpeed();
            if (mult != 1.0F && mult > 0.05F) {
                return baseMspt / mult;
            }
        }
        return baseMspt;
    }
}
