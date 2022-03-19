package org.walkersguide.android.data.angle;

import java.io.Serializable;

import org.walkersguide.android.data.Angle;
import org.walkersguide.android.data.Angle.Quadrant;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;


public class Turn extends Angle implements Serializable {
    private static final long serialVersionUID = 1l;


    public enum Instruction {

        CROSS(
                Angle.Quadrant.Q0, GlobalInstance.getStringResource(R.string.instructionCross)),
        TURN_RIGHT_SLIGHTLY(
                Angle.Quadrant.Q1, GlobalInstance.getStringResource(R.string.instructionTurnRightSlightly)),
        TURN_RIGHT(
                Angle.Quadrant.Q2, GlobalInstance.getStringResource(R.string.instructionTurnRight)),
        TURN_RIGHT_STRONGLY(
                Angle.Quadrant.Q3, GlobalInstance.getStringResource(R.string.instructionTurnRightStrongly)),
        TURN_ROUND(
                Angle.Quadrant.Q4, GlobalInstance.getStringResource(R.string.instructionTurnRound)),
        TURN_LEFT_STRONGLY(
                Angle.Quadrant.Q5, GlobalInstance.getStringResource(R.string.instructionTurnLeftStrongly)),
        TURN_LEFT(
                Angle.Quadrant.Q6, GlobalInstance.getStringResource(R.string.instructionTurnLeft)),
        TURN_LEFT_SLIGHTLY(
                Angle.Quadrant.Q7, GlobalInstance.getStringResource(R.string.instructionTurnLeftSlightly));

        public static Instruction newInstance(Quadrant quadrant) {
            if (quadrant != null) {
                for (Instruction instruction : Instruction.values()) {
                    if (instruction.quadrant == quadrant) {
                        return instruction;
                    }
                }
            }
            return null;
        }

        public Quadrant quadrant;
        public String name;

        private Instruction(Quadrant quadrant, String name) {
            this.quadrant = quadrant;
            this.name = name;
        }

        @Override public String toString() {
            return this.name;
        }
    }


    public Turn(int degree) {
        super(degree);
    }

    public Instruction getInstruction() {
        return Instruction.newInstance(super.getQuadrant());
    }

    public Turn shiftBy(int offset) {
        return new Turn(super.getDegree() + offset);
    }

    @Override public String toString() {
        return String.format("%1$s (%2$s)", super.toString(), getInstruction());
    }

}
