package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.blatant.NoSlow;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.util.player.MoveUtil;
import net.minecraft.potion.Potion;

public class Sprint extends Module {

    private final BooleanSetting omni = new BooleanSetting("Omni-Directional", false);

    public Sprint() {
        super("Sprint", "Automatically sprints while moving", Category.COMBAT);
        addSettings(omni);
        setState(true);
    }

    @Override
    public String getSuffix() {
        return omni.isEnabled() ? "Omni" : "Legit";
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.thePlayer == null || mc.thePlayer.movementInput == null) {
            return;
        }

        if (mc.thePlayer.isSneaking()
                || mc.thePlayer.isCollidedHorizontally
                || mc.thePlayer.isPotionActive(Potion.blindness)) {
            return;
        }

        if (mc.thePlayer.isUsingItem() && !NoSlow.shouldCancelSlowdown()) {
            return;
        }

        if (mc.thePlayer.getFoodStats().getFoodLevel() <= 6 && !mc.thePlayer.capabilities.allowFlying) {
            return;
        }

        if ((omni.isEnabled() && MoveUtil.isMoving()) || mc.thePlayer.movementInput.moveForward > 0.0F) {
            mc.thePlayer.setSprinting(true);
        }
    }
}
