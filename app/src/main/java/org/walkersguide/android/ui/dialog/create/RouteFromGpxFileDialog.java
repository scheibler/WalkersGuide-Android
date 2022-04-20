package org.walkersguide.android.ui.dialog.create;

import org.walkersguide.android.ui.activity.toolbar.tabs.MainActivity;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.ui.view.EditTextAndClearInputButton;
import org.walkersguide.android.ui.UiHelper;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;


import android.view.inputmethod.EditorInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;


import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.R;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;

import org.walkersguide.android.server.address.AddressException;
import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.widget.ListView;
import android.widget.AdapterView;
import androidx.appcompat.widget.SwitchCompat;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.sensor.PositionManager;

import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.address.ResolveAddressStringTask;
import org.walkersguide.android.server.address.AddressException;
import android.net.Uri;
import timber.log.Timber;
import android.widget.LinearLayout;
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


public class RouteFromGpxFileDialog extends DialogFragment {


    // instance constructors

    public static RouteFromGpxFileDialog newInstance() {
        RouteFromGpxFileDialog dialog = new RouteFromGpxFileDialog();
        return dialog;
    }

    // dialog
    private static final String KEY_GPX_FILE_URI = "gpxFileUri";
    private static final String KEY_ROUTE_POINT_LIST = "routePointList";

    private Uri gpxFileUri;
    private ArrayList<GPS> routePointList;

    private Button buttonGpxFile;
    private EditTextAndClearInputButton layoutRouteName;
    private SwitchCompat switchFilterRedundantPoints;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            String gpxFileUriString = savedInstanceState.getString(KEY_GPX_FILE_URI);
            if (! TextUtils.isEmpty(gpxFileUriString)) {
                gpxFileUri = Uri.parse(gpxFileUriString);
            }
            routePointList = (ArrayList<GPS>) savedInstanceState.getSerializable(KEY_ROUTE_POINT_LIST);
        } else {
            gpxFileUri = null;
            routePointList = null;
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_route_from_gpx_file, nullParent);

        buttonGpxFile = (Button) view.findViewById(R.id.buttonGpxFile);
		buttonGpxFile.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
                selectGpxFile();
            }
        });

        layoutRouteName = (EditTextAndClearInputButton) view.findViewById(R.id.layoutRouteName);
        layoutRouteName.setLabelText(getResources().getString(R.string.layoutRouteName));
        layoutRouteName.setEditorAction(
                EditorInfo.IME_ACTION_DONE,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        tryToImportRoute();
                    }
                });

        switchFilterRedundantPoints = (SwitchCompat) view.findViewById(R.id.switchFilterRedundantPoints);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.routeFromGpxFileDialogTitle))
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.dialogOK),
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

            buttonGpxFile.setText(
                    getResources().getString(R.string.buttonGpxFile));
            layoutRouteName.setVisibility(View.GONE);
            switchFilterRedundantPoints.setVisibility(View.GONE);
            buttonPositive.setVisibility(View.GONE);

            if (gpxFileUri != null) {
                if (routePointList != null) {
                    gpxFileParsingSuccessful(routePointList);
                } else {
                    parseGpxFile(gpxFileUri);
                }
            }
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (this.gpxFileUri != null) {
            savedInstanceState.putString(KEY_GPX_FILE_URI, this.gpxFileUri.toString());
        }
        if (this.routePointList != null) {
            savedInstanceState.putSerializable(KEY_ROUTE_POINT_LIST, this.routePointList);
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            UiHelper.hideKeyboard(this);
        }
    }

    private void tryToImportRoute() {
        Route route = null;
        try {
            route = Route.fromPointList(
                    this.routePointList, ! switchFilterRedundantPoints.isChecked());
        } catch (JSONException e) {
            Timber.e("Route creation error: %1$s", e.getMessage());
        }
        if (route != null) {
            final String routeName = layoutRouteName.getInputText();
            if (! TextUtils.isEmpty(routeName)) {
                route.rename(routeName);
            }
            DatabaseProfile.routesFromGpxFile().add(route);
            MainActivity.loadRoute(
                    RouteFromGpxFileDialog.this.getContext(), route);
            dismiss();
        }
    }


    // select gpx file
    private static final int BUFFER_SIZE = 1024;
    private static final int RC_SELECT_GPX_FILE = 31;

    private void selectGpxFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/*");
        startActivityForResult(intent, RC_SELECT_GPX_FILE);
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode != RC_SELECT_GPX_FILE || resultCode != AppCompatActivity.RESULT_OK) {
            return;
        }
        parseGpxFile(resultData.getData());
    }

    private void parseGpxFile(final Uri uri) {
        this.gpxFileUri = uri;
        buttonGpxFile.setText(
                getResources().getString(R.string.messagePleaseWait));
        Executors.newSingleThreadExecutor().execute(() -> {

            InputStream in = null;
            ArrayList<GPS> pointList = new ArrayList<GPS>();
            boolean multipleTracksFound = false;
            String metadataName = null, trackName = null, error = null;
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
                                metadataName = parser.nextText();
                                parser.require(XmlPullParser.END_TAG, null, "name");
                            } else {
                                skipTag(parser);
                            }
                        }
                        parser.require(XmlPullParser.END_TAG, null, "metadata");

                    } else if (parser.getName().equals("rte")) {
                        if (! pointList.isEmpty()) {
                            multipleTracksFound = true;
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
                            multipleTracksFound = true;
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

                if (multipleTracksFound) {
                    error = getResources().getString(R.string.messageMultipleTracksFound);
                } else if (pointList.isEmpty()) {
                    error = getResources().getString(R.string.messageNoTracksFound);
                }
            } catch (IOException e) {
                Timber.e("IOException: %1$s", e.getMessage());
                error = GlobalInstance.getStringResource(R.string.messageOpenGpxFileFailed);
            } catch (XmlPullParserException e) {
                Timber.e("XmlPullParserException: %1$s", e.getMessage());
                error = GlobalInstance.getStringResource(R.string.messageInvalidGpxFileContents);
            } catch (JSONException e) {
                Timber.e("JSONException: %1$s", e.getMessage());
                error = GlobalInstance.getStringResource(R.string.messageRoutePointParsingFailed);

            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {}
                }

                if (error != null) {
                    final String finalError = error;
                    (new Handler(Looper.getMainLooper())).post(() -> {
                        if (isAdded()) {
                            gpxFileParsingFailed(finalError);
                        }
                    });
                    return;
                }
            }

            final String finalRouteName = ! TextUtils.isEmpty(trackName) ? trackName : metadataName;
            final ArrayList<GPS> finalPointList = pointList;
            (new Handler(Looper.getMainLooper())).post(() -> {
                if (isAdded()) {
                    layoutRouteName.setInputText(finalRouteName);
                    gpxFileParsingSuccessful(finalPointList);
                }
            });
        });
    }

    private void gpxFileParsingSuccessful(ArrayList<GPS> pointList) {
        this.routePointList = pointList;

        String gpxFileName = "";
        Cursor returnCursor = null;
        try {
            if (gpxFileUri != null) {
                returnCursor = GlobalInstance.getContext().getContentResolver().query(gpxFileUri, null, null, null, null);
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (returnCursor != null && returnCursor.moveToFirst()) {
                    gpxFileName = returnCursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            Timber.e("cursor error: %1$s", e.getMessage());
        } finally {
            if (returnCursor != null) {
                returnCursor.close();
            }
        }
        buttonGpxFile.setText(gpxFileName);

        layoutRouteName.setVisibility(View.VISIBLE);
        switchFilterRedundantPoints.setVisibility(View.VISIBLE);
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setVisibility(View.VISIBLE);
        }
    }

    private void gpxFileParsingFailed(String error) {
        this.routePointList = null;
        buttonGpxFile.setText(error);
    }

    private GPS parsePoint(XmlPullParser parser, String defaultName)
            throws XmlPullParserException, IOException, JSONException {
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

}
