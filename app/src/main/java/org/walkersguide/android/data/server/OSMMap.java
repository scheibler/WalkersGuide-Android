package org.walkersguide.android.data.server;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;


public class OSMMap {

    private String id, name, description;
    private long created;

    public OSMMap(String id, String name, String description, long created) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.created = created;
    }

    public OSMMap(JSONObject inputData) throws JSONException {
        this.id = inputData.getString("id");
        this.name = inputData.getString("name");
        this.description = inputData.getString("description");
        this.created = inputData.getLong("created");
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public long getCreated() {
        return this.created;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", this.id);
        jsonObject.put("name", this.name);
        jsonObject.put("description", this.description);
        jsonObject.put("created", this.created);
        return jsonObject;
    }

    @Override public String toString() {
        if (! TextUtils.isEmpty(this.description)) {
            return String.format("%1$s\n%2$s", this.name, this.description);
        }
        return this.name;
    }

	@Override public int hashCode() {
        return this.id.hashCode();
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof OSMMap)) {
			return false;
        }
		OSMMap other = (OSMMap) obj;
        return this.id.equals(other.getId());
    }

}
