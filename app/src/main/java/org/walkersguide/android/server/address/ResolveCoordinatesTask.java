package org.walkersguide.android.server.address;

import org.walkersguide.android.database.profile.static_profile.HistoryProfile;
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
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.util.Helper;


public class ResolveCoordinatesTask extends ServerTask {

    public static StreetAddress getAddress(Point coordinatesPoint) throws AddressException {
        final HistoryProfile addressPointProfile = HistoryProfile.addressPoints();

        // first look into local database
        DatabaseProfileRequest databaseProfileRequest = new DatabaseProfileRequest(
                addressPointProfile, null, SortMethod.DISTANCE_ASC);
        for (ObjectWithId objectWithId : AccessDatabase.getInstance().getObjectListFor(databaseProfileRequest)) {
            if (objectWithId instanceof StreetAddress) {
                StreetAddress address = (StreetAddress) objectWithId;
                // calculate distance
                // and select, if within 20m radius
                if (coordinatesPoint.distanceTo(address) < 20) {
                    return address;
                }
                // closest street address from cache is to far away
                // that's possible to say because the request above sorts by distance
                break;
            }
        }

        StreetAddress newAddress = null;
        try {
            String requestUrl = String.format(
                    Locale.ROOT,
                    "%1$s/reverse?format=jsonv2&lat=%2$f&lon=%3$f&accept-language=%4$s&addressdetails=1&zoom=18",
                    AddressUtility.ADDRESS_RESOLVER_URL,
                    coordinatesPoint.getCoordinates().getLatitude(),
                    coordinatesPoint.getCoordinates().getLongitude(),
                    Helper.getAppLocale().getLanguage());
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

        // check for accuracy of address
        if (coordinatesPoint.distanceTo(newAddress) > 100) {
            // if the address differs for more than 100 meters, don't take it
            throw new AddressException(
                    AddressException.RC_NO_ADDRESS_FOR_COORDINATES);
        }

        addressPointProfile.addObject(newAddress);
        return newAddress;
    }


    private Point point;

    public ResolveCoordinatesTask(Point point) {
        this.point = point;
    }

    @Override public void execute() throws AddressException {
        StreetAddress newAddress = getAddress(this.point);
        if (! isCancelled() && newAddress != null) {
            ServerTaskExecutor.sendResolveCoordinatesTaskSuccessfulBroadcast(getId(), newAddress);
        }
    }

}
