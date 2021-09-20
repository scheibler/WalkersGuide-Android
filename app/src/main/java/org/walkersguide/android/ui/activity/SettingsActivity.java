package org.walkersguide.android.ui.activity;

import android.widget.LinearLayout.LayoutParams;
import android.widget.LinearLayout;
import timber.log.Timber;
import org.walkersguide.android.ui.dialog.SelectPublicTransportProviderDialog;
import org.walkersguide.android.helper.FileUtility;
import org.walkersguide.android.pt.PTHelper;
import org.walkersguide.android.pt.PTHelper.Country;
import android.content.IntentFilter;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.widget.Toolbar;

import android.text.InputType;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;

import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.data.server.AddressProvider;
import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.server.ServerStatusManager.ServerStatusListener;
import org.walkersguide.android.ui.dialog.SelectMapDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog.ChildDialogCloseListener;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.ServerSettings;
import android.content.BroadcastReceiver;
import org.walkersguide.android.database.util.SQLiteHelper;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.database.util.AccessDatabase;
import android.database.SQLException;
import android.app.Activity;
import android.net.Uri;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import android.os.Environment;
import android.content.pm.PackageManager;
import org.walkersguide.android.ui.listener.TextChangedListener;
import android.text.Editable;


public class SettingsActivity extends AbstractToolbarActivity implements ServerStatusListener {

    private Button buttonServerURL, buttonServerMap;
    private Button buttonPublicTransportProvider, buttonAddressProvider;
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

		buttonAddressProvider = (Button) findViewById(R.id.buttonAddressProvider);
		buttonAddressProvider.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
                SelectAddressProviderDialog.newInstance()
                    .show(getSupportFragmentManager(), "SelectAddressProviderDialog");
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
                ActivityCompat.requestPermissions(
                        SettingsActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        SETTINGS_IMPORT_ID);
            }
        });

		Button buttonExportSettings = (Button) findViewById(R.id.buttonExportSettings);
		buttonExportSettings.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
                ActivityCompat.requestPermissions(
                        SettingsActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        SETTINGS_EXPORT_ID);
            }
        });
    }

    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        //menu.findItem(R.id.menuItemDirection).setVisible(false);
        //menu.findItem(R.id.menuItemLocation).setVisible(false);
        return true;
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

        // address provider
        if (Constants.AddressProviderValueArray.length > 1) {
            if (serverSettings.getSelectedAddressProvider() != null) {
                buttonAddressProvider.setText(
                        String.format(
                            getResources().getString(R.string.buttonAddressProvider),
                            serverSettings.getSelectedAddressProvider().getName())
                        );
            } else {
                buttonAddressProvider.setText(
                        getResources().getString(R.string.buttonAddressProviderNoSelection));
            }
            buttonAddressProvider.setVisibility(View.VISIBLE);
        } else {
            buttonAddressProvider.setVisibility(View.GONE);
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


    public static class SelectAddressProviderDialog extends DialogFragment {

        // Store instance variables
        private SettingsManager settingsManagerInstance;

        public static SelectAddressProviderDialog newInstance() {
            SelectAddressProviderDialog selectAddressProviderDialogInstance = new SelectAddressProviderDialog();
            return selectAddressProviderDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            settingsManagerInstance = SettingsManager.getInstance();
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            String[] formattedAddressProviderNameArray = new String[Constants.AddressProviderValueArray.length];
            int indexOfSelectedAddressProvider = -1;
            for (int i=0; i<Constants.AddressProviderValueArray.length; i++) {
                AddressProvider addressProvider = new AddressProvider(getActivity(), Constants.AddressProviderValueArray[i]);
                formattedAddressProviderNameArray[i] = addressProvider.getName();
                if (addressProvider.equals(settingsManagerInstance.getServerSettings().getSelectedAddressProvider())) {
                    indexOfSelectedAddressProvider = i;
                }
            }

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectAddressProviderDialogTitle))
                .setSingleChoiceItems(
                        formattedAddressProviderNameArray,
                        indexOfSelectedAddressProvider,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                AddressProvider newAddressProvider = null;
                                try {
                                    newAddressProvider = new AddressProvider(
                                            getActivity(), Constants.AddressProviderValueArray[which]);
                                } catch (IndexOutOfBoundsException e) {
                                    newAddressProvider = null;
                                } finally {
                                    if (newAddressProvider != null) {
                                        settingsManagerInstance.getServerSettings().setSelectedAddressProvider(newAddressProvider);
                                        Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                                    }
                                }
                                dismiss();
                            }
                        }
            )
                .setNegativeButton(
                        getResources().getString(R.string.dialogCancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dismiss();
                            }
                        }
                        )
                .create();
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

    /**
     * import and export settings
     */
    private static final int SETTINGS_IMPORT_ID = 64;
    private static final int SETTINGS_EXPORT_ID = 65;

    @Override public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        for (int i=0; i<permissions.length; i++) {
            if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    switch (requestCode) {
                        case SETTINGS_IMPORT_ID:
                            ImportSettingsDialog.newInstance()
                                .show(getSupportFragmentManager(), "ImportSettingsDialog");
                            break;
                        case SETTINGS_EXPORT_ID:
                            if (GlobalInstance.getExportFolder().exists()) {
                                Dialog overwriteExistingSettingsDialog = new AlertDialog.Builder(SettingsActivity.this)
                                    .setMessage(
                                            getResources().getString(R.string.labelOverwriteExistingSettings))
                                    .setCancelable(false)
                                    .setPositiveButton(
                                            getResources().getString(R.string.dialogYes),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    FileUtility.deleteFolder(GlobalInstance.getExportFolder());
                                                    exportSettingsAndDatabase();
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
                            } else {
                                exportSettingsAndDatabase();
                            }
                            break;
                    }
                } else {
                    SimpleMessageDialog.newInstance(
                            getResources().getString(R.string.labelWriteExternalStoragePermissionDenied))
                        .show(getSupportFragmentManager(), "SimpleMessageDialog");
                }
            }
        }
    }

    private void exportSettingsAndDatabase() {
        GlobalInstance.getExportFolder().mkdirs();
        // export settings
        boolean settingsExportSuccessful = settingsManagerInstance.exportSettings(
                GlobalInstance.getExportSettingsFile());
        // copy database
        boolean databaseExportSuccessful = FileUtility.copyFile(
                GlobalInstance.getInternalDatabaseFile(), GlobalInstance.getExportDatabaseFile());
        // show message
        String message = null;
        if (settingsExportSuccessful && databaseExportSuccessful) {
            message = String.format(
                    getResources().getString(R.string.labelExportSuccessful),
                    GlobalInstance.getExportFolder().getAbsolutePath());
        } else {
            message = getResources().getString(R.string.labelExportFailed);
        }
        SimpleMessageDialog.newInstance(message)
            .show(getSupportFragmentManager(), "SimpleMessageDialog");
    }


    public static class ImportSettingsDialog extends DialogFragment implements ChildDialogCloseListener {

        private SettingsManager settingsManagerInstance;

        private TextView labelImport;

        public static ImportSettingsDialog newInstance() {
            ImportSettingsDialog importSettingsDialogInstance = new ImportSettingsDialog();
            return importSettingsDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            settingsManagerInstance = SettingsManager.getInstance();
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_single_text_view, nullParent);
            labelImport = (TextView) view.findViewById(R.id.label);

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.importSettingsDialogTitle))
                .setView(view)
                .setPositiveButton(
                        getResources().getString(R.string.dialogImport),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setNegativeButton(
                        getResources().getString(R.string.dialogClose),
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
                        final AlertDialog dialog = (AlertDialog)getDialog();
                        if(dialog != null) {
                            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            buttonPositive.setClickable(false);
                        }
                        String message = null;
                        if (importSettingsAndDatabase()) {
                            message = getResources().getString(R.string.labelImportSuccessful);
                            // remove imported settings folder
                            FileUtility.deleteFolder(GlobalInstance.getExportFolder());
                            // reset cache
                            ServerStatusManager.getInstance(GlobalInstance.getContext()).setCachedServerInstance(null);
                        } else {
                            message = getResources().getString(R.string.labelImportFailed);
                        }
                        // show result
                        SimpleMessageDialog simpleMessageDialog = SimpleMessageDialog.newInstance(message);
                        simpleMessageDialog.setTargetFragment(ImportSettingsDialog.this, 1);
                        simpleMessageDialog.show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
                    }
                });

                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.dismiss();
                    }
                });

                // check for import data availibility
                buttonPositive.setVisibility(View.GONE);
                if (! GlobalInstance.getExportFolder().exists()) {
                    labelImport.setText(
                            String.format(
                                getResources().getString(R.string.labelImportDataNotFound),
                                GlobalInstance.getExportFolder().getAbsolutePath())
                            );
                } else if (! GlobalInstance.getExportSettingsFile().exists()) {
                    labelImport.setText(
                            String.format(
                                getResources().getString(R.string.labelImportDataNotFound),
                                GlobalInstance.getExportSettingsFile().getAbsolutePath())
                            );
                } else if (! GlobalInstance.getExportDatabaseFile().exists()) {
                    labelImport.setText(
                            String.format(
                                getResources().getString(R.string.labelImportDataNotFound),
                                GlobalInstance.getExportDatabaseFile().getAbsolutePath())
                            );
                } else {
                    labelImport.setText(
                            getResources().getString(R.string.labelImportReady));
                    buttonPositive.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override public void childDialogClosed() {
            Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            dismiss();
        }

        private boolean importSettingsAndDatabase() {
            // import settings
            boolean settingsImportSuccessful = settingsManagerInstance.importSettings(
                    GlobalInstance.getExportSettingsFile());
            Timber.d("settings import: %1$s", settingsImportSuccessful);
            if (! settingsImportSuccessful) {
                return false;
            }
            // restore database
            boolean copyDatabaseSuccessful = FileUtility.copyFile(
                    GlobalInstance.getExportDatabaseFile(), GlobalInstance.getTempDatabaseFile());
            Timber.d("copy database successful: %1$s", copyDatabaseSuccessful);
            if (! copyDatabaseSuccessful) {
                return false;
            }
            // close database
            AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance();
            accessDatabaseInstance.close();
            // remove old database
            File databaseFileToBeReplaced = GlobalInstance.getInternalDatabaseFile();
            if (databaseFileToBeReplaced.exists()) {
                databaseFileToBeReplaced.delete();
            }
            // rename temp database file and open again
            GlobalInstance.getTempDatabaseFile().renameTo(GlobalInstance.getInternalDatabaseFile());
            accessDatabaseInstance.open();
            return true;
        }
    }

    public int getLayoutResourceId() {
		return R.layout.activity_settings;
    }

    public void configureToolbarNavigationButton() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
