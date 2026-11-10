package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class BridgeAssist extends Module {

    private final ModeSetting yawSnap = new ModeSetting("Yaw Snap", "45 Degrees", "45 Degrees", "90 Degrees", "None");
    private final BooleanSetting pitchAssist = new BooleanSetting("Pitch Assist", true);
    private final NumberSetting assistedPitch = new NumberSetting("Assisted Pitch", 78.0, 70.0, 85.0, 0.5);

    public BridgeAssist() {
        super("BridgeAssist", "Assists crosshair alignment for fast bridging", Category.UTILITY);
        addSettings(yawSnap, pitchAssist, assistedPitch);
    }

    @Override
    public String getSuffix() {
        return !yawSnap.is("None") ? yawSnap.getMode() : "Pitch";
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.player == null || mc.level == null) return;
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty() || !(held.getItem() instanceof BlockItem)) return;

        BlockPos under = BlockPos.containing(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ());
        if (!mc.level.getBlockState(under).isAir()) return;

        float sens = RotationUtil.getMouseSensitivity();
        if (!yawSnap.is("None")) {
            float snapAngle = yawSnap.is("45 Degrees") ? 45.0F : 90.0F;
            float curYaw = mc.player.getYRot();
            float wrapped = RotationUtil.wrapAngleTo180(curYaw);
            float snapped = Math.round(wrapped / snapAngle) * snapAngle;
            float delta = RotationUtil.wrapAngleTo180(snapped - wrapped);
            if (Math.abs(delta) < 18.0F) {
                mc.player.setYRot(RotationUtil.applySensitivityGCD(curYaw, curYaw + delta, sens));
            }
        }
        if (pitchAssist.isEnabled()) {
            float curPitch = mc.player.getXRot();
            if (Math.abs(curPitch - assistedPitch.getValueFloat()) < 8.0F) {
                mc.player.setXRot(RotationUtil.applySensitivityGCD(curPitch, assistedPitch.getValueFloat(), sens));
            }
        }
    }
}
