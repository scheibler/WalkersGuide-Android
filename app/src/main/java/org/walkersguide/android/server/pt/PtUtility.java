package org.walkersguide.android.server.pt;

import de.schildbach.pte.DbProvider;
import org.walkersguide.android.server.pt.PtException;
import org.walkersguide.android.R;
import org.walkersguide.android.util.FileUtility;
import org.walkersguide.android.util.GlobalInstance;

import android.content.Context;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.text.TextUtils;

import de.schildbach.pte.AbstractNetworkProvider;
import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Stop;
import de.schildbach.pte.RtProvider;
import de.schildbach.pte.VblProvider;
import de.schildbach.pte.VvoProvider;

import java.lang.Math;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import com.google.common.base.Charsets;
import java.io.File;
import org.json.JSONObject;
import java.io.IOException;
import org.json.JSONException;
import de.schildbach.pte.NetworkId;


public class PtUtility {

    /**
     * network provider
     */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.108 Safari/537.36";

    public enum Country {
        EUROPE, GERMANY, SWITZERLAND
    }

    public static final Map<Country,ArrayList<AbstractNetworkProvider>> supportedNetworkProviderMap;
    static {
        // europe
        ArrayList<AbstractNetworkProvider> europeProviderList = new ArrayList<AbstractNetworkProvider>();
        // rt
        europeProviderList.add(
                new RtProvider());
        // germany
        ArrayList<AbstractNetworkProvider> germanyProviderList = new ArrayList<AbstractNetworkProvider>();
        // DB
        DbProviderCredentials dbProviderCredentials = getDbProviderCredentials();
        if (dbProviderCredentials != null) {
            germanyProviderList.add(
                    new DbProvider(
                        dbProviderCredentials.apiAuthorization,
                        dbProviderCredentials.salt.getBytes(Charsets.UTF_8)));
        }
        // vvo
        germanyProviderList.add(
                new VvoProvider());
        // switzerland
        ArrayList<AbstractNetworkProvider> switzerlandProviderList = new ArrayList<AbstractNetworkProvider>();
        // vbl
        switzerlandProviderList.add(
                new VblProvider());
        // create country, provider_list map
        Map<Country,ArrayList<AbstractNetworkProvider>> staticMap = new LinkedHashMap<Country,ArrayList<AbstractNetworkProvider>>();
        staticMap.put(Country.EUROPE, europeProviderList);
        staticMap.put(Country.GERMANY, germanyProviderList);
        staticMap.put(Country.SWITZERLAND, switzerlandProviderList);
        supportedNetworkProviderMap = Collections.unmodifiableMap(staticMap);
    }

    public static AbstractNetworkProvider findNetworkProvider(NetworkId id) {
        for (Map.Entry<Country,ArrayList<AbstractNetworkProvider>> entry : supportedNetworkProviderMap.entrySet()) {
            for (AbstractNetworkProvider provider : entry.getValue()) {
                if (id == provider.id()) {
                    provider.setUserAgent(USER_AGENT);
                    return provider;
                }
            }
        }
        return null;
    }

    public static String getNameForNetworkId(NetworkId id) {
        if (id != null) {
            switch (id) {
                // europe
                case RT:
                    return GlobalInstance.getStringResource(R.string.publicTransportProviderRT);
                // germany
                case DB:
                    return GlobalInstance.getStringResource(R.string.publicTransportProviderDB);
                case VVO:
                    return GlobalInstance.getStringResource(R.string.publicTransportProviderVVO);
                // switzerland
                case VBL:
                    return GlobalInstance.getStringResource(R.string.publicTransportProviderVBL);
                // default provider name
                default:
                    return id.name();
            }
        } else {
            return "";
        }
    }


    // credentials
    //
    // folder
    private static final String PT_PROVIDER_CREDENTIALS_FOLDER_NAME = "pt_provider_credentials";
    private static File getPtProviderCredentialsFolder() {
        return new File(
                GlobalInstance.getContext().getExternalFilesDir(null),
                PT_PROVIDER_CREDENTIALS_FOLDER_NAME);
    }

    // deutsche bahn
    private static final String DB_PROVIDER_API_CREDENTIALS_FILE_NAME = "db_provider_api_credentials.json";

    private static DbProviderCredentials getDbProviderCredentials() {
        File dbProviderApiCredentialsFile = new File(
                getPtProviderCredentialsFolder(),
                DB_PROVIDER_API_CREDENTIALS_FILE_NAME);
        if (dbProviderApiCredentialsFile.exists()) {
            JSONObject jsonDbProviderApiCredentials = null;
            try {
                jsonDbProviderApiCredentials = FileUtility.readJsonObjectFromTextFile(dbProviderApiCredentialsFile);
            } catch (IOException | JSONException e) {
                jsonDbProviderApiCredentials = null;
            } finally {
                if (jsonDbProviderApiCredentials != null
                        && ! jsonDbProviderApiCredentials.isNull("apiAuthorization")
                        && ! jsonDbProviderApiCredentials.isNull("salt")) {
                    return new DbProviderCredentials(
                            jsonDbProviderApiCredentials.optString("apiAuthorization"),
                            jsonDbProviderApiCredentials.optString("salt"));
                        }
            }
        }
        return null;
    }

    private static class DbProviderCredentials {
        public String apiAuthorization, salt;
        public DbProviderCredentials(String apiAuthorization, String salt) {
            this.apiAuthorization = apiAuthorization;
            this.salt = salt;
        }
    }


    /**
     * string formatting
     */

    public static String vehicleTypesToString(Context context, Set<Product> products) {
        if (products == null) {
            return "";
        } else {
            ArrayList<String> vehicleTypeNameList = new ArrayList<String>();
            for (Product product : products) {
                vehicleTypeNameList.add(getVehicleName(context, product));
            }
            return TextUtils.join(", ", vehicleTypeNameList);
        }
    }

    public static String getLocationName(Location location) {
        if (location == null) {
            return "";
        } else if (! TextUtils.isEmpty(location.name)
                && ! TextUtils.isEmpty(location.place)
                && ! location.name.equals(location.place)) {
            return String.format("%1$s, %2$s", location.name, location.place);
        } else if (! TextUtils.isEmpty(location.name)) {
            return location.name;
        } else if (! TextUtils.isEmpty(location.place)) {
            return location.place;
        } else {
            return "";
        }
    }

    public static String getVehicleName(Context context, Product product) {
        if (product == null) {
            return "";
        } else if (product.code == Product.HIGH_SPEED_TRAIN.code) {
            return context.getResources().getString(R.string.productHighSpeedTrain);
        } else if (product.code == Product.REGIONAL_TRAIN.code) {
            return context.getResources().getString(R.string.productRegionalTrain);
        } else if (product.code == Product.SUBURBAN_TRAIN.code) {
            return context.getResources().getString(R.string.productSuburbanTrain);
        } else if (product.code == Product.SUBWAY.code) {
            return context.getResources().getString(R.string.productSubway);
        } else if (product.code == Product.TRAM.code) {
            return context.getResources().getString(R.string.productTram);
        } else if (product.code == Product.BUS.code) {
            return context.getResources().getString(R.string.productBus);
        } else if (product.code == Product.FERRY.code) {
            return context.getResources().getString(R.string.productFerry);
        } else if (product.code == Product.CABLECAR.code) {
            return context.getResources().getString(R.string.productCableCar);
        } else if (product.code == Product.ON_DEMAND.code) {
            return context.getResources().getString(R.string.productOnDemand);
        } else {
            return product.name().substring(0, 1).toUpperCase(Locale.getDefault())
                + product.name().substring(1).toLowerCase(Locale.getDefault());
        }
    }

    public static String formatAbsoluteDepartureTime(Context context, Date date) {
        if (date == null) {
            return "";
        } else {
            SimpleDateFormat hoursMinutesFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return String.format(
                    context.getResources().getString(R.string.contentDescriptionAbsoluteDepartureTime),
                    hoursMinutesFormat.format(date));
        }
    }

    public static String formatRelativeDepartureTime(Context context, Date date, boolean contentDescription) {
        if (date == null) {
            return "";
        } else {
            ArrayList<String> relativeDepartureTimeList = new ArrayList<String>();
            // calculate hours and minutes
            int hours = (int) Math.floor((date.getTime() - System.currentTimeMillis()) / (60*60*1000.0));
            int minutes = ( (int) Math.ceil((date.getTime() - System.currentTimeMillis()) / (60*1000.0)) ) % 60;
            // special case for everything between -59 sec and 0 sec
            if (minutes == 0) {
                hours += 1;
            }
            // fill string formatter list
            //
            // add hours
            if (hours > 0) {
                if (contentDescription) {
                    relativeDepartureTimeList.add(
                            context.getResources().getQuantityString(
                                R.plurals.hourLong, hours, hours));
                } else {
                    relativeDepartureTimeList.add(
                            context.getResources().getQuantityString(
                                R.plurals.hourShort, hours, hours));
                }
            }
            // add minutes
            if (contentDescription) {
                relativeDepartureTimeList.add(
                        context.getResources().getQuantityString(
                            R.plurals.minuteLong, minutes, minutes));
            } else {
                relativeDepartureTimeList.add(
                        context.getResources().getQuantityString(
                            R.plurals.minuteShort, minutes, minutes));
            }
            // return formatted string
            return String.format(
                    context.getResources().getString(R.string.contentDescriptionRelativeDepartureTime),
                    TextUtils.join(" ", relativeDepartureTimeList));
        }
    }


    /**
     * miscellaneous
     */

    public static int distanceBetweenTwoPoints(Point pointA, Point pointB) {
        if (pointA != null && pointB != null) {
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                    pointA.getLatAsDouble(), pointA.getLonAsDouble(),
                    pointB.getLatAsDouble(), pointB.getLonAsDouble(), results);
            return Math.round(results[0]);
        }
        return 1000000000;
    }

    public static Date getDepartureTime(Departure departure) {
        if (departure != null) {
            if (departure.predictedTime != null) {
                return departure.predictedTime;
            } else if (departure.plannedTime != null) {
                return departure.plannedTime;
            }
        }
        return null;
    }

    public static Date getDepartureTime(Stop stop) {
        if (stop != null) {
            if (stop.predictedDepartureTime != null) {
                return stop.predictedDepartureTime;
            } else if (stop.plannedDepartureTime != null) {
                return stop.plannedDepartureTime;
            } else if (stop.predictedArrivalTime != null) {
                return stop.predictedArrivalTime;
            } else if (stop.plannedArrivalTime != null) {
                return stop.plannedArrivalTime;
            }
        }
        return null;
    }

}
