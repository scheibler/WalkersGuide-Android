    package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.util.SettingsManager;
import android.widget.Toast;
import org.walkersguide.android.tts.TtsSettings;
import org.walkersguide.android.sensor.bearing.BearingSensor;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.angle.bearing.BearingSensorValue;
import org.walkersguide.android.sensor.DeviceSensorManager;
import android.text.InputFilter;
import android.text.Spanned;
import java.lang.NumberFormatException;
import org.walkersguide.android.ui.TextChangedListener;
import org.walkersguide.android.ui.UiHelper;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;


import android.os.Bundle;


import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TextView;



import org.walkersguide.android.R;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.EditText;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import java.util.Locale;
import timber.log.Timber;


public class TtsSettingsDialog extends DialogFragment {


    // instance constructors

    public static TtsSettingsDialog newInstance() {
        TtsSettingsDialog dialog = new TtsSettingsDialog();
        return dialog;
    }


    // dialog
    private SettingsManager settingsManagerInstance;

    private SwitchCompat switchAnnouncementsEnabled;
    private EditText editDistanceAnnouncementInterval;


	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsManagerInstance = SettingsManager.getInstance();
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        TtsSettings ttsSettings = settingsManagerInstance.getTtsSettings();

        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_tts_settings, nullParent);

        switchAnnouncementsEnabled = (SwitchCompat) view.findViewById(R.id.switchAnnouncementsEnabled);
        switchAnnouncementsEnabled.setChecked(ttsSettings.getAnnouncementsEnabled());
        switchAnnouncementsEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                Timber.d("onCheckedChanged: %1$s", isChecked);
                TtsSettings ttsSettings = settingsManagerInstance.getTtsSettings();
                if (ttsSettings.getAnnouncementsEnabled() != isChecked) {
                    ttsSettings.setAnnouncementsEnabled(isChecked);
                    settingsManagerInstance.setTtsSettings(ttsSettings);
                }
            }
        });

        editDistanceAnnouncementInterval = (EditText) view.findViewById(R.id.editDistanceAnnouncementInterval);
        editDistanceAnnouncementInterval.setText(
                String.valueOf(ttsSettings.getDistanceAnnouncementInterval()));
        editDistanceAnnouncementInterval.selectAll();
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
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UiHelper.hideKeyboard(TtsSettingsDialog.this);
                    return true;
                }
                return false;
            }
        });

        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.ttsSettingsDialogTitle))
            .setView(view)
            .setNegativeButton(
                    getResources().getString(R.string.dialogClose),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .create();
    }

}
