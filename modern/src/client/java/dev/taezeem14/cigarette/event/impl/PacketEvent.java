package dev.taezeem14.cigarette.event.impl;

import dev.taezeem14.cigarette.event.Event;
import net.minecraft.network.protocol.Packet;

public class PacketEvent extends Event {

    public enum Direction {
        INCOMING,
        OUTGOING
    }

    private final Direction direction;
    private Packet<?> packet;

    public PacketEvent(Direction direction, Packet<?> packet) {
        this.direction = direction;
        this.packet = packet;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isIncoming() {
        return direction == Direction.INCOMING;
    }

    public boolean isOutgoing() {
        return direction == Direction.OUTGOING;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }
}
