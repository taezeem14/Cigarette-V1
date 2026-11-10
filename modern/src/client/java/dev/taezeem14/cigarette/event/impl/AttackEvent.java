package dev.taezeem14.cigarette.event.impl;

import dev.taezeem14.cigarette.event.Event;
import net.minecraft.world.entity.Entity;

public class AttackEvent extends Event {

    private final Entity target;

    public AttackEvent(Entity target) {
        this.target = target;
    }

    public Entity getTarget() {
        return target;
    }
}
