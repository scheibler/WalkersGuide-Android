package org.walkersguide.android.server.address;



import java.util.Iterator;


import org.json.JSONException;
import org.json.JSONObject;


import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;


public class AddressUtility {

    // address resolver: https://wiki.openstreetmap.org/wiki/Nominatim
    public static final String ADDRESS_RESOLVER_URL = "https://nominatim.openstreetmap.org";


    public static StreetAddress createStreetAddressFromOSM(JSONObject jsonAddressData) throws JSONException {
        StreetAddress.Builder addressBuilder = new StreetAddress.Builder(
                jsonAddressData.getString("display_name"),
                jsonAddressData.getDouble("lat"),
                jsonAddressData.getDouble("lon"));
        addressBuilder.setDisplayName(jsonAddressData.getString("display_name"));

        // address sub object
        JSONObject jsonAddressComponentList = jsonAddressData.getJSONObject("address");
        Iterator<String> keysIterator = jsonAddressComponentList.keys();
        while (keysIterator.hasNext()) {
            String type = (String)keysIterator.next();
            if (type.equals("house_number")) {
                addressBuilder.setHouseNumber(jsonAddressComponentList.getString(type));
            } else if (type.equals("pedestrian")) {
                addressBuilder.setRoad(jsonAddressComponentList.getString(type));
            } else if (type.equals("road")) {
                addressBuilder.setRoad(jsonAddressComponentList.getString(type));
            } else if (type.equals("residential")) {
                addressBuilder.setResidential(jsonAddressComponentList.getString(type));
            } else if (type.equals("suburb")) {
                addressBuilder.setSuburb(jsonAddressComponentList.getString(type));
            } else if (type.equals("city_district")) {
                addressBuilder.setCityDistrict(jsonAddressComponentList.getString(type));
            } else if (type.equals("postcode")) {
                addressBuilder.setZipcode(jsonAddressComponentList.getString(type));
            } else if (type.equals("village")) {
                addressBuilder.setCity(jsonAddressComponentList.getString(type));
            } else if (type.equals("city")) {
                addressBuilder.setCity(jsonAddressComponentList.getString(type));
            } else if (type.equals("state")) {
                addressBuilder.setState(jsonAddressComponentList.getString(type));
            } else if (type.equals("country")) {
                addressBuilder.setCountry(jsonAddressComponentList.getString(type));
            } else if (type.equals("country_code")) {
                addressBuilder.setCountryCode(jsonAddressComponentList.getString(type));
            } else if (type.equals(jsonAddressData.getString("type"))) {
                addressBuilder.setExtraName(jsonAddressComponentList.getString(type));
            }
        }

        return addressBuilder.build();
    }

}
