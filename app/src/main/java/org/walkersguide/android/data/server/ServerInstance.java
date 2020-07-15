package org.walkersguide.android.data.server;

import java.util.ArrayList;

import org.walkersguide.android.data.poi.POICategory;


public class ServerInstance {

    private String serverName, serverURL, serverVersion;
    private ArrayList<OSMMap> availableMapList;
    private ArrayList<POICategory> supportedPOICategoryList;
    private ArrayList<Integer> supportedAPIVersionList;

    public ServerInstance(String serverName, String serverURL, String serverVersion,
            ArrayList<OSMMap> mapList, ArrayList<POICategory> poiCategoryList, ArrayList<Integer> apiVersionList) {
        this.serverName = serverName;
        this.serverURL = serverURL;
        this.serverVersion = serverVersion;
        this.availableMapList = mapList;
        this.supportedPOICategoryList = poiCategoryList;
        this.supportedAPIVersionList = apiVersionList;
    }

    public String getServerName() {
        return this.serverName;
    }

    public String getServerURL() {
        return this.serverURL;
    }

    public String getServerVersion() {
        return this.serverVersion;
    }

    public ArrayList<OSMMap> getAvailableMapList() {
        return this.availableMapList;
    }

    public ArrayList<POICategory> getSupportedPOICategoryList() {
        return this.supportedPOICategoryList;
    }

    public ArrayList<Integer> getSupportedAPIVersionList() {
        return this.supportedAPIVersionList;
    }

}
