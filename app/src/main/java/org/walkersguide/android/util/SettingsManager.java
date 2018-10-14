package org.walkersguide.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.route.WayClass;
import org.walkersguide.android.data.server.AddressProvider;
import org.walkersguide.android.data.server.OSMMap;
import org.walkersguide.android.data.server.PublicTransportProvider;
import org.walkersguide.android.sensor.PositionManager;


public class SettingsManager {
    public static final int MAX_NUMBER_OF_SEARCH_TERM_HISTORY_ENTRIES = 100;

    // class variables
    private static SettingsManager settingsManagerInstance;
    private Context context;
    private SharedPreferences settings;

    // settings
    private GeneralSettings generalSettings;
    private DirectionSettings directionSettings;
    private LocationSettings locationSettings;
    private POISettings poiSettings;
    private RouteSettings routeSettings;
    private ServerSettings serverSettings;
    private SearchTermHistory searchTermHistory;

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

	public POISettings getPOISettings() {
        if (poiSettings == null) {
            JSONObject jsonPOISettings;
            try {
    		    jsonPOISettings = new JSONObject(settings.getString("poiSettings", "{}"));
    		} catch (JSONException e) {
                jsonPOISettings = new JSONObject();
            }
            poiSettings = new POISettings(jsonPOISettings);
        }
		return poiSettings;
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


    /**
     * General, direction and location settings
     */

    public class GeneralSettings {

        private int recentOpenTab;
        private int shakeIntensity;
        private boolean enableTextInputHistory, showDevelopmentMaps;

        public GeneralSettings(JSONObject jsonObject) {
            this.recentOpenTab = Constants.MAIN_FRAGMENT.ROUTER;
            try {
                int tab = jsonObject.getInt("recentOpenTab");
                if (Ints.contains(Constants.MainActivityFragmentValueArray, tab)) {
                    this.recentOpenTab = tab;
                }
            } catch (JSONException e) {}
            this.shakeIntensity = Constants.SHAKE_INTENSITY.DISABLED;
            try {
                int intensity = jsonObject.getInt("shakeIntensity");
                if (Ints.contains(Constants.ShakeIntensityValueArray, intensity)) {
                    this.shakeIntensity = intensity;
                }
            } catch (JSONException e) {}
            // enable text input history
            this.enableTextInputHistory = true;
            try {
                this.enableTextInputHistory = jsonObject.getBoolean("enableTextInputHistory");
            } catch (JSONException e) {}
            // show development maps
            this.showDevelopmentMaps = BuildConfig.DEFAULT_SHOW_DEVELOPMENT_MAPS;;
            try {
                this.showDevelopmentMaps = jsonObject.getBoolean("showDevelopmentMaps");
            } catch (JSONException e) {}
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

        public boolean getEnableTextInputHistory() {
            return this.enableTextInputHistory;
        }

        public void setEnableTextInputHistory(boolean enabled) {
            this.enableTextInputHistory = enabled;
            storeGeneralSettings();
        }

        public boolean getShowDevelopmentMaps() {
            return this.showDevelopmentMaps;
        }

        public void setShowDevelopmentMaps(boolean enabled) {
            this.showDevelopmentMaps = enabled;
            storeGeneralSettings();
        }

        public void storeGeneralSettings() {
            JSONObject jsonGeneralSettings = new JSONObject();
            try {
                jsonGeneralSettings.put("recentOpenTab", this.recentOpenTab);
            } catch (JSONException e) {}
            try {
                jsonGeneralSettings.put("shakeIntensity", this.shakeIntensity);
            } catch (JSONException e) {}
            try {
                jsonGeneralSettings.put("enableTextInputHistory", this.enableTextInputHistory);
            } catch (JSONException e) {}
            try {
                jsonGeneralSettings.put("showDevelopmentMaps", this.showDevelopmentMaps);
            } catch (JSONException e) {}
    		// save settings
	    	Editor editor = settings.edit();
		    editor.putString("generalSettings", jsonGeneralSettings.toString());
    		editor.apply();
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
    		editor.apply();
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
    		editor.apply();
            // null LocationSettings object to force reload on next getLocationSettings()
            locationSettings = null;
        }
    }


    /**
     * poi settings
     */

    public class POISettings {

        private int selectedPOIProfileId;
        private boolean autoUpdate;
        private boolean directionFilter;
        private boolean showAllPoints;

        public POISettings(JSONObject jsonObject) {
            // selected poi profile id
            this.selectedPOIProfileId = -1;
            try {
                this.selectedPOIProfileId = jsonObject.getInt("selectedPOIProfileId");
            } catch (JSONException e) {}
            // auto update point and segment lists
            this.autoUpdate = false;
            try {
                this.autoUpdate = jsonObject.getBoolean("autoUpdate");
            } catch (JSONException e) {}
            // viewing direction filter
            this.directionFilter = false;
            try {
                this.directionFilter = jsonObject.getBoolean("directionFilter");
            } catch (JSONException e) {}
            // show all points switch at the next intersections fragment
            this.showAllPoints = false;
            try {
                this.showAllPoints = jsonObject.getBoolean("showAllPoints");
            } catch (JSONException e) {}
        }

        public int getSelectedPOIProfileId() {
            if (! AccessDatabase.getInstance(context).getPOIProfileMap().containsKey(this.selectedPOIProfileId)) {
                setSelectedPOIProfileId(-1);
            }
            return this.selectedPOIProfileId;
        }

        public void setSelectedPOIProfileId(int newPOIProfileId) {
            this.selectedPOIProfileId = newPOIProfileId;
            storePOISettings();
        }

        public boolean getAutoUpdate() {
            return this.autoUpdate;
        }

        public void setAutoUpdate(boolean newAutoUpdate) {
            this.autoUpdate = newAutoUpdate;
            storePOISettings();
        }

        public boolean filterPointListByDirection() {
            return this.directionFilter;
        }

        public void setDirectionFilterStatus(boolean newStatus) {
            this.directionFilter = newStatus;
            storePOISettings();
        }

        public boolean getShowAllPoints() {
            return this.showAllPoints;
        }

        public void setShowAllPoints(boolean newShowAllPoints) {
            this.showAllPoints = newShowAllPoints;
            storePOISettings();
        }

        public void storePOISettings() {
            JSONObject jsonPOISettings = new JSONObject();
            try {
                jsonPOISettings.put("selectedPOIProfileId", this.selectedPOIProfileId);
            } catch (JSONException e) {}
            try {
                jsonPOISettings.put("autoUpdate", this.autoUpdate);
            } catch (JSONException e) {}
            try {
                jsonPOISettings.put("directionFilter", this.directionFilter);
            } catch (JSONException e) {}
            try {
                jsonPOISettings.put("showAllPoints", this.showAllPoints);
            } catch (JSONException e) {}
            // save settings
            Editor editor = settings.edit();
            editor.putString("poiSettings", jsonPOISettings.toString());
            editor.apply();
            // null POISettings object to force reload on next getPOISettings()
            poiSettings = null;
        }
    }


    /**
     * route settings
     */

    public class RouteSettings {
        public static final double DEFAULT_INDIRECTION_FACTOR = 2.0;

        private int selectedRouteId;
        private PointWrapper start, destination;
        private ArrayList<PointWrapper> viaPointList;
        private double indirectionFactor;
        private ArrayList<WayClass> wayClassList;

        public RouteSettings(JSONObject jsonObject) {
            this.selectedRouteId = -1;
            try {
                this.selectedRouteId = jsonObject.getInt("selectedRouteId");
            } catch (JSONException e) {}
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
            this.indirectionFactor = DEFAULT_INDIRECTION_FACTOR;
            try {
                this.indirectionFactor = jsonObject.getDouble("indirectionFactor");
            } catch (JSONException e) {}
            // allowed way classes
            this.wayClassList = new ArrayList<WayClass>();
            JSONArray jsonWayClassList = null;
            try {
                jsonWayClassList = jsonObject.getJSONArray("wayClassList");
            } catch (JSONException e) {
                jsonWayClassList = null;
            } finally {
                if (jsonWayClassList != null) {
                    for (int i=0; i<jsonWayClassList.length(); i++) {
                        try {
                            wayClassList.add(
                                    new WayClass(
                                        context, jsonWayClassList.getString(i)));
                        } catch (JSONException e) {}
                    }
                }
            }
        }

        public int getSelectedRouteId() {
            if (! AccessDatabase.getInstance(context).getRouteIdList(null).contains(this.selectedRouteId)) {
                setSelectedRouteId(-1);
            }
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
            this.indirectionFactor = newIndirectionFactor;
            storeRouteSettings();
        }

        public ArrayList<WayClass> getWayClassList() {
            return this.wayClassList;
        }

        public void setWayClassList(ArrayList<WayClass> newWayClassList) {
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
            for (WayClass wayClass : this.wayClassList) {
                jsonWayClassList.put(wayClass.getId());
            }
            try {
                jsonRouteSettings.put("wayClassList", jsonWayClassList);
            } catch (JSONException e) {}
    		// save settings
	    	Editor editor = settings.edit();
		    editor.putString("routeSettings", jsonRouteSettings.toString());
    		editor.apply();
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
        private AddressProvider selectedAddressProvider;
        private boolean logQueriesOnServer;

        public ServerSettings(JSONObject jsonObject) {
            this.serverURL = BuildConfig.SERVER_URL;
            try {
                this.serverURL = jsonObject.getString("serverURL");
            } catch (JSONException e) {}
            // map
            this.selectedMap = null;
            try {
                this.selectedMap = new OSMMap(jsonObject.getJSONObject("selectedMap"));
            } catch (JSONException e) {
                System.out.println("xxx create map: " + e.getMessage());
            }
            // public transport provider
            this.selectedPublicTransportProvider = null;
            try {
                this.selectedPublicTransportProvider = new PublicTransportProvider(
                        context, jsonObject.getString("selectedPublicTransportProviderId"));
            } catch (JSONException e) {}
            // address provider
            this.selectedAddressProvider = new AddressProvider(context, Constants.ADDRESS_PROVIDER.OSM);
            try {
                String addressProviderIdFromJson = jsonObject.getString("selectedAddressProviderId");
                if (Arrays.asList(Constants.AddressProviderValueArray).contains(addressProviderIdFromJson)) {
                    this.selectedAddressProvider = new AddressProvider(context, addressProviderIdFromJson);
                }
            } catch (JSONException e) {}
            // allow server logging
            this.logQueriesOnServer = false;
            try {
                this.logQueriesOnServer = jsonObject.getBoolean("logQueriesOnServer");
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

        public PublicTransportProvider getSelectedPublicTransportProvider() {
            return this.selectedPublicTransportProvider;
        }

        public void setSelectedPublicTransportProvider(PublicTransportProvider newProvider) {
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

        public boolean getLogQueriesOnServer() {
            return this.logQueriesOnServer;
        }

        public void setLogQueriesOnServer(boolean newValue) {
            this.logQueriesOnServer = newValue;
            storeServerSettings();
        }

        public void storeServerSettings() {
            JSONObject jsonServerSettings = new JSONObject();
            try {
                jsonServerSettings.put("serverURL", this.serverURL);
            } catch (JSONException e) {}
            if (this.selectedMap != null) {
                try {
                    jsonServerSettings.put("selectedMap", this.selectedMap.toJson());
                } catch (JSONException e) {
                    System.out.println("xxx store map: " + e.getMessage());
                }
            }
            if (this.selectedPublicTransportProvider != null) {
                try {
                    jsonServerSettings.put("selectedPublicTransportProviderId", this.selectedPublicTransportProvider.getId());
                } catch (JSONException e) {}
            }
            if (this.selectedAddressProvider != null) {
                try {
                    jsonServerSettings.put("selectedAddressProviderId", this.selectedAddressProvider.getId());
                } catch (JSONException e) {}
            }
            // server logging
            try {
                jsonServerSettings.put("logQueriesOnServer", this.logQueriesOnServer);
            } catch (JSONException e) {}
    		// save settings
	    	Editor editor = settings.edit();
		    editor.putString("serverSettings", jsonServerSettings.toString());
    		editor.apply();
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
            for (String word : searchTerm.split("\\s")) {
                // add every single word at least four chars long
                if (word.length() > 3
                        && ! this.searchTermList.contains(word)) {
                    this.searchTermList.add(0, word);
                }
                if (! this.searchTermList.contains(searchTerm)) {
                    this.searchTermList.add(0, searchTerm);
                }
                // clear odd entries
                int numberOfOddEntries = this.searchTermList.size() - MAX_NUMBER_OF_SEARCH_TERM_HISTORY_ENTRIES;
                if (numberOfOddEntries > 0) {
                    searchTermList.subList(
                            searchTermList.size() - numberOfOddEntries,
                            searchTermList.size())
                        .clear();
                }
                storeSearchTermHistory();
            }
        }

        public void clearSearchTermList() {
            this.searchTermList = new ArrayList<String>();
            storeSearchTermHistory();
        }

        public void storeSearchTermHistory() {
            JSONArray jsonSearchTermList = new JSONArray();
            for (String searchTerm : this.searchTermList) {
                jsonSearchTermList.put(searchTerm);
            }
    		// save settings
	    	Editor editor = settings.edit();
		    editor.putString("searchTermList", jsonSearchTermList.toString());
    		editor.apply();
            // null searchTermHistory object to force reload on next getSearchTermHistory()
            searchTermHistory = null;
        }
    }

}
