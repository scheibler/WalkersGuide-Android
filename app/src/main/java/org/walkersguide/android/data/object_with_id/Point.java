package org.walkersguide.android.data.object_with_id;

import org.walkersguide.android.database.profile.FavoritesProfile;
import org.walkersguide.android.database.DatabaseProfile;

import org.walkersguide.android.data.object_with_id.point.point_with_address_data.Entrance;
import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import org.walkersguide.android.data.object_with_id.point.PedestrianCrossing;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.POI;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.poi.Station;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.angle.RelativeBearing;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.data.object_with_id.common.TactilePaving;
import org.walkersguide.android.data.object_with_id.common.Wheelchair;

import android.location.Location;
import android.location.LocationManager;


import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;
import java.io.Serializable;
import org.walkersguide.android.util.Helper;
import android.text.TextUtils;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.database.util.AccessDatabase;
import androidx.annotation.NonNull;
import java.util.Locale;
import android.content.Intent;
import android.view.SubMenu;
import android.view.Menu;
import android.content.Context;


public class Point extends ObjectWithId implements Serializable {
    private static final long serialVersionUID = 1l;


    /**
     * object creation helpers
     */

    protected enum Type {
        POINT, ENTRANCE, GPS, INTERSECTION, PEDESTRIAN_CROSSING, POI, STATION, STREET_ADDRESS;

        public static Type fromString(String name) {
            for (Type type : Type.values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }

        @Override public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public static Point create(JSONObject jsonPoint) throws JSONException {
        Type type = Type.fromString(jsonPoint.optString(KEY_TYPE, ""));
        if (type != null) {
            switch (type) {
                case POINT:
                    return new Point(jsonPoint);
                case ENTRANCE:
                    return new Entrance(jsonPoint);
                case GPS:
                    return new GPS(jsonPoint);
                case INTERSECTION:
                    return new Intersection(jsonPoint);
                case PEDESTRIAN_CROSSING:
                    return new PedestrianCrossing(jsonPoint);
                case POI:
                    return new POI(jsonPoint);
                case STATION:
                    return new Station(jsonPoint);
                case STREET_ADDRESS:
                    return new StreetAddress(jsonPoint);
            }
        }
        throw new JSONException("Invalid point type");
    }

    public static Point load(long id) {
        ObjectWithId object = AccessDatabase.getInstance().getObjectWithId(id);
        if (object instanceof Point) {
            return (Point) object;
        }
        return null;
    }


    public abstract static class Builder {
        public JSONObject inputData;
        public Builder(Type type, String name, double latitude, double longitude) {
            this.inputData = new JSONObject();
            try {
                this.inputData.put(KEY_TYPE, type.toString());
                this.inputData.put(KEY_NAME, name);
                this.inputData.put(KEY_SUB_TYPE, "");
                this.inputData.put(KEY_LATITUDE, latitude);
                this.inputData.put(KEY_LONGITUDE, longitude);
            } catch (JSONException e) {}
        }
        public abstract Point build() throws JSONException;
    }


    /**
     * constructor
     */

    // mandatory params
    private String name, type, subType;
    private double latitude, longitude;
    // optional params
    private String description, altName, oldName, note, wikidataId;
    private TactilePaving tactilePaving;
    private Wheelchair wheelchair;

    public Point(JSONObject inputData) throws JSONException {
        super(Helper.getNullableAndPositiveLongFromJsonObject(inputData, KEY_ID));

        // mandatory params
        this.name = inputData.getString(KEY_NAME);
        this.type = inputData.getString(KEY_TYPE);
        this.subType = inputData.getString(KEY_SUB_TYPE);
        this.latitude = inputData.getDouble(KEY_LATITUDE);
        this.longitude = inputData.getDouble(KEY_LONGITUDE);

        // optional parameters
        this.description = Helper.getNullableStringFromJsonObject(inputData, KEY_DESCRIPTION);
        this.altName = Helper.getNullableStringFromJsonObject(inputData, KEY_ALT_NAME);
        this.oldName = Helper.getNullableStringFromJsonObject(inputData, KEY_OLD_NAME);
        this.note = Helper.getNullableStringFromJsonObject(inputData, KEY_NOTE);
        this.wikidataId = Helper.getNullableStringFromJsonObject(inputData, KEY_WIKIDATA_ID);
        this.tactilePaving = TactilePaving.lookUpById(
                Helper.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_TACTILE_PAVING));
        this.wheelchair = Wheelchair.lookUpById(
                Helper.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_WHEELCHAIR));
    }


    /**
     * mandatory params
     */

    public String getOriginalName() {
        return this.name;
    }

    public String getSubType() {
        return this.subType;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public Location getLocationObject() {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(this.latitude);
        location.setLongitude(this.longitude);
        return location;
    }


    /**
     * optional params
     */
    private static final String WIKIDATA_BASE_URL = "https://m.wikidata.org/wiki/%1$s";

    public String getDescription() {
        return this.description;
    }

    public String getAltName() {
        return this.altName;
    }

    public String getOldName() {
        return this.oldName;
    }

    public String getNote() {
        return this.note;
    }

    public String getWikidataUrl() {
        if (this.wikidataId != null) {
            return String.format(WIKIDATA_BASE_URL, this.wikidataId);
        }
        return null;
    }

    public TactilePaving getTactilePaving() {
        return this.tactilePaving;
    }

    public Wheelchair getWheelchair() {
        return this.wheelchair;
    }


    /**
     * helper functions
     */

    public String formatNameAndSubType() {
        String customOrOriginalName = getName();
        if (! TextUtils.isEmpty(getSubType())
                && ! customOrOriginalName.toLowerCase(Locale.getDefault())
                        .contains(getSubType().toLowerCase(Locale.getDefault()))) {
            return String.format("%1$s (%2$s)", customOrOriginalName, getSubType());
        }
        return customOrOriginalName;
    }

    public String formatCoordinates() {
        return String.format(
                Locale.getDefault(),
                "%1$s: %2$f, %3$f",
                GlobalInstance.getStringResource(R.string.labelGPSCoordinates),
                this.latitude,
                this.longitude);
    }

    public String formatLatitude() {
        return String.format(
                Locale.getDefault(),
                "%1$s: %2$f",
                GlobalInstance.getStringResource(R.string.labelGPSLatitude),
                this.latitude);
    }

    public String formatLongitude() {
        return String.format(
                Locale.getDefault(),
                "%1$s: %2$f",
                GlobalInstance.getStringResource(R.string.labelGPSLongitude),
                this.longitude);
    }

    public String formatDistanceAndRelativeBearingFromCurrentLocation(int distancePluralResourceId) {
        return formatDistanceAndRelativeBearingFromCurrentLocation(distancePluralResourceId, false);
    }

    public String formatDistanceAndRelativeBearingFromCurrentLocation(
            int distancePluralResourceId, boolean showPreciseBearingValues) {
        Integer distance = distanceFromCurrentLocation();
        Bearing bearing = bearingFromCurrentLocation();
        if (distance != null && bearing != null) {
            RelativeBearing relativeBearing = bearing.relativeToCurrentBearing();
            if (relativeBearing != null) {
                String output = String.format(
                        Locale.getDefault(),
                        "%1$s, %2$s",
                        GlobalInstance.getPluralResource(distancePluralResourceId, distance),
                        relativeBearing.getDirection());
                if (showPreciseBearingValues) {
                    output += " ";
                    output += String.format(
                            Locale.ROOT,
                            GlobalInstance.getStringResource(R.string.preciseBearingValues),
                            relativeBearing.getDegree());
                }
                return output;
            }
        }
        return "";
    }

    public Integer distanceFromCurrentLocation() {
        Point currentLocation = PositionManager.getInstance().getCurrentLocation();
        if (currentLocation != null) {
            return currentLocation.distanceTo(this);
        }
        return null;
    }

    public Integer distanceTo(Point other) {
        if (other != null) {
            return Integer.valueOf(
                    (int) Math.round(
                        this.getLocationObject().distanceTo(other.getLocationObject())));
        }
        return null;
    }

    public Bearing bearingFromCurrentLocation() {
        Point currentLocation = PositionManager.getInstance().getCurrentLocation();
        if (currentLocation != null) {
            return currentLocation.bearingTo(this);
        }
        return null;
    }

    public Bearing bearingTo(Point other) {
        if (other != null) {
            return new Bearing(
                    (int) Math.round(
                        this.getLocationObject().bearingTo(other.getLocationObject())));
        }
        return null;
    }


    /**
     * share coordinates
     */

    // share submenu
    public static final int MENU_ITEM_SHARE_OSM_ORG_LINK  = 512911;
    public static final int MENU_ITEM_SHARE_GOOGLE_MAPS_LINK  = 512912;
    public static final int MENU_ITEM_SHARE_APPLE_MAPS_LINK  = 512913;

    public static void populateShareCoordinatesSubMenuEntries(SubMenu shareCoordinatesSubMenu) {
        shareCoordinatesSubMenu.add(
                Menu.NONE, MENU_ITEM_SHARE_OSM_ORG_LINK, 1, GlobalInstance.getStringResource(R.string.objectMenuItemShareOsmOrgLink));
        shareCoordinatesSubMenu.add(
                Menu.NONE, MENU_ITEM_SHARE_GOOGLE_MAPS_LINK, 2, GlobalInstance.getStringResource(R.string.objectMenuItemShareGoogleMapsLink));
        shareCoordinatesSubMenu.add(
                Menu.NONE, MENU_ITEM_SHARE_APPLE_MAPS_LINK, 3, GlobalInstance.getStringResource(R.string.objectMenuItemShareAppleMapsLink));
    }

    // share intent
    private static final String APPLE_MAPS_COORDINATES_URL = "https://maps.apple.com/?ll=%1$f,%2$f";
    private static final String GOOGLE_MAPS_COORDINATES_URL = "https://maps.google.com/maps?q=%1$f,%2$f";
    private static final String OSM_ORG_COORDINATES_URL = "https://www.openstreetmap.org/?mlat=%1$f&mlon=%2$f&zoom=18";

    public enum SharingService {
        OSM_ORG, GOOGLE_MAPS, APPLE_MAPS
    }

    public void startShareCoordinatesChooserActivity(@NonNull Context context, @NonNull SharingService service) {
        String shareLink = null;
        switch (service) {
            case APPLE_MAPS:
                shareLink = String.format(
                        Locale.ROOT, APPLE_MAPS_COORDINATES_URL, this.latitude, this.longitude);
                break;
            case GOOGLE_MAPS:
                shareLink = String.format(
                        Locale.ROOT, GOOGLE_MAPS_COORDINATES_URL, this.latitude, this.longitude);
                break;
            case OSM_ORG:
                shareLink = String.format(
                        Locale.ROOT, OSM_ORG_COORDINATES_URL, this.latitude, this.longitude);
                break;
        }

        Intent shareLinkIntent = new Intent();
        shareLinkIntent.setAction(Intent.ACTION_SEND);
        shareLinkIntent.putExtra(Intent.EXTRA_TEXT, shareLink);
        shareLinkIntent.setType("text/plain");

        context.startActivity(
                Intent.createChooser(shareLinkIntent, null));
    }


    /**
     * super class methods
     */

    public FavoritesProfile getDefaultFavoritesProfile() {
        return FavoritesProfile.favoritePoints();
    }

    @Override public String toString() {
        return formatNameAndSubType();
    }


    /**
     * to json
     */

    public static final String KEY_ID = "node_id";
    // mandatory params
    public static final String KEY_NAME = "name";
    public static final String KEY_TYPE = "type";
    public static final String KEY_SUB_TYPE = "sub_type";
    public static final String KEY_LATITUDE = "lat";
    public static final String KEY_LONGITUDE = "lon";
    // optional params
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_ALT_NAME = "alt_name";
    public static final String KEY_OLD_NAME = "old_name";
    public static final String KEY_NOTE = "note";
    public static final String KEY_WIKIDATA_ID = "wikidata";
    public static final String KEY_TACTILE_PAVING = "tactile_paving";
    public static final String KEY_WHEELCHAIR = "wheelchair";

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KEY_ID, this.getId());

        // mandatory params
        jsonObject.put(KEY_NAME, this.name);
        jsonObject.put(KEY_TYPE, this.type);
        jsonObject.put(KEY_SUB_TYPE, this.subType);
        jsonObject.put(KEY_LATITUDE, this.latitude);
        jsonObject.put(KEY_LONGITUDE, this.longitude);

        // optional parameters
        if (this.description != null) {
            jsonObject.put(KEY_DESCRIPTION, this.description);
        }
        if (this.altName != null) {
            jsonObject.put(KEY_ALT_NAME, this.altName);
        }
        if (this.oldName != null) {
            jsonObject.put(KEY_OLD_NAME, this.oldName);
        }
        if (this.note != null) {
            jsonObject.put(KEY_NOTE, this.note);
        }
        if (this.wikidataId != null) {
            jsonObject.put(KEY_WIKIDATA_ID, this.wikidataId);
        }
        if (this.tactilePaving != null) {
            jsonObject.put(KEY_TACTILE_PAVING, this.tactilePaving.id);
        }
        if (this.wheelchair != null) {
            jsonObject.put(KEY_WHEELCHAIR, this.wheelchair.id);
        }

        return jsonObject;
    }

    public static JSONObject addNodeIdToJsonObject(JSONObject jsonPoint) throws JSONException {
        if (jsonPoint.isNull("node_id")) {
            jsonPoint.put("node_id", ObjectWithId.generateId());
        }
        return jsonPoint;
    }

}
