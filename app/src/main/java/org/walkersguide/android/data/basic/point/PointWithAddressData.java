package org.walkersguide.android.data.basic.point;

import android.content.Context;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;
import org.walkersguide.android.util.StringUtility;


public abstract class PointWithAddressData extends Point implements Serializable {
    private static final long serialVersionUID = 1l;

    public abstract static class Builder extends Point.Builder {
        public Builder(Type type, String name, String subType, double latitude, double longitude) {
            super(type, name, subType, latitude, longitude);
        }

        // optional params
        public Builder setDisplayName(final String displayName) {
            try {
                super.inputData.put(KEY_DISPLAY_NAME, displayName);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setExtraName(final String extraName) {
            try {
                super.inputData.put(KEY_EXTRA_NAME, extraName);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setHouseNumber(final String houseNumber) {
            try {
                super.inputData.put(KEY_HOUSE_NUMBER, houseNumber);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setRoad(final String road) {
            try {
                super.inputData.put(KEY_ROAD, road);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setResidential(final String residential) {
            try {
                super.inputData.put(KEY_RESENTIAL, residential);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setSuburb(final String suburb) {
            try {
                super.inputData.put(KEY_SUBURB, suburb);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setCityDistrict(final String cityDistrict) {
            try {
                super.inputData.put(KEY_CITY_DISTRICT, cityDistrict);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setZipcode(final String zipCode) {
            try {
                super.inputData.put(KEY_ZIP_CODE, zipCode);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setCity(final String city) {
            try {
                super.inputData.put(KEY_CITY, city);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setState(final String state) {
            try {
                super.inputData.put(KEY_STATE, state);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setCountry(final String country) {
            try {
                super.inputData.put(KEY_COUNTRY, country);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setCountryCode(final String countryCode) {
            try {
                super.inputData.put(KEY_COUNTRY_CODE, countryCode);
            } catch (JSONException e) {}
            return this;
        }
    }


    private String displayName, extraName;
    private String houseNumber, road, residential, suburb, cityDistrict, zipCode, city, state, country, countryCode;

    public PointWithAddressData(JSONObject inputData) throws JSONException {
        super(inputData);

        // names
        this.displayName = StringUtility.getNullableStringFromJsonObject(inputData, KEY_DISPLAY_NAME);
        this.extraName = StringUtility.getNullableStringFromJsonObject(inputData, KEY_EXTRA_NAME);

        // address components
        this.houseNumber = StringUtility.getNullableStringFromJsonObject(inputData, KEY_HOUSE_NUMBER);
        this.road = StringUtility.getNullableStringFromJsonObject(inputData, KEY_ROAD);
        this.residential = StringUtility.getNullableStringFromJsonObject(inputData, KEY_RESENTIAL);
        this.suburb = StringUtility.getNullableStringFromJsonObject(inputData, KEY_SUBURB);
        this.cityDistrict = StringUtility.getNullableStringFromJsonObject(inputData, KEY_CITY_DISTRICT);
        this.zipCode = StringUtility.getNullableStringFromJsonObject(inputData, KEY_ZIP_CODE);
        this.city = StringUtility.getNullableStringFromJsonObject(inputData, KEY_CITY);
        this.state = StringUtility.getNullableStringFromJsonObject(inputData, KEY_STATE);
        this.country = StringUtility.getNullableStringFromJsonObject(inputData, KEY_COUNTRY);
        this.countryCode = StringUtility.getNullableStringFromJsonObject(inputData, KEY_COUNTRY_CODE);
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getExtraName() {
        return this.extraName;
    }

    public String getHouseNumber() {
        return this.houseNumber;
    }

    public String getRoad() {
        return this.road;
    }

    public String getResidential() {
        return this.residential;
    }

    public String getSuburb() {
        return this.suburb;
    }

    public String getCityDistrict() {
        return this.cityDistrict;
    }

    public String getZipcode() {
        return this.zipCode;
    }

    public String getCity() {
        return this.city;
    }

    public String getState() {
        return this.state;
    }

    public String getCountry() {
        return this.country;
    }

    public String getCountryCode() {
        return this.countryCode;
    }


    /**
     * address formatter
     */

    public boolean hasAddress() {
        return this.city != null
                && (this.extraName != null || this.road != null);
    }

    public String formatRoadAndHouseNumber() {
        if (this.road != null && this.houseNumber != null) {
            if (Locale.getDefault().getLanguage().equals(Locale.GERMAN.getLanguage())) {
                return String.format("%1$s %2$s", this.road, this.houseNumber);
            }
            return String.format("%1$s %2$s", this.houseNumber, this.road);
        } else if (this.road != null) {
            return this.road;
        }
        return this.extraName;
    }

    public String formatAddressShortLength() {
        if (this.hasAddress()) {
            ArrayList<String> addressComponentList = new ArrayList<String>();
            addressComponentList.add(this.formatRoadAndHouseNumber());
            addressComponentList.add(this.city);
            return TextUtils.join(", ", addressComponentList);
        }
        return super.getName();
    }

    public String formatAddressMediumLength() {
        if (this.hasAddress()) {
            ArrayList<String> addressComponentList = new ArrayList<String>();
            // road and house number
            addressComponentList.add(this.formatRoadAndHouseNumber());
            // add residential or  city district if houseNumber == null
            if (this.houseNumber == null) {
                if (this.residential != null) {
                    addressComponentList.add(this.residential);
                } else if (this.cityDistrict != null) {
                    addressComponentList.add(this.cityDistrict);
                } else if (this.suburb != null) {
                    addressComponentList.add(this.suburb);
                }
            }
            // city
            addressComponentList.add(this.city);
            return TextUtils.join(", ", addressComponentList);
        }
        return super.getName();
    }

    public String formatAddressLongLength() {
        if (this.hasAddress()) {
            ArrayList<String> addressComponentList = new ArrayList<String>();
            // road and house number
            addressComponentList.add(this.formatRoadAndHouseNumber());
            // extra name if not already present
            if (this.extraName != null
                    && ! TextUtils.join(", ", addressComponentList).toLowerCase().contains(this.extraName.toLowerCase())) {
                addressComponentList.add(0, this.extraName);
            }
            // residential, suburb or  city district
            if (this.residential != null) {
                addressComponentList.add(this.residential);
            } else if (this.cityDistrict != null) {
                addressComponentList.add(this.cityDistrict);
            } else if (this.suburb != null) {
                addressComponentList.add(this.suburb);
            }
            // zip code, city and country
            if (this.zipCode != null) {
                addressComponentList.add(this.zipCode);
            }
            addressComponentList.add(this.city);
            if (this.country != null) {
                addressComponentList.add(this.country);
            }
            return TextUtils.join(", ", addressComponentList);
        } else if (this.displayName != null) {
            return this.displayName;
        }
        return super.getName();
    }


    /**
     * to json
     */

    // names
    public static final String KEY_DISPLAY_NAME = "display_name";
    public static final String KEY_EXTRA_NAME = "extra_name";
    // address components
    public static final String KEY_HOUSE_NUMBER = "house_number";
    public static final String KEY_ROAD = "road";
    public static final String KEY_RESENTIAL = "residential";
    public static final String KEY_SUBURB= "suburb";
    public static final String KEY_CITY_DISTRICT = "city_district";
    public static final String KEY_ZIP_CODE = "postcode";
    public static final String KEY_CITY = "city";
    public static final String KEY_STATE = "state";
    public static final String KEY_COUNTRY = "country";
    public static final String KEY_COUNTRY_CODE = "country_code";

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();

        // names
        if (this.displayName != null) {
            jsonObject.put(KEY_DISPLAY_NAME, this.displayName);
        }
        if (this.extraName != null) {
            jsonObject.put(KEY_EXTRA_NAME, this.extraName);
        }

        // address components
        if (this.houseNumber != null) {
            jsonObject.put(KEY_HOUSE_NUMBER, this.houseNumber);
        }
        if (this.road != null) {
            jsonObject.put(KEY_ROAD, this.road);
        }
        if (this.residential != null) {
            jsonObject.put(KEY_RESENTIAL, this.residential);
        }
        if (this.suburb != null) {
            jsonObject.put(KEY_SUBURB, this.suburb);
        }
        if (this.cityDistrict != null) {
            jsonObject.put(KEY_CITY_DISTRICT, this.cityDistrict);
        }
        if (this.zipCode != null) {
            jsonObject.put(KEY_ZIP_CODE, this.zipCode);
        }
        if (this.city != null) {
            jsonObject.put(KEY_CITY, this.city);
        }
        if (this.state != null) {
            jsonObject.put(KEY_STATE, this.state);
        }
        if (this.country != null) {
            jsonObject.put(KEY_COUNTRY, this.country);
        }
        if (this.countryCode != null) {
            jsonObject.put(KEY_COUNTRY_CODE, this.countryCode);
        }

        return jsonObject;
    }

}
