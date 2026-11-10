package dev.taezeem14.cigarette.util.player;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class CPSTracker {

    private static final long WINDOW_MS = 1000L;
    private static final Deque<Long> LEFT_CLICKS = new ConcurrentLinkedDeque<>();
    private static final Deque<Long> RIGHT_CLICKS = new ConcurrentLinkedDeque<>();

    private CPSTracker() {
    }

    public static void recordLeftClick() {
        long now = System.currentTimeMillis();
        LEFT_CLICKS.addLast(now);
        prune(LEFT_CLICKS, now);
    }

    public static void recordRightClick() {
        long now = System.currentTimeMillis();
        RIGHT_CLICKS.addLast(now);
        prune(RIGHT_CLICKS, now);
    }

    public static int getLeftCPS() {
        long now = System.currentTimeMillis();
        prune(LEFT_CLICKS, now);
        return LEFT_CLICKS.size();
    }

    public static int getRightCPS() {
        long now = System.currentTimeMillis();
        prune(RIGHT_CLICKS, now);
        return RIGHT_CLICKS.size();
    }

    public static void clear() {
        LEFT_CLICKS.clear();
        RIGHT_CLICKS.clear();
    }

    private static void prune(Deque<Long> queue, long now) {
        long cutoff = now - WINDOW_MS;
        queue.removeIf(timestamp -> timestamp <= cutoff);
    }
}
