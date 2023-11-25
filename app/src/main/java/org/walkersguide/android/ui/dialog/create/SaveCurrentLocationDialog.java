package org.walkersguide.android.ui.dialog.create;

import org.walkersguide.android.ui.interfaces.ViewChangedListener;
import org.walkersguide.android.ui.dialog.select.SelectProfileFromMultipleSourcesDialog;
import org.walkersguide.android.ui.view.ProfileView;
import androidx.core.view.ViewCompat;
import org.walkersguide.android.ui.view.EditTextAndClearInputButton;
import org.walkersguide.android.ui.UiHelper;
import org.walkersguide.android.ui.dialog.toolbar.LocationSensorDetailsDialog;
import androidx.appcompat.app.AlertDialog;

import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.inputmethod.EditorInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.Toast;



import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import android.text.TextUtils;
import org.json.JSONException;
import org.walkersguide.android.database.profile.StaticProfile;
import android.app.Dialog;
import org.walkersguide.android.util.GlobalInstance;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.widget.TextView;
import android.content.Context;
import org.walkersguide.android.database.DatabaseProfile;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentResultListener;
import org.walkersguide.android.data.Profile;


public class SaveCurrentLocationDialog extends DialogFragment implements FragmentResultListener {
    public static final String REQUEST_SAVE_CURRENT_LOCATION = "saveCurrentLocation";
    public static final String EXTRA_CURRENT_LOCATION = "currentLocation";


    public static SaveCurrentLocationDialog addToDatabaseProfile() {
        SaveCurrentLocationDialog dialog = new SaveCurrentLocationDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_DIALOG_TITLE, GlobalInstance.getStringResource(R.string.saveCurrentLocationDialogTitle));
        args.putSerializable(KEY_SEND_RESULT_BUNDLE, false);
        dialog.setArguments(args);
        return dialog;
    }

    public static SaveCurrentLocationDialog sendResultBundle(String dialogTitle) {
        SaveCurrentLocationDialog dialog = new SaveCurrentLocationDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_DIALOG_TITLE, dialogTitle);
        args.putSerializable(KEY_SEND_RESULT_BUNDLE, true);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_DIALOG_TITLE = "dialogTitle";
    private static final String KEY_SEND_RESULT_BUNDLE = "sendResultBundle";
    private static final String KEY_TARGET_DATABASE_PROFILE = "targetDatabaseProfile";

    private boolean sendResultBundle;

    private EditTextAndClearInputButton layoutName;
    private TextView labelGPSSignal;
    private ProfileView layoutTargetDatabaseProfile;


	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectProfileFromMultipleSourcesDialog.REQUEST_SELECT_PROFILE, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectProfileFromMultipleSourcesDialog.REQUEST_SELECT_PROFILE)) {
            SelectProfileFromMultipleSourcesDialog.Target profileTarget = (SelectProfileFromMultipleSourcesDialog.Target)
                bundle.getSerializable(SelectProfileFromMultipleSourcesDialog.EXTRA_TARGET);
            Profile selectedProfile = (Profile) bundle.getSerializable(SelectProfileFromMultipleSourcesDialog.EXTRA_PROFILE);
            if (profileTarget == SelectProfileFromMultipleSourcesDialog.Target.SAVE_CURRENT_LOCATION
                    && selectedProfile instanceof DatabaseProfile) {
                setTargetDatabaseProfile((DatabaseProfile) selectedProfile);
            }
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        sendResultBundle = getArguments().getBoolean(KEY_SEND_RESULT_BUNDLE);

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_save_current_location, nullParent);

        layoutName = (EditTextAndClearInputButton) view.findViewById(R.id.layoutName);
        layoutName.setLabelText(getResources().getString(R.string.labelName));
        layoutName.setEditorAction(
                EditorInfo.IME_ACTION_DONE,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        tryToSaveCurrentLocation();
                    }
                });

        labelGPSSignal = (TextView) view.findViewById(R.id.labelGPSSignal);
        labelGPSSignal.setTag(null);
        labelGPSSignal.setText(
                getResources().getString(R.string.messageSearchLocationInProgress));
        ViewCompat.setAccessibilityLiveRegion(
                labelGPSSignal, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);

        layoutTargetDatabaseProfile = (ProfileView) view.findViewById(R.id.layoutTargetDatabaseProfile);
        layoutTargetDatabaseProfile.setOnProfileDefaultActionListener(new ProfileView.OnProfileDefaultActionListener() {
            @Override public void onProfileDefaultActionClicked(Profile profile) {
                SelectProfileFromMultipleSourcesDialog.newInstance(
                        SelectProfileFromMultipleSourcesDialog.Target.SAVE_CURRENT_LOCATION)
                    .show(getChildFragmentManager(), "SelectProfileFromMultipleSourcesDialog");
            }
        });
        setTargetDatabaseProfile(
                savedInstanceState != null
                ? (DatabaseProfile) savedInstanceState.getSerializable(KEY_TARGET_DATABASE_PROFILE)
                : StaticProfile.pinnedPointsAndRoutes());

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getArguments().getString(KEY_DIALOG_TITLE))
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
                    tryToSaveCurrentLocation();
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

        IntentFilter filter = new IntentFilter();
        filter.addAction(PositionManager.ACTION_NEW_GPS_LOCATION);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);

        layoutName.showKeyboard();
    }

    @Override public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            UiHelper.hideKeyboard(this);
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(KEY_TARGET_DATABASE_PROFILE, layoutTargetDatabaseProfile.getProfile());
    }

    private void setTargetDatabaseProfile(DatabaseProfile profile) {
        layoutTargetDatabaseProfile.configureAsSingleObject(profile);
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PositionManager.ACTION_NEW_GPS_LOCATION)) {
                GPS gpsLocation = (GPS) intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION);
                if (gpsLocation != null && gpsLocation.getAccuracy() != null) {
                    labelGPSSignal.setTag(gpsLocation);
                    labelGPSSignal.setText(
                            String.format(
                                context.getResources().getString(R.string.labelGPSSignal),
                                Math.round(gpsLocation.getAccuracy()),
                                gpsLocation.formatTimestamp(
                                    GlobalInstance.getStringResource(R.string.labelGPSTime)))
                            );
                    // only tell me once
                    ViewCompat.setAccessibilityLiveRegion(
                            labelGPSSignal, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
                }
            }
        }
    };

    private void tryToSaveCurrentLocation() {
        // name
        String name = layoutName.getInputText();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageNameMissing),
                    Toast.LENGTH_LONG).show();
            return;
        }
        UiHelper.hideKeyboard(SaveCurrentLocationDialog.this);

        // check if current location is available
        GPS currentLocation = (GPS) labelGPSSignal.getTag();
        if (currentLocation == null) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.errorNoLocationFound),
                    Toast.LENGTH_LONG).show();
            return;
        }
        currentLocation.rename(name);

        if (sendResultBundle) {
            Bundle result = new Bundle();
            result.putSerializable(EXTRA_CURRENT_LOCATION, currentLocation);
            getParentFragmentManager().setFragmentResult(REQUEST_SAVE_CURRENT_LOCATION, result);
            dismiss();

        } else {
            Profile selectedProfile = layoutTargetDatabaseProfile.getProfile();
            if (selectedProfile instanceof DatabaseProfile
                    && ((DatabaseProfile) selectedProfile).addObject(currentLocation)) {
                Toast.makeText(
                        getActivity(),
                        selectedProfile.equals(StaticProfile.pinnedPointsAndRoutes())
                        ? getResources().getString(R.string.messageCurrentLocationAddedToPinnedPointsAndRoutesSuccessful)
                        : String.format(
                            getResources().getString(R.string.messageCurrentLocationAddedToCollectionSuccessful),
                            selectedProfile.getName()),
                        Toast.LENGTH_LONG).show();
                ViewChangedListener.sendObjectWithIdListChangedBroadcast();
                dismiss();
            } else {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.errorSavePointFailed),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

}
