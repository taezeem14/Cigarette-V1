package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class NoFall extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Packet", "Packet", "NoGround");

    private boolean savedOnGround = false;
    private boolean spoofingNoGround = false;

    public NoFall() {
        super("NoFall", "Prevents or mitigates fall damage", Category.WORLD);
        addSettings(mode);
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    @Override
    public void onDisable() {
        if (spoofingNoGround && mc.player != null) {
            mc.player.setOnGround(savedOnGround);
        }
        spoofingNoGround = false;
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.player == null || mc.level == null || mc.player.isCreative() || mc.player.isSpectator()) {
            spoofingNoGround = false;
            return;
        }

        if (mode.is("Packet")) {
            if (event.isPre() && mc.player.fallDistance > 2.5 && mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, mc.player.horizontalCollision));
                mc.player.fallDistance = 0.0;
            }
        } else if (mode.is("NoGround")) {
            if (event.isPre()) {
                savedOnGround = mc.player.onGround();
                spoofingNoGround = true;
                mc.player.setOnGround(false);
                event.setOnGround(false);
            } else if (event.isPost() && spoofingNoGround) {
                mc.player.setOnGround(savedOnGround);
                spoofingNoGround = false;
            }
        }
    }
}
