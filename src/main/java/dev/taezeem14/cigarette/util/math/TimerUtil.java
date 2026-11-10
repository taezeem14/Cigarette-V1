package dev.taezeem14.cigarette.util.math;

public class TimerUtil {

    private long lastMS = System.currentTimeMillis();

    public boolean hasTimeElapsed(long time) {
        return System.currentTimeMillis() - lastMS >= time;
    }

    public boolean hasTimeElapsed(double time) {
        return System.currentTimeMillis() - lastMS >= (long) time;
    }

    public void reset() {
        this.lastMS = System.currentTimeMillis();
    }

    public long getTime() {
        return System.currentTimeMillis() - lastMS;
    }

    public void setTime(long time) {
        this.lastMS = time;
    }

    public long getLastMS() {
        return lastMS;
    }
}
