package org.walkersguide.android.server.address;

import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.server.ServerUtility;
import org.walkersguide.android.server.ServerTask;
import org.walkersguide.android.server.ServerTaskExecutor;
import org.json.JSONException;
import timber.log.Timber;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import org.walkersguide.android.database.SortMethod;
import android.location.Location;
import java.util.Locale;
import org.json.JSONObject;


public class ResolveCoordinatesTask extends ServerTask {

    private double latitude, longitude;

    public ResolveCoordinatesTask(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override public void execute() throws AddressException {
        // first look into local database
        StreetAddress addressFromDatabase = null;
        DatabaseProfileRequest databaseProfileRequest = new DatabaseProfileRequest(
                DatabaseProfile.addressPoints(), null, SortMethod.DISTANCE_ASC);
        for (ObjectWithId objectWithId : AccessDatabase.getInstance().getObjectListFor(databaseProfileRequest)) {
            if (objectWithId instanceof StreetAddress) {
                StreetAddress address = (StreetAddress) objectWithId;
                // calculate distance
                float[] results = new float[1];
                Location.distanceBetween(
                        this.latitude, this.longitude, address.getLatitude(), address.getLongitude(), results);
                // select, if within 20m radius
                if (results[0] < 20) {
                    addressFromDatabase = address;
                }
                break;
            }
        }
        if (addressFromDatabase != null) {
            ServerTaskExecutor.sendResolveCoordinatesTaskSuccessfulBroadcast(getId(), addressFromDatabase);
            return;
        }

        StreetAddress newAddress = null;
        try {
            String requestUrl = String.format(
                    Locale.ROOT,
                    "%1$s/reverse?format=jsonv2&lat=%2$f&lon=%3$f&accept-language=%4$s&addressdetails=1&zoom=18",
                    AddressUtility.ADDRESS_RESOLVER_URL,
                    this.latitude, this.longitude,
                    Locale.getDefault().getLanguage());
            JSONObject jsonStreetAddress = ServerUtility.performRequestAndReturnJsonObject(
                    requestUrl, null, AddressException.class);
            if (jsonStreetAddress.has("error")) {
                throw new AddressException(
                        AddressException.RC_NO_ADDRESS_FOR_COORDINATES);
            } else {
                newAddress = AddressUtility.createStreetAddressFromOSM(jsonStreetAddress);
            }
        } catch (JSONException e) {
            Timber.e("JSONException: %1$s", e.getMessage());
            throw new AddressException(
                    AddressException.RC_BAD_RESPONSE);
        }

        if (! isCancelled()) {
            // check for accuracy of address
            float[] results = new float[1];
            Location.distanceBetween(
                    this.latitude, this.longitude, newAddress.getLatitude(), newAddress.getLongitude(), results);
            if (results[0] > 100) {
                // if the address differs for more than 100 meters, don't take it
                throw new AddressException(
                        AddressException.RC_NO_ADDRESS_FOR_COORDINATES);
            }

            DatabaseProfile.addressPoints().add(newAddress);
            ServerTaskExecutor.sendResolveCoordinatesTaskSuccessfulBroadcast(getId(), newAddress);
        }
    }

}
