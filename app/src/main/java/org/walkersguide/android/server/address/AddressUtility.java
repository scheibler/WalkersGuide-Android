package org.walkersguide.android.server.address;



import java.util.Iterator;


import org.json.JSONException;
import org.json.JSONObject;


import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import org.walkersguide.android.data.object_with_id.point.PointWithAddressData;
import timber.log.Timber;


public class AddressUtility {

    // address resolver: https://wiki.openstreetmap.org/wiki/Nominatim
    public static final String ADDRESS_RESOLVER_URL = "https://nominatim.openstreetmap.org";


    public static StreetAddress createStreetAddressFromOSM(JSONObject jsonAddressData) throws JSONException {
        String displayName = jsonAddressData.getString("display_name");
        //Timber.d("json: %1$s", jsonAddressData.toString());

        StreetAddress.Builder addressBuilder = new StreetAddress.Builder(
                displayName,
                jsonAddressData.getDouble("lat"),
                jsonAddressData.getDouble("lon"));
        addressBuilder.setDisplayName(displayName);

        // address sub object
        JSONObject jsonAddressComponentList = jsonAddressData.getJSONObject("address");
        Iterator<String> keysIterator = jsonAddressComponentList.keys();
        while (keysIterator.hasNext()) {
            String type = (String)keysIterator.next();
            String value = jsonAddressComponentList.optString(type, null);
            if (value == null) continue;

            if (type.equals(jsonAddressData.getString("category"))
                    || type.equals(jsonAddressData.getString("type"))) {
                addressBuilder.setExtraName(value);
            } else if (type.equals("house_number")
                    || type.equals("house_name")) {
                addressBuilder.setHouseNumber(value);
            } else if (type.equals("pedestrian")
                    || type.equals("road")) {
                addressBuilder.setRoad(value);
            } else if (type.equals("neighbourhood")
                    || type.equals("allotments")
                    || type.equals("quarter")) {
                addressBuilder.setResidential(value);
            } else if (type.equals("suburb")
                    || type.equals("subdivision")) {
                addressBuilder.setSuburb(value);
            } else if (type.equals("city_district")
                    || type.equals("district")) {
                addressBuilder.setCityDistrict(value);
            } else if (type.equals("postcode")) {
                addressBuilder.setZipcode(value);
            } else if (type.equals("village")
                    || type.equals("town")
                    || type.equals("city")) {
                addressBuilder.setCity(value);
            } else if (type.equals("county")
                    || type.equals("state_district")) {
                addressBuilder.setStateDistrict(value);
            } else if (type.equals("state")) {
                addressBuilder.setState(value);
            } else if (type.equals("country")) {
                addressBuilder.setCountry(value);
            } else if (type.equals("country_code")) {
                addressBuilder.setCountryCode(value);
            }
        }

        return addressBuilder.build();
    }

}
