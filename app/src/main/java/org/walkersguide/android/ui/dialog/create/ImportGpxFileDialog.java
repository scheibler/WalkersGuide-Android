package org.walkersguide.android.ui.dialog.create;

import java.util.Locale;
import androidx.core.view.ViewCompat;
import org.walkersguide.android.ui.dialog.select.SelectProfileFromMultipleSourcesDialog;
import org.walkersguide.android.database.profile.Collection;
import android.widget.CompoundButton.OnCheckedChangeListener;
import org.walkersguide.android.ui.view.ProfileView;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.ui.view.EditTextAndClearInputButton;
import org.walkersguide.android.ui.UiHelper;
import androidx.appcompat.app.AlertDialog;
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
import org.walkersguide.android.data.ObjectWithId;
import android.widget.RadioButton;
import org.walkersguide.android.data.Profile;
import android.widget.CompoundButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.walkersguide.android.util.Helper;
import androidx.core.util.Pair;


public class ImportGpxFileDialog extends DialogFragment implements FragmentResultListener {
    public static final String REQUEST_IMPORT_OF_GPX_FILE_WAS_SUCCESSFUL = "requestImportOfGpxFileWasSuccessful";
    public static final String EXTRA_GPX_FILE_PROFILE = "gpxFileProfile";


    // instance constructors

    public static ImportGpxFileDialog newInstance(boolean isPinned) {
        ImportGpxFileDialog dialog = new ImportGpxFileDialog();
        Bundle args = new Bundle();
        args.putBoolean(KEY_IS_PINNED, isPinned);
        dialog.setArguments(args);
        return dialog;
    }

    // dialog
    private static final String KEY_IS_PINNED = "isPinned";
    private static final String KEY_FILE_PICKING_IN_PROGRESS = "filePickingInProgress";
    private static final String KEY_GPX_FILE_NAME = "gpxFileName";
    private static final String KEY_OBJECT_LIST = "objectList";
    private static final String KEY_RADIO_BUTTON_NEW_COLLECTION_IS_CHECKED = "radioButtonNewCollectionIsChecked";
    private static final String KEY_SELECTED_EXISTING_DATABASE_PROFILE = "selectedExistingDatabaseProfile";

    private boolean isPinned, filePickingInProgress;
    private String gpxFileName;
    private ArrayList<ObjectWithId> objectList;
    private boolean radioButtonNewCollectionIsChecked;

    private TextView labelImportResult;
    // new collection
    private RadioButton radioButtonNewCollection;
    private EditTextAndClearInputButton layoutNewCollectionName;
    // add to existing DatabaseProfile
    private RadioButton radioButtonExistingDatabaseProfile;
    private ProfileView layoutExistingDatabaseProfile;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectProfileFromMultipleSourcesDialog.REQUEST_SELECT_PROFILE, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SimpleMessageDialog.REQUEST_DIALOG_CLOSED, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectProfileFromMultipleSourcesDialog.REQUEST_SELECT_PROFILE)) {
            SelectProfileFromMultipleSourcesDialog.Target profileTarget = (SelectProfileFromMultipleSourcesDialog.Target)
                bundle.getSerializable(SelectProfileFromMultipleSourcesDialog.EXTRA_TARGET);
            Profile selectedProfile = (Profile) bundle.getSerializable(SelectProfileFromMultipleSourcesDialog.EXTRA_PROFILE);
            if (profileTarget == SelectProfileFromMultipleSourcesDialog.Target.GPX_FILE_IMPORT
                    && selectedProfile instanceof DatabaseProfile) {
                setSelectedExistingDatabaseProfile((DatabaseProfile) selectedProfile);
                updateUI();
            }
        } else if (requestKey.equals(SimpleMessageDialog.REQUEST_DIALOG_CLOSED)) {
            dismiss();
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        isPinned = getArguments().getBoolean(KEY_IS_PINNED);

        if (savedInstanceState != null) {
            filePickingInProgress = savedInstanceState.getBoolean(KEY_FILE_PICKING_IN_PROGRESS);
            gpxFileName = savedInstanceState.getString(KEY_GPX_FILE_NAME);
            objectList = (ArrayList<ObjectWithId>) savedInstanceState.getSerializable(KEY_OBJECT_LIST);
            radioButtonNewCollectionIsChecked = savedInstanceState.getBoolean(KEY_RADIO_BUTTON_NEW_COLLECTION_IS_CHECKED);
        } else {
            filePickingInProgress = false;
            gpxFileName = null;
            objectList = null;
            radioButtonNewCollectionIsChecked = true;
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_import_gpx_file, nullParent);

        labelImportResult = (TextView) view.findViewById(R.id.labelImportResult);

        // new collection
        radioButtonNewCollection = (RadioButton) view.findViewById(R.id.radioButtonNewCollection);
        radioButtonNewCollection.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    layoutNewCollectionName.setVisibility(View.VISIBLE);
                    // uncheck existing profile radio button
                    radioButtonExistingDatabaseProfile.setChecked(false);
                } else {
                    layoutNewCollectionName.setVisibility(View.GONE);
                }
                radioButtonNewCollectionIsChecked = isChecked;
            }
        });

        layoutNewCollectionName = (EditTextAndClearInputButton) view.findViewById(R.id.layoutNewCollectionName);
        layoutNewCollectionName.setLabelText(
                getResources().getString(R.string.layoutCollectionName));
        layoutNewCollectionName.setEditorAction(
                EditorInfo.IME_ACTION_DONE,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        execute();
                    }
                });

        // existing database profile
        radioButtonExistingDatabaseProfile = (RadioButton) view.findViewById(R.id.radioButtonExistingDatabaseProfile);
        radioButtonExistingDatabaseProfile.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    layoutExistingDatabaseProfile.setVisibility(View.VISIBLE);
                    // uncheck new collection radio button
                    radioButtonNewCollection.setChecked(false);
                } else {
                    layoutExistingDatabaseProfile.setVisibility(View.GONE);
                }
            }
        });

        layoutExistingDatabaseProfile = (ProfileView) view.findViewById(R.id.layoutExistingDatabaseProfile);
        layoutExistingDatabaseProfile.setOnProfileDefaultActionListener(new ProfileView.OnProfileDefaultActionListener() {
            @Override public void onProfileDefaultActionClicked(Profile profile) {
                SelectProfileFromMultipleSourcesDialog.newInstance(
                        SelectProfileFromMultipleSourcesDialog.Target.GPX_FILE_IMPORT)
                    .show(getChildFragmentManager(), "SelectProfileFromMultipleSourcesDialog");
            }
        });
        setSelectedExistingDatabaseProfile(
                savedInstanceState != null
                ? (DatabaseProfile) savedInstanceState.getSerializable(KEY_SELECTED_EXISTING_DATABASE_PROFILE)
                : null);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(
                    isPinned
                    ? getResources().getString(R.string.importGpxFileDialogTitlePinned)
                    : getResources().getString(R.string.importGpxFileDialogTitle))
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
                    execute();
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
                if (filePickingInProgress) {
                    return;
                }

                ViewCompat.setAccessibilityLiveRegion(
                        labelImportResult, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
                labelImportResult.setText(
                        getResources().getString(R.string.messagePleaseWait));
                radioButtonNewCollection.setVisibility(View.GONE);
                layoutNewCollectionName.setVisibility(View.GONE);
                radioButtonExistingDatabaseProfile.setVisibility(View.GONE);
                layoutExistingDatabaseProfile.setVisibility(View.GONE);
                buttonPositive.setVisibility(View.GONE);

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/*");
                startActivityForResult(intent, RC_SELECT_GPX_FILE);
                filePickingInProgress = true;

            } else {
                updateUI();
            }
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(KEY_FILE_PICKING_IN_PROGRESS, this.filePickingInProgress);
        if (this.gpxFileName != null) {
            savedInstanceState.putString(KEY_GPX_FILE_NAME, this.gpxFileName);
        }
        if (this.objectList != null) {
            savedInstanceState.putSerializable(KEY_OBJECT_LIST, this.objectList);
        }
        savedInstanceState.putBoolean(KEY_RADIO_BUTTON_NEW_COLLECTION_IS_CHECKED, this.radioButtonNewCollectionIsChecked);
        savedInstanceState.putSerializable(KEY_SELECTED_EXISTING_DATABASE_PROFILE, layoutExistingDatabaseProfile.getProfile());
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            UiHelper.hideKeyboard(this);
        }
    }

    private void updateUI() {
        labelImportResult.setText(
                String.format(
                    GlobalInstance.getStringResource(R.string.labelImportResult),
                    this.gpxFileName,
                    ObjectWithId.summarizeObjectListContents(this.objectList))
                );

        if (radioButtonNewCollectionIsChecked) {
            radioButtonNewCollection.setChecked(true);
        } else {
            radioButtonExistingDatabaseProfile.setChecked(true);
        }
        if (! isPinned) {
            radioButtonNewCollection.setVisibility(View.VISIBLE);
            radioButtonExistingDatabaseProfile.setVisibility(View.VISIBLE);
        }

        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
        }
    }

    private void setSelectedExistingDatabaseProfile(DatabaseProfile profile) {
        layoutExistingDatabaseProfile.configureAsSingleObject(profile);
    }

    private void execute() {
        if (objectList == null) {
            return;
        }

        DatabaseProfile profileToAddObjectsTo = null;
        if (radioButtonNewCollectionIsChecked) {
            // create new collection

            final String newCollectionName = layoutNewCollectionName.getInputText();
            if (TextUtils.isEmpty(newCollectionName)) {
                Toast.makeText(
                        getActivity(),
                        GlobalInstance.getStringResource(R.string.messageCollectionNameMissing),
                        Toast.LENGTH_LONG)
                    .show();
                return;
            }

            profileToAddObjectsTo = Collection.create(newCollectionName, isPinned);
            if (profileToAddObjectsTo == null) {
                Toast.makeText(
                        getActivity(),
                        GlobalInstance.getStringResource(R.string.messageCouldNotCreateCollection),
                        Toast.LENGTH_LONG)
                    .show();
                return;
            }

        } else {
            // add to existing database profile
            if (layoutExistingDatabaseProfile.getProfile() instanceof DatabaseProfile) {
                profileToAddObjectsTo = (DatabaseProfile) layoutExistingDatabaseProfile.getProfile();
            }

            if (profileToAddObjectsTo == null) {
                Toast.makeText(
                        getActivity(),
                        GlobalInstance.getStringResource(R.string.messageNoProfileSelected),
                        Toast.LENGTH_LONG)
                    .show();
                return;
            }
        }

        for (ObjectWithId objectToAdd : objectList) {
            profileToAddObjectsTo.addObject(objectToAdd);
        }
        Toast.makeText(
                getActivity(),
                String.format(
                    GlobalInstance.getStringResource(R.string.messageAddObjectsToDatabaseProfileWasSuccessful),
                    ObjectWithId.summarizeObjectListContents(this.objectList),
                    profileToAddObjectsTo.getName()),
                Toast.LENGTH_LONG)
            .show();

        Bundle result = new Bundle();
        result.putSerializable(EXTRA_GPX_FILE_PROFILE, profileToAddObjectsTo);
        getParentFragmentManager().setFragmentResult(REQUEST_IMPORT_OF_GPX_FILE_WAS_SUCCESSFUL, result);
        dismiss();
    }


    // select gpx file
    private static final int BUFFER_SIZE = 1024;
    private static final int RC_SELECT_GPX_FILE = 31;

    @Override public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        filePickingInProgress = false;
        if (requestCode != RC_SELECT_GPX_FILE
                || resultCode != AppCompatActivity.RESULT_OK
                || resultData == null) {
            dismiss();
            return;
        }

        final Uri uri = resultData.getData();
        labelImportResult.setText(
                getResources().getString(R.string.messagePleaseWait));
        ViewCompat.setAccessibilityLiveRegion(
                labelImportResult, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
        Executors.newSingleThreadExecutor().execute(() -> {

            try {
                final String extractedFileName = extractFileNameFrom(uri);
                final Pair<ArrayList<ObjectWithId>,String> extractedFileContents = parseGpxFile(uri);
                (new Handler(Looper.getMainLooper())).post(() -> {
                    if (isAdded()) {
                        this.gpxFileName = extractedFileName;
                        this.objectList = extractedFileContents.first;
                        this.layoutNewCollectionName.setInputText(
                                TextUtils.isEmpty(extractedFileContents.second)
                                ? extractedFileName
                                : extractedFileContents.second);
                        updateUI();
                    }
                });

            } catch (GpxFileParseException e) {
                final GpxFileParseException routeParseException = e;
                (new Handler(Looper.getMainLooper())).post(() -> {
                    if (isAdded()) {
                        this.gpxFileName = null;
                        this.objectList = null;
                        this.labelImportResult.setText(routeParseException.getMessage());
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


    /**
     * xml parser
     * GPX format documentation: https://www.topografix.com/GPX/1/1/
     */

    private Pair<ArrayList<ObjectWithId>,String> parseGpxFile(Uri uri) throws GpxFileParseException {
        ArrayList<ObjectWithId> objectList = new ArrayList<ObjectWithId>();
        String collectionName = null;
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
                            collectionName = parser.nextText();
                            parser.require(XmlPullParser.END_TAG, null, "name");
                        } else {
                            skipTag(parser);
                        }
                    }
                    parser.require(XmlPullParser.END_TAG, null, "metadata");

                } else if (parser.getName().equals("wpt")) {
                    parser.require(XmlPullParser.START_TAG, null, "wpt");
                    try {
                        objectList.add(parsePoint(parser, null));
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
                        objectList.add(route);
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
                        objectList.add(route);
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
        return Pair.create(objectList, collectionName);
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
                Date createdDate = Helper.parseTimestamp(parser.nextText());
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
                ? Helper.filterPointListByTurnValueAndImportantIntersections(routePointList)
                : routePointList);
    }


    private class GpxFileParseException extends Exception {
        public GpxFileParseException(String message) {
            super(message);
        }
    }

}
