package org.walkersguide.android.util;

import org.walkersguide.android.database.profiles.DatabaseRouteProfile;
import org.walkersguide.android.database.profiles.DatabasePointProfile;
import org.walkersguide.android.server.poi.PoiProfile;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.server.route.WayClass;
import org.walkersguide.android.data.sensor.Direction;
import org.walkersguide.android.data.server.AddressProvider;
import org.walkersguide.android.data.server.OSMMap;

import org.walkersguide.android.pt.PTHelper;
import org.walkersguide.android.pt.PTHelper.Country;

import de.schildbach.pte.AbstractNetworkProvider;
import java.util.Map;
import timber.log.Timber;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Set;
import java.lang.Long;
import java.lang.ClassNotFoundException;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.point.Point;
import java.util.List;
import android.text.TextUtils;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.data.route.Route;


public class SettingsManager {

    // constants
    public static final String SETTINGS_FILE_NAME = "WalkersGuide-Android-Settings";
    public static final int MAX_NUMBER_OF_SEARCH_TERM_HISTORY_ENTRIES = 100;

	// defaults
    public static final int DEFAULT_SELECTED_POI_PROFILE_ID = 1;
    public static final int DEFAULT_SELECTED_ROUTE_ID = 1;
    public static final int DEFAULT_SELECTED_TAB_MAIN_ACTIVITY = 0;
    public static final int DEFAULT_SHAKE_INTENSITY = Constants.SHAKE_INTENSITY.DISABLED;
    public static final boolean DEFAULT_ENABLE_SEARCH_TERM_HISTORY = true;
    public static final boolean DEFAULT_SHOW_ACTION_BUTTON = true;
    public static final boolean DEFAULT_AUTO_SKIP_TO_NEXT_ROUTE_POINT = true;

    // keys
    private static final String KEY_SELECTED_POI_PROFILE_ID = "selectedPoiProfileId";
    private static final String KEY_SELECTED_ROUTE_ID = "selectedRouteId";
    private static final String KEY_SELECTED_TAB_MAIN_ACTIVITY = "selectedTabMainActivity";
    private static final String KEY_SHAKE_INTENSITY = "shakeIntensity";
    private static final String KEY_SHOW_ACTION_BUTTON = "showActionButton";
    private static final String KEY_ENABLE_SEARCH_TERM_HISTORY = "enableSearchTermHistory";
    private static final String KEY_AUTO_SKIP_TO_NEXT_ROUTE_POINT = "autoSkipToNextRoutePoint";
    private static final String KEY_SEARCH_TERM_HISTORY = "searchTermHistory";
    // subclasses
    private static final String KEY_DIRECTION_SETTINGS = "directionSettings";
    private static final String KEY_LOCATION_SETTINGS = "locationSettings";
    private static final String KEY_PLAN_ROUTE_SETTINGS = "planRouteSettings";
    private static final String KEY_SERVER_SETTINGS = "serverSettings";


    // class variables
    private static SettingsManager managerInstance;
    private Context context;
    private SharedPreferences settings;

    public static SettingsManager getInstance() {
        if (managerInstance == null){
            managerInstance = getInstanceSynchronized();
        }
        return managerInstance;
    }

    private static synchronized SettingsManager getInstanceSynchronized() {
        if (managerInstance == null){
            managerInstance = new SettingsManager();
        }
        return managerInstance;
    }

    private SettingsManager() {
		this.context = GlobalInstance.getContext();
		this.settings = GlobalInstance.getContext().getSharedPreferences(SETTINGS_FILE_NAME, Context.MODE_PRIVATE);

        // remove deprecated settings
        if (settings.contains("generalSettings")) {
            Editor editor = settings.edit();
            editor.remove("generalSettings");
            editor.apply();
        }
        if (settings.contains("poiSettings")) {
            Editor editor = settings.edit();
            editor.remove("poiSettings");
            editor.apply();
        }
	}


    /**
     * main settings
     */

    public PoiProfile getSelectedPoiProfile() {
        return PoiProfile.load(
                settings.getLong(KEY_SELECTED_POI_PROFILE_ID, DEFAULT_SELECTED_POI_PROFILE_ID));
    }

    public void setSelectedPoiProfile(PoiProfile newProfile) {
        Editor editor = settings.edit();
        if (newProfile != null) {
            editor.putLong(KEY_SELECTED_POI_PROFILE_ID, newProfile.getId());
        } else {
            editor.putLong(KEY_SELECTED_POI_PROFILE_ID, DEFAULT_SELECTED_POI_PROFILE_ID);
        }
        editor.apply();
    }

    public Route getSelectedRoute() {
        return Route.load(
                settings.getLong(KEY_SELECTED_ROUTE_ID, DEFAULT_SELECTED_ROUTE_ID));
    }

    public void setSelectedRoute(Route newRoute) {
        Editor editor = settings.edit();
        if (newRoute != null
                && AccessDatabase.getInstance().addObjectToDatabaseProfile(newRoute, DatabaseRouteProfile.PLANNED_ROUTES)) {
            editor.putLong(KEY_SELECTED_ROUTE_ID, newRoute.getId());
        } else {
            editor.putLong(KEY_SELECTED_ROUTE_ID, DEFAULT_SELECTED_ROUTE_ID);
        }
        editor.apply();
    }

    public int getSelectedTabForMainActivity() {
        return settings.getInt(KEY_SELECTED_TAB_MAIN_ACTIVITY, DEFAULT_SELECTED_TAB_MAIN_ACTIVITY);
    }

    public void setSelectedTabForMainActivity(int newTab) {
        Editor editor = settings.edit();
        editor.putInt(KEY_SELECTED_TAB_MAIN_ACTIVITY, newTab);
        editor.apply();
    }

    public int getSelectedShakeIntensity() {
        int intensity = settings.getInt(KEY_SHAKE_INTENSITY, DEFAULT_SHAKE_INTENSITY);
        return Ints.contains(Constants.ShakeIntensityValueArray, intensity) ? intensity : DEFAULT_SHAKE_INTENSITY;
    }

    public void setSelectedShakeIntensity(int newIntensity) {
        Editor editor = settings.edit();
        editor.putInt(KEY_SHAKE_INTENSITY, newIntensity);
        editor.apply();
    }

    public boolean getEnableSearchTermHistory() {
        return settings.getBoolean(KEY_ENABLE_SEARCH_TERM_HISTORY, DEFAULT_ENABLE_SEARCH_TERM_HISTORY);
    }

    public void setEnableSearchTermHistory(boolean enableSearchTermHistory) {
        Editor editor = settings.edit();
        editor.putBoolean(KEY_ENABLE_SEARCH_TERM_HISTORY, enableSearchTermHistory);
        editor.apply();
    }

    public boolean getShowActionButton() {
        return settings.getBoolean(KEY_SHOW_ACTION_BUTTON, DEFAULT_SHOW_ACTION_BUTTON);
    }

    public void setShowActionButton(boolean showActionButton) {
        Editor editor = settings.edit();
        editor.putBoolean(KEY_SHOW_ACTION_BUTTON, showActionButton);
        editor.apply();
    }

    public boolean getAutoSkipToNextRoutePoint() {
        return settings.getBoolean(KEY_AUTO_SKIP_TO_NEXT_ROUTE_POINT, DEFAULT_AUTO_SKIP_TO_NEXT_ROUTE_POINT);
    }

    public void setAutoSkipToNextRoutePoint(boolean newValue) {
        Editor editor = settings.edit();
        editor.putBoolean(KEY_AUTO_SKIP_TO_NEXT_ROUTE_POINT, newValue);
        editor.apply();
    }


    /**
     * search term history
     */

    public ArrayList<String> getSearchTermHistory() {
        Gson gson = new Gson();
        ArrayList<String> searchTermHistory = gson.fromJson(
                settings.getString(KEY_SEARCH_TERM_HISTORY, "[]"),
                new TypeToken<List<String>>() {}.getType());
        if (searchTermHistory == null) {
            searchTermHistory = new ArrayList<String>();
        }
        return searchTermHistory;
    }

    public void addToSearchTermHistory(String newSearchTerm) {
        if (! TextUtils.isEmpty(newSearchTerm)) {
            ArrayList<String> searchTermHistory = getSearchTermHistory();

            // add every single word at least four chars long
            for (String word : newSearchTerm.split("\\s")) {
                if (word.length() > 3
                        && ! searchTermHistory.contains(word)) {
                    searchTermHistory.add(0, word);
                }
            }

            // add complete phrase
            if (! searchTermHistory.contains(newSearchTerm)) {
                searchTermHistory.add(0, newSearchTerm);
            }

            // clear odd entries
            int numberOfOddEntries = searchTermHistory.size() - MAX_NUMBER_OF_SEARCH_TERM_HISTORY_ENTRIES;
            if (numberOfOddEntries > 0) {
                searchTermHistory.subList(
                        searchTermHistory.size() - numberOfOddEntries,
                        searchTermHistory.size())
                    .clear();
            }

            // save
            Gson gson = new Gson();
            Editor editor = settings.edit();
            editor.putString(
                    KEY_SEARCH_TERM_HISTORY,
                    gson.toJson(
                        searchTermHistory,
                        new TypeToken<List<String>>() {}.getType()));
            editor.apply();
        }
    }

    public void clearSearchTermHistory() {
        if (settings.contains(KEY_SEARCH_TERM_HISTORY)) {
            Editor editor = settings.edit();
            editor.remove(KEY_SEARCH_TERM_HISTORY);
            editor.apply();
        }
    }


    /**
     * direction settings
     */

    public DirectionSettings getDirectionSettings() {
        JSONObject jsonDirectionSettings = new JSONObject();
        try {
            jsonDirectionSettings = new JSONObject(
                    settings.getString(KEY_DIRECTION_SETTINGS, "{}"));
    	} catch (JSONException e) {}
        return new DirectionSettings(jsonDirectionSettings);
    }

    public void setDirectionSettings(JSONObject jsonDirectionSettings) {
        Editor editor = settings.edit();
        editor.putString(KEY_DIRECTION_SETTINGS, jsonDirectionSettings.toString());
        editor.apply();
    }


    public class DirectionSettings {

        private int selectedDirectionSource;
        private Direction compassDirection, gpsDirection, simulatedDirection;

        public DirectionSettings(JSONObject jsonObject) {
            this.selectedDirectionSource = Constants.DIRECTION_SOURCE.COMPASS;
            try {
                int directionSource = jsonObject.getInt("selectedDirectionSource");
                if (Ints.contains(Constants.DirectionSourceValueArray, directionSource)) {
                    this.selectedDirectionSource = directionSource;
                }
            } catch (JSONException e) {}
            // compass direction
            this.compassDirection = null;
            try {
                this.compassDirection= new Direction(
                        context, jsonObject.getJSONObject("compassDirection"));
            } catch (JSONException e) {}
            // gps direction
            this.gpsDirection = null;
            try {
                this.gpsDirection= new Direction(
                        context, jsonObject.getJSONObject("gpsDirection"));
            } catch (JSONException e) {}
            this.simulatedDirection = null;
            try {
                this.simulatedDirection= new Direction(
                        context, jsonObject.getJSONObject("simulatedDirection"));
            } catch (JSONException e) {}
        }

        public int getSelectedDirectionSource() {
            return this.selectedDirectionSource;
        }

        public void setSelectedDirectionSource(int newDirectionSource) {
            if (Ints.contains(Constants.DirectionSourceValueArray, newDirectionSource)) {
                this.selectedDirectionSource = newDirectionSource;
                storeDirectionSettings();
            }
        }

        public Direction getCompassDirection() {
            return this.compassDirection;
        }

        public void setCompassDirection(Direction newCompassDirection) {
            if (newCompassDirection != null) {
                this.compassDirection = newCompassDirection;
                storeDirectionSettings();
            }
        }

        public Direction getGPSDirection() {
            return this.gpsDirection;
        }

        public void setGPSDirection(Direction newGPSDirection) {
            if (newGPSDirection != null) {
                this.gpsDirection = newGPSDirection;
                storeDirectionSettings();
            }
        }

        public Direction getSimulatedDirection() {
            return this.simulatedDirection;
        }

        public void setSimulatedDirection(Direction newSimulatedDirection) {
            if (newSimulatedDirection != null) {
                this.simulatedDirection = newSimulatedDirection;
                storeDirectionSettings();
            }
        }

        public void storeDirectionSettings() {
            JSONObject jsonDirectionSettings = new JSONObject();
            try {
                jsonDirectionSettings.put("selectedDirectionSource", this.selectedDirectionSource);
            } catch (JSONException e) {}
            if (this.compassDirection != null) {
                try {
                    jsonDirectionSettings.put("compassDirection", this.compassDirection.toJson());
                } catch (JSONException e) {}
            }
            if (this.gpsDirection != null) {
                try {
                    jsonDirectionSettings.put("gpsDirection", this.gpsDirection.toJson());
                } catch (JSONException e) {}
            }
            if (this.simulatedDirection != null) {
                try {
                    jsonDirectionSettings.put("simulatedDirection", this.simulatedDirection.toJson());
                } catch (JSONException e) {}
            }
    		// save settings
            setDirectionSettings(jsonDirectionSettings);
        }
    }


    /**
     * location settings
     */

    public LocationSettings getLocationSettings() {
        JSONObject jsonLocationSettings = new JSONObject();
        try {
            jsonLocationSettings = new JSONObject(
                    settings.getString(KEY_LOCATION_SETTINGS, "{}"));
    	} catch (JSONException e) {}
        return new LocationSettings(jsonLocationSettings);
    }

    public void setLocationSettings(JSONObject jsonLocationSettings) {
        Editor editor = settings.edit();
        editor.putString(KEY_LOCATION_SETTINGS, jsonLocationSettings.toString());
        editor.apply();
    }


    public class LocationSettings {

        private GPS gpsLocation;
        private Point simulatedLocation;

        public LocationSettings(JSONObject jsonObject) {
            // gps location
            this.gpsLocation = null;
            try {
                this.gpsLocation = (GPS) Point.create(
                        jsonObject.getJSONObject("gpsLocation"));
            } catch (JSONException e) {}
            // simulated location
            this.simulatedLocation = null;
            try {
                this.simulatedLocation = Point.create(
                        jsonObject.getJSONObject("simulatedLocation"));
            } catch (JSONException e) {}
        }

        public GPS getGPSLocation() {
            return this.gpsLocation;
        }

        public void setGPSLocation(GPS newLocation) {
            this.gpsLocation = newLocation;
            storeLocationSettings();
        }

        public Point getSimulatedLocation() {
            return this.simulatedLocation;
        }

        public void setSimulatedLocation(Point newLocation) {
            this.simulatedLocation = newLocation;
            storeLocationSettings();
        }

        public void storeLocationSettings() {
            JSONObject jsonLocationSettings = new JSONObject();
            if (this.gpsLocation != null) {
                try {
                    jsonLocationSettings.put("gpsLocation", this.gpsLocation.toJson());
                } catch (JSONException e) {}
            }
            if (this.simulatedLocation != null) {
                try {
                    jsonLocationSettings.put("simulatedLocation", this.simulatedLocation.toJson());
                } catch (JSONException e) {}
            }
    		// save settings
            setLocationSettings(jsonLocationSettings);
        }
    }


    /**
     * plan route settings
     */


    public PlanRouteSettings getPlanRouteSettings() {
        JSONObject jsonPlanRouteSettings = new JSONObject();
        try {
            jsonPlanRouteSettings = new JSONObject(
                    settings.getString(KEY_PLAN_ROUTE_SETTINGS, "{}"));
    	} catch (JSONException e) {}
        return new PlanRouteSettings(jsonPlanRouteSettings);
    }

    public void setPlanRouteSettings(JSONObject jsonPlanRouteSettings) {
        Editor editor = settings.edit();
        editor.putString(KEY_PLAN_ROUTE_SETTINGS, jsonPlanRouteSettings.toString());
        editor.apply();
    }


    public class PlanRouteSettings {
        private static final String KEY_START_POINT_ID = "startPointId";
        private static final String KEY_DESTINATION_POINT_ID = "destinationPointId";
        private static final String KEY_VIA_POINT_1_ID = "viaPoint1Id";
        private static final String KEY_VIA_POINT_2_ID = "viaPoint2Id";
        private static final String KEY_VIA_POINT_3_ID = "viaPoint3Id";

        private Long startPointId, destinationPointId;
        private Long viaPoint1Id, viaPoint2Id, viaPoint3Id;
        private ArrayList<WayClass> wayClassList;

        public PlanRouteSettings(JSONObject jsonObject) {
            this.startPointId = StringUtility.getNullableAndPositiveLongFromJsonObject(jsonObject, KEY_START_POINT_ID);
            this.destinationPointId = StringUtility.getNullableAndPositiveLongFromJsonObject(jsonObject, KEY_DESTINATION_POINT_ID);
            this.viaPoint1Id = StringUtility.getNullableAndPositiveLongFromJsonObject(jsonObject, KEY_VIA_POINT_1_ID);
            this.viaPoint2Id = StringUtility.getNullableAndPositiveLongFromJsonObject(jsonObject, KEY_VIA_POINT_2_ID);
            this.viaPoint3Id = StringUtility.getNullableAndPositiveLongFromJsonObject(jsonObject, KEY_VIA_POINT_3_ID);
            // way classes and weights
            JSONObject jsonWayClassIdAndWeights = null;
            try {
                jsonWayClassIdAndWeights = jsonObject.getJSONObject("wayClassIdAndWeights");
            } catch (JSONException e) {}
            this.wayClassList = new ArrayList<WayClass>();
            for (int i=0; i<Constants.RoutingWayClassIdValueArray.length; i++) {
                String wayClassId = Constants.RoutingWayClassIdValueArray[i];
                double wayClassWeight = WayClass.defaultWeightForWayClass(wayClassId);
                try {
                    wayClassWeight = jsonWayClassIdAndWeights.getDouble(wayClassId);
                } catch (JSONException | NullPointerException e) {}
                wayClassList.add(new WayClass(wayClassId, wayClassWeight));
            }
        }

        public Point getStartPoint() {
            return getPoint(startPointId);
        }

        public void setStartPoint(Point newStartPoint) {
            this.startPointId = setPoint(newStartPoint);
            storePlanRouteSettings();
        }

        public Point getDestinationPoint() {
            return getPoint(destinationPointId);
        }

        public void setDestinationPoint(Point newDestinationPoint) {
            this.destinationPointId = setPoint(newDestinationPoint);
            storePlanRouteSettings();
        }

        public Point getViaPoint1() {
            return getPoint(viaPoint1Id);
        }

        public void setViaPoint1(Point newViaPoint1) {
            this.viaPoint1Id = setPoint(newViaPoint1);
            storePlanRouteSettings();
        }

        public Point getViaPoint2() {
            return getPoint(viaPoint2Id);
        }

        public void setViaPoint2(Point newViaPoint2) {
            this.viaPoint2Id = setPoint(newViaPoint2);
            storePlanRouteSettings();
        }

        public Point getViaPoint3() {
            return getPoint(viaPoint3Id);
        }

        public void setViaPoint3(Point newViaPoint3) {
            this.viaPoint3Id = setPoint(newViaPoint3);
            storePlanRouteSettings();
        }

        public boolean hasViaPoint() {
            return getViaPoint1() != null || getViaPoint2() != null || getViaPoint3() != null;
        }

        public void clearViaPointList() {
            setViaPoint1(null);
            setViaPoint2(null);
            setViaPoint3(null);
        }

        private Point getPoint(Long id) {
            if (id != null) {
                return Point.load(id);
            }
            return null;
        }

        private Long setPoint(Point newPoint) {
            if (newPoint != null
                    && AccessDatabase.getInstance().addObjectToDatabaseProfile(newPoint, DatabasePointProfile.ROUTE_POINTS)) {
                return newPoint.getId();
            }
            return null;
        }

        public ArrayList<WayClass> getWayClassList() {
            return this.wayClassList;
        }

        public void setWayClassList(ArrayList<WayClass> newWayClassList) {
            if (newWayClassList != null) {
                this.wayClassList = newWayClassList;
                storePlanRouteSettings();
            }
        }

        private void storePlanRouteSettings() {
            JSONObject jsonPlanRouteSettings = new JSONObject();
            if (this.startPointId != null) {
                try {
                    jsonPlanRouteSettings.put(KEY_START_POINT_ID, this.startPointId);
                } catch (JSONException e) {}
            }
            if (this.destinationPointId != null) {
                try {
                    jsonPlanRouteSettings.put(KEY_DESTINATION_POINT_ID, this.destinationPointId);
                } catch (JSONException e) {}
            }
            if (this.viaPoint1Id != null) {
                try {
                    jsonPlanRouteSettings.put(KEY_VIA_POINT_1_ID, this.viaPoint1Id);
                } catch (JSONException e) {}
            }
            if (this.viaPoint2Id != null) {
                try {
                    jsonPlanRouteSettings.put(KEY_VIA_POINT_2_ID, this.viaPoint2Id);
                } catch (JSONException e) {}
            }
            if (this.viaPoint3Id != null) {
                try {
                    jsonPlanRouteSettings.put(KEY_VIA_POINT_3_ID, this.viaPoint3Id);
                } catch (JSONException e) {}
            }
            // way classes and weights
            JSONObject jsonWayClassIdAndWeights = new JSONObject();
            for (WayClass wayClass : this.wayClassList) {
                try {
                    jsonWayClassIdAndWeights.put(wayClass.getId(), wayClass.getWeight());
                } catch (JSONException e) {}
            }
            try {
                jsonPlanRouteSettings.put("wayClassIdAndWeights", jsonWayClassIdAndWeights);
            } catch (JSONException e) {}
    		// save settings
            setPlanRouteSettings(jsonPlanRouteSettings);
        }
    }


    /**
     * server settings
     */

    public ServerSettings getServerSettings() {
        JSONObject jsonServerSettings = new JSONObject();
        try {
            jsonServerSettings = new JSONObject(
                    settings.getString(KEY_SERVER_SETTINGS, "{}"));
    	} catch (JSONException e) {}
        return new ServerSettings(jsonServerSettings);
    }

    public void setServerSettings(JSONObject jsonServerSettings) {
        Editor editor = settings.edit();
        editor.putString(KEY_SERVER_SETTINGS, jsonServerSettings.toString());
        editor.apply();
    }


    public class ServerSettings {

        private String serverURL;
        private OSMMap selectedMap;
        private AbstractNetworkProvider selectedPublicTransportProvider;
        private AddressProvider selectedAddressProvider;

        public ServerSettings(JSONObject jsonObject) {
            this.serverURL = BuildConfig.SERVER_URL;
            try {
                this.serverURL = jsonObject.getString("serverURL");
            } catch (JSONException e) {}
            if (this.serverURL.contains("scheibler-dresden")) {
                this.serverURL = BuildConfig.SERVER_URL;
            }
            // map
            this.selectedMap = null;
            try {
                this.selectedMap = new OSMMap(jsonObject.getJSONObject("selectedMap"));
            } catch (JSONException e) {
            }
            // public transport provider
            String networkProviderId = null;
            try {
                networkProviderId = jsonObject.getString("networkProviderId");
            } catch (JSONException e) {
                networkProviderId = null;
            } finally {
                this.selectedPublicTransportProvider = null;
                if (networkProviderId != null) {
                    for( Map.Entry<Country,ArrayList<AbstractNetworkProvider>> entry : PTHelper.supportedNetworkProviderMap.entrySet()){
                        for (AbstractNetworkProvider provider : entry.getValue()) {
                            if (networkProviderId.equals(provider.id().name())) {
                                this.selectedPublicTransportProvider = provider;
                                break;
                            }
                        }
                        if (this.selectedPublicTransportProvider != null) {
                            break;
                        }
                    }
                }
            }
            // address provider
            this.selectedAddressProvider = new AddressProvider(context, Constants.ADDRESS_PROVIDER.OSM);
            try {
                String addressProviderIdFromJson = jsonObject.getString("selectedAddressProviderId");
                if (Arrays.asList(Constants.AddressProviderValueArray).contains(addressProviderIdFromJson)) {
                    this.selectedAddressProvider = new AddressProvider(context, addressProviderIdFromJson);
                }
            } catch (JSONException e) {}
        }

        public String getServerURL() {
            return this.serverURL;
        }

        public void setServerURL(String newServerURL) {
            this.serverURL = newServerURL;
            storeServerSettings();
        }

        public OSMMap getSelectedMap() {
            return this.selectedMap;
        }

        public void setSelectedMap(OSMMap newMap) {
            this.selectedMap = newMap;
            storeServerSettings();
        }

        public AbstractNetworkProvider getSelectedPublicTransportProvider() {
            return this.selectedPublicTransportProvider;
        }

        public void setSelectedPublicTransportProvider(AbstractNetworkProvider newProvider) {
            this.selectedPublicTransportProvider = newProvider;
            storeServerSettings();
        }

        public AddressProvider getSelectedAddressProvider() {
            return this.selectedAddressProvider;
        }

        public void setSelectedAddressProvider(AddressProvider newAddressProvider) {
            if (Arrays.asList(Constants.AddressProviderValueArray).contains(newAddressProvider.getId())) {
                this.selectedAddressProvider = newAddressProvider;
                storeServerSettings();
            }
        }

        public void storeServerSettings() {
            JSONObject jsonServerSettings = new JSONObject();
            try {
                jsonServerSettings.put("serverURL", this.serverURL);
            } catch (JSONException e) {}
            if (this.selectedMap != null) {
                try {
                    jsonServerSettings.put("selectedMap", this.selectedMap.toJson());
                } catch (JSONException e) {}
            }
            if (this.selectedPublicTransportProvider != null) {
                try {
    	    		jsonServerSettings.put("networkProviderId", this.selectedPublicTransportProvider.id().name());
                } catch (JSONException e) {}
            }
            if (this.selectedAddressProvider != null) {
                try {
                    jsonServerSettings.put("selectedAddressProviderId", this.selectedAddressProvider.getId());
                } catch (JSONException e) {}
            }
    		// save settings
            setServerSettings(jsonServerSettings);
        }
    }


    /**
     * import and export settings
     */

    public boolean importSettings(File sourceFile) {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        Map<String, Object> map = null;
        boolean success = true;
        try {
            fis = new FileInputStream(sourceFile);
            ois = new ObjectInputStream(fis);
            map = (Map) ois.readObject();
        } catch(IOException | ClassNotFoundException e) {
            Timber.e("Settings import failed: " + e.getMessage());
            success = false;
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {}
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {}
            }
            if (! success) {
                return false;
            }
        }

        // restore settings
        SharedPreferences.Editor editor = this.settings.edit();
        editor.clear();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (e.getValue() instanceof Boolean) {
                editor.putBoolean(e.getKey(), (Boolean)e.getValue());
            } else if (e.getValue() instanceof String) {
                editor.putString(e.getKey(), (String)e.getValue());
            } else if (e.getValue() instanceof Integer) {
                editor.putInt(e.getKey(), (int)e.getValue());
            } else if (e.getValue() instanceof Float) {
                editor.putFloat(e.getKey(), (float)e.getValue());
            } else if (e.getValue() instanceof Long) {
                editor.putLong(e.getKey(), (Long) e.getValue());
            } else if (e.getValue() instanceof Set) {
                editor.putStringSet(e.getKey(), (Set<String>) e.getValue());
            } else {
                Timber.e("Settings type " + e.getValue().getClass().getName() + " is unknown");
            }
        }
        success = editor.commit();

        return success;
    }

    public boolean exportSettings(File destinationFile) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        boolean success = true;
        try {
            fos = new FileOutputStream(destinationFile);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(settings.getAll());
        } catch(IOException e) {
            Timber.e("Settings export failed: " + e.getMessage());
            success = false;
            if (destinationFile.exists()) {
                destinationFile.delete();
            }
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {}
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {}
            }
            return success;
        }
    }

}
