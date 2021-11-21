package org.walkersguide.android.server.util;

import java.util.ArrayList;

import org.walkersguide.android.server.poi.PoiCategory;
import java.io.Serializable;


public class ServerInstance implements Serializable {
    private static final long serialVersionUID = 1l;

    private String serverName, serverURL, serverVersion;
    private ArrayList<OSMMap> availableMapList;
    private ArrayList<PoiCategory> supportedPoiCategoryList;
    private ArrayList<Integer> supportedAPIVersionList;

    public ServerInstance(String serverName, String serverURL, String serverVersion,
            ArrayList<OSMMap> mapList, ArrayList<PoiCategory> poiCategoryList, ArrayList<Integer> apiVersionList) {
        this.serverName = serverName;
        this.serverURL = serverURL;
        this.serverVersion = serverVersion;
        this.availableMapList = mapList;
        this.supportedPoiCategoryList = poiCategoryList;
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

    public ArrayList<PoiCategory> getSupportedPoiCategoryList() {
        return this.supportedPoiCategoryList;
    }

    public ArrayList<Integer> getSupportedAPIVersionList() {
        return this.supportedAPIVersionList;
    }

}
