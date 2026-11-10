package dev.taezeem14.cigarette.event.impl;

import dev.taezeem14.cigarette.event.Event;

public class KeyEvent extends Event {

    private final int keyCode;

    public KeyEvent(int keyCode) {
        this.keyCode = keyCode;
    }

    public int getKeyCode() {
        return keyCode;
    }
}
