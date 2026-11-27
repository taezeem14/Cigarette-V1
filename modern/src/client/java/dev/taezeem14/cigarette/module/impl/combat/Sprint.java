package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.blatant.NoSlow;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import net.minecraft.world.phys.Vec2;

public class Sprint extends Module {

    private final BooleanSetting omniDirectional = new BooleanSetting("Omni-Directional", false);

    public Sprint() {
        super("Sprint", "Automatically maintains sprinting while moving", Category.COMBAT);
        addSettings(omniDirectional);
        setState(true);
    }

    @Override
    public String getSuffix() {
        return omniDirectional.isEnabled() ? "Omni" : "Legit";
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.player == null || mc.level == null || mc.player.input == null) return;
        if (mc.player.isShiftKeyDown() || mc.player.horizontalCollision) return;
        if (mc.player.isUsingItem() && !NoSlow.shouldCancelSlowdown()) return;
        if (!mc.player.getAbilities().mayfly && mc.player.getFoodData().getFoodLevel() <= 6) return;

        Vec2 move = mc.player.input.getMoveVector();
        boolean moving = omniDirectional.isEnabled()
                ? (move.y != 0.0F || move.x != 0.0F)
                : (move.y > 0.0F);
        if (moving) {
            mc.player.setSprinting(true);
        }
    }
}
