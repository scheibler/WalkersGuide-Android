package org.walkersguide.android.util;

import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.server.OSMMap;
import org.walkersguide.android.data.server.PublicTransportProvider;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.sensor.PositionManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import java.util.ArrayList;
import org.json.JSONArray;
import java.util.Arrays;

public class SettingsManager {
    public static final int MAX_NUMBER_OF_SEARCH_TERM_HISTORY_ENTRIES = 50;

    // class variables
    private static SettingsManager settingsManagerInstance;
    private Context context;
    private SharedPreferences settings;

    // settings
    private GeneralSettings generalSettings;
    private DirectionSettings directionSettings;
    private LocationSettings locationSettings;
    private FavoritesFragmentSettings favoritesFragmentSettings;
    private POIFragmentSettings poiFragmentSettings;
    private RouteSettings routeSettings;
    private ServerSettings serverSettings;
    private SearchTermHistory searchTermHistory;
    private FavoritesSearchSettings favoritesSearchSettings;

	public static SettingsManager getInstance(Context context) {
		if (settingsManagerInstance == null) {
			settingsManagerInstance = new SettingsManager(
					context.getApplicationContext());
		}
		return settingsManagerInstance;
	}

	private SettingsManager(Context context) {
		this.context = context;
		this.settings = context.getSharedPreferences("WalkersGuide-Android-Settings", Context.MODE_PRIVATE);
	}

	public GeneralSettings getGeneralSettings() {
        if (generalSettings == null) {
            JSONObject jsonGeneralSettings;
            try {
    		    jsonGeneralSettings = new JSONObject(settings.getString("generalSettings", "{}"));
    		} catch (JSONException e) {
                jsonGeneralSettings = new JSONObject();
            }
            generalSettings = new GeneralSettings(jsonGeneralSettings);
        }
		return generalSettings;
	}

	public DirectionSettings getDirectionSettings() {
        if (directionSettings == null) {
            JSONObject jsonDirectionSettings;
            try {
    		    jsonDirectionSettings = new JSONObject(settings.getString("directionSettings", "{}"));
    		} catch (JSONException e) {
                jsonDirectionSettings = new JSONObject();
            }
            directionSettings = new DirectionSettings(jsonDirectionSettings);
        }
		return directionSettings;
	}

	public LocationSettings getLocationSettings() {
        if (locationSettings == null) {
            JSONObject jsonLocationSettings;
            try {
    		    jsonLocationSettings = new JSONObject(settings.getString("locationSettings", "{}"));
    		} catch (JSONException e) {
                jsonLocationSettings = new JSONObject();
            }
            locationSettings = new LocationSettings(jsonLocationSettings);
        }
		return locationSettings;
	}

	public FavoritesFragmentSettings getFavoritesFragmentSettings() {
        if (favoritesFragmentSettings == null) {
            JSONObject jsonFavoritesFragmentSettings;
            try {
    		    jsonFavoritesFragmentSettings = new JSONObject(settings.getString("favoritesFragmentSettings", "{}"));
    		} catch (JSONException e) {
                jsonFavoritesFragmentSettings = new JSONObject();
            }
            favoritesFragmentSettings = new FavoritesFragmentSettings(jsonFavoritesFragmentSettings);
        }
		return favoritesFragmentSettings;
	}

	public POIFragmentSettings getPOIFragmentSettings() {
        if (poiFragmentSettings == null) {
            JSONObject jsonPOIFragmentSettings;
            try {
    		    jsonPOIFragmentSettings = new JSONObject(settings.getString("poiFragmentSettings", "{}"));
    		} catch (JSONException e) {
                jsonPOIFragmentSettings = new JSONObject();
            }
            poiFragmentSettings = new POIFragmentSettings(jsonPOIFragmentSettings);
        }
		return poiFragmentSettings;
	}

	public RouteSettings getRouteSettings() {
        if (routeSettings == null) {
            JSONObject jsonRouteSettings;
            try {
    		    jsonRouteSettings = new JSONObject(settings.getString("routeSettings", "{}"));
    		} catch (JSONException e) {
                jsonRouteSettings = new JSONObject();
            }
            routeSettings = new RouteSettings(jsonRouteSettings);
        }
		return routeSettings;
	}

	public ServerSettings getServerSettings() {
        if (serverSettings == null) {
            JSONObject jsonServerSettings;
            try {
    		    jsonServerSettings = new JSONObject(settings.getString("serverSettings", "{}"));
    		} catch (JSONException e) {
                jsonServerSettings = new JSONObject();
            }
            serverSettings = new ServerSettings(jsonServerSettings);
        }
		return serverSettings;
	}

	public SearchTermHistory getSearchTermHistory() {
        if (searchTermHistory == null) {
            JSONArray jsonSearchTermList;
            try {
    		    jsonSearchTermList = new JSONArray(settings.getString("searchTermList", "[]"));
    		} catch (JSONException e) {
                jsonSearchTermList = new JSONArray();
                System.out.println("xxx searchTermHistory json error " + e);
            }
            searchTermHistory = new SearchTermHistory(jsonSearchTermList);
        }
		return searchTermHistory;
	}


	public FavoritesSearchSettings getFavoritesSearchSettings() {
        if (favoritesSearchSettings == null) {
            JSONObject jsonFavoritesSearchSettings;
            try {
    		    jsonFavoritesSearchSettings = new JSONObject(settings.getString("favoritesSearchSettings", "{}"));
    		} catch (JSONException e) {
                jsonFavoritesSearchSettings = new JSONObject();
            }
            favoritesSearchSettings = new FavoritesSearchSettings(jsonFavoritesSearchSettings);
        }
		return favoritesSearchSettings;
	}


    /**
     * General, direction and location settings
     */

    public class GeneralSettings {

        private int recentOpenTab;
        private int shakeIntensity;

        public GeneralSettings(JSONObject jsonObject) {
            this.recentOpenTab = Constants.MAIN_FRAGMENT.ROUTER;
            try {
                int tab = jsonObject.getInt("recentOpenTab");
                if (Ints.contains(Constants.MainActivityFragmentValueArray, tab)) {
                    this.recentOpenTab = tab;
                }
            } catch (JSONException e) {}
            this.shakeIntensity = Constants.SHAKE_INTENSITY.MEDIUM;
            try {
                int intensity = jsonObject.getInt("shakeIntensity");
                if (Ints.contains(Constants.ShakeIntensityValueArray, intensity)) {
                    this.shakeIntensity = intensity;
                }
            } catch (JSONException e) {}
        }

        public String getClientVersion() {
            try {
                return context.getPackageManager().getPackageInfo(
                        context.getPackageName(), 0).versionName;
            } catch (NameNotFoundException e) {
                return "";
            }
        }

        public int getRecentOpenTab() {
            return this.recentOpenTab;
        }

        public void setRecentOpenTab(int tabIndex) {
            this.recentOpenTab = tabIndex;
            storeGeneralSettings();
        }

        public int getShakeIntensity() {
            return this.shakeIntensity;
        }

        public void setShakeIntensity(int newIntensity) {
            if (Ints.contains(Constants.ShakeIntensityValueArray, newIntensity)) {
                this.shakeIntensity = newIntensity;
                storeGeneralSettings();
            }
        }

        public void storeGeneralSettings() {
            JSONObject jsonGeneralSettings = new JSONObject();
            try {
                jsonGeneralSettings.put("recentOpenTab", this.recentOpenTab);
            } catch (JSONException e) {}
            try {
                jsonGeneralSettings.put("shakeIntensity", this.shakeIntensity);
            } catch (JSONException e) {}
    		// save settings
	    	Editor editor = settings.edit();
		    editor.putString("generalSettings", jsonGeneralSettings.toString());
    		editor.commit();
            // null GeneralSettings object to force reload on next getGeneralSettings()
            generalSettings = null;
        }
    }


    public class DirectionSettings {

        private int selectedDirectionSource, previousDirectionSource;
        private int compassDirection, gpsDirection, simulatedDirection;
        private float differenceToTrueNorth;

        public DirectionSettings(JSONObject jsonObject) {
            this.selectedDirectionSource = Constants.DIRECTION_SOURCE.COMPASS;
            try {
                int directionSource = jsonObject.getInt("selectedDirectionSource");
                if (Ints.contains(Constants.DirectionSourceValueArray, directionSource)) {
                    this.selectedDirectionSource = directionSource;
                }
            } catch (JSONException e) {}
            this.previousDirectionSource = Constants.DIRECTION_SOURCE.COMPASS;
            try {
                int directionSource = jsonObject.getInt("previousDirectionSource");
                if (Ints.contains(Constants.DirectionSourceValueArray, directionSource)) {
                    this.previousDirectionSource = directionSource;
                }
            } catch (JSONException e) {}
            this.compassDirection = Constants.DUMMY.DIRECTION;
            try {
                int compass = jsonObject.getInt("compassDirection");
                if (compass >= 0 && compass <= 359) {
                    this.compassDirection = compass;
                }
            } catch (JSONException e) {}
            this.gpsDirection = Constants.DUMMY.DIRECTION;
            try {
                int gps = jsonObject.getInt("gpsDirection");
                if (gps >= 0 && gps <= 359) {
                    this.gpsDirection = gps;
                }
            } catch (JSONException e) {}
            this.simulatedDirection = Constants.DUMMY.DIRECTION;
            try {
                int simulated = jsonObject.getInt("simulatedDirection");
                if (simulated >= 0 && simulated <= 359) {
                    this.simulatedDirection = simulated;
                }
            } catch (JSONException e) {}
            this.differenceToTrueNorth = 0.0f;
            try {
                this.differenceToTrueNorth = Double.valueOf(jsonObject.getDouble("differenceToTrueNorth")).floatValue();
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

        public int getPreviousDirectionSource() {
            return this.previousDirectionSource;
        }

        public void setPreviousDirectionSource(int newDirectionSource) {
            if (Ints.contains(Constants.DirectionSourceValueArray, newDirectionSource)) {
                this.previousDirectionSource = newDirectionSource;
                storeDirectionSettings();
            }
        }

        public int getCompassDirection() {
            return this.compassDirection;
        }

        public void setCompassDirection(int newCompassDirection) {
            if (newCompassDirection>= 0 && newCompassDirection <= 359) {
                this.compassDirection = newCompassDirection;
                storeDirectionSettings();
            }
        }

        public int getGPSDirection() {
            return this.gpsDirection;
        }

        public void setGPSDirection(int newGPSDirection) {
            if (newGPSDirection>= 0 && newGPSDirection <= 359) {
                this.gpsDirection = newGPSDirection;
                storeDirectionSettings();
            }
        }

        public int getSimulatedDirection() {
            return this.simulatedDirection;
        }

        public void setSimulatedDirection(int newSimulatedDirection) {
            if (newSimulatedDirection>= 0 && newSimulatedDirection <= 359) {
                this.simulatedDirection = newSimulatedDirection;
                storeDirectionSettings();
            }
        }

        public float getDifferenceToTrueNorth() {
            return this.differenceToTrueNorth;
        }

        public void setDifferenceToTrueNorth(float newDifference) {
            this.differenceToTrueNorth = newDifference;
            storeDirectionSettings();
        }

        public void storeDirectionSettings() {
            JSONObject jsonDirectionSettings = new JSONObject();
            try {
                jsonDirectionSettings.put("selectedDirectionSource", this.selectedDirectionSource);
            } catch (JSONException e) {}
            try {
                jsonDirectionSettings.put("previousDirectionSource", this.previousDirectionSource);
            } catch (JSONException e) {}
            try {
                jsonDirectionSettings.put("compassDirection", this.compassDirection);
            } catch (JSONException e) {}
            try {
                jsonDirectionSettings.put("gpsDirection", this.gpsDirection);
            } catch (JSONException e) {}
            try {
                jsonDirectionSettings.put("simulatedDirection", this.simulatedDirection);
            } catch (JSONException e) {}
            try {
                jsonDirectionSettings.put("differenceToTrueNorth", this.differenceToTrueNorth);
            } catch (JSONException e) {}
    		// save settings
	    	Editor editor = settings.edit();
		    editor.putString("directionSettings", jsonDirectionSettings.toString());
    		editor.commit();
            // null DirectionSettings object to force reload on next getDirectionSettings()
            directionSettings = null;
        }
    }



    public class LocationSettings {

        private int selectedLocationSource;
        private PointWrapper gpsLocation, simulatedLocation;

        public LocationSettings(JSONObject jsonObject) {
            this.selectedLocationSource = Constants.LOCATION_SOURCE.GPS;
            try {
                int locationSource = jsonObject.getInt("selectedLocationSource");
                if (Ints.contains(Constants.LocationSourceValueArray, locationSource)) {
                    this.selectedLocationSource = locationSource;
                }
            } catch (JSONException e) {}
            // gps location
            this.gpsLocation = PositionManager.getDummyLocation(context);
            try {
                this.gpsLocation = new PointWrapper(
                        context, jsonObject.getJSONObject("gpsLocation"));
            } catch (JSONException e) {}
            // simulated location
            this.simulatedLocation = PositionManager.getDummyLocation(context);
            try {
                this.simulatedLocation = new PointWrapper(
                        context, jsonObject.getJSONObject("simulatedLocation"));
            } catch (JSONException e) {}
        }

        public int getSelectedLocationSource() {
            return this.selectedLocationSource;
        }

        public void setSelectedLocationSource(int newLocationSource) {
            if (Ints.contains(Constants.LocationSourceValueArray, newLocationSource)) {
                this.selectedLocationSource = newLocationSource;
                storeLocationSettings();
            }
        }

        public PointWrapper getGPSLocation() {
            return this.gpsLocation;
        }

        public void setGPSLocation(PointWrapper newLocation) {
            if (newLocation != null) {
                this.gpsLocation = newLocation;
                storeLocationSettings();
            }
        }

        public PointWrapper getSimulatedLocation() {
            return this.simulatedLocation;
        }

        public void setSimulatedLocation(PointWrapper newLocation) {
            if (newLocation != null) {
                this.simulatedLocation = newLocation;
                storeLocationSettings();
            }
        }

        public void storeLocationSettings() {
            JSONObject jsonLocationSettings = new JSONObject();
            try {
                jsonLocationSettings.put("selectedLocationSource", this.selectedLocationSource);
            } catch (JSONException e) {}
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
	    	Editor editor = settings.edit();
		    editor.putString("locationSettings", jsonLocationSettings.toString());
    		editor.commit();
            // null LocationSettings object to force reload on next getLocationSettings()
            locationSettings = null;
        }
    }


    /**
     * fragment settings
     */

    public abstract class PointFragmentSettings {

        private int selectedPositionInPointList;
        private boolean directionFilter;

        public PointFragmentSettings(JSONObject jsonObject) {
            // direction filter
            this.directionFilter = false;
            try {
                this.directionFilter = jsonObject.getBoolean("directionFilter");
            } catch (JSONException e) {}
            // position in list
            this.selectedPositionInPointList = 0;
            try {
                this.selectedPositionInPointList = jsonObject.getInt("selectedPositionInPointList");
            } catch (JSONException e) {}
        }

        public boolean filterPointListByDirection() {
            return this.directionFilter;
        }

        public void setDirectionFilterStatus(boolean newStatus) {
            this.directionFilter = newStatus;
            this.selectedPositionInPointList = 0;
        }

        public int getSelectedPositionInPointList() {
            return this.selectedPositionInPointList;
        }

        public void setSelectedPositionInPointList(int newPosition) {
            this.selectedPositionInPointList = newPosition;
        }

        public JSONObject toJson() {
            JSONObject jsonSettings = new JSONObject();
            try {
                jsonSettings.put("directionFilter", this.directionFilter);
            } catch (JSONException e) {}
            try {
                jsonSettings.put("selectedPositionInPointList", this.selectedPositionInPointList);
            } catch (JSONException e) {}
            return jsonSettings;
        }
    }


    public class FavoritesFragmentSettings extends PointFragmentSettings {

        private int selectedFavoritesProfileId;

        public FavoritesFragmentSettings(JSONObject jsonObject) {
            super(jsonObject);
            // profile id
            int restoredFavoritesProfileId = -1;
            try {
                restoredFavoritesProfileId = jsonObject.getInt("selectedFavoritesProfileId");
            } catch (JSONException e) {}
            // verify, that profile still exists
            TreeMap<Integer,String> favoritesProfileMap = AccessDatabase.getInstance(context).getFavoritesProfileMap();
            if (favoritesProfileMap.containsKey(restoredFavoritesProfileId)) {
                this.selectedFavoritesProfileId = restoredFavoritesProfileId;
            } else if (! favoritesProfileMap.isEmpty()) {
                this.selectedFavoritesProfileId = favoritesProfileMap.firstKey();
            } else {
                this.selectedFavoritesProfileId = -1;
            }
        }

        public int getSelectedFavoritesProfileId() {
            return this.selectedFavoritesProfileId;
        }

        public void setSelectedFavoritesProfileId(int newFavoritesProfileId) {
            this.selectedFavoritesProfileId = newFavoritesProfileId;
            storeFavoritesFragmentSettings();
        }

        @Override public void setDirectionFilterStatus(boolean newStatus) {
            super.setDirectionFilterStatus(newStatus);
            storeFavoritesFragmentSettings();
        }

        @Override public void setSelectedPositionInPointList(int newPosition) {
            if (newPosition >= 0) {
                super.setSelectedPositionInPointList(newPosition);
                storeFavoritesFragmentSettings();
            }
        }

        public void storeFavoritesFragmentSettings() {
            JSONObject jsonFavoritesFragmentSettings = super.toJson();
            try {
                jsonFavoritesFragmentSettings.put("selectedFavoritesProfileId", this.selectedFavoritesProfileId);
            } catch (JSONException e) {}
            // save settings
            Editor editor = settings.edit();
            editor.putString("favoritesFragmentSettings", jsonFavoritesFragmentSettings.toString());
            editor.commit();
            // null FavoritesFragmentSettings object to force reload on next getFavoritesFragmentSettings()
            favoritesFragmentSettings = null;
        }
    }


    public class POIFragmentSettings extends PointFragmentSettings {

        private int selectedPOIProfileId;

        public POIFragmentSettings(JSONObject jsonObject) {
            super(jsonObject);
            // restore poi profile id from settings
            int restoredPOIProfileId = -1;
            try {
                restoredPOIProfileId = jsonObject.getInt("selectedPOIProfileId");
            } catch (JSONException e) {}
            // verify, that profile still exists
            TreeMap<Integer,String> poiProfileMap = AccessDatabase.getInstance(context).getPOIProfileMap();
            if (poiProfileMap.containsKey(restoredPOIProfileId)) {
                this.selectedPOIProfileId = restoredPOIProfileId;
            } else if (! poiProfileMap.isEmpty()) {
                this.selectedPOIProfileId = poiProfileMap.firstKey();
            } else {
                this.selectedPOIProfileId = -1;
            }
        }

        public int getSelectedPOIProfileId() {
            return this.selectedPOIProfileId;
        }

        public void setSelectedPOIProfileId(int newPOIProfileId) {
            this.selectedPOIProfileId = newPOIProfileId;
            storePOIFragmentSettings();
        }

        @Override public void setDirectionFilterStatus(boolean newStatus) {
            super.setDirectionFilterStatus(newStatus);
            storePOIFragmentSettings();
        }

        @Override public void setSelectedPositionInPointList(int newPosition) {
            if (newPosition >= 0) {
                super.setSelectedPositionInPointList(newPosition);
                storePOIFragmentSettings();
            }
        }

        public void storePOIFragmentSettings() {
            JSONObject jsonPOIFragmentSettings = super.toJson();
            try {
                jsonPOIFragmentSettings.put("selectedPOIProfileId", this.selectedPOIProfileId);
            } catch (JSONException e) {}
            // save settings
            Editor editor = settings.edit();
            editor.putString("poiFragmentSettings", jsonPOIFragmentSettings.toString());
            editor.commit();
            // null POIFragmentSettings object to force reload on next getPOIFragmentSettings()
            poiFragmentSettings = null;
        }
    }


    /**
     * route settings
     */

    public class RouteSettings {

        private int selectedRouteId;
        private PointWrapper start, destination;
        private ArrayList<PointWrapper> viaPointList;
        private double indirectionFactor;
        private ArrayList<String> wayClassList;

        public RouteSettings(JSONObject jsonObject) {
            int restoredRouteId = -1;
            try {
                restoredRouteId = jsonObject.getInt("selectedRouteId");
            } catch (JSONException e) {}
            // verify, that route still exists
            TreeMap<Integer,String> routeMap = AccessDatabase.getInstance(context).getRouteMap();
            if (routeMap.containsKey(restoredRouteId)) {
                this.selectedRouteId = restoredRouteId;
            } else if (! routeMap.isEmpty()) {
                this.selectedRouteId = routeMap.firstKey();
            } else {
                this.selectedRouteId = -1;
            }
            // start point
            this.start = PositionManager.getDummyLocation(context);
            try {
                this.start = new PointWrapper(
                        context, jsonObject.getJSONObject("start"));
            } catch (JSONException e) {}
            // destination point
            this.destination = PositionManager.getDummyLocation(context);
            try {
                this.destination = new PointWrapper(
                        context, jsonObject.getJSONObject("destination"));
            } catch (JSONException e) {}
            // via points
            viaPointList = new ArrayList<PointWrapper>();
            JSONArray jsonViaPointList = null;
            try {
                jsonViaPointList = jsonObject.getJSONArray("viaPointList");
            } catch (JSONException e) {
                jsonViaPointList = null;
            } finally {
                if (jsonViaPointList != null) {
                    for (int i=0; i<jsonViaPointList.length(); i++) {
                        try {
                            this.viaPointList.add(
                                    new PointWrapper(
                                        context, jsonViaPointList.getJSONObject(i)));
                        } catch (JSONException e) {}
                    }
                }
            }
            // indirection factor
            this.indirectionFactor = 2.0;
            try {
                double factor = jsonObject.getDouble("indirectionFactor");
                if (Doubles.contains(Constants.IndirectionFactorValueArray, factor)) {
                    this.indirectionFactor = factor;
                }
            } catch (JSONException e) {}
            // allowed way classes
            this.wayClassList = new ArrayList<String>();
            JSONArray jsonWayClassList = null;
            try {
                jsonWayClassList = jsonObject.getJSONArray("wayClassList");
            } catch (JSONException e) {
                jsonWayClassList = null;
            } finally {
                if (jsonWayClassList != null) {
                    for (int i=0; i<jsonWayClassList.length(); i++) {
                        try {
                            String wayClassFromJson = jsonWayClassList.getString(i);
                            if (Arrays.asList(Constants.RoutingWayClassValueArray).contains(wayClassFromJson)) {
                                wayClassList.add(wayClassFromJson);
                            }
                        } catch (JSONException e) {}
                    }
                }
                // default: select all
                if (wayClassList.isEmpty()) {
                    for (String defaultWayClass : Constants.RoutingWayClassValueArray) {
                        wayClassList.add(defaultWayClass);
                    }
                }
            }
        }

        public int getSelectedRouteId() {
            return this.selectedRouteId;
        }

        public void setSelectedRouteId(int newRouteId) {
            this.selectedRouteId = newRouteId;
            storeRouteSettings();
        }

        public PointWrapper getStartPoint() {
            return this.start;
        }

        public void setStartPoint(PointWrapper newLocation) {
            if (newLocation != null) {
                this.start = newLocation;
                storeRouteSettings();
            }
        }

        public PointWrapper getDestinationPoint() {
            return this.destination;
        }

        public void setDestinationPoint(PointWrapper newLocation) {
            if (newLocation != null) {
                this.destination = newLocation;
                storeRouteSettings();
            }
        }

        public ArrayList<PointWrapper> getViaPointList() {
            return this.viaPointList;
        }

        public void setViaPointList(ArrayList<PointWrapper> newViaPointList) {
            if (newViaPointList != null) {
                this.viaPointList = newViaPointList;
                storeRouteSettings();
            }
        }

        public void appendEmptyViaPoint() {
            this.viaPointList.add(PositionManager.getDummyLocation(context));
            storeRouteSettings();
        }

        public void replaceViaPointAtIndex(int index, PointWrapper viaPointToReplace) {
            try {
                this.viaPointList.set(index, viaPointToReplace);
            } catch (IndexOutOfBoundsException e) {
            } finally {
                storeRouteSettings();
            }
        }

        public void removeViaPointAtIndex(int index) {
            try {
                this.viaPointList.remove(index);
            } catch (IndexOutOfBoundsException e) {
            } finally {
                storeRouteSettings();
            }
        }

        public void clearViaPointList() {
            this.viaPointList = new ArrayList<PointWrapper>();
            storeRouteSettings();
        }

        public double getIndirectionFactor() {
            return this.indirectionFactor;
        }

        public void setIndirectionFactor(double newIndirectionFactor) {
            if (Doubles.contains(Constants.IndirectionFactorValueArray, newIndirectionFactor)) {
                this.indirectionFactor = newIndirectionFactor;
                storeRouteSettings();
            }
        }

        public ArrayList<String> getWayClassList() {
            return this.wayClassList;
        }

        public void setWayClassList(ArrayList<String> newWayClassList) {
            if (newWayClassList != null) {
                this.wayClassList = newWayClassList;
                storeRouteSettings();
            }
        }

        public void storeRouteSettings() {
            JSONObject jsonRouteSettings = new JSONObject();
            try {
                jsonRouteSettings.put("selectedRouteId", this.selectedRouteId);
            } catch (JSONException e) {}
            // start and destination
            try {
                jsonRouteSettings.put("start", this.start.toJson());
            } catch (JSONException e) {}
            try {
                jsonRouteSettings.put("destination", this.destination.toJson());
            } catch (JSONException e) {}
            // via points
            JSONArray jsonViaPointList = new JSONArray();
            for (PointWrapper viaPoint : this.viaPointList) {
                try {
                    jsonViaPointList.put(viaPoint.toJson());
                } catch (JSONException e) {}
            }
            try {
                jsonRouteSettings.put("viaPointList", jsonViaPointList);
            } catch (JSONException e) {}
            // indirection factor
            try {
                jsonRouteSettings.put("indirectionFactor", this.indirectionFactor);
            } catch (JSONException e) {}
            // allowed way classes
            JSONArray jsonWayClassList = new JSONArray();
            for (String wayClass : this.wayClassList) {
                jsonWayClassList.put(wayClass);
            }
            try {
                jsonRouteSettings.put("wayClassList", jsonWayClassList);
            } catch (JSONException e) {}
    		// save settings
	    	Editor editor = settings.edit();
		    editor.putString("routeSettings", jsonRouteSettings.toString());
    		editor.commit();
            // null RouteSettings object to force reload on next getRouteSettings()
            routeSettings = null;
        }
    }


    /**
     * server settings
     */

    public class ServerSettings {

        private String serverURL;
        private OSMMap selectedMap;
        private PublicTransportProvider selectedPublicTransportProvider;

        public ServerSettings(JSONObject jsonObject) {
            this.serverURL = Constants.DEFAULT.SERVER_URL;
            try {
                this.serverURL = jsonObject.getString("serverURL");
            } catch (JSONException e) {}
            // map
            this.selectedMap = null;
            try {
                this.selectedMap = AccessDatabase.getInstance(context).getMap(
                        jsonObject.getString("selectedMapName"));
            } catch (JSONException e) {}
            // public transport provider
            this.selectedPublicTransportProvider = null;
            try {
                this.selectedPublicTransportProvider = AccessDatabase.getInstance(context).getPublicTransportProvider(
                        jsonObject.getString("selectedPublicTransportProviderIdentifier"));
            } catch (JSONException e) {}
        }

        public String getServerURL() {
            return this.serverURL;
        }

        public void setServerURL(String newServerURL) {
            this.serverURL = newServerURL;
            this.selectedMap = null;
            this.selectedPublicTransportProvider = null;
            storeServerSettings();
        }

        public OSMMap getSelectedMap() {
            return this.selectedMap;
        }

        public void setSelectedMap(OSMMap newMap) {
            this.selectedMap = newMap;
            storeServerSettings();
        }

        public PublicTransportProvider getSelectedPublicTransportProvider() {
            return this.selectedPublicTransportProvider;
        }

        public void setSelectedPublicTransportProvider(PublicTransportProvider newProvider) {
            this.selectedPublicTransportProvider = newProvider;
            storeServerSettings();
        }

        public void storeServerSettings() {
            JSONObject jsonServerSettings = new JSONObject();
            try {
                jsonServerSettings.put("serverURL", this.serverURL);
            } catch (JSONException e) {}
            if (this.selectedMap != null) {
                try {
                    jsonServerSettings.put("selectedMapName", this.selectedMap.getName());
                } catch (JSONException e) {}
            }
            if (this.selectedPublicTransportProvider != null) {
                try {
                    jsonServerSettings.put("selectedPublicTransportProviderIdentifier", this.selectedPublicTransportProvider.getIdentifier());
                } catch (JSONException e) {}
            }
    		// save settings
	    	Editor editor = settings.edit();
		    editor.putString("serverSettings", jsonServerSettings.toString());
    		editor.commit();
            // null ServerSettings object to force reload on next getServerSettings()
            serverSettings = null;
        }
    }


    /**
     * search term history and settings
     */

    public class SearchTermHistory {

        private ArrayList<String> searchTermList;

        public SearchTermHistory(JSONArray jsonSearchTermList) {
            searchTermList = new ArrayList<String>();
            for(int i=0; i<jsonSearchTermList.length(); i++){
                try {
                    searchTermList.add(jsonSearchTermList.getString(i));
                } catch (JSONException e) {}
            }
        }

        public ArrayList<String> getSearchTermList() {
            return this.searchTermList;
        }

        public void addSearchTerm(String searchTerm) {
            if (! this.searchTermList.contains(searchTerm)) {
                this.searchTermList.add(0, searchTerm);
                if (this.searchTermList.size() > MAX_NUMBER_OF_SEARCH_TERM_HISTORY_ENTRIES) {
                    searchTermList.remove(searchTermList.size() - 1);
                }
                storeSearchTermHistory();
            }
        }

        public void storeSearchTermHistory() {
            JSONArray jsonSearchTermList = new JSONArray();
            for (String searchTerm : this.searchTermList) {
                jsonSearchTermList.put(searchTerm);
            }
    		// save settings
	    	Editor editor = settings.edit();
		    editor.putString("searchTermList", jsonSearchTermList.toString());
    		editor.commit();
            // null searchTermHistory object to force reload on next getSearchTermHistory()
            searchTermHistory = null;
        }
    }


    public class FavoritesSearchSettings {

        private String searchTerm;
        private ArrayList<Integer> favoritesProfileIdList;
        private int sortCriteria;

        public FavoritesSearchSettings(JSONObject jsonObject) {
            // search term
            this.searchTerm = "";
            try {
                this.searchTerm = jsonObject.getString("searchTerm");
            } catch (JSONException e) {}
            // selected favorites profiles
            favoritesProfileIdList = new ArrayList<Integer>();
            JSONArray jsonFavoritesProfileIdList = null;
            try {
                jsonFavoritesProfileIdList = jsonObject.getJSONArray("favoritesProfileIdList");
            } catch (JSONException e) {
                jsonFavoritesProfileIdList = null;
            } finally {
                if (jsonFavoritesProfileIdList != null) {
                    for (int i=0; i<jsonFavoritesProfileIdList.length(); i++) {
                        try {
                            this.favoritesProfileIdList.add(jsonFavoritesProfileIdList.getInt(i));
                        } catch (JSONException e) {}
                    }
                } else {
                    for (Integer favoritesProfileId : AccessDatabase.getInstance(context).getFavoritesProfileMap().keySet()) {
                        favoritesProfileIdList.add(favoritesProfileId);
                    }
                }
            }
            // search sort criteria
            this.sortCriteria = Constants.SORT_CRITERIA.DISTANCE_ASC;
            try {
                int sortCriteriaFromJson = jsonObject.getInt("sortCriteria");
                if (Ints.contains(Constants.SearchFavoritesProfileSortCriteriaValueArray, sortCriteriaFromJson)) {
                    this.sortCriteria = sortCriteriaFromJson;
                }
            } catch (JSONException e) {}
        }

        public String getSearchTerm() {
            return this.searchTerm;
        }

        public void setSearchTerm(String newSearchTerm) {
            if (newSearchTerm != null) {
                this.searchTerm = newSearchTerm;
                getSearchTermHistory().addSearchTerm(newSearchTerm);
                storeFavoritesSearchSettings();
            }
        }

        public ArrayList<Integer> getFavoritesProfileIdList() {
            return this.favoritesProfileIdList;
        }

        public void setFavoritesProfileIdList(ArrayList<Integer> newFavoritesProfileIdList) {
            if (newFavoritesProfileIdList != null) {
                this.favoritesProfileIdList = newFavoritesProfileIdList;
                storeFavoritesSearchSettings();
            }
        }

        public int getSortCriteria() {
            return this.sortCriteria;
        }


        public void setSortCriteria(int newSortCriteria) {
            if (Ints.contains(Constants.SearchFavoritesProfileSortCriteriaValueArray, newSortCriteria)) {
                this.sortCriteria = newSortCriteria;
                storeFavoritesSearchSettings();
            }
        }

        public void storeFavoritesSearchSettings() {
            JSONObject jsonFavoritesSearchSettings = new JSONObject();
            try {
                jsonFavoritesSearchSettings.put("searchTerm", this.searchTerm);
            } catch (JSONException e) {}
            JSONArray jsonFavoritesProfileIdList = new JSONArray();
            for (Integer favoritesProfileId : this.favoritesProfileIdList) {
                jsonFavoritesProfileIdList.put(favoritesProfileId);
            }
            try {
                jsonFavoritesSearchSettings.put("favoritesProfileIdList", jsonFavoritesProfileIdList);
            } catch (JSONException e) {}
            // search sort criteria
            try {
                jsonFavoritesSearchSettings.put("sortCriteria", this.sortCriteria);
            } catch (JSONException e) {}
            // save settings
            Editor editor = settings.edit();
            editor.putString("favoritesSearchSettings", jsonFavoritesSearchSettings.toString());
            editor.commit();
            // null FavoritesSearchSettings object to force reload on next getFavoritesSearchSettings()
            favoritesSearchSettings = null;
        }
    }


}
