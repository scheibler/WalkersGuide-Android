package org.walkersguide.android.server.wg;

import org.walkersguide.android.server.ServerUtility;
import org.walkersguide.android.server.wg.status.OSMMap;
import org.walkersguide.android.server.wg.status.ServerInstance;



import android.text.TextUtils;



import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.server.wg.poi.PoiCategory;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.Helper;


public class WgUtility {

    public static ServerInstance getServerInstanceForServerUrlFromSettings() throws WgException {
        return getServerInstance(SettingsManager.getInstance().getServerURL());
    }


    public static ServerInstance getServerInstance(String serverURL) throws WgException {
        // check server url
        if (TextUtils.isEmpty(serverURL)) {
            throw new WgException(WgException.RC_NO_SERVER_URL);
        }

        // server instance from cache
        ServerInstance cachedServerInstance = GlobalInstance.getInstance().getCachedServerInstance();
        if (cachedServerInstance != null
                && cachedServerInstance.getServerURL().equals(serverURL)) {
            return cachedServerInstance;
        }

        // status request
        JSONObject jsonServerParams = null;
        try {
            jsonServerParams = createServerParamList();
        } catch (JSONException e) {
            throw new WgException(WgException.RC_BAD_REQUEST);
        }
        JSONObject jsonServerResponse = ServerUtility.performRequestAndReturnJsonObject(
                String.format(
                    "%1$s/get_status", serverURL),
                jsonServerParams,
                WgException.class);

        // analyse status output

        ArrayList<Integer> supportedAPIVersionList = new ArrayList<Integer>();
        JSONArray jsonAPIVersionList = null;
        try {
            jsonAPIVersionList = jsonServerResponse.getJSONArray("supported_api_version_list");
        } catch (JSONException e) {
            jsonAPIVersionList = null;
        } finally {
            if (jsonAPIVersionList != null) {
                for (int i=0; i<jsonAPIVersionList.length(); i++) {
                    try {
                        supportedAPIVersionList.add(
                                jsonAPIVersionList.getInt(i));
                    } catch (JSONException e) {}
                }
            }
        }
        if (supportedAPIVersionList.isEmpty()) {
            throw new WgException(WgException.RC_BAD_RESPONSE);
        } else {
            // check for server api version
            Collections.sort(supportedAPIVersionList);
            // check if app is outdated
            int minServerApiVersion = supportedAPIVersionList.get(0);
            if (BuildConfig.SUPPORTED_API_VERSION_LIST[BuildConfig.SUPPORTED_API_VERSION_LIST.length-1] < minServerApiVersion) {
                throw new WgException(WgException.RC_API_CLIENT_OUTDATED);
            }
            // check if server is outdated
            int maxServerApiVersion = supportedAPIVersionList.get(supportedAPIVersionList.size()-1);
            if (BuildConfig.SUPPORTED_API_VERSION_LIST[0] > maxServerApiVersion) {
                throw new WgException(WgException.RC_API_SERVER_OUTDATED);
            }
        }

        String serverName;
        try {
            serverName = jsonServerResponse.getString("server_name");
        } catch (JSONException e) {
            throw new WgException(WgException.RC_BAD_RESPONSE);
        }
        String serverVersion;
        try {
            serverVersion = jsonServerResponse.getString("server_version");
        } catch (JSONException e) {
            throw new WgException(WgException.RC_BAD_RESPONSE);
        }

        ArrayList<OSMMap> availableMapList = new ArrayList<OSMMap>();
        JSONObject jsonMapDict = null;
        try {
            jsonMapDict = jsonServerResponse.getJSONObject("maps");
        } catch (JSONException e) {
            jsonMapDict = null;
        } finally {
            if (jsonMapDict != null) {
                Iterator<String> iter = jsonMapDict.keys();
                while (iter.hasNext()) {
                    String mapId = iter.next();
                    try {
                        JSONObject jsonMap = jsonMapDict.getJSONObject(mapId);
                        availableMapList.add(
                                new OSMMap(
                                    mapId,
                                    jsonMap.getString("name"),
                                    jsonMap.getString("description"),
                                    jsonMap.getLong("created")));
                    } catch (JSONException e) {}
                }
            }
        }
        if (availableMapList.isEmpty()) {
            throw new WgException(WgException.RC_NO_MAP_LIST);
        }

        ArrayList<PoiCategory> supportedPoiCategoryList = new ArrayList<PoiCategory>();
        JSONArray jsonPoiCategoryList = null;
        try {
            jsonPoiCategoryList = jsonServerResponse.getJSONArray("supported_poi_category_list");
        } catch (JSONException e) {
            jsonPoiCategoryList = null;
        } finally {
            if (jsonPoiCategoryList != null) {
                supportedPoiCategoryList = PoiCategory.listFromJson(jsonPoiCategoryList);
            }
        }
        if (supportedPoiCategoryList.isEmpty()) {
            throw new WgException(WgException.RC_BAD_RESPONSE);
        }

        // create server instance object
        ServerInstance serverInstance = new ServerInstance(
                serverName, serverURL, serverVersion, availableMapList,
                supportedPoiCategoryList, supportedAPIVersionList);

        // update server settings
        SettingsManager settingsManagerInstance = SettingsManager.getInstance();
        if (! serverInstance.getServerURL().equals(settingsManagerInstance.getServerURL())) {
            settingsManagerInstance.setServerURL(serverInstance.getServerURL());
        }
        if (settingsManagerInstance.getSelectedMap() != null) {
            int indexOfSelectedMap = serverInstance.getAvailableMapList().indexOf(
                    settingsManagerInstance.getSelectedMap());
            if (indexOfSelectedMap == -1) {
                // reset
                settingsManagerInstance.setSelectedMap(null);
            } else {
                // update
                settingsManagerInstance.setSelectedMap(
                        serverInstance.getAvailableMapList().get(indexOfSelectedMap));
            }
        }

        // cache new server instance object and return
        GlobalInstance.getInstance().setCachedServerInstance(serverInstance);
        return serverInstance;
    }


    public static JSONObject createServerParamList() throws JSONException {
        JSONObject requestJson = new JSONObject();
        // session id and language
        requestJson.put(
                "session_id", GlobalInstance.getInstance().getSessionId());
        requestJson.put(
                "language", Helper.getAppLocale().getLanguage());
        requestJson.put(
                "prefer_translated_strings_in_osm_tags", SettingsManager.getInstance().getPreferTranslatedStrings());
        // selected map id
        OSMMap selectedMap = SettingsManager.getInstance().getSelectedMap();
        if (selectedMap != null) {
            requestJson.put("map_id", selectedMap.getId());
        }
        return requestJson;
    }

}
