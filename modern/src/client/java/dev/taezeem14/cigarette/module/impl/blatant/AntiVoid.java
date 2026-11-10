package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class AntiVoid extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Setback", "Setback", "Packet");
    private final NumberSetting fallDist = new NumberSetting("Fall Distance", 4.0, 2.0, 10.0, 0.5);

    private double safeX, safeY, safeZ;
    private boolean hasSafePos = false;

    public AntiVoid() {
        super("AntiVoid", "Pulls the player back before falling into the void", Category.BLATANT);
        addSettings(mode, fallDist);
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    @Override
    public void onEnable() {
        hasSafePos = false;
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (!event.isPre() || mc.player == null || mc.level == null) return;

        if (mc.player.onGround()) {
            BlockPos below = BlockPos.containing(mc.player.getX(), mc.player.getY() - 0.5, mc.player.getZ());
            if (!mc.level.getBlockState(below).isAir()) {
                safeX = mc.player.getX();
                safeY = mc.player.getY();
                safeZ = mc.player.getZ();
                hasSafePos = true;
            }
        }

        if (mc.player.fallDistance >= fallDist.getValue() && isOverVoid() && hasSafePos) {
            if (mode.is("Setback")) {
                mc.player.setPos(safeX, safeY, safeZ);
                mc.player.setDeltaMovement(Vec3.ZERO);
                mc.player.fallDistance = 0.0;
            } else if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(safeX, safeY + 0.1, safeZ, false, false));
                mc.player.fallDistance = 0.0;
            }
        }
    }

    private boolean isOverVoid() {
        int minY = mc.level.getMinY();
        int px = (int) Math.floor(mc.player.getX());
        int pz = (int) Math.floor(mc.player.getZ());
        int startY = (int) Math.floor(mc.player.getY());
        for (int y = startY; y >= minY; y--) {
            if (!mc.level.getBlockState(new BlockPos(px, y, pz)).isAir()) {
                return false;
            }
        }
        return true;
    }
}
