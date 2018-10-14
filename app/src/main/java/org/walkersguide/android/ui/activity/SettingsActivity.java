package org.walkersguide.android.ui.activity;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

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
import android.widget.Toast;

import java.io.File;

import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.data.server.AddressProvider;
import org.walkersguide.android.data.server.PublicTransportProvider;
import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.listener.ChildDialogCloseListener;
import org.walkersguide.android.listener.ServerStatusListener;
import org.walkersguide.android.listener.SettingsImportListener;
import org.walkersguide.android.R;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.ui.dialog.SelectMapDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsImport;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.ServerSettings;


public class SettingsActivity extends AbstractActivity {

    private Button buttonServerURL, buttonServerMap, buttonServerPublicTransportProvider;
    private Button buttonAddressProvider;
    private Button buttonShakeIntensity;
    private Switch buttonEnableTextInputHistory, buttonLogQueriesOnServer, buttonShowDevelopmentMaps;

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

		buttonServerPublicTransportProvider = (Button) findViewById(R.id.buttonServerPublicTransportProvider);
		buttonServerPublicTransportProvider.setOnClickListener(new View.OnClickListener() {
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

        buttonLogQueriesOnServer = (Switch) findViewById(R.id.buttonLogQueriesOnServer);
        buttonLogQueriesOnServer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                if (isChecked != settingsManagerInstance.getServerSettings().getLogQueriesOnServer()) {
                    settingsManagerInstance.getServerSettings().setLogQueriesOnServer(isChecked);
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

        // development settings
        buttonShowDevelopmentMaps = (Switch) findViewById(R.id.buttonShowDevelopmentMaps);
        buttonShowDevelopmentMaps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                if (isChecked != settingsManagerInstance.getGeneralSettings().getShowDevelopmentMaps()) {
                    settingsManagerInstance.getGeneralSettings().setShowDevelopmentMaps(isChecked);
                }
            }
        });
    }

    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menuItemNextRoutePoint).setVisible(false);
        menu.findItem(R.id.menuItemDirection).setVisible(false);
        menu.findItem(R.id.menuItemLocation).setVisible(false);
        return true;
    }

	@Override public void onResume() {
		super.onResume();
        updateUI();
    }

    private void updateUI() {
        ServerSettings serverSettings = settingsManagerInstance.getServerSettings();
        buttonServerURL.setText(
                String.format(
                    getResources().getString(R.string.buttonServerURL),
                    serverSettings.getServerURL())
                );
        if (serverSettings.getSelectedMap() != null) {
            buttonServerMap.setText(
                    String.format(
                        getResources().getString(R.string.buttonServerMap),
                        serverSettings.getSelectedMap().getName())
                    );
        } else {
            buttonServerMap.setText(
                    getResources().getString(R.string.buttonServerMapNoSelection));
        }
        if (serverSettings.getSelectedPublicTransportProvider() != null) {
            buttonServerPublicTransportProvider.setText(
                    String.format(
                        getResources().getString(R.string.buttonServerPublicTransportProvider),
                        serverSettings.getSelectedPublicTransportProvider().getName())
                    );
        } else {
            buttonServerPublicTransportProvider.setText(
                    getResources().getString(R.string.buttonServerPublicTransportProviderNoSelection));
        }

        // address provider
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
        buttonLogQueriesOnServer.setChecked(settingsManagerInstance.getServerSettings().getLogQueriesOnServer());
        buttonShowDevelopmentMaps.setChecked(settingsManagerInstance.getGeneralSettings().getShowDevelopmentMaps());
    }


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
                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }
            // show keyboard
            new Handler().postDelayed(
                    new Runnable() {
                        @Override public void run() {
                            imm.showSoftInput(editServerURL, InputMethodManager.SHOW_IMPLICIT);
                        }
                    }, 50);
        }

        @Override public void onStop() {
            super.onStop();
            serverStatusManagerInstance.invalidateServerStatusRequest((NewServerDialog) this);
        }

        private void tryToContactServer() {
            String serverURL = editServerURL.getText().toString().trim();
            if (serverURL.equals("")) {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageServerURLMissing),
                        Toast.LENGTH_LONG).show();
            } else {
                serverStatusManagerInstance.requestServerStatus(
                        (NewServerDialog) this, serverURL);
            }
        }

        @Override public void serverStatusRequestFinished(int returnCode, String returnMessage, ServerInstance serverInstance) {
            if (returnCode == Constants.RC.OK) {
                if (settingsManagerInstance.getServerSettings().getSelectedMap() == null) {
                    SelectMapDialog.newInstance()
                        .show(getActivity().getSupportFragmentManager(), "SelectMapDialog");
                }
                Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                dismiss();
            } else {
                SimpleMessageDialog.newInstance(returnMessage)
                    .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
            }
        }
    }


    public static class SelectPublicTransportProviderDialog extends DialogFragment {

        // Store instance variables
        private SettingsManager settingsManagerInstance;
        private ServerStatusManager serverStatusManagerInstance;

        public static SelectPublicTransportProviderDialog newInstance() {
            SelectPublicTransportProviderDialog selectPublicTransportProviderDialogInstance = new SelectPublicTransportProviderDialog();
            return selectPublicTransportProviderDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            settingsManagerInstance = SettingsManager.getInstance(context);
            serverStatusManagerInstance = ServerStatusManager.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            ServerInstance serverInstance = serverStatusManagerInstance.getServerInstance();
            String[] formattedPublicTransportProviderNameArray = new String[0];
            if (serverInstance != null) {
                formattedPublicTransportProviderNameArray = new String[serverInstance.getSupportedPublicTransportProviderList().size()];
            }
            int indexOfSelectedPublicTransportProvider = -1;
            int index = 0;
            if (serverInstance != null) {
                for (PublicTransportProvider provider : serverInstance.getSupportedPublicTransportProviderList()) {
                    formattedPublicTransportProviderNameArray[index] = provider.toString();
                    if (provider.equals(settingsManagerInstance.getServerSettings().getSelectedPublicTransportProvider())) {
                        indexOfSelectedPublicTransportProvider = index;
                    }
                    index += 1;
                }
            }

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectPublicTransportProviderDialogTitle))
                .setSingleChoiceItems(
                        formattedPublicTransportProviderNameArray,
                        indexOfSelectedPublicTransportProvider,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                PublicTransportProvider selectedProvider = null;
                                try {
                                    selectedProvider = serverStatusManagerInstance.getServerInstance().getSupportedPublicTransportProviderList().get(which);
                                } catch (IndexOutOfBoundsException e) {
                                    selectedProvider = null;
                                } finally {
                                    if (selectedProvider != null) {
                                        settingsManagerInstance.getServerSettings().setSelectedPublicTransportProvider(selectedProvider);
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

        // Store instance variables
        private SettingsManager settingsManagerInstance;
        private SettingsImport settingsImportRequest;
        private ServerStatusManager serverStatusManagerInstance;
        private TextView labelDatabaseImport;

        public static ImportSettingsDialog newInstance() {
            ImportSettingsDialog importSettingsDialogInstance = new ImportSettingsDialog();
            return importSettingsDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            settingsManagerInstance = SettingsManager.getInstance(context);
            serverStatusManagerInstance = ServerStatusManager.getInstance(context);
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
                serverStatusManagerInstance.requestServerStatus(
                        null, settingsManagerInstance.getServerSettings().getServerURL());
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
