package org.walkersguide.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.walkersguide.android.ui.dialog.create.EnterCoordinatesDialog;


public class DmsCoordinateConverterTest {
    private static final double EXPECTED_LATITUDE = 6.084534;
    private static final double EXPECTED_LONGITUDE = 123.748794;
    private static final double ACCURACY = 0.000001;

    @Test
    public void testValidDmsLatitudesFormat1() {
        for (String strLatitude : new String[]
                { "6°5′4.321″N", "6°5′4.321″n", "N6°5′4.321″" }) {
            testValidDmsLatitude(strLatitude, false);
        }
        for (String strLatitude : new String[]
                { "6°5′4.321″S", "6°5′4.321″s", "S6°5′4.321″" }) {
            testValidDmsLatitude(strLatitude, true);
        }
    }

    @Test
    public void testValidDmsLatitudesFormat2() {
        for (String strLatitude : new String[]
                { "6°5\"4.321'N", "6°5\"4.321'n", "N6°5\"4.321'" }) {
            testValidDmsLatitude(strLatitude, false);
        }
        for (String strLatitude : new String[]
                { "6°5\"4.321'S", "6°5\"4.321's", "S6°5\"4.321'" }) {
            testValidDmsLatitude(strLatitude, true);
        }
    }

    @Test
    public void testValidDmsLatitudesFormat3() {
        for (String strLatitude : new String[]
                { "6 5 4.321 N", "6 5 4.321 n", "N6 5 4.321" }) {
            testValidDmsLatitude(strLatitude, false);
        }
        for (String strLatitude : new String[]
                { "6 5 4.321 S", "6 5 4.321 s", "S6 5 4.321" }) {
            testValidDmsLatitude(strLatitude, true);
        }
    }

    private void testValidDmsLatitude(String strLatitude, boolean shouldBeNegative) {
        Double latitude = EnterCoordinatesDialog.convertStringLatitudeToDouble(strLatitude);
        assertNotNull(
                String.format("strLatitude %1$s should not be null", strLatitude), latitude);
        assertEquals(
                shouldBeNegative ? EXPECTED_LATITUDE * -1.0 : EXPECTED_LATITUDE, latitude, ACCURACY);
    }

    @Test
    public void testValidDmsLongitudesFormat1() {
        for (String strLongitude : new String[]
                { "123°44′55.66″E", "123°44′55.66″e", "E123°44′55.66″", "123°44′55.66″O" }) {
            testValidDmsLongitude(strLongitude, false);
        }
        for (String strLongitude : new String[]
                { "123°44′55.66″W", "123°44′55.66″w", "W123°44′55.66″" }) {
            testValidDmsLongitude(strLongitude, true);
        }
    }

    @Test
    public void testValidDmsLongitudesFormat2() {
        for (String strLongitude : new String[]
                { "123°44\"55.66'E", "123°44\"55.66'e", "E123°44\"55.66'", "123°44\"55.66'O" }) {
            testValidDmsLongitude(strLongitude, false);
        }
        for (String strLongitude : new String[]
                { "123°44\"55.66'W", "123°44\"55.66'w", "W123°44\"55.66'" }) {
            testValidDmsLongitude(strLongitude, true);
        }
    }

    @Test
    public void testValidDmsLongitudesFormat3() {
        for (String strLongitude : new String[]
                { "123 44 55.66 E", "123 44 55.66 e", "E123 44 55.66", "123 44 55.66 O" }) {
            testValidDmsLongitude(strLongitude, false);
        }
        for (String strLongitude : new String[]
                { "123 44 55.66 W", "123 44 55.66 w", "W123 44 55.66" }) {
            testValidDmsLongitude(strLongitude, true);
        }
    }

    private void testValidDmsLongitude(String strLongitude, boolean shouldBeNegative) {
        Double longitude = EnterCoordinatesDialog.convertStringLongitudeToDouble(strLongitude);
        assertNotNull(
                String.format("strLongitude %1$s should not be null", strLongitude), longitude);
        assertEquals(
                shouldBeNegative ? EXPECTED_LONGITUDE * -1.0 : EXPECTED_LONGITUDE, longitude, ACCURACY);
    }

    @Test
    public void testInvalidDmsLatitudes() {
        for (String strLatitude : new String[]
                { "6°5′4.321″E", "6°5′4.321″W", "E6°5′4.321″", "W6°5′4.321″",
                  "6°60\"59.99'N", "6°59\"61.99'N", "95°59\"59.99'N",
                  "6°1\"59.99", "6°1\"59.99'", "6.54321N", "6,54321n", "6°5′.321″n",
                  "6°5′4.″N", "6°5!4.321″n", "S6°5′4.321″ " }) {
            assertNull(
                    String.format("strLatitude %1$s should be null", strLatitude),
                    EnterCoordinatesDialog.convertStringLatitudeToDouble(strLatitude));
        }

        // longitudes
        for (String strLongitude : new String[]
                { "123°44′55.66″N", "123°44′55.66″S", "N123°44′55.66″", "S123°44′55.66″",
                  "123°60\"59.99'W", "123°59\"61.99'W", "189°59\"59.99'W",
                  "123°1\"59.99", "123°1\"59.99'", "°44′55.66″E", "44′55.66″E" }) {
            assertNull(
                    String.format("strLongitude %1$s should be null", strLongitude),
                    EnterCoordinatesDialog.convertStringLongitudeToDouble(strLongitude));
        }
    }

}
