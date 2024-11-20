package org.walkersguide.android.util.gpx;

import java.util.Locale;







import java.util.ArrayList;


import org.walkersguide.android.R;

import android.text.TextUtils;

import android.net.Uri;
import timber.log.Timber;
import java.io.InputStream;
import org.walkersguide.android.util.GlobalInstance;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.util.Xml;
import org.json.JSONException;
import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.data.object_with_id.Route;

import org.walkersguide.android.data.ObjectWithId;
import java.util.Date;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.util.FileUtility;


public class GpxFileReader {

    private Uri uri;

    public GpxFileReader(Uri uri) {
        this.uri = uri;
    }

    public GpxFileParseResult read() throws GpxFileParseException {
        GpxFileParseResult result = new GpxFileParseResult(
                FileUtility.extractFileNameFrom(this.uri));
        int routeIndex = 0;
        GpxFileParseException parseException = null;

        InputStream in = null;
        try {
            in = GlobalInstance.getContext().getContentResolver().openInputStream(uri);
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();

            parser.require(XmlPullParser.START_TAG, null, "gpx");
            while (parser.nextTag() == XmlPullParser.START_TAG) {

                if (parser.getName().equals("metadata")) {
                    parser.require(XmlPullParser.START_TAG, null, "metadata");
                    while (parser.nextTag() == XmlPullParser.START_TAG) {
                        if (parser.getName().equals("name")) {
                            parser.require(XmlPullParser.START_TAG, null, "name");
                            result.collectionName = parser.nextText();
                            parser.require(XmlPullParser.END_TAG, null, "name");
                        } else {
                            skipTag(parser);
                        }
                    }
                    parser.require(XmlPullParser.END_TAG, null, "metadata");

                } else if (parser.getName().equals("wpt")) {
                    parser.require(XmlPullParser.START_TAG, null, "wpt");
                    try {
                        result.objectList.add(parsePoint(parser, null));
                    } catch (JSONException e) {}
                    parser.require(XmlPullParser.END_TAG, null, "wpt");

                } else if (parser.getName().equals("rte")) {
                    Route route = null;
                    try {
                        String routeName = null, routeDescription = null;
                        ArrayList<GPS> routePointList = new ArrayList<GPS>();

                        parser.require(XmlPullParser.START_TAG, null, "rte");
                        while (parser.nextTag() == XmlPullParser.START_TAG) {
                            if (parser.getName().equals("name")) {
                                parser.require(XmlPullParser.START_TAG, null, "name");
                                routeName = parser.nextText();
                                parser.require(XmlPullParser.END_TAG, null, "name");
                            } else if (parser.getName().equals("desc")) {
                                parser.require(XmlPullParser.START_TAG, null, "desc");
                                routeDescription = parser.nextText();
                                parser.require(XmlPullParser.END_TAG, null, "desc");
                            } else if (parser.getName().equals("rtept")) {
                                parser.require(XmlPullParser.START_TAG, null, "rtept");
                                routePointList.add(
                                        parsePoint(
                                            parser, String.valueOf(routePointList.size()+1)));
                                parser.require(XmlPullParser.END_TAG, null, "rtept");
                            } else {
                                skipTag(parser);
                            }
                        }
                        parser.require(XmlPullParser.END_TAG, null, "rte");

                        route = createRoute(routeIndex, routeName, routeDescription, routePointList, false);
                    } catch (JSONException e) {}
                    if (route != null) {
                        result.objectList.add(route);
                        routeIndex += 1;
                    }

                } else if (parser.getName().equals("trk")) {
                    Route route = null;
                    try {
                        String routeName = null, routeDescription = null;
                        ArrayList<GPS> routePointList = new ArrayList<GPS>();

                        parser.require(XmlPullParser.START_TAG, null, "trk");
                        while (parser.nextTag() == XmlPullParser.START_TAG) {
                            if (parser.getName().equals("name")) {
                                parser.require(XmlPullParser.START_TAG, null, "name");
                                routeName = parser.nextText();
                                parser.require(XmlPullParser.END_TAG, null, "name");
                            } else if (parser.getName().equals("desc")) {
                                parser.require(XmlPullParser.START_TAG, null, "desc");
                                routeDescription = parser.nextText();
                                parser.require(XmlPullParser.END_TAG, null, "desc");
                            } else if (parser.getName().equals("trkseg")) {
                                parser.require(XmlPullParser.START_TAG, null, "trkseg");
                                while (parser.nextTag() == XmlPullParser.START_TAG) {
                                    if (parser.getName().equals("trkpt")) {
                                        parser.require(XmlPullParser.START_TAG, null, "trkpt");
                                        routePointList.add(
                                                parsePoint(
                                                    parser, String.valueOf(routePointList.size()+1)));
                                        parser.require(XmlPullParser.END_TAG, null, "trkpt");
                                    } else {
                                        skipTag(parser);
                                    }
                                }
                                parser.require(XmlPullParser.END_TAG, null, "trkseg");
                            } else {
                                skipTag(parser);
                            }
                        }
                        parser.require(XmlPullParser.END_TAG, null, "trk");

                        route = createRoute(routeIndex, routeName, routeDescription, routePointList, true);
                    } catch (JSONException e) {}
                    if (route != null) {
                        result.objectList.add(route);
                        routeIndex += 1;
                    }

                } else {
                    skipTag(parser);
                }
            }
            parser.require(XmlPullParser.END_TAG, null, "gpx");

        } catch (IOException e) {
            Timber.e("IOException: %1$s", e.getMessage());
            parseException = new GpxFileParseException(
                    GlobalInstance.getStringResource(R.string.messageOpenGpxFileFailed));
        } catch (XmlPullParserException e) {
            Timber.e("XmlPullParserException: %1$s", e.getMessage());
            parseException = new GpxFileParseException(
                    GlobalInstance.getStringResource(R.string.messageInvalidGpxFileContents));
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
        }

        if (parseException != null) {
            throw parseException;
        }
        return result;
    }

    private GPS parsePoint(XmlPullParser parser, String defaultName)
            throws XmlPullParserException, IOException, JSONException {
        GPS.Builder gpsBuilder = new GPS.Builder(
                Double.valueOf(parser.getAttributeValue(null, "lat")),
                Double.valueOf(parser.getAttributeValue(null, "lon")));
        if (! TextUtils.isEmpty(defaultName)) {
            gpsBuilder.setName(defaultName);
        }

        // parse optional attributes
        while (parser.nextTag() == XmlPullParser.START_TAG) {

            if (parser.getName().equals("ele")) {
                parser.require(XmlPullParser.START_TAG, null, "ele");
                Double altitude = null;
                try {
                    altitude = Double.valueOf(
                            parser.nextText());
                } catch (NumberFormatException | NullPointerException e) {}
                if (altitude != null) {
                    gpsBuilder.setAltitude(altitude);
                }
                parser.require(XmlPullParser.END_TAG, null, "ele");

            } else if (parser.getName().equals("name")) {
                parser.require(XmlPullParser.START_TAG, null, "name");
                String name = parser.nextText();
                if (! TextUtils.isEmpty(name)) {
                    gpsBuilder.setName(name);
                }
                parser.require(XmlPullParser.END_TAG, null, "name");

            } else if (parser.getName().equals("desc")) {
                parser.require(XmlPullParser.START_TAG, null, "desc");
                String description = parser.nextText();
                if (! TextUtils.isEmpty(description)) {
                    gpsBuilder.setDescription(description);
                }
                parser.require(XmlPullParser.END_TAG, null, "desc");

            } else if (parser.getName().equals("time")) {
                parser.require(XmlPullParser.START_TAG, null, "time");
                Date createdDate = Helper.parseTimestampInIso8601Format(parser.nextText());
                if (createdDate != null) {
                    gpsBuilder.setTime(createdDate.getTime());
                }
                parser.require(XmlPullParser.END_TAG, null, "time");

            } else {
                skipTag(parser);
            }
        }

        return gpsBuilder.build();
    }

    private void skipTag(XmlPullParser parser) throws XmlPullParserException, IOException {
        int depth = 1;
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
            }
        }
    }

    private static Route createRoute(int routeIndex, String routeName, String routeDescription,
            ArrayList<GPS> routePointList, boolean isTrack) throws JSONException {
        return Route.fromPointList(
                Route.Type.GPX_TRACK,
                TextUtils.isEmpty(routeName)
                ? String.format(
                    Locale.getDefault(), "%1$s %2$d", ObjectWithId.Icon.ROUTE, routeIndex+1)
                : routeName,
                routeDescription,
                false,
                isTrack
                ? Helper.filterPointListByTurnValueAndImportantIntersections(routePointList, true)
                : routePointList);
    }


    public class GpxFileParseResult {
        public ArrayList<ObjectWithId> objectList;
        public String collectionName;
        public String gpxFileName;

        public GpxFileParseResult(String fileName) throws GpxFileParseException {
            if (fileName == null) {
                throw new GpxFileParseException(
                        GlobalInstance.getStringResource(R.string.messageExtractGpxFileNameFailed));
            }
            this.objectList = new ArrayList<ObjectWithId>();
            this.collectionName = null;
            this.gpxFileName = fileName;
        }

        public String getCollectionName() {
            return this.collectionName != null
                ? this.collectionName
                : this.gpxFileName.replaceAll("(?i)\\.gpx$", "");
        }
    }


    public class GpxFileParseException extends Exception {
        public GpxFileParseException(String message) {
            super(message);
        }
    }

}
