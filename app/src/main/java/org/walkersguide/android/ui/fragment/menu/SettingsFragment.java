package org.walkersguide.android.ui.fragment.menu;

import android.view.accessibility.AccessibilityEvent;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

import timber.log.Timber;
import org.walkersguide.android.ui.dialog.edit.SpeechRateDialog;
import org.walkersguide.android.ui.dialog.edit.ChangeServerUrlDialog;
import org.walkersguide.android.ui.dialog.select.SelectPublicTransportProviderDialog;
import org.walkersguide.android.ui.dialog.select.SelectShakeIntensityDialog;
import org.walkersguide.android.sensor.shake.ShakeIntensity;
import org.walkersguide.android.util.FileUtility;
import org.walkersguide.android.server.pt.PtUtility;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;
import android.content.Intent;




import android.widget.Button;
import android.widget.CompoundButton;

import java.io.File;

import org.walkersguide.android.ui.dialog.select.SelectMapDialog;
import org.walkersguide.android.database.util.SQLiteHelper;
import org.walkersguide.android.database.util.AccessDatabase;
import android.net.Uri;
import java.io.BufferedInputStream;
import java.util.zip.ZipOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayOutputStream;
import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import de.schildbach.pte.NetworkId;

import java.util.Date;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.server.wg.status.OSMMap;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.R;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import org.walkersguide.android.util.SettingsManager;


import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import androidx.appcompat.widget.SwitchCompat;

import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.wg.status.ServerStatusTask;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.server.wg.WgException;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.Intent;
import android.content.Context;
import android.widget.EditText;
import org.walkersguide.android.tts.TtsSettings;
import org.walkersguide.android.ui.interfaces.TextChangedListener;
import android.text.Editable;
import android.widget.TextView;
import android.view.KeyEvent;
import org.walkersguide.android.ui.UiHelper;
import android.view.inputmethod.EditorInfo;
import org.walkersguide.android.ui.dialog.select.SelectObjectWithIdFromMultipleSourcesDialog;
import org.walkersguide.android.ui.view.ObjectWithIdView;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.ui.fragment.RootFragment;
import android.widget.Toast;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.profile.static_profile.HistoryProfile;
import androidx.fragment.app.DialogFragment;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.dialog.edit.ConfigureWayClassWeightsDialog;
import org.walkersguide.android.server.wg.p2p.WayClassWeightSettings.Preset;
import org.walkersguide.android.ui.fragment.object_list.extended.ObjectListFromDatabaseFragment;
import org.walkersguide.android.database.profile.StaticProfile;


public class SettingsFragment extends RootFragment implements FragmentResultListener {
    private static final String KEY_TASK_ID = "taskId";
    private static String KEY_SETTINGS_IMPORT_SUCCESSFUL = "settingsImportSuccessful";

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        return fragment;
    }


    private SettingsManager settingsManagerInstance;
    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;
    private boolean settingsImportSuccessful;

    private ObjectWithIdView layoutHomeAddress;
    private Button buttonServerURL, buttonServerMap, buttonRoutingWayClasses;
    private SwitchCompat switchPreferTranslatedStrings;
    private Button buttonPublicTransportProvider;
    private Button buttonShakeIntensity;
    private SwitchCompat switchShowActionButton, switchDisplayRemainsActive, switchPreferFusedLocationProviderInsteadOfNetworkProvider;
    private SwitchCompat switchAnnouncementsEnabled, switchKeepBluetoothHeadsetConnectionAlive;
    private Button buttonSpeechRate;
    private EditText editDistanceAnnouncementInterval;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsManagerInstance = SettingsManager.getInstance();
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();

        if (savedInstanceState != null) {
            taskId = savedInstanceState.getLong(KEY_TASK_ID);
            settingsImportSuccessful = savedInstanceState.getBoolean(KEY_SETTINGS_IMPORT_SUCCESSFUL);
        } else {
            taskId = ServerTaskExecutor.NO_TASK_ID;
            settingsImportSuccessful = false;
        }

        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectObjectWithIdFromMultipleSourcesDialog.REQUEST_SELECT_OBJECT_WITH_ID, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    ChangeServerUrlDialog.REQUEST_SERVER_URL_CHANGED, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectMapDialog.REQUEST_SELECT_MAP, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    ConfigureWayClassWeightsDialog.REQUEST_WAY_CLASS_WEIGHTS_CHANGED, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectPublicTransportProviderDialog.REQUEST_SELECT_PT_PROVIDER, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectShakeIntensityDialog.REQUEST_SELECT_SHAKE_INTENSITY, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SpeechRateDialog.REQUEST_CHANGE_SPEECH_RATE, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    ImportSettingsInBackgroundDialog.REQUEST_IMPORT_OF_SETTINGS_IN_BACKGROUND_WAS_SUCCESSFUL, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SimpleMessageDialog.REQUEST_DIALOG_CLOSED, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectObjectWithIdFromMultipleSourcesDialog.REQUEST_SELECT_OBJECT_WITH_ID)) {
            SelectObjectWithIdFromMultipleSourcesDialog.Target objectWithIdTarget = (SelectObjectWithIdFromMultipleSourcesDialog.Target)
                bundle.getSerializable(SelectObjectWithIdFromMultipleSourcesDialog.EXTRA_TARGET);
            ObjectWithId selectedObjectWithId = (ObjectWithId) bundle.getSerializable(SelectObjectWithIdFromMultipleSourcesDialog.EXTRA_OBJECT_WITH_ID);
            if (objectWithIdTarget == SelectObjectWithIdFromMultipleSourcesDialog.Target.USE_AS_HOME_ADDRESS
                    && selectedObjectWithId instanceof Point) {
                settingsManagerInstance.setHomeAddress((Point) selectedObjectWithId);
                // history
                if (selectedObjectWithId instanceof StreetAddress) {
                    HistoryProfile.addressPoints().addObject((StreetAddress) selectedObjectWithId);
                } else {
                    HistoryProfile.allPoints().addObject(selectedObjectWithId);
                }
                updateUI();
            }

        } else if (requestKey.equals(ChangeServerUrlDialog.REQUEST_SERVER_URL_CHANGED)) {
            // new server url is already updated in settings
            // see WgUtility#getServerInstance for details
            //
            // show SelectMapDialog if no map is selected
            if (settingsManagerInstance.getSelectedMap() == null) {
                SelectMapDialog.newInstance(null)
                    .show(getChildFragmentManager(), "SelectMapDialog");
            }
            updateUI();

        } else if (requestKey.equals(SelectMapDialog.REQUEST_SELECT_MAP)) {
            settingsManagerInstance.setSelectedMap(
                    (OSMMap) bundle.getSerializable(SelectMapDialog.EXTRA_MAP));
            updateUI();

        } else if (requestKey.equals(ConfigureWayClassWeightsDialog.REQUEST_WAY_CLASS_WEIGHTS_CHANGED)) {
            updateUI();

        } else if (requestKey.equals(SelectPublicTransportProviderDialog.REQUEST_SELECT_PT_PROVIDER)) {
            settingsManagerInstance.setSelectedNetworkId(
                    (NetworkId) bundle.getSerializable(SelectPublicTransportProviderDialog.EXTRA_NETWORK_ID));
            updateUI();

        } else if (requestKey.equals(SelectShakeIntensityDialog.REQUEST_SELECT_SHAKE_INTENSITY)) {
            settingsManagerInstance.setSelectedShakeIntensity(
                    (ShakeIntensity) bundle.getSerializable(SelectShakeIntensityDialog.EXTRA_SHAKE_INTENSITY));
            updateUI();

        } else if (requestKey.equals(SpeechRateDialog.REQUEST_CHANGE_SPEECH_RATE)) {
            TtsSettings ttsSettings = settingsManagerInstance.getTtsSettings();
            ttsSettings.setSpeechRate(
                    bundle.getFloat(SpeechRateDialog.EXTRA_SPEECH_RATE));
            settingsManagerInstance.setTtsSettings(ttsSettings);
            updateUI();

        } else if (requestKey.equals(ImportSettingsInBackgroundDialog.REQUEST_IMPORT_OF_SETTINGS_IN_BACKGROUND_WAS_SUCCESSFUL)) {
            settingsImportSuccessful = bundle.getBoolean(ImportSettingsInBackgroundDialog.EXTRA_SETTINGS_IMPORT_SUCCESSFUL);
            cleanupCache();
            SimpleMessageDialog.newInstance(
                    settingsImportSuccessful
                    ? GlobalInstance.getStringResource(R.string.labelImportSuccessful)
                    : GlobalInstance.getStringResource(R.string.labelImportFailed))
                .show(getChildFragmentManager(), "SimpleMessageDialog");

        } else if (requestKey.equals(SimpleMessageDialog.REQUEST_DIALOG_CLOSED)) {
            updateUI();
            if (settingsImportSuccessful) {
                settingsImportSuccessful = false;
                mainActivityController.recreateActivity();
            }
        }
    }


    /**
     * create view
     */

    @Override public String getTitle() {
        return getResources().getString(R.string.fragmentSettingsName);
    }

    @Override public int getLayoutResourceId() {
        return R.layout.fragment_settings;
    }

    @Override public View configureView(View view, Bundle savedInstanceState) {
        layoutHomeAddress = (ObjectWithIdView) view.findViewById(R.id.layoutHomeAddress);
        layoutHomeAddress.setOnDefaultObjectActionListener(new ObjectWithIdView.OnDefaultObjectActionListener() {
            @Override public void onDefaultObjectActionClicked(ObjectWithIdView view, View subView, ObjectWithId objectWithId) {
                SelectObjectWithIdFromMultipleSourcesDialog.newInstance(
                        SelectObjectWithIdFromMultipleSourcesDialog.Target.USE_AS_HOME_ADDRESS)
                    .show(getChildFragmentManager(), "SelectObjectWithIdFromMultipleSourcesDialog");
            }
        });
        layoutHomeAddress.setOnRemoveObjectActionListener(new ObjectWithIdView.OnRemoveObjectActionListener() {
            @Override public void onRemoveObjectActionClicked(ObjectWithId objectWithId) {
                settingsManagerInstance.setHomeAddress(null);
                updateUI();
            }
        });

        // wg server settings

        buttonServerURL = (Button) view.findViewById(R.id.buttonServerURL);
        buttonServerURL.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ChangeServerUrlDialog.newInstance(
                        settingsManagerInstance.getServerURL())
                    .show(getChildFragmentManager(), "ChangeServerUrlDialog");
            }
        });

        buttonServerMap = (Button) view.findViewById(R.id.buttonServerMap);
        buttonServerMap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectMapDialog.newInstance(
                        settingsManagerInstance.getSelectedMap())
                    .show(getChildFragmentManager(), "SelectMapDialog");
            }
        });

        buttonRoutingWayClasses = (Button) view.findViewById(R.id.buttonRoutingWayClasses);
        buttonRoutingWayClasses.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ConfigureWayClassWeightsDialog.newInstance()
                    .show(getChildFragmentManager(), "ConfigureWayClassWeightsDialog");
            }
        });

        Button buttonExcludedWays = (Button) view.findViewById(R.id.buttonExcludedWays);
        buttonExcludedWays.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                    ObjectListFromDatabaseFragment.newInstance(StaticProfile.excludedRoutingSegments())
                        .show(getChildFragmentManager(), "excludedRoutingSegments");
            }
        });

        switchPreferTranslatedStrings = (SwitchCompat) view.findViewById(R.id.switchPreferTranslatedStrings);
        switchPreferTranslatedStrings.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                if (isChecked != settingsManagerInstance.getPreferTranslatedStrings()) {
                    GlobalInstance.getInstance().clearCaches();
                    settingsManagerInstance.setPreferTranslatedStrings(isChecked);
                }
            }
        });

        // public transport

        buttonPublicTransportProvider = (Button) view.findViewById(R.id.buttonPublicTransportProvider);
        buttonPublicTransportProvider.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectPublicTransportProviderDialog.newInstance(
                        settingsManagerInstance.getSelectedNetworkId())
                    .show(getChildFragmentManager(), "SelectPublicTransportProviderDialog");
            }
        });

        // ui
        switchShowActionButton = (SwitchCompat) view.findViewById(R.id.switchShowActionButton);
        switchShowActionButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                if (isChecked != settingsManagerInstance.getShowActionButton()) {
                    settingsManagerInstance.setShowActionButton(isChecked);
                }
            }
        });

        switchDisplayRemainsActive = (SwitchCompat) view.findViewById(R.id.switchDisplayRemainsActive);
        switchDisplayRemainsActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                if (settingsManagerInstance.getDisplayRemainsActive() != isChecked) {
                    settingsManagerInstance.setDisplayRemainsActive(isChecked);
                    mainActivityController.displayRemainsActiveSettingChanged(isChecked);
                }
            }
        });

        switchPreferFusedLocationProviderInsteadOfNetworkProvider = (SwitchCompat) view.findViewById(R.id.switchPreferFusedLocationProviderInsteadOfNetworkProvider);
        switchPreferFusedLocationProviderInsteadOfNetworkProvider.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                if (settingsManagerInstance.getPreferFusedLocationProviderInsteadOfNetworkProvider() != isChecked) {
                    settingsManagerInstance.setPreferFusedLocationProviderInsteadOfNetworkProvider(isChecked);
                    // toggle position manager
                    PositionManager positionManagerInstance = PositionManager.getInstance();
                    if (positionManagerInstance.isRunning()) {
                        positionManagerInstance.stopGPS();
                        positionManagerInstance.startGPS();
                    }
                }
            }
        });

        buttonShakeIntensity = (Button) view.findViewById(R.id.buttonShakeIntensity);
        buttonShakeIntensity.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectShakeIntensityDialog.newInstance(
                        settingsManagerInstance.getSelectedShakeIntensity())
                    .show(getChildFragmentManager(), "SelectShakeIntensityDialog");
            }
        });

        // tts

        switchAnnouncementsEnabled = (SwitchCompat) view.findViewById(R.id.switchAnnouncementsEnabled);
        switchAnnouncementsEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                TtsSettings ttsSettings = settingsManagerInstance.getTtsSettings();
                if (ttsSettings.getAnnouncementsEnabled() != isChecked) {
                    ttsSettings.setAnnouncementsEnabled(isChecked);
                    settingsManagerInstance.setTtsSettings(ttsSettings);
                }
            }
        });

        buttonSpeechRate = (Button) view.findViewById(R.id.buttonSpeechRate);
        buttonSpeechRate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SpeechRateDialog.newInstance(
                        settingsManagerInstance.getTtsSettings().getSpeechRate())
                    .show(getChildFragmentManager(), "SpeechRateDialog");
            }
        });

        editDistanceAnnouncementInterval = (EditText) view.findViewById(R.id.editDistanceAnnouncementInterval);
        editDistanceAnnouncementInterval.addTextChangedListener(new TextChangedListener<EditText>(editDistanceAnnouncementInterval) {
            @Override public void onTextChanged(EditText view, Editable s) {
                int newDistanceAnnouncementInterval = 0;
                try {
                    newDistanceAnnouncementInterval = Integer.parseInt(view.getText().toString());
                } catch (NumberFormatException nfe) {}
                if (newDistanceAnnouncementInterval > 0) {
                    TtsSettings ttsSettings = settingsManagerInstance.getTtsSettings();
                    ttsSettings.setDistanceAnnouncementInterval(newDistanceAnnouncementInterval);
                    settingsManagerInstance.setTtsSettings(ttsSettings);
                }
            }
        });
        editDistanceAnnouncementInterval.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (UiHelper.isDoSomeThingEditorAction(actionId, EditorInfo.IME_ACTION_DONE, event)) {
                    UiHelper.hideKeyboard(SettingsFragment.this);
                    return true;
                }
                return false;
            }
        });

        switchKeepBluetoothHeadsetConnectionAlive = (SwitchCompat) view.findViewById(R.id.switchKeepBluetoothHeadsetConnectionAlive);
        switchKeepBluetoothHeadsetConnectionAlive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                if (isChecked != settingsManagerInstance.getKeepBluetoothHeadsetConnectionAlive()) {
                    GlobalInstance globalInstance = GlobalInstance.getInstance();
                    if (isChecked) {
                        globalInstance.startPlaybackOfSilenceWavFile();
                    } else {
                        globalInstance.stopPlaybackOfSilenceWavFile();
                    }
                    settingsManagerInstance.setKeepBluetoothHeadsetConnectionAlive(isChecked);
                }
            }
        });

        // import and export settings
        Button buttonImportSettings = (Button) view.findViewById(R.id.buttonImportSettings);
        buttonImportSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startSettingsImport();
            }
        });

        Button buttonExportSettings = (Button) view.findViewById(R.id.buttonExportSettings);
        buttonExportSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (! startSettingsExport()) {
                    cleanupCache();
                    SimpleMessageDialog.newInstance(
                            getResources().getString(R.string.labelExportFailed))
                        .show(getChildFragmentManager(), "SimpleMessageDialog");
                }
            }
        });

        return view;
    }

    @Override public void onResume() {
        super.onResume();

        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_STATUS_TASK_SUCCESSFUL);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(localIntentReceiver, localIntentFilter);

        updateUI();
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(localIntentReceiver);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong(KEY_TASK_ID, taskId);
        savedInstanceState.putBoolean(KEY_SETTINGS_IMPORT_SUCCESSFUL, settingsImportSuccessful);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            serverTaskExecutorInstance.cancelTask(taskId, false);
        }
    }

    private void updateUI() {
        layoutHomeAddress.configureAsSingleObject(
                settingsManagerInstance.getHomeAddress());

        // WalkersGuide server url
        buttonServerURL.setText(
                String.format(
                    getResources().getString(R.string.buttonServerURL),
                    settingsManagerInstance.getServerURL())
                );

        // WalkersGuide server map placeholder
        buttonServerMap.setText(
                getResources().getString(R.string.buttonServerMapNoSelection));

        switchPreferTranslatedStrings.setChecked(settingsManagerInstance.getPreferTranslatedStrings());

        Preset matchingWcwsPreset = Preset.matchesSettings(settingsManagerInstance.getWayClassWeightSettings());
        buttonRoutingWayClasses.setText(
                String.format(
                    "%1$s: %2$s",
                    getResources().getString(R.string.planRouteMenuItemRoutingWayClasses),
                    matchingWcwsPreset != null
                    ? matchingWcwsPreset.toString()
                    : getResources().getString(R.string.fillingWordCustom))
                );

        // public transport provider
        if (settingsManagerInstance.getSelectedNetworkId() != null) {
            buttonPublicTransportProvider.setText(
                    String.format(
                        getResources().getString(R.string.buttonPublicTransportProvider),
                        PtUtility.getNameForNetworkId(
                            settingsManagerInstance.getSelectedNetworkId())
                        )
                    );
        } else {
            buttonPublicTransportProvider.setText(
                    getResources().getString(R.string.buttonPublicTransportProviderNoSelection));
        }

        // ui settings
        switchShowActionButton.setChecked(settingsManagerInstance.getShowActionButton());
        switchDisplayRemainsActive.setChecked(settingsManagerInstance.getDisplayRemainsActive());
        switchPreferFusedLocationProviderInsteadOfNetworkProvider.setChecked(
                settingsManagerInstance.getPreferFusedLocationProviderInsteadOfNetworkProvider());
        switchPreferFusedLocationProviderInsteadOfNetworkProvider.setVisibility(
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                ? View.VISIBLE : View.GONE);
        buttonShakeIntensity.setText(
                String.format(
                    "%1$s: %2$s",
                    getResources().getString(R.string.buttonShakeIntensity),
                    settingsManagerInstance.getSelectedShakeIntensity())
                );
        TtsSettings ttsSettings = settingsManagerInstance.getTtsSettings();
        switchAnnouncementsEnabled.setChecked(ttsSettings.getAnnouncementsEnabled());
        buttonSpeechRate.setText(
                String.format(
                    Locale.getDefault(),
                    "%1$s: %2$.2fx",
                    getResources().getString(R.string.buttonSpeechRate),
                    ttsSettings.getSpeechRate())
                );
        editDistanceAnnouncementInterval.setText(
                String.valueOf(ttsSettings.getDistanceAnnouncementInterval()));
        editDistanceAnnouncementInterval.selectAll();
        switchKeepBluetoothHeadsetConnectionAlive.setChecked(settingsManagerInstance.getKeepBluetoothHeadsetConnectionAlive());

        // request server instance
        if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
            taskId = serverTaskExecutorInstance.executeTask(new ServerStatusTask());
        }
    }


    // background task results

    private BroadcastReceiver localIntentReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_STATUS_TASK_SUCCESSFUL)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                if (taskId != intent.getLongExtra(ServerTaskExecutor.EXTRA_TASK_ID, ServerTaskExecutor.INVALID_TASK_ID)) {
                    return;
                }

                if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_STATUS_TASK_SUCCESSFUL)) {
                    ServerInstance serverInstance = (ServerInstance) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_SERVER_INSTANCE);
                    if (serverInstance != null) {
                        if (settingsManagerInstance.getSelectedMap() != null) {
                            buttonServerMap.setText(
                                    String.format(
                                        GlobalInstance.getStringResource(R.string.buttonServerMap),
                                        settingsManagerInstance.getSelectedMap().getName())
                                    );
                        }
                    }

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)) {

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                    WgException wgException = (WgException) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_EXCEPTION);
                    if (wgException != null) {
                        Toast.makeText(
                                context,
                                wgException.getMessage(),
                                Toast.LENGTH_LONG)
                            .show();
                    }
                }
            }
        }
    };


    /**
     * import and export settings
     *
     * Doc: Storage Access Framework
     *   Access documents and other files from shared storage
     *   https://developer.android.com/training/data-storage/shared/documents-files
     */
    private static final int BUFFER_SIZE = 1024;
    private static final String MIME_TYPE_ZIP = "application/zip";

    private static final int IMPORT_SETTINGS = 13;
    private static final int EXPORT_SETTINGS = 14;

    private void startSettingsImport() {
        cleanupCache();

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(MIME_TYPE_ZIP);
        startActivityForResult(intent, IMPORT_SETTINGS);
    }

    private boolean startSettingsExport() {
        cleanupCache();

        ArrayList<File> inputFileList = new ArrayList<File>();
        // settings
        File settingsFile = getCachedSettingsFile();
        boolean settingsFileCreation = settingsManagerInstance.exportSettings(settingsFile);
        if (! settingsFileCreation || ! settingsFile.exists()) {
            return false;
        } else {
            inputFileList.add(settingsFile);
        }
        // database
        File databaseFile = getCachedDatabaseFile();
        boolean databaseFileCreation = FileUtility.copyFile(
                SQLiteHelper.getDatabaseFile(), databaseFile);
        if (! databaseFileCreation || ! databaseFile.exists()) {
            return false;
        } else {
            inputFileList.add(databaseFile);
        }

        // create zip file
        File zipFile = getCachedZipFile();
        ZipOutputStream out = null;
        boolean zipFileCreationSuccessful = true;
        try {
            out = new ZipOutputStream(
                    new BufferedOutputStream(
                        new FileOutputStream(zipFile)));
            BufferedInputStream origin = null;
            byte data[] = new byte[BUFFER_SIZE];

            for (File inputFile : inputFileList) {
                FileInputStream fi = new FileInputStream(inputFile);
                origin = new BufferedInputStream(fi, BUFFER_SIZE);
                ZipEntry entry = new ZipEntry(inputFile.getName());
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }

        } catch (Exception e) {
            zipFileCreationSuccessful = false;
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.finish();
                    out.close();
                } catch (IOException e) {}
            }
            if (! zipFileCreationSuccessful || ! zipFile.exists()) {
                return false;
            }
        }

        final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(MIME_TYPE_ZIP);
        intent.putExtra(
                Intent.EXTRA_TITLE,
                String.format(
                    Locale.ROOT, "walkersguide_backup_%1$s.zip", isoDateFormat.format(new Date()))
                );
        startActivityForResult(intent, EXPORT_SETTINGS);
        return true;
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode != AppCompatActivity.RESULT_OK) {
            Timber.i("import or export cancelled");
            cleanupCache();
            return;
        }

        final Uri selectedUri = resultData.getData();
        switch (requestCode) {

            case IMPORT_SETTINGS:
                ImportSettingsInBackgroundDialog.newInstance(selectedUri)
                    .show(getChildFragmentManager(), "ImportSettingsInBackgroundDialog");
                break;

            case EXPORT_SETTINGS:
                boolean copySelectedUriSuccessful = FileUtility.copyFile(getCachedZipFile(), selectedUri);
                cleanupCache();
                SimpleMessageDialog.newInstance(
                        copySelectedUriSuccessful
                        ? getResources().getString(R.string.labelExportSuccessful)
                        : getResources().getString(R.string.labelExportFailed))
                    .show(getChildFragmentManager(), "SimpleMessageDialog");
                break;
        }
    }


    public static class ImportSettingsInBackgroundDialog extends DialogFragment {
    public static final String REQUEST_IMPORT_OF_SETTINGS_IN_BACKGROUND_WAS_SUCCESSFUL = "requestImportOfSettingsInBackgroundWasSuccessful";
    public static final String EXTRA_SETTINGS_IMPORT_SUCCESSFUL = "settingsImportSuccessful";

        public static ImportSettingsInBackgroundDialog newInstance(Uri selectedUri) {
            ImportSettingsInBackgroundDialog dialog = new ImportSettingsInBackgroundDialog();
            Bundle args = new Bundle();
            args.putParcelable(KEY_SELECTED_URI, selectedUri);
            dialog.setArguments(args);
            return dialog;
        }

        private static final String KEY_SELECTED_URI = "selectedUri";
        private Uri selectedUri;
        private TextView labelSimpleMessage;

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            selectedUri = (Uri) getArguments().getParcelable(KEY_SELECTED_URI);

            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_single_text_view, nullParent);
            labelSimpleMessage = (TextView) view.findViewById(R.id.label);
            labelSimpleMessage.setText(
                    getResources().getString(R.string.labelProceedWithImport));

            return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(
                        getResources().getString(R.string.dialogYes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setNegativeButton(
                        getResources().getString(R.string.dialogNo),
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

                final Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        cleanupCache();
                        dismiss();
                    }
                });

                final Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.setCancelable(false);
                        labelSimpleMessage.setText(
                                getResources().getString(R.string.messagePleaseWait));
                        buttonPositive.setVisibility(View.GONE);
                        buttonNegative.setVisibility(View.GONE);
                        labelSimpleMessage.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);

                        Executors.newSingleThreadExecutor().execute(() -> {
                            final boolean successful = doSettingsImport(selectedUri, getCachedZipFile());
                            (new Handler(Looper.getMainLooper())).post(() -> {
                                Bundle result = new Bundle();
                                result.putBoolean(EXTRA_SETTINGS_IMPORT_SUCCESSFUL, successful);
                                getParentFragmentManager().setFragmentResult(REQUEST_IMPORT_OF_SETTINGS_IN_BACKGROUND_WAS_SUCCESSFUL, result);
                                dismiss();
                            });
                        });
                    }
                });
            }
        }

        private static boolean doSettingsImport(Uri uri, File zipFile) {
            boolean zipFileCreationSuccessful = FileUtility.copyFile(uri, zipFile);
            Timber.d("import: zipFileCreationSuccessful = %1$s", zipFileCreationSuccessful);
            if (! zipFileCreationSuccessful || ! zipFile.exists()) {
                return false;
            }

            // unzip
            ZipInputStream zin = null;
            try {
                zin = new ZipInputStream(new FileInputStream(zipFile));
                ZipEntry ze = null;
                while ((ze = zin.getNextEntry()) != null) {
                    if (ze.isDirectory()) {
                        continue;
                    }
                    FileOutputStream fout = new FileOutputStream(
                            new File(getCacheDirectory(), ze.getName()));
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int count;

                    // reading and writing
                    while ((count = zin.read(buffer)) != -1) {
                        baos.write(buffer, 0, count);
                        byte[] bytes = baos.toByteArray();
                        fout.write(bytes);
                        baos.reset();
                    }
                    fout.close();
                    zin.closeEntry();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (zin != null) {
                    try {
                        zin.close();
                    } catch (IOException e) {}
                }
            }

            // restore settings
            File settingsFile = getCachedSettingsFile();
            if (! settingsFile.exists()) {
                return false;
            }
            if (! SettingsManager.getInstance().importSettings(settingsFile)) {
                Timber.e("settings import failed");
                return false;
            }

            // restore database
            File newDatabaseFile = getCachedDatabaseFile();
            if (! newDatabaseFile.exists()) {
                return false;
            }
            // close database
            AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance();
            accessDatabaseInstance.close();
            // remove old database
            File oldDatabaseFile = SQLiteHelper.getDatabaseFile();
            if (oldDatabaseFile.exists()) {
                oldDatabaseFile.delete();
            }
            // copy from cache
            boolean databaseImportSuccessful = FileUtility.copyFile(
                    newDatabaseFile, SQLiteHelper.getDatabaseFile());
            // open again
            accessDatabaseInstance.open();

            // reset cache and reload ui
            cleanupCache();
            GlobalInstance.getInstance().clearCaches();
            return databaseImportSuccessful;
        }
    }


    // static helpers

    private static File getCacheDirectory() {
        return GlobalInstance.getContext().getCacheDir();
    }

    private static File getCachedSettingsFile() {
        return new File(getCacheDirectory(), "settings.xml");
    }

    private static File getCachedDatabaseFile() {
        return new File(getCacheDirectory(), "database.sql");
    }

    private static File getCachedZipFile() {
        return new File(getCacheDirectory(), "backup.zip");
    }

    private static void cleanupCache() {
        File settingsFile = getCachedSettingsFile();
        if (settingsFile.exists()) {
            settingsFile.delete();
        }
        File databaseFile = getCachedDatabaseFile();
        if (databaseFile.exists()) {
            databaseFile.delete();
        }
        File zipFile = getCachedZipFile();
        if (zipFile.exists()) {
            zipFile.delete();
        }
    }

}
