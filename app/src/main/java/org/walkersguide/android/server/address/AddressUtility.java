package org.walkersguide.android.server.address;



import java.util.Iterator;


import org.json.JSONException;
import org.json.JSONObject;


import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import org.walkersguide.android.data.object_with_id.point.PointWithAddressData;


public class AddressUtility {

    // address resolver: https://wiki.openstreetmap.org/wiki/Nominatim
    public static final String ADDRESS_RESOLVER_URL = "https://nominatim.openstreetmap.org";


    public static StreetAddress createStreetAddressFromOSM(JSONObject jsonAddressData) throws JSONException {
        String houseNumber = null, road = null, city = null;
        String displayName = jsonAddressData.getString("display_name");

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
            if (type.equals("house_number")) {
                houseNumber = jsonAddressComponentList.getString(type);
                addressBuilder.setHouseNumber(houseNumber);
            } else if (type.equals("pedestrian")) {
                road = jsonAddressComponentList.getString(type);
                addressBuilder.setRoad(road);
            } else if (type.equals("road")) {
                road = jsonAddressComponentList.getString(type);
                addressBuilder.setRoad(road);
            } else if (type.equals("residential")) {
                addressBuilder.setResidential(jsonAddressComponentList.getString(type));
            } else if (type.equals("suburb")) {
                addressBuilder.setSuburb(jsonAddressComponentList.getString(type));
            } else if (type.equals("city_district")) {
                addressBuilder.setCityDistrict(jsonAddressComponentList.getString(type));
            } else if (type.equals("postcode")) {
                addressBuilder.setZipcode(jsonAddressComponentList.getString(type));
            } else if (type.equals("village")) {
                city = jsonAddressComponentList.getString(type);
                addressBuilder.setCity(city);
            } else if (type.equals("city")) {
                city = jsonAddressComponentList.getString(type);
                addressBuilder.setCity(city);
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

        // set shorter name, if possible
        if (road != null && city != null) {
            addressBuilder.setName(
                    String.format(
                        "%1$s, %2$s", PointWithAddressData.formatRoadAndHouseNumber(road, houseNumber, null), city));
        }

        return addressBuilder.build();
    }

}
