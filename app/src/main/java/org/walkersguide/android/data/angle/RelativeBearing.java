package org.walkersguide.android.data.angle;

import java.io.Serializable;

import org.walkersguide.android.data.Angle;
import org.walkersguide.android.data.Angle.Quadrant;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;


public class RelativeBearing extends Angle implements Serializable {
    private static final long serialVersionUID = 1l;


    public enum Direction {

        STRAIGHT_AHEAD(
                Angle.Quadrant.Q0, GlobalInstance.getStringResource(R.string.directionStraightAhead)),
        RIGHT_SLIGHTLY(
                Angle.Quadrant.Q1, GlobalInstance.getStringResource(R.string.directionRightSlightly)),
        RIGHT(
                Angle.Quadrant.Q2, GlobalInstance.getStringResource(R.string.directionRight)),
        RIGHT_STRONGLY(
                Angle.Quadrant.Q3, GlobalInstance.getStringResource(R.string.directionRightStrongly)),
        BEHIND(
                Angle.Quadrant.Q4, GlobalInstance.getStringResource(R.string.directionBehind)),
        LEFT_STRONGLY(
                Angle.Quadrant.Q5, GlobalInstance.getStringResource(R.string.directionLeftStrongly)),
        LEFT(
                Angle.Quadrant.Q6, GlobalInstance.getStringResource(R.string.directionLeft)),
        LEFT_SLIGHTLY(
                Angle.Quadrant.Q7, GlobalInstance.getStringResource(R.string.directionLeftSlightly));

        public static Direction newInstance(Quadrant quadrant) {
            if (quadrant != null) {
                for (Direction direction : Direction.values()) {
                    if (direction.quadrant == quadrant) {
                        return direction;
                    }
                }
            }
            return null;
        }

        public Quadrant quadrant;
        public String name;

        private Direction(Quadrant quadrant, String name) {
            this.quadrant = quadrant;
            this.name = name;
        }

        @Override public String toString() {
            return this.name;
        }
    }


    public RelativeBearing(int degree) {
        super(degree);
    }

    public Direction getDirection() {
        return Direction.newInstance(super.getQuadrant());
    }

    public RelativeBearing shiftBy(int offset) {
        return new RelativeBearing(super.getDegree() + offset);
    }

    @Override public String toString() {
        return String.format("%1$s (%2$s)", super.toString(), getDirection());
    }

}
