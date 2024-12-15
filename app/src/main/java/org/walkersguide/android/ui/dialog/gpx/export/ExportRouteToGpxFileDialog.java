package org.walkersguide.android.ui.dialog.create.gpx.export;

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


public class ExportRouteToGpxFileDialog extends ExportGpxFileDialog {

    public static ExportRouteToGpxFileDialog newInstance(Route route) {
        ExportRouteToGpxFileDialog dialog = new ExportRouteToGpxFileDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ROUTE, route);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_ROUTE = "route";
    private Route route;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        route = (Route) getArguments().getSerializable(KEY_ROUTE);
        if (route == null) dismiss();
    }

    @Override public String getFileNameSuggestion() {
        return String.format(
                GlobalInstance.getStringResource(R.string.suggestedNameForRouteInGpxFile),
                route.getDestinationPoint().getName());
    }

    @Override public void createGpxFileInBackground(GpxFileWriter gpxFileWriter) throws IOException {
        gpxFileWriter.start();
        gpxFileWriter.addRoute(route);
        gpxFileWriter.finish();
    }

}
