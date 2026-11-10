package dev.taezeem14.cigarette.event.impl;

import dev.taezeem14.cigarette.event.Event;
import dev.taezeem14.cigarette.event.EventState;

public class TickEvent extends Event {
    private final EventState state;

    public TickEvent(EventState state) {
        this.state = state;
    }

    public EventState getState() {
        return state;
    }

    public boolean isPre() {
        return state == EventState.PRE;
    }

    public boolean isPost() {
        return state == EventState.POST;
    }
}
