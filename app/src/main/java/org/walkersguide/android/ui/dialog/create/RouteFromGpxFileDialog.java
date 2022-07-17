package org.walkersguide.android.ui.dialog.create;

import org.walkersguide.android.ui.activity.toolbar.tabs.MainActivity;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.ui.view.EditTextAndClearInputButton;
import org.walkersguide.android.ui.UiHelper;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;


import android.view.inputmethod.EditorInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;


import org.walkersguide.android.R;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;

import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import android.content.Intent;
import android.net.Uri;
import timber.log.Timber;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import org.walkersguide.android.util.GlobalInstance;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.util.Xml;
import android.database.Cursor;
import android.provider.OpenableColumns;
import org.json.JSONException;
import org.walkersguide.android.data.object_with_id.point.GPS;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import org.walkersguide.android.data.object_with_id.Route;

import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;


public class RouteFromGpxFileDialog extends DialogFragment implements FragmentResultListener {


    // instance constructors

    public static RouteFromGpxFileDialog newInstance() {
        RouteFromGpxFileDialog dialog = new RouteFromGpxFileDialog();
        return dialog;
    }

    // dialog
    private static final String KEY_GPX_FILE_NAME = "gpxFileName";
    private static final String KEY_ROUTE = "route";

    private String gpxFileName;
    private Route route;

    private EditTextAndClearInputButton layoutRouteName;
    private TextView labelRouteDescription;

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SimpleMessageDialog.REQUEST_DIALOG_CLOSED, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SimpleMessageDialog.REQUEST_DIALOG_CLOSED)) {
            dismiss();
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            gpxFileName = savedInstanceState.getString(KEY_GPX_FILE_NAME);
            route = (Route) savedInstanceState.getSerializable(KEY_ROUTE);
        } else {
            gpxFileName = null;
            route = null;
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_route_from_gpx_file, nullParent);

        layoutRouteName = (EditTextAndClearInputButton) view.findViewById(R.id.layoutRouteName);
        layoutRouteName.setLabelText(getResources().getString(R.string.layoutRouteName));
        layoutRouteName.setEditorAction(
                EditorInfo.IME_ACTION_DONE,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        tryToImportRoute();
                    }
                });

        labelRouteDescription = (TextView) view.findViewById(R.id.labelRouteDescription);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.routeFromGpxFileDialogTitle))
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.dialogImport),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNegativeButton(
                    getResources().getString(R.string.dialogCancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .create();
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {

            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    tryToImportRoute();
                }
            });

            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                }
            });

            if (gpxFileName == null) {
                buttonPositive.setVisibility(View.GONE);
                layoutRouteName.setVisibility(View.GONE);
                labelRouteDescription.setVisibility(View.GONE);

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/*");
                startActivityForResult(intent, RC_SELECT_GPX_FILE);

            } else {
                updateUI();
            }
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (this.gpxFileName != null) {
            savedInstanceState.putString(KEY_GPX_FILE_NAME, this.gpxFileName);
        }
        if (this.route != null) {
            savedInstanceState.putSerializable(KEY_ROUTE, this.route);
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            UiHelper.hideKeyboard(this);
        }
    }

    private void updateUI() {
        labelRouteDescription.setText(route.getDescription());

        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) {
            dialog.setTitle(gpxFileName);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
            layoutRouteName.setVisibility(View.VISIBLE);
            labelRouteDescription.setVisibility(View.VISIBLE);
        }
    }

    private void tryToImportRoute() {
        if (route == null) {
            return;
        }
        final String routeName = layoutRouteName.getInputText();
        if (TextUtils.isEmpty(routeName)) {
            Toast.makeText(
                    getActivity(),
                    GlobalInstance.getStringResource(R.string.messageGpxRouteNameMissing),
                    Toast.LENGTH_LONG)
                .show();
            return;
        }
        route.rename(routeName);
        DatabaseProfile.routesFromGpxFile().add(route);
        MainActivity.loadRoute(
                RouteFromGpxFileDialog.this.getContext(), route);
        dismiss();
    }


    // select gpx file
    private static final int BUFFER_SIZE = 1024;
    private static final int RC_SELECT_GPX_FILE = 31;

    @Override public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode != RC_SELECT_GPX_FILE
                || resultCode != AppCompatActivity.RESULT_OK
                || resultData == null) {
            return;
        }

        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) {
            dialog.setTitle(
                    getResources().getString(R.string.messagePleaseWait));
        }

        final Uri uri = resultData.getData();
        Executors.newSingleThreadExecutor().execute(() -> {

            try {
                final String extractedFileName = extractFileNameFrom(uri);
                final Route routeFromGpxFile = parseGpxFile(uri, false);
                (new Handler(Looper.getMainLooper())).post(() -> {
                    if (isAdded()) {
                        layoutRouteName.setInputText(routeFromGpxFile.getName());
                        this.gpxFileName = extractedFileName;
                        this.route = routeFromGpxFile;
                        updateUI();
                    }
                });

            } catch (GpxFileParseException e) {
                final GpxFileParseException routeParseException = e;
                (new Handler(Looper.getMainLooper())).post(() -> {
                    if (isAdded()) {
                        this.gpxFileName = null;
                        this.route = null;
                        SimpleMessageDialog.newInstance(
                                routeParseException.getMessage())
                            .show(getChildFragmentManager(), "SimpleMessageDialog");
                    }
                });
            }
        });
    }

    private String extractFileNameFrom(Uri uri) throws GpxFileParseException {
        String fileName = null;
        Cursor returnCursor = null;
        try {
            if (uri != null) {
                returnCursor = GlobalInstance.getContext().getContentResolver().query(uri, null, null, null, null);
                if (returnCursor != null && returnCursor.moveToFirst()) {
                    try {
                        fileName = returnCursor.getString(
                                returnCursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                    } catch (IllegalArgumentException e) {}
                }
            }
        } catch (Exception e) {
            Timber.e("extractFileNameFrom: %1$s", e.getMessage());
        } finally {
            if (returnCursor != null) {
                returnCursor.close();
            }
        }
        if (TextUtils.isEmpty(fileName)) {
            throw new GpxFileParseException(
                    GlobalInstance.getStringResource(R.string.messageExtractGpxFileNameFailed));
        }
        return fileName;
    }

    private Route parseGpxFile(Uri uri, boolean filterRedundantPoints) throws GpxFileParseException {
        Route route = null;
        GpxFileParseException parseException = null;

        InputStream in = null;
        try {
            in = GlobalInstance.getContext().getContentResolver().openInputStream(uri);
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();

            String metadataName = null, trackName = null;
            ArrayList<GPS> pointList = new ArrayList<GPS>();
            parser.require(XmlPullParser.START_TAG, null, "gpx");
            while (parser.nextTag() == XmlPullParser.START_TAG) {

                if (parser.getName().equals("metadata")) {
                    parser.require(XmlPullParser.START_TAG, null, "metadata");
                    while (parser.nextTag() == XmlPullParser.START_TAG) {
                        if (parser.getName().equals("name")) {
                            parser.require(XmlPullParser.START_TAG, null, "name");
                            metadataName = parser.nextText();
                            parser.require(XmlPullParser.END_TAG, null, "name");
                        } else {
                            skipTag(parser);
                        }
                    }
                    parser.require(XmlPullParser.END_TAG, null, "metadata");

                } else if (parser.getName().equals("rte")) {
                    if (! pointList.isEmpty()) {
                        throw new GpxFileParseException(
                                GlobalInstance.getStringResource(R.string.messageMultipleGpxTracksFound));
                    }
                    parser.require(XmlPullParser.START_TAG, null, "rte");
                    while (parser.nextTag() == XmlPullParser.START_TAG) {
                        if (parser.getName().equals("name")) {
                            parser.require(XmlPullParser.START_TAG, null, "name");
                            trackName = parser.nextText();
                            parser.require(XmlPullParser.END_TAG, null, "name");
                        } else if (parser.getName().equals("rtept")) {
                            parser.require(XmlPullParser.START_TAG, null, "rtept");
                            pointList.add(
                                    parsePoint(parser, String.valueOf(pointList.size()+1)));
                            parser.require(XmlPullParser.END_TAG, null, "rtept");
                        } else {
                            skipTag(parser);
                        }
                    }
                    parser.require(XmlPullParser.END_TAG, null, "rte");

                } else if (parser.getName().equals("trk")) {
                    if (! pointList.isEmpty()) {
                        throw new GpxFileParseException(
                                GlobalInstance.getStringResource(R.string.messageMultipleGpxTracksFound));
                    }
                    parser.require(XmlPullParser.START_TAG, null, "trk");
                    while (parser.nextTag() == XmlPullParser.START_TAG) {
                        if (parser.getName().equals("name")) {
                            parser.require(XmlPullParser.START_TAG, null, "name");
                            trackName = parser.nextText();
                            parser.require(XmlPullParser.END_TAG, null, "name");
                        } else if (parser.getName().equals("trkseg")) {
                            parser.require(XmlPullParser.START_TAG, null, "trkseg");
                            while (parser.nextTag() == XmlPullParser.START_TAG) {
                                if (parser.getName().equals("trkpt")) {
                                    parser.require(XmlPullParser.START_TAG, null, "trkpt");
                                    pointList.add(
                                            parsePoint(parser, String.valueOf(pointList.size()+1)));
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

                } else {
                    skipTag(parser);
                }
            }
            parser.require(XmlPullParser.END_TAG, null, "gpx");

            // create route
            if (pointList.isEmpty()) {
                throw new GpxFileParseException(
                        GlobalInstance.getStringResource(R.string.messageNoGpxTracksFound));
            }
            try {
                route = Route.fromPointList(pointList, filterRedundantPoints);
            } catch (JSONException e) {
                throw new GpxFileParseException(
                            GlobalInstance.getStringResource(R.string.messageGpxRouteParsingFailed));
            }

            // overwrite route name
            if (! TextUtils.isEmpty(trackName)) {
                route.rename(trackName);
            } else if (! TextUtils.isEmpty(metadataName)) {
                route.rename(metadataName);
            }

        } catch (GpxFileParseException e) {
            Timber.e("GpxFileParseException: %1$s", e.getMessage());
            parseException = e;
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
        return route;
    }

    private GPS parsePoint(XmlPullParser parser, String defaultName)
            throws XmlPullParserException, IOException, GpxFileParseException {
        GPS.Builder gpsBuilder = new GPS.Builder(
                Double.valueOf(parser.getAttributeValue(null, "lat")),
                Double.valueOf(parser.getAttributeValue(null, "lon")));

        String name = defaultName;
        while (parser.nextTag() == XmlPullParser.START_TAG) {
            if (parser.getName().equals("name")) {
                parser.require(XmlPullParser.START_TAG, null, "name");
                name = parser.nextText();
                parser.require(XmlPullParser.END_TAG, null, "name");
            } else {
                skipTag(parser);
            }
        }
        gpsBuilder.setName(name);

        try {
            return gpsBuilder.build();
        } catch (JSONException e) {
            throw new GpxFileParseException(
                    GlobalInstance.getStringResource(R.string.messageGpxRoutePointParsingFailed));
        }
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


    private class GpxFileParseException extends Exception {
        public GpxFileParseException(String message) {
            super(message);
        }
    }

}
