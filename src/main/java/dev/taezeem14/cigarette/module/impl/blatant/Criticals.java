package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.AttackEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import net.minecraft.network.play.client.C03PacketPlayer;

public class Criticals extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Packet", "Packet", "MiniHop");

    public Criticals() {
        super("Criticals", "Forces critical strikes on every attack", Category.BLATANT);
        addSettings(mode);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (mc.thePlayer == null || !mc.thePlayer.onGround) return;
        if (mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || mc.thePlayer.isOnLadder()) return;

        double x = mc.thePlayer.posX;
        double y = mc.thePlayer.posY;
        double z = mc.thePlayer.posZ;

        if (mode.is("Packet")) {
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.0625D, z, false));
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.0125D, z, false));
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
        } else if (mode.is("MiniHop")) {
            mc.thePlayer.motionY = 0.1D;
        }
    }
}
