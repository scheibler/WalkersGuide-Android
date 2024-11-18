package org.walkersguide.android.ui.dialog.edit;

import org.walkersguide.android.tts.TTSWrapper;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.View;


import org.walkersguide.android.R;

import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.LayoutInflater;
import java.util.Locale;
import android.widget.Button;
import android.widget.ImageButton;


public class SpeechRateDialog extends DialogFragment {
    public static final String REQUEST_CHANGE_SPEECH_RATE = "changeSpeechRate";
    public static final String EXTRA_SPEECH_RATE = "speechRate";

    // instance constructors

    public static SpeechRateDialog newInstance(float selectedSpeechRate) {
        SpeechRateDialog dialog = new SpeechRateDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SELECTED_SPEECH_RATE, selectedSpeechRate);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final float[] SPEECH_RATE_PRESETS = new float[]{ 0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f };
    private static final float SPEECH_RATE_INTERVAL = 0.05f;
    private static final String KEY_SELECTED_SPEECH_RATE = "selectedSpeechRate";

    private float selectedSpeechRate;
    private TextView labelSpeechRate;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        selectedSpeechRate = savedInstanceState != null
            ? savedInstanceState.getFloat(KEY_SELECTED_SPEECH_RATE)
            : getArguments().getFloat(KEY_SELECTED_SPEECH_RATE);

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_speech_rate, nullParent);

        ImageButton buttonDecreaseSpeechRate = (ImageButton) view.findViewById(R.id.buttonDecreaseSpeechRate);
        buttonDecreaseSpeechRate.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (selectedSpeechRate - SPEECH_RATE_INTERVAL > 0.01f) {
                    selectedSpeechRate -= SPEECH_RATE_INTERVAL;
                    updateSpeechRateLabel();
                }
            }
        });

        labelSpeechRate = (TextView) view.findViewById(R.id.labelSpeechRate);
        updateSpeechRateLabel();

        ImageButton buttonIncreaseSpeechRate = (ImageButton) view.findViewById(R.id.buttonIncreaseSpeechRate);
        buttonIncreaseSpeechRate.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                selectedSpeechRate += SPEECH_RATE_INTERVAL;
                updateSpeechRateLabel();
            }
        });

        LinearLayout layoutSpeechRatePresets = (LinearLayout) view.findViewById(R.id.layoutSpeechRatePresets);
        layoutSpeechRatePresets.removeAllViews();

        for (final float speechRatePreset : SPEECH_RATE_PRESETS) {
            Button buttonSpeechRatePreset = new Button(getActivity());
            buttonSpeechRatePreset.setText(
                    String.format(
                        Locale.getDefault(), "%1$.2fx", speechRatePreset));

            // Create LayoutParams with width=0dp, height=wrap_content, weight=1.0
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0,      // width: 0dp
                    ViewGroup.LayoutParams.WRAP_CONTENT, // height: wrap_content
                    1.0f);  // weight: 1.0
            buttonSpeechRatePreset.setLayoutParams(params);

            // listener
            buttonSpeechRatePreset.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    selectedSpeechRate = speechRatePreset;
                    updateSpeechRateLabel();
                }
            });

            // Add the Button to the LinearLayout
            layoutSpeechRatePresets.addView(buttonSpeechRatePreset);
        }

        return  new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.buttonSpeechRate))
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.dialogOK),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNeutralButton(
                    getResources().getString(R.string.dialogTest),
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
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {

            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                    Bundle result = new Bundle();
                    result.putSerializable(EXTRA_SPEECH_RATE, selectedSpeechRate);
                    getParentFragmentManager().setFragmentResult(REQUEST_CHANGE_SPEECH_RATE, result);
                }
            });

            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    TTSWrapper.getInstance().testInstruction(selectedSpeechRate);
                }
            });

            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                }
            });
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putFloat(KEY_SELECTED_SPEECH_RATE, selectedSpeechRate);
    }

    private void updateSpeechRateLabel() {
        labelSpeechRate.setText(
                String.format(
                    Locale.getDefault(), "%1$.2fx", selectedSpeechRate));
    }

}
