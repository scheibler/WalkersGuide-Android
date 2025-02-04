package org.walkersguide.android.ui.dialog.gpx.export;

import org.walkersguide.android.ui.dialog.gpx.ExportGpxFileDialog;
import org.walkersguide.android.util.gpx.GpxFileWriter;
import java.util.Locale;
import androidx.core.view.ViewCompat;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;



import org.walkersguide.android.R;


import android.content.Intent;
import android.net.Uri;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.walkersguide.android.util.GlobalInstance;
import java.io.IOException;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import org.walkersguide.android.data.object_with_id.Route;

import org.walkersguide.android.util.FileUtility;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.ObjectWithId;
import java.util.ArrayList;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.database.SortMethod;
import org.walkersguide.android.database.DatabaseProfile;


public class ExportDatabaseProfileToGpxFileDialog extends ExportGpxFileDialog {

    public static ExportDatabaseProfileToGpxFileDialog newInstance(DatabaseProfile databaseProfile) {
        ExportDatabaseProfileToGpxFileDialog dialog = new ExportDatabaseProfileToGpxFileDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_DATABASE_PROFILE, databaseProfile);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_DATABASE_PROFILE = "databaseProfile";
    private DatabaseProfile databaseProfile;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseProfile = (DatabaseProfile) getArguments().getSerializable(KEY_DATABASE_PROFILE);
        if (databaseProfile == null) dismiss();
    }

    @Override public String getFileNameSuggestion() {
        return databaseProfile.getName();
    }

    @Override public void createGpxFileInBackground(GpxFileWriter gpxFileWriter) throws IOException {
        gpxFileWriter.start();

        DatabaseProfileRequest databaseProfileRequest = new DatabaseProfileRequest(
                databaseProfile, null, SortMethod.NAME_ASC);
        ArrayList<ObjectWithId> objectList = AccessDatabase.getInstance().getObjectListFor(databaseProfileRequest);

        // first all points
        for (ObjectWithId objectWithId : objectList) {
            if (objectWithId instanceof Point) {
                gpxFileWriter.addPoint((Point) objectWithId);
            }
        }

        // then all routes
        for (ObjectWithId objectWithId : objectList) {
            if (objectWithId instanceof Route) {
                gpxFileWriter.addRoute((Route) objectWithId);
            }
        }

        gpxFileWriter.finish();
    }

}
