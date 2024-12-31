package org.walkersguide.android.util.gpx;

import org.walkersguide.android.R;
import android.util.Xml;
import org.xmlpull.v1.XmlSerializer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.os.ParcelFileDescriptor;
import android.content.ContentResolver;
import org.walkersguide.android.util.GlobalInstance;
import android.net.Uri;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.route.RouteObject;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.data.object_with_id.point.GPS;
import java.util.Locale;


public class GpxFileWriter {

    private Uri uri;
    private String collectionName;

    private ParcelFileDescriptor destinationFileDescriptor;
    private FileOutputStream destinationFileOutputStream;
    private XmlSerializer serializer;

    public GpxFileWriter(Uri uri, String collectionName) {
        this.uri = uri;
        this.collectionName = collectionName;
    }

    public void start() throws IOException {
        ContentResolver resolver = GlobalInstance.getContext().getContentResolver();
        destinationFileDescriptor = resolver.openFileDescriptor(this.uri, "w");
        destinationFileOutputStream = new FileOutputStream(destinationFileDescriptor.getFileDescriptor());
        serializer = Xml.newSerializer();
        serializer.setOutput(destinationFileOutputStream, "UTF-8");
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

        // Start gpx tag
        serializer.startDocument("UTF-8", true);
        serializer.startTag(null, "gpx");
        serializer.attribute(null, "version", "1.1");
        serializer.attribute(null, "creator", GlobalInstance.getStringResource(R.string.app_name));
        serializer.attribute("", "xmlns", "http://www.topografix.com/GPX/1/1");

        // <metadata>
        serializer.startTag(null, "metadata");
        addName(this.collectionName);
        serializer.endTag(null, "metadata");
    }

    public void addPoint(Point point) throws IOException {
        addPoint(point, "wpt", true);
    }

    public void addRoute(Route route) throws IOException {
        serializer.startTag(null, "trk");
        addName(route.getName());
        serializer.startTag(null, "trkseg");
        for (RouteObject routeObject : route.getRouteObjectList()) {
            addPoint(routeObject.getPoint(), "trkpt", routeObject.getIsImportant());
        }
        serializer.endTag(null, "trkseg");
        serializer.endTag(null, "trk");
    }

    public void finish() throws IOException {
        serializer.endTag(null, "gpx");
        serializer.endDocument();
        serializer.flush();
        closeFile();
    }

    public void cleanupOnFailure() {
        closeFile();
        File gpxFile = new File(this.uri.getPath());
        if (gpxFile != null && gpxFile.exists()) {
            gpxFile.delete();
        }
    }

    private void addPoint(Point point, String tagName, boolean isImportant) throws IOException {
        serializer.startTag(null, tagName);
        serializer.attribute(
                null, "lat", String.valueOf(point.getCoordinates().getLatitude()));
        serializer.attribute(
                null, "lon", String.valueOf(point.getCoordinates().getLongitude()));

        if (point instanceof GPS) {
            GPS gps = (GPS) point;
            // altitude
            Double altitude = gps.getAltitude();
            if (altitude != null) {
                serializer.startTag(null, "ele");
                serializer.text(String.format(Locale.ROOT, "%1$.2f", altitude));
                serializer.endTag(null, "ele");
            }
            // timestamp must come after ele
            addTime(gps.getTimestamp());
        }

        if (isImportant) {
            addName(point.getName());
        }

        serializer.endTag(null, tagName);
    }

    private void addName(String name) throws IOException {
        serializer.startTag(null, "name");
        serializer.text(name.replace("\n", ". "));
        serializer.endTag(null, "name");
    }

    private void addTime(long timestamp) throws IOException {
        serializer.startTag(null, "time");
        serializer.text(Helper.formatTimestampInIso8601Format(timestamp));
        serializer.endTag(null, "time");
    }

    private void closeFile() {
        // close destination file
        if (destinationFileOutputStream != null) {
            try {
                destinationFileOutputStream.flush();
                destinationFileOutputStream.close();
            } catch (IOException e) {}
        }
        if (destinationFileDescriptor != null) {
            try {
                destinationFileDescriptor.close();
            } catch (IOException e) {}
        }
    }

}
