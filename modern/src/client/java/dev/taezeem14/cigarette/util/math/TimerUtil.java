package dev.taezeem14.cigarette.util.math;

public class TimerUtil {

    private long lastMS = System.currentTimeMillis();

    public void reset() {
        this.lastMS = System.currentTimeMillis();
    }

    public boolean hasTimeElapsed(long time) {
        return System.currentTimeMillis() - this.lastMS >= time;
    }

    public boolean hasTimeElapsed(long time, boolean reset) {
        if (hasTimeElapsed(time)) {
            if (reset) {
                reset();
            }
            return true;
        }
        return false;
    }

    public long getTimeElapsed() {
        return System.currentTimeMillis() - this.lastMS;
    }

    public long getLastMS() {
        return lastMS;
    }

    public void setTime(long time) {
        this.lastMS = time;
    }
}
