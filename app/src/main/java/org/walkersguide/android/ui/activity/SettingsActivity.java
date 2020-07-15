package org.walkersguide.android.ui.activity;

import org.walkersguide.android.ui.dialog.SelectPublicTransportProviderDialog;
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

import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;

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
import org.walkersguide.android.util.SettingsImport;
import org.walkersguide.android.util.SettingsImport.SettingsImportListener;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.ServerSettings;
import android.content.BroadcastReceiver;


public class SettingsActivity extends AbstractActivity implements ServerStatusListener {

    private Button buttonServerURL, buttonServerMap;
    private Button buttonPublicTransportProvider, buttonAddressProvider;
    private Button buttonShakeIntensity;
    private Switch buttonEnableTextInputHistory;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

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
                if (isChecked != settingsManagerInstance.getGeneralSettings().getEnableTextInputHistory()) {
                    settingsManagerInstance.getGeneralSettings().setEnableTextInputHistory(isChecked);
                    if (! isChecked) {
                        settingsManagerInstance.getSearchTermHistory().clearSearchTermList();
                    }
                }
            }
        });

        // import and export settings
		Button buttonImportSettings = (Button) findViewById(R.id.buttonImportSettings);
		buttonImportSettings.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
                ImportSettingsDialog.newInstance()
                    .show(getSupportFragmentManager(), "ImportSettingsDialog");
            }
        });
    }

    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menuItemDirection).setVisible(false);
        menu.findItem(R.id.menuItemLocation).setVisible(false);
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
        switch (settingsManagerInstance.getGeneralSettings().getShakeIntensity()) {
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
                shakeIntensityName = String.valueOf(settingsManagerInstance.getGeneralSettings().getShakeIntensity());
                break;
        }
        buttonShakeIntensity.setText(
                String.format(
                    "%1$s: %2$s",
                    getResources().getString(R.string.buttonShakeIntensity),
                    shakeIntensityName)
                );

        // privacy and development settings
        buttonEnableTextInputHistory.setChecked(settingsManagerInstance.getGeneralSettings().getEnableTextInputHistory());

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

        // Store instance variables
        private InputMethodManager imm;
        private ServerStatusManager serverStatusManagerInstance;
        private SettingsManager settingsManagerInstance;
        private EditText editServerURL;

        public static NewServerDialog newInstance() {
            NewServerDialog newServerDialogInstance = new NewServerDialog();
            return newServerDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            serverStatusManagerInstance = ServerStatusManager.getInstance(context);
            settingsManagerInstance = SettingsManager.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_single_edit_text, nullParent);

            editServerURL = (EditText) view.findViewById(R.id.editInput);
            editServerURL.setHint(getResources().getString(R.string.editHintServerURL));
            editServerURL.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
            editServerURL.setImeOptions(EditorInfo.IME_ACTION_DONE);
            editServerURL.setText(
                    settingsManagerInstance.getServerSettings().getServerURL());
            editServerURL.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        tryToContactServer();
                        return true;
                    }
                    return false;
                }
            });

            ImageButton buttonDelete = (ImageButton) view.findViewById(R.id.buttonDelete);
            buttonDelete.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    // clear edit text
                    editServerURL.setText("");
                    // show keyboard
                    imm.showSoftInput(editServerURL, InputMethodManager.SHOW_IMPLICIT);
                }
            });

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.newServerDialogTitle))
                .setView(view)
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

        @Override public void onStop() {
            super.onStop();
            serverStatusManagerInstance.invalidateServerStatusRequest((NewServerDialog) this);
        }

        private void tryToContactServer() {
            // hide keyboard
            imm.hideSoftInputFromWindow(editServerURL.getWindowToken(), 0);
            // 
            serverStatusManagerInstance.requestServerStatus(
                    (NewServerDialog) this, editServerURL.getText().toString().trim());
        }

    	@Override public void serverStatusRequestFinished(Context context, int returnCode, ServerInstance serverInstance) {
            if (returnCode == Constants.RC.OK
                    && serverInstance != null) {
                if (isAdded() && settingsManagerInstance.getServerSettings().getSelectedMap() == null) {
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
            settingsManagerInstance = SettingsManager.getInstance(context);
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
            settingsManagerInstance = SettingsManager.getInstance(context);
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
                if (Constants.ShakeIntensityValueArray[i] == settingsManagerInstance.getGeneralSettings().getShakeIntensity()) {
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
                                        settingsManagerInstance.getGeneralSettings().setShakeIntensity(selectedShakeIntensity);
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


    public static class ImportSettingsDialog extends DialogFragment implements SettingsImportListener, ChildDialogCloseListener {
        private static final String DATABASE_FILE_TO_IMPORT = "walkersguide.db";

        private SettingsManager settingsManagerInstance;
        private SettingsImport settingsImportRequest;

        private TextView labelDatabaseImport;

        public static ImportSettingsDialog newInstance() {
            ImportSettingsDialog importSettingsDialogInstance = new ImportSettingsDialog();
            return importSettingsDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            settingsManagerInstance = SettingsManager.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_single_text_view, nullParent);
            labelDatabaseImport = (TextView) view.findViewById(R.id.label);

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
                        tryToImportSettings();
                    }
                });
                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
                // database file label
                File databaseFileToImport = new File(
                        getActivity().getExternalFilesDir(null), DATABASE_FILE_TO_IMPORT);
                if (databaseFileToImport.exists()) {
                    labelDatabaseImport.setText(
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelImportDatabase),
                                databaseFileToImport.getAbsolutePath())
                            );
                    buttonPositive.setVisibility(View.VISIBLE);
                } else {
                    labelDatabaseImport.setText(
                            getResources().getString(R.string.labelImportDatabaseNotFound));
                    buttonPositive.setVisibility(View.GONE);
                }
            }
        }

        private void tryToImportSettings() {
            // cancel previous request
            if (settingsImportRequest != null
                    && settingsImportRequest.getStatus() != AsyncTask.Status.FINISHED) {
                settingsImportRequest.cancel();
            }
            // change positive button label
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setText(getResources().getString(R.string.dialogCancel));
            }
            settingsImportRequest = new SettingsImport(
                    getActivity(), ImportSettingsDialog.this, new File(getActivity().getExternalFilesDir(null), DATABASE_FILE_TO_IMPORT));
            settingsImportRequest.execute();
        }

    	@Override public void settingsImportFinished(int returnCode, String returnMessage) {
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setText(getResources().getString(R.string.dialogImport));
            }
            SimpleMessageDialog simpleMessageDialog = SimpleMessageDialog.newInstance(returnMessage);
            if (returnCode == Constants.RC.OK) {
                simpleMessageDialog.setTargetFragment(ImportSettingsDialog.this, 1);
            }
            simpleMessageDialog.show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
            // re-query server information (maps, public transport providers, ...)
            if (returnCode == Constants.RC.OK) {
                ServerStatusManager.getInstance(getActivity()).setCachedServerInstance(null);
            }
        }

        @Override public void childDialogClosed() {
            Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            dismiss();
        }

        @Override public void onDismiss(final DialogInterface dialog) {
            super.onDismiss(dialog);
            if (settingsImportRequest != null
                    && settingsImportRequest.getStatus() != AsyncTask.Status.FINISHED) {
                settingsImportRequest.cancel();
            }
        }
    }

}
