package org.walkersguide.android.utils;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;

public class SettingsManager {

	// static settings

	// class variables
	private static SettingsManager settingsManagerInstance;
	private Context mContext;
	private SharedPreferences settings;

	// settings
    private GeneralSettings generalSettings;

	public static SettingsManager getInstance(Context context) {
		if (settingsManagerInstance == null) {
			settingsManagerInstance = new SettingsManager(
					context.getApplicationContext());
		}
		return settingsManagerInstance;
	}

	private SettingsManager(Context context) {
		this.mContext = context;
		this.settings = context.getSharedPreferences("WalkersGuide-Android-Settings", Context.MODE_PRIVATE);
	}

    public String getClientVersion() {
        try {
            return mContext.getPackageManager().getPackageInfo(
                    mContext.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            return "";
        }
    }

	public GeneralSettings getGeneralSettings() {
        if (generalSettings == null) {
            JSONObject jsonGeneralSettings;
            try {
    		    jsonGeneralSettings = new JSONObject(settings.getString("generalSettings", ""));
    		} catch (JSONException e) {
                jsonGeneralSettings = new JSONObject();
                System.out.println("xxx generalSettings json error " + e);
            }
            generalSettings = new GeneralSettings(jsonGeneralSettings);
        }
		return generalSettings;
	}


    public class GeneralSettings {

        private int recentOpenTab;

        public GeneralSettings(JSONObject jsonObject) {
            try {
                this.recentOpenTab = jsonObject.getInt("recentOpenTab");
            } catch (JSONException e) {
                this.recentOpenTab = 0;
            }
        }

        public int getRecentOpenTab() {
            return this.recentOpenTab;
        }

        public void setRecentOpenTab(int tabIndex) {
            this.recentOpenTab = tabIndex;
            storeGeneralSettings();
        }

        public void storeGeneralSettings() {
            JSONObject jsonGeneralSettings = new JSONObject();
            try {
                jsonGeneralSettings.put("recentOpenTab", this.recentOpenTab);
            } catch (JSONException e) {}
    		// save settings
	    	Editor editor = settings.edit();
		    editor.putString("generalSettings", jsonGeneralSettings.toString());
    		editor.commit();
            // null GeneralSettings object to force reload on next getGeneralSettings()
            generalSettings = null;
        }
    }

}
