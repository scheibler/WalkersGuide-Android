package org.walkersguide.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.walkersguide.android.ui.dialog.create.EnterCoordinatesDialog;


public class DecimalCoordinateConverterTest {

    @Test
    public void testValidDecimalLatitudes() {
        for (String strLatitude : new String[]
                { "6.54321", "6,54321", "-6.54321", "-6,54321" }) {
            assertNotNull(
                    String.format("strLatitude %1$s should not be null", strLatitude),
                    EnterCoordinatesDialog.convertStringLatitudeToDouble(strLatitude));
        }
    }

    @Test
    public void testValidDecimalLongitudes() {
        for (String strLongitude : new String[]
                { "123.45678", "123,45678", "-123.45678", "-123,45678" }) {
            assertNotNull(
                    String.format("strLongitude %1$s should not be null", strLongitude),
                    EnterCoordinatesDialog.convertStringLongitudeToDouble(strLongitude));
        }
    }

}
