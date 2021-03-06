package org.walkersguide.android.util;

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
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.profile.HistoryPointProfile;
import org.walkersguide.android.data.route.WayClass;
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


public class SettingsManager {
    public static final String SETTINGS_FILE_NAME = "WalkersGuide-Android-Settings";
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
		this.settings = context.getSharedPreferences(SETTINGS_FILE_NAME, Context.MODE_PRIVATE);
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
        private boolean enableTextInputHistory;

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
    		// save settings
	    	Editor editor = settings.edit();
		    editor.putString("generalSettings", jsonGeneralSettings.toString());
    		editor.apply();
            // null GeneralSettings object to force reload on next getGeneralSettings()
            generalSettings = null;
        }
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
	    	Editor editor = settings.edit();
		    editor.putString("directionSettings", jsonDirectionSettings.toString());
    		editor.apply();
            // null DirectionSettings object to force reload on next getDirectionSettings()
            directionSettings = null;
        }
    }



    public class LocationSettings {

        private PointWrapper gpsLocation, simulatedLocation;

        public LocationSettings(JSONObject jsonObject) {
            // gps location
            this.gpsLocation = null;
            try {
                this.gpsLocation = new PointWrapper(
                        context, jsonObject.getJSONObject("gpsLocation"));
            } catch (JSONException e) {}
            // simulated location
            this.simulatedLocation = null;
            try {
                this.simulatedLocation = new PointWrapper(
                        context, jsonObject.getJSONObject("simulatedLocation"));
            } catch (JSONException e) {}
        }

        public PointWrapper getGPSLocation() {
            return this.gpsLocation;
        }

        public void removeGPSLocation() {
            this.gpsLocation = null;
            storeLocationSettings();
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

        public void removeSimulatedLocation() {
            this.simulatedLocation = null;
            storeLocationSettings();
        }

        public void setSimulatedLocation(PointWrapper newLocation) {
            if (newLocation != null) {
                this.simulatedLocation = newLocation;
                storeLocationSettings();
            }
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

        private int selectedHistoryPointProfileId, selectedPOIProfileId;
        private boolean autoUpdate;
        private boolean directionFilter;
        private boolean showAllPoints;

        public POISettings(JSONObject jsonObject) {
            // selected history point profile id
            this.selectedHistoryPointProfileId = HistoryPointProfile.ID_ALL_POINTS;
            try {
                this.selectedHistoryPointProfileId = jsonObject.getInt("selectedHistoryPointProfileId");
            } catch (JSONException e) {}
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

        public int getSelectedHistoryPointProfileId() {
            if (! HistoryPointProfile.getProfileMap(context).containsKey(this.selectedHistoryPointProfileId)) {
                setSelectedHistoryPointProfileId(-1);
            }
            return this.selectedHistoryPointProfileId;
        }

        public void setSelectedHistoryPointProfileId(int newHistoryPointProfileId) {
            this.selectedHistoryPointProfileId = newHistoryPointProfileId;
            storePOISettings();
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
                jsonPOISettings.put("selectedHistoryPointProfileId", this.selectedHistoryPointProfileId);
            } catch (JSONException e) {}
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

        private int selectedRouteId;
        private PointWrapper start, destination;
        private ArrayList<PointWrapper> viaPointList;
        private ArrayList<WayClass> wayClassList;
        private boolean autoSkipToNextRoutePoint;

        public RouteSettings(JSONObject jsonObject) {
            this.selectedRouteId = -1;
            try {
                this.selectedRouteId = jsonObject.getInt("selectedRouteId");
            } catch (JSONException e) {}
            // start point
            this.start = null;
            try {
                this.start = new PointWrapper(
                        context, jsonObject.getJSONObject("start"));
            } catch (JSONException e) {}
            // destination point
            this.destination = null;
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
            // auto skik to next route point
            this.autoSkipToNextRoutePoint = true;
            try {
                this.autoSkipToNextRoutePoint= jsonObject.getBoolean("autoSkipToNextRoutePoint");
            } catch (JSONException e) {}
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

        public void removeStartPoint() {
            this.start = null;
            storeRouteSettings();
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

        public void removeDestinationPoint() {
            this.destination = null;
            storeRouteSettings();
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

        public void replaceViaPointAtIndex(int index, PointWrapper viaPointToReplace) {
            try {
                this.viaPointList.set(index, viaPointToReplace);
            } catch (IndexOutOfBoundsException e) {
                this.viaPointList.add(index, viaPointToReplace);
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

        public ArrayList<WayClass> getWayClassList() {
            return this.wayClassList;
        }

        public void setWayClassList(ArrayList<WayClass> newWayClassList) {
            if (newWayClassList != null) {
                this.wayClassList = newWayClassList;
                storeRouteSettings();
            }
        }

        public boolean getAutoSkipToNextRoutePoint() {
            return this.autoSkipToNextRoutePoint;
        }

        public void setAutoSkipToNextRoutePoint(boolean newValue) {
            this.autoSkipToNextRoutePoint = newValue;
            storeRouteSettings();
        }

        public void storeRouteSettings() {
            JSONObject jsonRouteSettings = new JSONObject();
            try {
                jsonRouteSettings.put("selectedRouteId", this.selectedRouteId);
            } catch (JSONException e) {}
            // start and destination
            if (this.start != null) {
                try {
                    jsonRouteSettings.put("start", this.start.toJson());
                } catch (JSONException e) {}
            }
            if (this.destination != null) {
                try {
                    jsonRouteSettings.put("destination", this.destination.toJson());
                } catch (JSONException e) {}
            }
            // via points
            JSONArray jsonViaPointList = new JSONArray();
            for (PointWrapper viaPoint : this.viaPointList) {
                if (viaPoint != null) {
                    try {
                        jsonViaPointList.put(viaPoint.toJson());
                    } catch (JSONException e) {}
                }
            }
            try {
                jsonRouteSettings.put("viaPointList", jsonViaPointList);
            } catch (JSONException e) {}
            // way classes and weights
            JSONObject jsonWayClassIdAndWeights = new JSONObject();
            for (WayClass wayClass : this.wayClassList) {
                try {
                    jsonWayClassIdAndWeights.put(wayClass.getId(), wayClass.getWeight());
                } catch (JSONException e) {}
            }
            try {
                jsonRouteSettings.put("wayClassIdAndWeights", jsonWayClassIdAndWeights);
            } catch (JSONException e) {}
            try {
                jsonRouteSettings.put("autoSkipToNextRoutePoint", this.autoSkipToNextRoutePoint);
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
                Timber.w("Settings type " + e.getValue().getClass().getName() + " is unknown");
            }
        }
        success = editor.commit();

        // reset and return
        generalSettings = null;
        directionSettings = null;
        locationSettings = null;
        poiSettings = null;
        routeSettings = null;
        serverSettings = null;
        searchTermHistory = null;
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
