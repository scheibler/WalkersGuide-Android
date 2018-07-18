package org.walkersguide.android.ui.activity;

import org.walkersguide.android.R;
import org.walkersguide.android.listener.ServerStatusListener;
import org.walkersguide.android.server.ServerStatus;
import org.walkersguide.android.ui.dialog.SelectMapDialog;
import org.walkersguide.android.ui.dialog.SelectPublicTransportProviderDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager.ServerSettings;

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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends AbstractActivity {

    private Button buttonServerURL, buttonServerMap, buttonServerPublicTransportProvider;

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
                SelectMapDialog.newInstance(
                        settingsManagerInstance.getServerSettings().getSelectedMap())
                    .show(getSupportFragmentManager(), "SelectMapDialog");
            }
        });

		buttonServerPublicTransportProvider = (Button) findViewById(R.id.buttonServerPublicTransportProvider);
		buttonServerPublicTransportProvider.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
                SelectPublicTransportProviderDialog.newInstance(
                        settingsManagerInstance.getServerSettings().getSelectedPublicTransportProvider())
                    .show(getSupportFragmentManager(), "SelectPublicTransportProviderDialog");
            }
        });
    }

    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menuItemDirection).setVisible(false);
        menu.findItem(R.id.menuItemLocation).setVisible(false);
        menu.findItem(R.id.menuItemPlanRoute).setVisible(false);
        menu.findItem(R.id.menuItemRequestAddress).setVisible(false);
        menu.findItem(R.id.menuItemSaveCurrentPosition).setVisible(false);
        menu.findItem(R.id.menuItemSettings).setVisible(false);
        menu.findItem(R.id.menuItemInfo).setVisible(false);
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
    }


    public static class NewServerDialog extends DialogFragment implements ServerStatusListener {

        // Store instance variables
        private InputMethodManager imm;
        private ServerStatus serverStatusRequest;
        private EditText editServerURL;

        public static NewServerDialog newInstance() {
            NewServerDialog newServerDialogInstance = new NewServerDialog();
            return newServerDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            serverStatusRequest = null;
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
                        getResources().getString(R.string.dialogNext),
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

        private void tryToContactServer() {
            String serverURL = editServerURL.getText().toString();
            if (serverURL.equals("")) {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageServerURLMissing),
                        Toast.LENGTH_LONG).show();
            } else {
                serverStatusRequest = new ServerStatus(
                        getActivity(), NewServerDialog.this, ServerStatus.ACTION_UPDATE_MANAGEMENT, serverURL, null);
                serverStatusRequest.execute();
            }
        }

        @Override public void statusRequestFinished(int updateAction, int returnCode, String returnMessage) {
            if (returnCode == Constants.ID.OK) {
                ((GlobalInstance) getActivity().getApplicationContext()).setApplicationInBackground(true);
                Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                dismiss();
            } else {
                SimpleMessageDialog.newInstance(returnMessage)
                    .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
            }
        }

        @Override public void onDismiss(final DialogInterface dialog) {
            super.onDismiss(dialog);
            if (serverStatusRequest != null
                    && serverStatusRequest.getStatus() != AsyncTask.Status.FINISHED) {
                serverStatusRequest.cancel();
            }
        }
    }

}
