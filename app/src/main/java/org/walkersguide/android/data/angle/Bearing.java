package org.walkersguide.android.data.angle;

import timber.log.Timber;

import java.io.Serializable;

import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.data.Angle;
import org.walkersguide.android.data.Angle.Quadrant;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;
import java.lang.Math;


public class Bearing extends Angle implements Serializable {
    private static final long serialVersionUID = 1l;

    public enum Orientation {

        NORTH(
                Angle.Quadrant.Q0, GlobalInstance.getStringResource(R.string.orientationNorth)),
        NORTH_EAST(
                Angle.Quadrant.Q1, GlobalInstance.getStringResource(R.string.orientationNorthEast)),
        EAST(
                Angle.Quadrant.Q2, GlobalInstance.getStringResource(R.string.orientationEast)),
        SOUTH_EAST(
                Angle.Quadrant.Q3, GlobalInstance.getStringResource(R.string.orientationSouthEast)),
        SOUTH(
                Angle.Quadrant.Q4, GlobalInstance.getStringResource(R.string.orientationSouth)),
        SOUTH_WEST(
                Angle.Quadrant.Q5, GlobalInstance.getStringResource(R.string.orientationSouthWest)),
        WEST(
                Angle.Quadrant.Q6, GlobalInstance.getStringResource(R.string.orientationWest)),
        NORTH_WEST(
                Angle.Quadrant.Q7, GlobalInstance.getStringResource(R.string.orientationNorthWest));

        public static Orientation newInstance(Quadrant quadrant) {
            if (quadrant != null) {
                for (Orientation orientation : Orientation.values()) {
                    if (orientation.quadrant == quadrant) {
                        return orientation;
                    }
                }
            }
            return null;
        }

        public Quadrant quadrant;
        public String name;

        private Orientation(Quadrant quadrant, String name) {
            this.quadrant = quadrant;
            this.name = name;
        }

        @Override public String toString() {
            return this.name;
        }
    }


    public Bearing(int degree) {
        super(degree);
    }

    public Orientation getOrientation() {
        return Orientation.newInstance(super.getQuadrant());
    }

    public Bearing shiftBy(int offset) {
        return new Bearing(super.getDegree() + offset);
    }

    public Bearing inverse() {
        return shiftBy(180);
    }

    public RelativeBearing relativeToCurrentBearing() {
        return relativeTo(DeviceSensorManager.getInstance().getCurrentBearing());
    }

    public RelativeBearing relativeTo(Bearing bearing) {
        if (bearing != null) {
            return new RelativeBearing(
                    super.getDegree() - bearing.getDegree());
        }
        return null;
    }

    public Turn turnTo(Bearing bearing) {
        if (bearing != null) {
            return new Turn(
                    bearing.getDegree() - super.getDegree());
        }
        return null;
    }

    @Override public String toString() {
        return String.format("%1$s (%2$s)", super.toString(), getOrientation());
    }

}
