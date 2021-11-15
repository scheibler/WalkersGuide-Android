package org.walkersguide.android.ui.activity;

import android.widget.LinearLayout.LayoutParams;
import android.widget.LinearLayout;
import timber.log.Timber;
import org.walkersguide.android.ui.dialog.SelectPublicTransportProviderDialog;
import org.walkersguide.android.helper.FileUtility;
import org.walkersguide.android.pt.PTHelper;
import android.content.IntentFilter;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.widget.Toolbar;

import android.text.InputType;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;

import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;

import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.server.ServerStatusManager.ServerStatusListener;
import org.walkersguide.android.ui.dialog.SelectMapDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.ServerSettings;
import android.content.BroadcastReceiver;
import org.walkersguide.android.database.util.SQLiteHelper;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.database.util.AccessDatabase;
import android.net.Uri;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import org.walkersguide.android.ui.listener.TextChangedListener;
import android.text.Editable;
import java.io.BufferedInputStream;
import java.util.zip.ZipOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayOutputStream;


public class SettingsActivity extends AbstractToolbarActivity implements ServerStatusListener {

    private Button buttonServerURL, buttonServerMap;
    private Button buttonPublicTransportProvider;
    private Button buttonShakeIntensity;
    private Switch buttonEnableTextInputHistory;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_settings);

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(
                getResources().getString(R.string.settingsActivityTitle));

        // server settings

		buttonServerURL = (Button) findViewById(R.id.buttonServerURL);
		buttonServerURL.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
                NewServerDialog.newInstance()
                    .show(getSupportFragmentManager(), "NewServerDialog");
            }
        });

		buttonServerMap = (Button) findViewById(R.id.buttonServerMap);
		buttonServerMap.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
                SelectMapDialog.newInstance()
                    .show(getSupportFragmentManager(), "SelectMapDialog");
            }
        });

		buttonPublicTransportProvider = (Button) findViewById(R.id.buttonPublicTransportProvider);
		buttonPublicTransportProvider.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
                SelectPublicTransportProviderDialog.newInstance()
                    .show(getSupportFragmentManager(), "SelectPublicTransportProviderDialog");
            }
        });

        buttonShakeIntensity = (Button) findViewById(R.id.buttonShakeIntensity);
        buttonShakeIntensity.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectShakeIntensityDialog.newInstance()
                    .show(getSupportFragmentManager(), "SelectShakeIntensityDialog");
            }
        });

        // privacy settings
        buttonEnableTextInputHistory = (Switch) findViewById(R.id.buttonEnableTextInputHistory);
        buttonEnableTextInputHistory.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                if (isChecked != settingsManagerInstance.getEnableSearchTermHistory()) {
                    settingsManagerInstance.setEnableSearchTermHistory(isChecked);
                    if (! isChecked) {
                        settingsManagerInstance.clearSearchTermHistory();
                    }
                }
            }
        });

        // import and export settings
		Button buttonImportSettings = (Button) findViewById(R.id.buttonImportSettings);
		buttonImportSettings.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
                importSettings();
            }
        });

		Button buttonExportSettings = (Button) findViewById(R.id.buttonExportSettings);
		buttonExportSettings.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
                exportSettings();
            }
        });
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localIntentReceiver);
        ServerStatusManager.getInstance(this).invalidateServerStatusRequest(this);
    }

	@Override public void onResume() {
		super.onResume();
        // listen for local intents
        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(SelectPublicTransportProviderDialog.NEW_NETWORK_PROVIDER);
        LocalBroadcastManager.getInstance(this).registerReceiver(localIntentReceiver, localIntentFilter);
        // update ui
        updateUI();
    }

    private void updateUI() {
        ServerSettings serverSettings = settingsManagerInstance.getServerSettings();

        // WalkersGuide server url
        buttonServerURL.setText(
                String.format(
                    getResources().getString(R.string.buttonServerURL),
                    serverSettings.getServerURL())
                );
        // WalkersGuide server map placeholder
        buttonServerMap.setText(
                getResources().getString(R.string.buttonServerMapNoSelection));

        // public transport provider
        if (serverSettings.getSelectedPublicTransportProvider() != null) {
            buttonPublicTransportProvider.setText(
                    String.format(
                        getResources().getString(R.string.buttonPublicTransportProvider),
                        PTHelper.getNetworkProviderName(
                            (SettingsActivity) this, serverSettings.getSelectedPublicTransportProvider()))
                    );
        } else {
            buttonPublicTransportProvider.setText(
                    getResources().getString(R.string.buttonPublicTransportProviderNoSelection));
        }

        // shake intensity button
        String shakeIntensityName;
        switch (settingsManagerInstance.getSelectedShakeIntensity()) {
            case Constants.SHAKE_INTENSITY.DISABLED:
                shakeIntensityName = getResources().getString(R.string.shakeIntensityDisabled);
                break;
            case Constants.SHAKE_INTENSITY.VERY_WEAK:
                shakeIntensityName = getResources().getString(R.string.shakeIntensityVeryWeak);
                break;
            case Constants.SHAKE_INTENSITY.WEAK:
                shakeIntensityName = getResources().getString(R.string.shakeIntensityWeak);
                break;
            case Constants.SHAKE_INTENSITY.MEDIUM:
                shakeIntensityName = getResources().getString(R.string.shakeIntensityMedium);
                break;
            case Constants.SHAKE_INTENSITY.STRONG:
                shakeIntensityName = getResources().getString(R.string.shakeIntensityStrong);
                break;
            case Constants.SHAKE_INTENSITY.VERY_STRONG:
                shakeIntensityName = getResources().getString(R.string.shakeIntensityVeryStrong);
                break;
            default:
                shakeIntensityName = String.valueOf(settingsManagerInstance.getSelectedShakeIntensity());
                break;
        }
        buttonShakeIntensity.setText(
                String.format(
                    "%1$s: %2$s",
                    getResources().getString(R.string.buttonShakeIntensity),
                    shakeIntensityName)
                );

        // privacy and development settings
        buttonEnableTextInputHistory.setChecked(settingsManagerInstance.getEnableSearchTermHistory());

        // request server status instance
        ServerStatusManager.getInstance(this).requestServerStatus(
                (SettingsActivity) this, serverSettings.getServerURL());
    }

	@Override public void serverStatusRequestFinished(Context context, int returnCode, ServerInstance serverInstance) {
        if (returnCode == Constants.RC.OK
                && serverInstance != null) {
            ServerSettings serverSettings = settingsManagerInstance.getServerSettings();
            if (serverSettings.getSelectedMap() != null) {
                buttonServerMap.setText(
                        String.format(
                            context.getResources().getString(R.string.buttonServerMap),
                            serverSettings.getSelectedMap().getName())
                        );
            }
        } else {
            SimpleMessageDialog.newInstance(
                    ServerUtility.getErrorMessageForReturnCode(context, returnCode))
                .show(getSupportFragmentManager(), "SimpleMessageDialog");
        }
    }


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

    private void importSettings() {
        cleanupCache();

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(MIME_TYPE_ZIP);
        startActivityForResult(intent, IMPORT_SETTINGS);
    }

    private void exportSettings() {
        cleanupCache();

        ArrayList<File> inputFileList = new ArrayList<File>();
        // settings
        File settingsFile = getCachedSettingsFile();
        boolean settingsFileCreation = settingsManagerInstance.exportSettings(settingsFile);
        if (! settingsFileCreation || ! settingsFile.exists()) {
            return;
        } else {
            inputFileList.add(settingsFile);
        }
        // database
        File databaseFile = getCachedDatabaseFile();
        boolean databaseFileCreation = FileUtility.copyFile(
                SQLiteHelper.getDatabaseFile(), databaseFile);
        if (! databaseFileCreation || ! databaseFile.exists()) {
            return;
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
                return;
            }
        }

        final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(MIME_TYPE_ZIP);
        intent.putExtra(
                Intent.EXTRA_TITLE,
                String.format(
                    Locale.ROOT, "walkersguide_backup_%1$s.zip", isoDateFormat.format(new Date()))
                );
        startActivityForResult(intent, EXPORT_SETTINGS);
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode != AppCompatActivity.RESULT_OK) {
            Timber.i("import or export cancelled");
            cleanupCache();
            return;
        }

        Uri selectedUri = resultData.getData();
        File zipFile = getCachedZipFile();
        switch (requestCode) {

            case IMPORT_SETTINGS:
                boolean zipFileCreationSuccessful = FileUtility.copyFile(selectedUri, zipFile);
                Timber.d("import: zipFileCreationSuccessful = %1$s", zipFileCreationSuccessful);
                if (! zipFileCreationSuccessful || ! zipFile.exists()) {
                    SimpleMessageDialog.newInstance(
                            GlobalInstance.getStringResource(R.string.labelImportFailed))
                        .show(getSupportFragmentManager(), "SimpleMessageDialog");
                    return;
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
                    return;
                }
                boolean settingsImportSuccessful = settingsManagerInstance.importSettings(settingsFile);
                Timber.d("settings import: %1$s", settingsImportSuccessful);
                if (! settingsImportSuccessful) {
                    return;
                }

                // restore database
                File newDatabaseFile = getCachedDatabaseFile();
                if (! newDatabaseFile.exists()) {
                    return;
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
                ServerStatusManager.getInstance(GlobalInstance.getContext()).setCachedServerInstance(null);
                SimpleMessageDialog.newInstance(
                        GlobalInstance.getStringResource(R.string.labelImportSuccessful), true)
                    .show(getSupportFragmentManager(), "SimpleMessageDialog");
                break;

            case EXPORT_SETTINGS:
                boolean copySelectedUriSuccessful = FileUtility.copyFile(zipFile, selectedUri);
                cleanupCache();
                Timber.d("export: copy successful = %1$s", copySelectedUriSuccessful);
                break;
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


    /**
     * local broadcasts
     */

    private BroadcastReceiver localIntentReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SelectPublicTransportProviderDialog.NEW_NETWORK_PROVIDER)) {
                buttonPublicTransportProvider.setText(
                        String.format(
                            context.getResources().getString(R.string.buttonPublicTransportProvider),
                            PTHelper.getNetworkProviderName(
                                context, settingsManagerInstance.getServerSettings().getSelectedPublicTransportProvider()))
                        );
            }
        }
    };


    /**
     * dialogs
     */

    public static class NewServerDialog extends DialogFragment implements ServerStatusListener {
        private static final String KEY_SERVER_URL = "serverUrl";

        // Store instance variables
        private ServerStatusManager serverStatusManagerInstance;
        private String serverUrl;

        private EditText editServerURL;

        public static NewServerDialog newInstance() {
            NewServerDialog dialog = new NewServerDialog();
            return dialog;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            serverStatusManagerInstance = ServerStatusManager.getInstance(context);
        }

        @Override public void onDetach(){
            super.onDetach();
            serverStatusManagerInstance.invalidateServerStatusRequest((NewServerDialog) this);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            if(savedInstanceState != null) {
                serverUrl = savedInstanceState.getString(KEY_SERVER_URL);
            } else {
                serverUrl = SettingsManager.getInstance().getServerSettings().getServerURL();
            }

            editServerURL = new EditText(NewServerDialog.this.getContext());
            LayoutParams lp = new LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            editServerURL.setLayoutParams(lp);
            editServerURL.setText(serverUrl);
            editServerURL.selectAll();
            editServerURL.setHint(getResources().getString(R.string.editHintServerURL));
            editServerURL.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
            editServerURL.setImeOptions(EditorInfo.IME_ACTION_DONE);
            editServerURL.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        InputMethodManager imm =(InputMethodManager) GlobalInstance.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        tryToContactServer();
                        return true;
                    }
                    return false;
                }
            });
            editServerURL.addTextChangedListener(new TextChangedListener<EditText>(editServerURL) {
                @Override public void onTextChanged(EditText view, Editable s) {
                    serverUrl = view.getText().toString();
                }
            });

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.newServerDialogTitle))
                .setView(editServerURL)
                .setPositiveButton(
                        getResources().getString(R.string.dialogDone),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setNeutralButton(
                        getResources().getString(R.string.dialogDefault),
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
                        tryToContactServer();
                    }
                });
                // neutral button
                Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                buttonNeutral.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        editServerURL.setText(BuildConfig.SERVER_URL);
                    }
                });
                buttonNeutral.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View v) {
                        editServerURL.setText(BuildConfig.SERVER_URL_DEV);
                        return true;
                    }
                });
                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }
        }

        @Override public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            savedInstanceState.putString(KEY_SERVER_URL, serverUrl);
        }

        private void tryToContactServer() {
            serverStatusManagerInstance.requestServerStatus((NewServerDialog) this, serverUrl);
        }

    	@Override public void serverStatusRequestFinished(Context context, int returnCode, ServerInstance serverInstance) {
            if (returnCode == Constants.RC.OK
                    && serverInstance != null) {
                if (isAdded() && SettingsManager.getInstance().getServerSettings().getSelectedMap() == null) {
                    SelectMapDialog.newInstance()
                        .show(getActivity().getSupportFragmentManager(), "SelectMapDialog");
                }
                Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                dismiss();
            } else {
                if (isAdded()) {
                    SimpleMessageDialog.newInstance(
                            ServerUtility.getErrorMessageForReturnCode(context, returnCode))
                        .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
                }
            }
        }
    }


    public static class SelectShakeIntensityDialog extends DialogFragment {

        // Store instance variables
        private SettingsManager settingsManagerInstance;

        public static SelectShakeIntensityDialog newInstance() {
            SelectShakeIntensityDialog selectShakeIntensityDialogInstance = new SelectShakeIntensityDialog();
            return selectShakeIntensityDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            settingsManagerInstance = SettingsManager.getInstance();
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            int indexOfSelectedShakeIntensity = -1;
            String[] formattedShakeIntensityArray = new String[Constants.ShakeIntensityValueArray.length];
            for (int i=0; i<Constants.ShakeIntensityValueArray.length; i++) {
                switch (Constants.ShakeIntensityValueArray[i]) {
                    case Constants.SHAKE_INTENSITY.DISABLED:
                        formattedShakeIntensityArray[i] = getResources().getString(R.string.shakeIntensityDisabled);
                        break;
                    case Constants.SHAKE_INTENSITY.VERY_WEAK:
                        formattedShakeIntensityArray[i] = getResources().getString(R.string.shakeIntensityVeryWeak);
                        break;
                    case Constants.SHAKE_INTENSITY.WEAK:
                        formattedShakeIntensityArray[i] = getResources().getString(R.string.shakeIntensityWeak);
                        break;
                    case Constants.SHAKE_INTENSITY.MEDIUM:
                        formattedShakeIntensityArray[i] = getResources().getString(R.string.shakeIntensityMedium);
                        break;
                    case Constants.SHAKE_INTENSITY.STRONG:
                        formattedShakeIntensityArray[i] = getResources().getString(R.string.shakeIntensityStrong);
                        break;
                    case Constants.SHAKE_INTENSITY.VERY_STRONG:
                        formattedShakeIntensityArray[i] = getResources().getString(R.string.shakeIntensityVeryStrong);
                        break;
                    default:
                        formattedShakeIntensityArray[i] = String.valueOf(Constants.ShakeIntensityValueArray[i]);
                        break;
                }
                if (Constants.ShakeIntensityValueArray[i] == settingsManagerInstance.getSelectedShakeIntensity()) {
                    indexOfSelectedShakeIntensity = i;
                }
            }

            return  new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectShakeIntensityDialogTitle))
                .setSingleChoiceItems(
                        formattedShakeIntensityArray,
                        indexOfSelectedShakeIntensity,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                int selectedShakeIntensity = -1;
                                try {
                                    selectedShakeIntensity = Constants.ShakeIntensityValueArray[which];
                                } catch (ArrayIndexOutOfBoundsException e) {
                                    selectedShakeIntensity = -1;
                                } finally {
                                    if (selectedShakeIntensity > -1) {
                                        settingsManagerInstance.setSelectedShakeIntensity(selectedShakeIntensity);
                                        Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                                    }
                                }
                                dismiss();
                            }
                        })
            .setNegativeButton(
                    getResources().getString(R.string.dialogCancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    })
            .create();
        }
    }

    /*
                                Dialog overwriteExistingSettingsDialog = new AlertDialog.Builder(SettingsActivity.this)
                                    .setMessage(
                                            getResources().getString(R.string.labelOverwriteExistingSettings))
                                    .setCancelable(false)
                                    .setPositiveButton(
                                            getResources().getString(R.string.dialogYes),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            })
                                    .setNegativeButton(
                                            getResources().getString(R.string.dialogNo),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            })
                                    .create();
                                overwriteExistingSettingsDialog.show();

            message = String.format(
                    getResources().getString(R.string.labelExportSuccessful),
                    GlobalInstance.getExportFolder().getAbsolutePath());
        } else {
            message = getResources().getString(R.string.labelExportFailed);
        }
        SimpleMessageDialog.newInstance(message)
            .show(getSupportFragmentManager(), "SimpleMessageDialog");
    */


    public int getLayoutResourceId() {
		return R.layout.activity_settings;
    }

    public void configureToolbarNavigationButton() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
