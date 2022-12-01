package org.walkersguide.android.data;

import java.io.Serializable;
import java.lang.Comparable;
import java.util.Locale;
import java.lang.Math;


public abstract class Angle implements Comparable<Angle>, Serializable {
    private static final long serialVersionUID = 1l;
    private static final int NUMBER_OF_ANGLES = 360;


    public enum Quadrant {

        Q0(-22, 22),
        Q1(23, 67),
        Q2(68, 112),
        Q3(113, 157),
        Q4(158, 202),
        Q5(203, 247),
        Q6(248, 292),
        Q7(293, 337);

        public static Quadrant newInstance(int degree) {
            // exception for left part of q0
            // interval: 338 <= degree <= 359
            if (degree >= (Q0.min + NUMBER_OF_ANGLES) && degree <= (NUMBER_OF_ANGLES - 1)) {
                return Q0;
            }
            // otherwise
            // interval: 0 <= degree <= 337
            for (Quadrant quadrant : Quadrant.values()) {
                if ((degree >= quadrant.min) && (degree <= quadrant.max)) {
                    return quadrant;
                }
            }
            return null;
        }

        public int min, mean, max;

        private Quadrant(int min, int max) {
            this.min = min;
            this.mean = min + ((max - min) / 2);
            this.max = max;
        }
    }


    private int degree;
    private Quadrant quadrant;

    public Angle(int degree) {
        this.degree = sanitise(degree);
        this.quadrant = Quadrant.newInstance(this.degree);
    }

    public int getDegree() {
        return this.degree;
    }

    public Quadrant getQuadrant() {
        return quadrant;
    }

    public boolean withinRange(int min, int max) {
        return min < max
            ? this.degree >= min && this.degree <= max
            : this.degree >= min || this.degree <= max;
    }

    private int sanitise(int degree) {
        degree %= NUMBER_OF_ANGLES;
        if (degree < 0) {
            degree += NUMBER_OF_ANGLES;
        }
        return degree;
    }

    @Override public String toString() {
        return String.format(
                Locale.getDefault(), "%1$d°", this.degree);
    }

	@Override public int hashCode() {
        return this.degree;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof Angle)) {
			return false;
        }
		Angle other = (Angle) obj;
        return this.degree == other.getDegree();
    }

    @Override public int compareTo(Angle other) {
        if (this.degree < other.getDegree()) {
            return -1;
        } else if (this.degree == other.getDegree()) {
            return 0;
        } else {
            return 1;
        }
    }

}
