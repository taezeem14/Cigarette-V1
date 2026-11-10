package dev.taezeem14.cigarette.event;

public interface Cancellable {
    boolean isCancelled();

    void setCancelled(boolean cancelled);
}
