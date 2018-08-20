package org.walkersguide.android.data.server;

import org.walkersguide.android.data.poi.POICategory;
import org.walkersguide.android.data.route.WayClass;
import java.util.ArrayList;

public class ServerInstance {

    private String serverName, serverURL, serverVersion;
    private ArrayList<OSMMap> availableMapList;
    private ArrayList<PublicTransportProvider> supportedPublicTransportProviderList;
    private ArrayList<POICategory> supportedPOICCategoryList;
    private ArrayList<WayClass> supportedWayClassList;
    private ArrayList<Double> supportedIndirectionFactorList;
    private ArrayList<Integer> supportedAPIVersionList;

    public ServerInstance(String serverName, String serverURL, String serverVersion,
            ArrayList<OSMMap> mapList, ArrayList<PublicTransportProvider> publicTransportProviderList,
            ArrayList<POICategory> poiCategoryList, ArrayList<WayClass> wayClassList,
            ArrayList<Double> indirectionFactorList, ArrayList<Integer> apiVersionList) {
        this.serverName = serverName;
        this.serverURL = serverURL;
        this.serverVersion = serverVersion;
        this.availableMapList = mapList;
        this.supportedPublicTransportProviderList = publicTransportProviderList;
        this.supportedPOICCategoryList = poiCategoryList;
        this.supportedWayClassList = wayClassList;
        this.supportedIndirectionFactorList = indirectionFactorList;
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

    public ArrayList<PublicTransportProvider> getSupportedPublicTransportProviderList() {
        return this.supportedPublicTransportProviderList;
    }

    public ArrayList<POICategory> getSupportedPOICCategoryList() {
        return this.supportedPOICCategoryList;
    }

    public ArrayList<WayClass> getSupportedWayClassList() {
        return this.supportedWayClassList;
    }

    public ArrayList<Double> getSupportedIndirectionFactorList() {
        return this.supportedIndirectionFactorList;
    }

    public ArrayList<Integer> getSupportedAPIVersionList() {
        return this.supportedAPIVersionList;
    }

}
