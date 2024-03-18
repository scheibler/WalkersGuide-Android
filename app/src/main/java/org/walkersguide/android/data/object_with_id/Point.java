package org.walkersguide.android.data.object_with_id;

import org.walkersguide.android.data.object_with_id.common.Coordinates;

import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.data.ObjectWithId.Icon;
import org.walkersguide.android.data.object_with_id.common.TactilePaving;
import org.walkersguide.android.data.object_with_id.common.Wheelchair;



import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;
import java.io.Serializable;
import org.walkersguide.android.util.Helper;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import java.util.Locale;
import android.content.Intent;
import android.view.SubMenu;
import android.view.Menu;
import android.content.Context;


public class Point extends ObjectWithId implements Serializable {
    private static final long serialVersionUID = 1l;

    public enum Type {
        POINT, ENTRANCE, GPS, INTERSECTION, PEDESTRIAN_CROSSING, POI, STATION, STREET_ADDRESS
    }


    /**
     * object creation helpers
     */

    public static Point fromJson(JSONObject jsonObject) throws JSONException {
        return castToPointOrReturnNull(ObjectWithId.fromJson(jsonObject));
    }

    public static Point load(long id) {
        return castToPointOrReturnNull(ObjectWithId.load(id));
    }

    private static Point castToPointOrReturnNull(ObjectWithId objectWithId) {
        return objectWithId instanceof Point ? (Point) objectWithId : null;
    }


    public static class Builder extends ObjectWithId.Builder {
        public Builder(Type type, String name, double latitude, double longitude) throws JSONException {
            super(
                    type,
                    TextUtils.isEmpty(name)
                    ? String.format(
                        Locale.getDefault(), "%1$.3f, %2$.3f", latitude, longitude)
                    : name);
            super.inputData.put(KEY_SUB_TYPE, "");
            super.inputData.put(KEY_LATITUDE, latitude);
            super.inputData.put(KEY_LONGITUDE, longitude);
        }

        public Point build() throws JSONException {
            return new Point(super.inputData);
        }
    }


    /**
     * constructor
     */

    // mandatory params
    private String subType;
    private Coordinates coordinates;
    // optional params
    private String inscription, altName, oldName, note, wikidataId;
    private TactilePaving tactilePaving;
    private Wheelchair wheelchair;

    public Point(JSONObject inputData) throws JSONException {
        super(
                Helper.getNullableAndPositiveLongFromJsonObject(inputData, KEY_ID),
                Helper.getNullableEnumFromJsonObject(inputData, ObjectWithId.KEY_TYPE, Type.class),
                inputData);
        this.subType = inputData.getString(KEY_SUB_TYPE);
        this.coordinates = new Coordinates(
                inputData.getDouble(KEY_LATITUDE),
                inputData.getDouble(KEY_LONGITUDE));

        // optional parameters
        this.inscription = Helper.getNullableStringFromJsonObject(inputData, KEY_INSCRIPTION);
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

    public String getSubType() {
        return this.subType;
    }


    /**
     * optional params
     */
    private static final String WIKIDATA_BASE_URL = "https://m.wikidata.org/wiki/%1$s";

    public String getInscription() {
        return this.inscription;
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
                "%1$s: %2$s",
                GlobalInstance.getStringResource(R.string.labelGPSCoordinates),
                this.coordinates.toString());
    }

    public String formatLatitude() {
        return String.format(
                Locale.getDefault(),
                "%1$s: %2$f",
                GlobalInstance.getStringResource(R.string.labelGPSLatitude),
                this.coordinates.getLatitude());
    }

    public String formatLongitude() {
        return String.format(
                Locale.getDefault(),
                "%1$s: %2$f",
                GlobalInstance.getStringResource(R.string.labelGPSLongitude),
                this.coordinates.getLongitude());
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
                Menu.NONE, MENU_ITEM_SHARE_OSM_ORG_LINK, 1, GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdShareOsmOrgLink));
        shareCoordinatesSubMenu.add(
                Menu.NONE, MENU_ITEM_SHARE_GOOGLE_MAPS_LINK, 2, GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdShareGoogleMapsLink));
        shareCoordinatesSubMenu.add(
                Menu.NONE, MENU_ITEM_SHARE_APPLE_MAPS_LINK, 3, GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdShareAppleMapsLink));
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
        double latitude = this.coordinates.getLatitude();
        double longitude = this.coordinates.getLongitude();

        switch (service) {
            case APPLE_MAPS:
                shareLink = String.format(
                        Locale.ROOT, APPLE_MAPS_COORDINATES_URL, latitude, longitude);
                break;
            case GOOGLE_MAPS:
                shareLink = String.format(
                        Locale.ROOT, GOOGLE_MAPS_COORDINATES_URL, latitude, longitude);
                break;
            case OSM_ORG:
                shareLink = String.format(
                        Locale.ROOT, OSM_ORG_COORDINATES_URL, latitude, longitude);
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

    @Override public Type getType() {
        return (Type) super.getType();
    }

    @Override public Icon getIcon() {
        return Icon.POINT;
    }

    @Override public Coordinates getCoordinates() {
        return this.coordinates;
    }

    @Override public String toString() {
        return formatNameAndSubType();
    }


    /**
     * to json
     */

    public static final String KEY_ID = "node_id";
    // mandatory params
    public static final String KEY_SUB_TYPE = "sub_type";
    public static final String KEY_LATITUDE = "lat";
    public static final String KEY_LONGITUDE = "lon";
    // optional params
    public static final String KEY_INSCRIPTION = "inscription";
    public static final String KEY_ALT_NAME = "alt_name";
    public static final String KEY_OLD_NAME = "old_name";
    public static final String KEY_NOTE = "note";
    public static final String KEY_WIKIDATA_ID = "wikidata";
    public static final String KEY_TACTILE_PAVING = "tactile_paving";
    public static final String KEY_WHEELCHAIR = "wheelchair";

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        jsonObject.put(KEY_ID, this.getId());

        // mandatory params
        jsonObject.put(KEY_SUB_TYPE, this.subType);
        jsonObject.put(KEY_LATITUDE, this.coordinates.getLatitude());
        jsonObject.put(KEY_LONGITUDE, this.coordinates.getLongitude());

        // optional parameters
        if (this.inscription != null) {
            jsonObject.put(KEY_INSCRIPTION, this.inscription);
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

}
