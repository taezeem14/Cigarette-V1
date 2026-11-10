package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.PacketEvent;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.phys.AABB;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Blink extends Module {

    private final BooleanSetting pulse = new BooleanSetting("Pulse", false);
    private final NumberSetting pulseDelay = new NumberSetting("Pulse Delay (ticks)", 10.0, 2.0, 60.0, 1.0);
    private final BooleanSetting spawnClone = new BooleanSetting("Render Origin Marker", true);

    private final Queue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private AABB originBox;
    private int ticks = 0;
    private volatile boolean flushing = false;

    public Blink() {
        super("Blink", "Buffers outgoing movement packets and flushes them in a burst", Category.BLATANT);
        addSettings(pulse, pulseDelay, spawnClone);
        pulseDelay.setVisibility(pulse::isEnabled);
    }

    @Override
    public String getSuffix() {
        return String.valueOf(packetQueue.size());
    }

    @Override
    public void onEnable() {
        packetQueue.clear();
        ticks = 0;
        flushing = false;
        originBox = mc.player != null ? mc.player.getBoundingBox() : null;
    }

    @Override
    public void onDisable() {
        flushPackets();
        originBox = null;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!event.isOutgoing() || flushing || mc.player == null) return;
        Packet<?> p = event.getPacket();
        if (p instanceof ServerboundMovePlayerPacket
                || p instanceof ServerboundPlayerCommandPacket
                || p instanceof ServerboundPlayerActionPacket
                || p instanceof ServerboundInteractPacket) {
            event.cancel();
            packetQueue.add(p);
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.player == null) return;
        if (pulse.isEnabled()) {
            ticks++;
            if (ticks >= pulseDelay.getValueInt()) {
                flushPackets();
                originBox = mc.player.getBoundingBox();
                ticks = 0;
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (spawnClone.isEnabled() && originBox != null) {
            int accent = ColorUtil.getAccent();
            Gizmos.cuboid(originBox, GizmoStyle.strokeAndFill(accent, 1.8F, ColorUtil.modifyAlpha(accent, 55)));
        }
    }

    private void flushPackets() {
        if (flushing || mc.getConnection() == null) {
            packetQueue.clear();
            return;
        }
        flushing = true;
        try {
            while (!packetQueue.isEmpty()) {
                Packet<?> p = packetQueue.poll();
                if (p != null) {
                    mc.getConnection().send(p);
                }
            }
        } finally {
            flushing = false;
        }
    }
}
