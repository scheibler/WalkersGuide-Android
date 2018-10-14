package org.walkersguide.android.data.basic.segment;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;


public abstract class Segment {

    private Context context;
    private String name, type, subType;

    public Segment(Context context, JSONObject inputData) throws JSONException {
        this.context = context;
        this.name = inputData.getString("name");
        this.type = inputData.getString("type");
        this.subType = inputData.getString("sub_type");
    }

    public Context getContext() {
        return this.context;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public String getSubType() {
        return this.subType;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", this.name);
        jsonObject.put("type", this.type);
        jsonObject.put("sub_type", this.subType);
        return jsonObject;
    }

    @Override public String toString() {
        if (this.subType.equals("")
                || this.name.equals(this.subType)) {
            return this.name;
        }
        return this.name + " (" + this.subType + ")";
    }

	@Override public int hashCode() {
        int hash = 17;
		hash = hash * 31 + this.name.hashCode();
		hash = hash * 31 + this.type.hashCode();
		return hash;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof Segment)) {
			return false;
        }
		Segment other = (Segment) obj;
        return this.name.equals(other.getName())
            && this.type.equals(other.getType());
    }

}
