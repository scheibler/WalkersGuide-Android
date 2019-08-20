package org.walkersguide.android.data.basic.point;

import android.content.Context;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;


public abstract class PointWithAddressData extends Point {

    private String displayName, extraName;
    private String houseNumber, road, residential, suburb, cityDistrict, postcode, city, state, country, countryCode;

    public PointWithAddressData(Context context, JSONObject inputData) throws JSONException {
        // point super constructor
        super(context, inputData);
        // names
        this.displayName = "";
        try {
            this.displayName = inputData.getString("display_name");
        } catch (JSONException e) {}
        this.extraName = "";
        try {
            this.extraName = inputData.getString("extra_name");
        } catch (JSONException e) {}
        // address components
        this.houseNumber = "";
        try {
            this.houseNumber = inputData.getString("house_number");
        } catch (JSONException e) {}
        this.road = "";
        try {
            this.road = inputData.getString("road");
        } catch (JSONException e) {}
        this.residential = "";
        try {
            this.residential = inputData.getString("residential");
        } catch (JSONException e) {}
        this.suburb = "";
        try {
            this.suburb = inputData.getString("suburb");
        } catch (JSONException e) {}
        this.cityDistrict = "";
        try {
            this.cityDistrict = inputData.getString("city_district");
        } catch (JSONException e) {}
        this.postcode = "";
        try {
            this.postcode = inputData.getString("postcode");
        } catch (JSONException e) {}
        this.city = "";
        try {
            this.city = inputData.getString("city");
        } catch (JSONException e) {}
        this.state = "";
        try {
            this.state = inputData.getString("state");
        } catch (JSONException e) {}
        this.country = "";
        try {
            this.country = inputData.getString("country");
        } catch (JSONException e) {}
        this.countryCode = "";
        try {
            this.countryCode = inputData.getString("country_code");
        } catch (JSONException e) {}
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

    public String getPostcode() {
        return this.postcode;
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

    public String createPrintableAddress() {
        if (! TextUtils.isEmpty(this.road)
                && ! TextUtils.isEmpty(this.city)) {
            ArrayList<String> addressComponentList = new ArrayList<String>();
            // street and house number
            if (! TextUtils.isEmpty(this.road)
                    && ! TextUtils.isEmpty(this.houseNumber)) {
                if (Locale.getDefault().getLanguage().equals(Locale.GERMAN.getLanguage())) {
                    addressComponentList.add(
                            String.format("%1$s %2$s", this.road, this.houseNumber));
                } else {
                    addressComponentList.add(this.houseNumber);
                    addressComponentList.add(this.road);
                }
            } else if (! TextUtils.isEmpty(this.road)) {
                addressComponentList.add(this.road);
            }
            if (! TextUtils.isEmpty(this.extraName)
                    && ! TextUtils.join(", ", addressComponentList).toLowerCase().contains(this.extraName.toLowerCase())) {
                addressComponentList.add(0, this.extraName);
            }
            // residential, suburb or  city district
            if (! TextUtils.isEmpty(this.residential)) {
                addressComponentList.add(this.residential);
            } else if (! TextUtils.isEmpty(this.cityDistrict)) {
                addressComponentList.add(this.cityDistrict);
            } else if (! TextUtils.isEmpty(this.suburb)) {
                addressComponentList.add(this.suburb);
            }
            // postcode, city and country
            if (! TextUtils.isEmpty(this.postcode)) {
                addressComponentList.add(this.postcode);
            }
            if (! TextUtils.isEmpty(this.city)) {
                addressComponentList.add(this.city);
            }
            return TextUtils.join(", ", addressComponentList);
        } else if (! TextUtils.isEmpty(this.displayName)) {
            return this.displayName;
        }
        return super.getName();
    }

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        // names
        if (! TextUtils.isEmpty(this.displayName)) {
            try {
                jsonObject.put("display_name", this.displayName);
            } catch (JSONException e) {}
        }
        if (! TextUtils.isEmpty(this.extraName)) {
            try {
                jsonObject.put("extra_name", this.extraName);
            } catch (JSONException e) {}
        }
        // address components
        if (! TextUtils.isEmpty(this.houseNumber)) {
            try {
                jsonObject.put("house_number", this.houseNumber);
            } catch (JSONException e) {}
        }
        if (! TextUtils.isEmpty(this.road)) {
            try {
                jsonObject.put("road", this.road);
            } catch (JSONException e) {}
        }
        if (! TextUtils.isEmpty(this.residential)) {
            try {
                jsonObject.put("residential", this.residential);
            } catch (JSONException e) {}
        }
        if (! TextUtils.isEmpty(this.suburb)) {
            try {
                jsonObject.put("suburb", this.suburb);
            } catch (JSONException e) {}
        }
        if (! TextUtils.isEmpty(this.cityDistrict)) {
            try {
                jsonObject.put("city_district", this.cityDistrict);
            } catch (JSONException e) {}
        }
        if (! TextUtils.isEmpty(this.postcode)) {
            try {
                jsonObject.put("postcode", this.postcode);
            } catch (JSONException e) {}
        }
        if (! TextUtils.isEmpty(this.city)) {
            try {
                jsonObject.put("city", this.city);
            } catch (JSONException e) {}
        }
        if (! TextUtils.isEmpty(this.state)) {
            try {
                jsonObject.put("state", this.state);
            } catch (JSONException e) {}
        }
        if (! TextUtils.isEmpty(this.country)) {
            try {
                jsonObject.put("country", this.country);
            } catch (JSONException e) {}
        }
        if (! TextUtils.isEmpty(this.countryCode)) {
            try {
                jsonObject.put("country_code", this.countryCode);
            } catch (JSONException e) {}
        }
        return jsonObject;
    }

}
