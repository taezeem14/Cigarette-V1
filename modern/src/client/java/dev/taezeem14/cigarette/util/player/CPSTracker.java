package dev.taezeem14.cigarette.util.player;

import java.util.ArrayDeque;
import java.util.Deque;

public final class CPSTracker {

    private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>();
    private static final Deque<Long> RIGHT_CLICKS = new ArrayDeque<>();

    private CPSTracker() {
    }

    public static synchronized void recordLeftClick() {
        long now = System.currentTimeMillis();
        LEFT_CLICKS.addLast(now);
        prune(LEFT_CLICKS, now);
    }

    public static synchronized void recordRightClick() {
        long now = System.currentTimeMillis();
        RIGHT_CLICKS.addLast(now);
        prune(RIGHT_CLICKS, now);
    }

    public static synchronized int getLeftCPS() {
        long now = System.currentTimeMillis();
        prune(LEFT_CLICKS, now);
        return LEFT_CLICKS.size();
    }

    public static synchronized int getRightCPS() {
        long now = System.currentTimeMillis();
        prune(RIGHT_CLICKS, now);
        return RIGHT_CLICKS.size();
    }

    public static synchronized void clear() {
        LEFT_CLICKS.clear();
        RIGHT_CLICKS.clear();
    }

    private static void prune(Deque<Long> queue, long now) {
        while (!queue.isEmpty() && now - queue.peekFirst() > 1000L) {
            queue.removeFirst();
        }
    }
}
