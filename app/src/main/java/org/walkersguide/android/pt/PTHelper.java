package org.walkersguide.android.pt;

import org.walkersguide.android.R;

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


public class PTHelper {

    /**
     * network provider
     */
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.108 Safari/537.36";

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

    public static String getNetworkProviderName(Context context, AbstractNetworkProvider provider) {
        if (provider != null) {
            switch (provider.id()) {
                // europe
                case RT:
                    return context.getResources().getString(R.string.publicTransportProviderRT);
                // germany
                case DB:
                    return context.getResources().getString(R.string.publicTransportProviderDB);
                case VVO:
                    return context.getResources().getString(R.string.publicTransportProviderVVO);
                // switzerland
                case VBL:
                    return context.getResources().getString(R.string.publicTransportProviderVBL);
                // default provider name
                default:
                    return provider.id().name();
            }
        } else {
            return "";
        }
    }


    /**
     * string formatting
     */

    public static String getLocationName(Location location) {
        if (! TextUtils.isEmpty(location.name)
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

    public static String vehicleTypesToString(Set<Product> products) {
        ArrayList<String> vehicleTypeNameList = new ArrayList<String>();
        if (products != null) {
            for (Product product : products) {
                vehicleTypeNameList.add(
                          product.name().substring(0, 1).toUpperCase()
                        + product.name().substring(1).toLowerCase());
            }
        }
        return TextUtils.join(", ", vehicleTypeNameList);
    }

    public static String formatAbsoluteDepartureTime(Context context, Date date, boolean shortOutput) {
        SimpleDateFormat hoursMinutesFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        if (shortOutput) {
            return hoursMinutesFormat.format(date);
        } else {
            return String.format(
                    context.getResources().getString(R.string.contentDescriptionAbsoluteDepartureTime),
                    hoursMinutesFormat.format(date));
        }
    }

    public static String formatRelativeDepartureTimeInMinutes(Context context, Date date, boolean shortOutput) {
        int departureInMinutes = (int) Math.ceil((date.getTime() - System.currentTimeMillis()) / 60000.0);
        if (shortOutput) {
            return context.getResources().getQuantityString(
                    R.plurals.minuteShort, departureInMinutes, departureInMinutes);
        } else {
            return String.format(
                    context.getResources().getString(R.string.contentDescriptionRelativeDepartureTime),
                    context.getResources().getQuantityString(
                        R.plurals.minute, departureInMinutes, departureInMinutes));
        }
    }


    /**
     * miscellaneous
     */

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
