package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.AttackEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class Criticals extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Packet", "Packet", "Mini-Jump", "NoGround");

    public Criticals() {
        super("Criticals", "Forces critical strikes on targets", Category.BLATANT);
        addSettings(mode);
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (!mc.player.onGround() || mc.player.isInWater() || mc.player.onClimbable()) return;

        if (mode.is("Packet") && mc.getConnection() != null) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, true, mc.player.horizontalCollision));
            mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, mc.player.horizontalCollision));
            mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y + 1.1E-5, z, false, mc.player.horizontalCollision));
            mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, mc.player.horizontalCollision));
        } else if (mode.is("Mini-Jump")) {
            Vec3 delta = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(delta.x, 0.25, delta.z);
        } else if (mode.is("NoGround")) {
            mc.player.setOnGround(false);
        }
    }
}
