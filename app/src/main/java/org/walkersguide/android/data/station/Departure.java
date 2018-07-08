package org.walkersguide.android.data.station;

import org.walkersguide.android.helper.StringUtility;

public class Departure {

    private String line, direction;
    private long time;

    public Departure(String line, String direction, long time) {
        this.line = line;
        this.direction = direction;
        this.time = time;
    }

    public String getLine() {
        return this.line;
    }

    public String getDirection() {
        return this.direction;
    }

    public long getTime() {
        return this.time;
    }

    public int getRemaining() {
        return (int) ((this.time - System.currentTimeMillis()) / 60000);
    }

    @Override public String toString() {
        return String.format(
                "%1$s\t%2$s\t%3$d Min\t%4$s",
                getLine(), getDirection(), getRemaining(),
                StringUtility.formatHoursMinutes(getTime()));
    }

}
