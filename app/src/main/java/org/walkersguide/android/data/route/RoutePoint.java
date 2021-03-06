package org.walkersguide.android.data.route;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;


public class RoutePoint extends PointWrapper {

    private int turn;

    public RoutePoint(Context context, JSONObject inputData) throws JSONException {
        super(context, inputData);
        this.turn = inputData.getInt("turn");;
    }

    public int getTurn() {
        return this.turn;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        jsonObject.put("turn", this.turn);
        return jsonObject;
    }

    @Override public String toString() {
        if (this.turn == -1) {
            return this.getPoint().getName();
        } else if (StringUtility.getDirectionConstant(turn) == Constants.DIRECTION.NORTH) {
            return String.format(
                    super.getContext().getResources().getString(R.string.routePointToStringCross),
                    StringUtility.formatInstructionDirection(super.getContext(), this.turn),
                    this.getPoint().getName());
        } else {
            return String.format(
                    super.getContext().getResources().getString(R.string.routePointToString),
                    StringUtility.formatInstructionDirection(super.getContext(), this.turn),
                    this.getPoint().getName());
        }
    }

	@Override public int hashCode() {
        int hash = 17;
		hash = hash * 31 + this.getPoint().hashCode();
		hash = hash * 31 + this.turn;
        return hash;
    }

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof RoutePoint)) {
			return false;
        }
		RoutePoint other = (RoutePoint) obj;
        return (this.getPoint().equals(other.getPoint())
                && this.turn == other.getTurn());
    }

}
