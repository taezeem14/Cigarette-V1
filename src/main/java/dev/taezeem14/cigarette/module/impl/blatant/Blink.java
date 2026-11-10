package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.PacketEvent;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Blink extends Module {

    private final BooleanSetting pulse = new BooleanSetting("Pulse", false);
    private final NumberSetting pulseDelay = new NumberSetting("Pulse Delay", 200.0, 50.0, 1000.0, 50.0);

    private final Queue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private EntityOtherPlayerMP fakePlayer;
    private final TimerUtil pulseTimer = new TimerUtil();
    private volatile boolean flushing = false;

    public Blink() {
        super("Blink", "Suspends outgoing movement packets to teleport or choke updates", Category.BLATANT);
        addSettings(pulse, pulseDelay);
    }

    @Override
    public String getSuffix() {
        return String.valueOf(packetQueue.size());
    }

    @Override
    public void onEnable() {
        packetQueue.clear();
        flushing = false;
        pulseTimer.reset();

        if (mc.thePlayer != null && mc.theWorld != null) {
            fakePlayer = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
            fakePlayer.copyLocationAndAnglesFrom(mc.thePlayer);
            fakePlayer.rotationYawHead = mc.thePlayer.rotationYawHead;
            fakePlayer.renderYawOffset = mc.thePlayer.renderYawOffset;
            fakePlayer.inventory.copyInventory(mc.thePlayer.inventory);
            mc.theWorld.addEntityToWorld(-1337, fakePlayer);
        }
    }

    @Override
    public void onDisable() {
        flushPackets();
        removeFakePlayer();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (flushing || !event.isOutgoing() || mc.thePlayer == null) return;

        Packet<?> packet = event.getPacket();
        if (packet instanceof C03PacketPlayer || packet instanceof C0BPacketEntityAction
                || packet instanceof C0APacketAnimation || packet instanceof C02PacketUseEntity
                || packet instanceof C07PacketPlayerDigging || packet instanceof C08PacketPlayerBlockPlacement) {
            packetQueue.add(packet);
            event.cancel();
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre()) return;

        if (pulse.isEnabled() && pulseTimer.hasTimeElapsed(pulseDelay.getValueLong())) {
            flushPackets();
            if (fakePlayer != null && mc.thePlayer != null) {
                fakePlayer.copyLocationAndAnglesFrom(mc.thePlayer);
                fakePlayer.rotationYawHead = mc.thePlayer.rotationYawHead;
                fakePlayer.renderYawOffset = mc.thePlayer.renderYawOffset;
            }
            pulseTimer.reset();
        }
    }

    private void flushPackets() {
        if (mc.getNetHandler() == null || mc.getNetHandler().getNetworkManager() == null) {
            packetQueue.clear();
            return;
        }

        flushing = true;
        try {
            while (!packetQueue.isEmpty()) {
                Packet<?> p = packetQueue.poll();
                if (p != null) {
                    mc.getNetHandler().getNetworkManager().sendPacket(p);
                }
            }
        } finally {
            flushing = false;
        }
    }

    private void removeFakePlayer() {
        if (fakePlayer != null && mc.theWorld != null) {
            mc.theWorld.removeEntityFromWorld(fakePlayer.getEntityId());
            fakePlayer = null;
        }
    }

    public int getQueuedCount() {
        return packetQueue.size();
    }
}
