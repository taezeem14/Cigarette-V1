package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.mapping.MappingResolver;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

public class SpeedMine extends Module {

    private final NumberSetting speed = new NumberSetting("Speed Multiplier", 1.35, 1.0, 2.5, 0.05);
    private final BooleanSetting noBreakDelay = new BooleanSetting("No Break Delay", true);

    public SpeedMine() {
        super("SpeedMine", "Accelerates block breaking speed and removes mining delay", Category.WORLD);
        addSettings(speed, noBreakDelay);
    }

    @Override
    public String getSuffix() {
        return String.format("%.2fx", speed.getValue());
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.thePlayer == null || mc.theWorld == null || mc.playerController == null) {
            return;
        }

        try {
            if (noBreakDelay.isEnabled()) {
                Integer hitDelay = MappingResolver.getFieldValue(
                        mc.playerController,
                        PlayerControllerMP.class,
                        MappingResolver.PLAYERCONTROLLERMP_BLOCK_HIT_DELAY
                );
                if (hitDelay != null && hitDelay > 0) {
                    MappingResolver.setFieldValue(
                            mc.playerController,
                            0,
                            PlayerControllerMP.class,
                            MappingResolver.PLAYERCONTROLLERMP_BLOCK_HIT_DELAY
                    );
                }
            }

            float multiplier = speed.getValueFloat();
            if (multiplier > 1.0F) {
                Float curDamage = MappingResolver.getFieldValue(
                        mc.playerController,
                        PlayerControllerMP.class,
                        MappingResolver.PLAYERCONTROLLERMP_CUR_BLOCK_DAMAGE
                );
                if (curDamage != null && curDamage > 0.0F && curDamage < 1.0F) {
                    float extra = 0.0F;
                    if (mc.objectMouseOver != null
                            && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                            && mc.objectMouseOver.getBlockPos() != null) {
                        BlockPos pos = mc.objectMouseOver.getBlockPos();
                        Block block = mc.theWorld.getBlockState(pos).getBlock();
                        float hardness = block.getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, pos);
                        if (hardness > 0.0F) {
                            extra = hardness * (multiplier - 1.0F);
                        }
                    }
                    if (extra <= 0.0F) {
                        extra = 0.03F * (multiplier - 1.0F);
                    }
                    float boosted = Math.min(1.0F, curDamage + extra);
                    MappingResolver.setFieldValue(
                            mc.playerController,
                            boosted,
                            PlayerControllerMP.class,
                            MappingResolver.PLAYERCONTROLLERMP_CUR_BLOCK_DAMAGE
                    );
                }
            }
        } catch (Exception ignored) {
        }
    }
}
