package org.walkersguide.android.server.address;

import org.walkersguide.android.data.object_with_id.common.Coordinates;
import org.walkersguide.android.server.ServerUtility;
import org.walkersguide.android.server.ServerTask;
import org.walkersguide.android.server.ServerTaskExecutor;
import org.json.JSONException;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import java.util.Locale;
import org.walkersguide.android.data.object_with_id.Point;
import org.json.JSONArray;
import java.net.URLEncoder;
import java.io.IOException;
import java.util.ArrayList;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;


public class ResolveAddressStringTask extends ServerTask {

    private String addressString;
    private Point nearbyCurrentLocation;

    public ResolveAddressStringTask(String addressString, Point nearbyCurrentLocation) {
        this.addressString = addressString;
        this.nearbyCurrentLocation = nearbyCurrentLocation;
    }

    @Override public void execute() throws AddressException {
        String requestUrl = null;
        try {
            requestUrl = String.format(
                    Locale.ROOT,
                    "%1$s/search?format=jsonv2&q=%2$s&accept-language=%3$s&addressdetails=1&limit=10",
                    AddressUtility.ADDRESS_RESOLVER_URL,
                    URLEncoder.encode(
                        this.addressString, StandardCharsets.UTF_8.toString()),
                    Locale.getDefault().getLanguage());
        } catch (UnsupportedEncodingException e) {
            // since java 7 utf-8 is guaranteed to be there
        }
        // add current location if required
        if (nearbyCurrentLocation != null) {
            Coordinates coordinates = nearbyCurrentLocation.getCoordinates();
            requestUrl += String.format(
                    Locale.ROOT,
                    "&viewboxlbrt=%1$f,%2$f,%3$f,%4$f&bounded=1",
                    coordinates.getLongitude() - 0.1,
                    coordinates.getLatitude() - 0.1,
                    coordinates.getLongitude() + 0.1,
                    coordinates.getLatitude() + 0.1);
        }

        JSONArray jsonAddressList = ServerUtility.performRequestAndReturnJsonArray(
                requestUrl, null, AddressException.class);
        if (jsonAddressList.length() == 0) {
            throw new AddressException(AddressException.RC_NO_COORDINATES_FOR_ADDRESS);
        }

        ArrayList<StreetAddress> addressList = new ArrayList<StreetAddress>();
        for (int i=0; i<jsonAddressList.length(); i++) {
            try {
                addressList.add(
                        AddressUtility.createStreetAddressFromOSM(
                            jsonAddressList.getJSONObject(i)));
            } catch (JSONException e) {}
        }
        if (addressList.isEmpty()) {
            throw new AddressException(AddressException.RC_BAD_RESPONSE);
        }

        if (! isCancelled()) {
            ServerTaskExecutor.sendResolveAddressStringTaskSuccessfulBroadcast(getId(), addressList);
        }
    }

}
