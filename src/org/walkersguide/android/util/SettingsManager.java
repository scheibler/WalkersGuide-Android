package org.walkersguide.android.util;

import java.util.ArrayList;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.basic.point.GPS;
import org.walkersguide.android.basic.point.Point;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.poi.POIProfile;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;

import com.google.common.primitives.Ints;

public class SettingsManager {

    // static settings
    public static final String SERVER_URL = "https://wasserbett.ath.cx:19021";

    // class variables
    private static SettingsManager settingsManagerInstance;
    private Context context;
    private SharedPreferences settings;

    // settings
    private String uniqueId;
    private GeneralSettings generalSettings;
    private DirectionSettings directionSettings;
    private LocationSettings locationSettings;
    private POISettings poiSettings;

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
        this.uniqueId = UUID.randomUUID().toString();
	}

    public String getClientVersion() {
        try {
            return context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            return "";
        }
    }

    public String getSessionId() {
        return this.uniqueId;
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


    public class GeneralSettings {

        private int recentOpenTab;
        private int shakeIntensity;

        public GeneralSettings(JSONObject jsonObject) {
            this.recentOpenTab = Constants.FRAGMENT.ROUTER;
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

        private int selectedDirectionSource;
        private int compassDirection, gpsDirection, manualDirection;
        private float differenceToTrueNorth;

        public DirectionSettings(JSONObject jsonObject) {
            this.selectedDirectionSource = Constants.DIRECTION_SOURCE.COMPASS;
            try {
                int directionSource = jsonObject.getInt("selectedDirectionSource");
                if (Ints.contains(Constants.DirectionSourceValueArray, directionSource)) {
                    this.selectedDirectionSource = directionSource;
                }
            } catch (JSONException e) {}
            this.compassDirection = 0;
            try {
                int compass = jsonObject.getInt("compassDirection");
                if (compass >= 0 && compass < 360) {
                    this.compassDirection = compass;
                }
            } catch (JSONException e) {}
            this.gpsDirection = 0;
            try {
                int gps = jsonObject.getInt("gpsDirection");
                if (gps >= 0 && gps < 360) {
                    this.gpsDirection = gps;
                }
            } catch (JSONException e) {}
            this.manualDirection = 0;
            try {
                int manual = jsonObject.getInt("manualDirection");
                if (manual >= 0 && manual < 360) {
                    this.manualDirection = manual;
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

        public int getCompassDirection() {
            return this.compassDirection;
        }

        public void setCompassDirection(int newCompassDirection) {
            if (newCompassDirection>= 0 && newCompassDirection < 360) {
                this.compassDirection = newCompassDirection;
                storeDirectionSettings();
            }
        }

        public int getGPSDirection() {
            return this.gpsDirection;
        }

        public void setGPSDirection(int newGPSDirection) {
            if (newGPSDirection>= 0 && newGPSDirection < 360) {
                this.gpsDirection = newGPSDirection;
                storeDirectionSettings();
            }
        }

        public int getManualDirection() {
            return this.manualDirection;
        }

        public void setManualDirection(int newManualDirection) {
            if (newManualDirection>= 0 && newManualDirection < 360) {
                this.manualDirection = newManualDirection;
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
                jsonDirectionSettings.put("compassDirection", this.compassDirection);
            } catch (JSONException e) {}
            try {
                jsonDirectionSettings.put("gpsDirection", this.gpsDirection);
            } catch (JSONException e) {}
            try {
                jsonDirectionSettings.put("manualDirection", this.manualDirection);
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
        private GPS currentGPSPosition;
        private Point simulatedLocation;

        public LocationSettings(JSONObject jsonObject) {
            this.selectedLocationSource = Constants.LOCATION_SOURCE.GPS;
            try {
                int locationSource = jsonObject.getInt("selectedLocationSource");
                if (Ints.contains(Constants.LocationSourceValueArray, locationSource)) {
                    this.selectedLocationSource = locationSource;
                }
            } catch (JSONException e) {}
            // current location
            this.currentGPSPosition = null;
            try {
                this.currentGPSPosition = new GPS(
                        context, jsonObject.getJSONObject("currentGPSPosition"));
            } catch (JSONException e) {}
            // simulated location
            this.simulatedLocation = null;
            try {
                this.simulatedLocation = new Point(
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

        public GPS getCurrentGPSPosition() {
            return this.currentGPSPosition;
        }

        public void setCurrentGPSPosition(GPS newLocation) {
            if (newLocation != null) {
                this.currentGPSPosition = newLocation;
                storeLocationSettings();
            }
        }

        public Point getSimulatedLocation() {
            return this.simulatedLocation;
        }

        public void setSimulatedLocation(Point newLocation) {
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
            if (this.currentGPSPosition != null) {
                try {
                    jsonLocationSettings.put("currentGPSPosition", this.currentGPSPosition.toJson());
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


    public class POISettings {

        private int selectedPOIProfileId;
        private int selectedPositionInPOIList;

        public POISettings(JSONObject jsonObject) {
            this.selectedPOIProfileId = -1;
            ArrayList<POIProfile> poiProfileList = AccessDatabase.getInstance(context).getPOIProfileList();
            if (! poiProfileList.isEmpty()) {
                // restore poi profile id from settings
                int restoredPOIProfileId;
                try {
                    restoredPOIProfileId = jsonObject.getInt("selectedPOIProfileId");
                } catch (JSONException e) {
                    restoredPOIProfileId = -1;
                }
                // verify, that profile still exists
                for (POIProfile poiProfile : poiProfileList) {
                    if (restoredPOIProfileId == poiProfile.getId()) {
                        this.selectedPOIProfileId = restoredPOIProfileId;
                        break;
                    }
                }
                // if not found, get the first profile
                if (this.selectedPOIProfileId == -1) {
                    this.selectedPOIProfileId = poiProfileList.get(0).getId();
                }
            }
            // position in list
            try {
                this.selectedPositionInPOIList = jsonObject.getInt("selectedPositionInPOIList");
            } catch (JSONException e) {
                this.selectedPositionInPOIList = 0;
            }
        }

        public int getSelectedPOIProfileId() {
            return this.selectedPOIProfileId;
        }

        public void setSelectedPOIProfileId(int newProfileId) {
            this.selectedPOIProfileId = newProfileId;
            this.selectedPositionInPOIList = 0;
            storePOISettings();
        }

        public int getSelectedPositionInPOIList() {
            return this.selectedPositionInPOIList;
        }

        public void setSelectedPositionInPOIList(int newPosition) {
            if (newPosition >= 0) {
                this.selectedPositionInPOIList = newPosition;
                storePOISettings();
            }
        }

        public void storePOISettings() {
            JSONObject jsonPOISettings = new JSONObject();
            try {
                jsonPOISettings.put("selectedPOIProfileId", this.selectedPOIProfileId);
            } catch (JSONException e) {}
            try {
                jsonPOISettings.put("selectedPositionInPOIList", this.selectedPositionInPOIList);
            } catch (JSONException e) {}
    		// save settings
	    	Editor editor = settings.edit();
		    editor.putString("poiSettings", jsonPOISettings.toString());
    		editor.commit();
            // null POISettings object to force reload on next getPOISettings()
            poiSettings = null;
        }
    }

}
