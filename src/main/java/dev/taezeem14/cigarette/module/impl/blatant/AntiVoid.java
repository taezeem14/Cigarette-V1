package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;

public class AntiVoid extends Module {

    private final NumberSetting fallDist = new NumberSetting("Fall Distance", 5.0, 3.0, 15.0, 0.5);
    private final ModeSetting mode = new ModeSetting("Mode", "Packet Bounce", "Packet Bounce", "Motion");

    public AntiVoid() {
        super("AntiVoid", "Prevents falling into the void by bouncing or packet spoofing", Category.BLATANT);
        addSettings(fallDist, mode);
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (!event.isPre() || mc.thePlayer == null || mc.theWorld == null) return;

        if (mc.thePlayer.fallDistance > fallDist.getValue() && isOverVoid()) {
            if (mode.is("Packet Bounce")) {
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                        mc.thePlayer.posX,
                        mc.thePlayer.posY + 9.0D,
                        mc.thePlayer.posZ,
                        false
                ));
                mc.thePlayer.fallDistance = 0.0F;
            } else if (mode.is("Motion")) {
                mc.thePlayer.motionY = 0.8D;
                mc.thePlayer.fallDistance = 0.0F;
            }
        }
    }

    private boolean isOverVoid() {
        if (mc.thePlayer.posY < 0) return true;

        for (int y = (int) Math.floor(mc.thePlayer.posY); y >= 0; y--) {
            BlockPos pos = new BlockPos(mc.thePlayer.posX, y, mc.thePlayer.posZ);
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            if (block != null && block != Blocks.air && block.isCollidable()) {
                return false;
            }
        }
        return true;
    }
}
