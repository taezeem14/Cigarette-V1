package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import net.minecraft.network.play.client.C03PacketPlayer;

public class NoFall extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Packet", "Packet", "NoGround");

    public NoFall() {
        super("NoFall", "Prevents fall damage when landing from heights", Category.WORLD);
        addSettings(mode);
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (!event.isPre() || mc.thePlayer == null) {
            return;
        }

        if (mode.is("Packet")) {
            if (mc.thePlayer.fallDistance > 2.5F && mc.getNetHandler() != null) {
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer(true));
                mc.thePlayer.fallDistance = 0.0F;
            }
        } else if (mode.is("NoGround")) {
            event.setOnGround(false);
        }
    }
}
