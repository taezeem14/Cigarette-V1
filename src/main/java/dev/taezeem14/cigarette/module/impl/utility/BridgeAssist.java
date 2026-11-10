package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;

public class BridgeAssist extends Module {

    private final ModeSetting yawSnap = new ModeSetting("Yaw Snap", "45 Degrees", "45 Degrees", "90 Degrees", "None");
    private final BooleanSetting pitchAssist = new BooleanSetting("Pitch Assist", true);
    private final NumberSetting assistedPitch = new NumberSetting("Assisted Pitch", 78.0, 70.0, 85.0, 0.5);

    public BridgeAssist() {
        super("BridgeAssist", "Assists crosshair snapping to angles for fast bridging", Category.UTILITY);
        addSettings(yawSnap, pitchAssist, assistedPitch);
    }

    @Override
    public String getSuffix() {
        return !yawSnap.is("None") ? yawSnap.getMode() : "Pitch";
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.thePlayer == null || mc.theWorld == null) return;

        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemBlock)) return;

        BlockPos under = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ);
        if (!mc.theWorld.isAirBlock(under)) return;

        if (!yawSnap.is("None")) {
            float snapAngle = yawSnap.is("45 Degrees") ? 45.0F : 90.0F;
            float currentYaw = mc.thePlayer.rotationYaw;
            float wrappedYaw = RotationUtil.wrapAngleTo180(currentYaw);
            float snapped = Math.round(wrappedYaw / snapAngle) * snapAngle;

            if (Math.abs(RotationUtil.wrapAngleTo180(snapped - wrappedYaw)) < 18.0F) {
                float delta = RotationUtil.wrapAngleTo180(snapped - wrappedYaw);
                mc.thePlayer.rotationYaw = RotationUtil.applySensitivityGCD(
                        currentYaw,
                        currentYaw + delta,
                        mc.gameSettings.mouseSensitivity
                );
            }
        }

        if (pitchAssist.isEnabled()) {
            if (Math.abs(mc.thePlayer.rotationPitch - assistedPitch.getValueFloat()) < 8.0F) {
                mc.thePlayer.rotationPitch = RotationUtil.applySensitivityGCD(
                        mc.thePlayer.rotationPitch,
                        assistedPitch.getValueFloat(),
                        mc.gameSettings.mouseSensitivity
                );
            }
        }
    }
}
