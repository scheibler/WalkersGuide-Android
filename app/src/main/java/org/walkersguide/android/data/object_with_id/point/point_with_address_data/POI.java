package org.walkersguide.android.data.object_with_id.point.point_with_address_data;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.R;
import java.io.Serializable;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.point.PointWithAddressData;


public class POI extends PointWithAddressData implements Serializable {
    private static final long serialVersionUID = 1l;

    private ArrayList<Entrance> entranceList;
    private POI isInside;
    private String email, phone, website, openingHours;

    public POI(JSONObject inputData) throws JSONException {
        super(inputData);

        // entrance list
        this.entranceList = new ArrayList<Entrance>();
        if (! inputData.isNull(KEY_ENTRANCE_LIST)) {
            JSONArray jsonEntranceList = inputData.getJSONArray(KEY_ENTRANCE_LIST);
            for (int j=0; j<jsonEntranceList.length(); j++) {
                try {
                    entranceList.add(
                            new Entrance(
                                jsonEntranceList.getJSONObject(j)));
                } catch (JSONException e) {}
            }
        }

        // is inside a building
        if (! inputData.isNull(KEY_IS_INSIDE)) {
            try {
                this.isInside = new POI(inputData.getJSONObject(KEY_IS_INSIDE));
            } catch (JSONException e) {}
        }

        // other optional attributes
        this.email = Helper.getNullableStringFromJsonObject(inputData, KEY_EMAIL);
        this.phone = Helper.getNullableStringFromJsonObject(inputData, KEY_PHONE);
        this.website = Helper.getNullableStringFromJsonObject(inputData, KEY_WEBSITE);
        this.openingHours = Helper.getNullableStringFromJsonObject(inputData, KEY_OPENING_HOURS);
    }

    public ArrayList<Entrance> getEntranceList() {
        return this.entranceList;
    }

    public boolean hasEntrance() {
        return this.entranceList != null && ! this.entranceList.isEmpty();
    }

    public String formatNumberOfEntrances() {
        return GlobalInstance.getPluralResource(
                R.plurals.entrance, hasEntrance() ? this.entranceList.size() : 0);
    }

    public POI getOuterBuilding() {
        return this.isInside;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getWebsite() {
        return this.website;
    }

    public String getOpeningHours() {
        return this.openingHours;
    }

    @Override public String toString() {
        String description = super.toString();
        // number of entrances
        if (hasEntrance()) {
            description += System.lineSeparator();
            description += String.format(
                    "%1$s %2$s",
                    GlobalInstance.getStringResource(R.string.fillingWordHas),
                    formatNumberOfEntrances());
        }
        return description;
    }


    /**
     * to json
     */

    public static final String KEY_ENTRANCE_LIST = "entrance_list";
    public static final String KEY_IS_INSIDE = "is_inside";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_WEBSITE = "website";
    public static final String KEY_OPENING_HOURS = "opening_hours";

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();

        // entrances
        JSONArray jsonEntranceList = new JSONArray();
        for (Entrance entrance : this.entranceList) {
            jsonEntranceList.put(entrance.toJson());
        }
        jsonObject.put(KEY_ENTRANCE_LIST, jsonEntranceList);

        // outer building
        if (this.isInside != null) {
            jsonObject.put(KEY_IS_INSIDE, this.isInside.toJson());
        }

        // other parameters
        if (this.email != null) {
            jsonObject.put(KEY_EMAIL, this.email);
        }
        if (this.phone != null) {
            jsonObject.put(KEY_PHONE, this.phone);
        }
        if (this.website != null) {
            jsonObject.put(KEY_WEBSITE, this.website);
        }
        if (this.openingHours != null) {
            jsonObject.put(KEY_OPENING_HOURS, this.openingHours);
        }

        return jsonObject;
    }

}
