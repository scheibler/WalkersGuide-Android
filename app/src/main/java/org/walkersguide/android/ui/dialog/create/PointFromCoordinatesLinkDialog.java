package org.walkersguide.android.ui.dialog.create;

import org.walkersguide.android.ui.view.EditTextAndClearInputButton;
import org.walkersguide.android.ui.UiHelper;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.inputmethod.EditorInfo;
import android.view.View;

import android.widget.Button;
import android.widget.Toast;

import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.R;
import android.text.TextUtils;
import android.text.InputType;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.lang.NumberFormatException;
import android.content.ClipboardManager;
import android.content.ClipData;
import org.json.JSONException;

public class PointFromCoordinatesLinkDialog extends DialogFragment {
    public static final String REQUEST_FROM_COORDINATES_LINK = "fromCoordinatesLink";
    public static final String EXTRA_COORDINATES = "corrdinates";


    // instance constructors

    public static PointFromCoordinatesLinkDialog newInstance() {
        PointFromCoordinatesLinkDialog dialog = new PointFromCoordinatesLinkDialog();
        return dialog;
    }


    // dialog
    private static final String KEY_LINK_URL = "linkUrl";

    private EditTextAndClearInputButton layoutLinkUrl;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        String linkUrl;
        if(savedInstanceState != null) {
            linkUrl = savedInstanceState.getString(KEY_LINK_URL);
        } else {
            linkUrl = "";
            // try to get coordinates url from clipboard
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = clipboard.getPrimaryClip();
            if (clipData != null) {
                ClipData.Item clipItem = clipData.getItemAt(0);
                if (clipItem != null) {
                    String clipText = clipItem.coerceToText(getActivity()).toString();
                    if (clipText.contains(LINK_APPLE_MAPS)
                            || clipText.contains(LINK_GOOGLE_MAPS)
                            || clipText.contains(LINK_OSM_AND)
                            || clipText.contains(LINK_OSM_ORG)) {
                        linkUrl = clipText;
                    }
                }
            }
        }

        layoutLinkUrl = new EditTextAndClearInputButton(getActivity());
        layoutLinkUrl.setHint(getResources().getString(R.string.editHintMapLinkURL));
        layoutLinkUrl.setInputText(linkUrl);
        layoutLinkUrl.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        layoutLinkUrl.setEditorAction(
                EditorInfo.IME_ACTION_DONE,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        tryToExtractCoordinates();
                    }
                });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.pointFromCoordinatesLinkDialogTitle))
            .setView(layoutLinkUrl)
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
                    tryToExtractCoordinates();
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
        savedInstanceState.putString(KEY_LINK_URL, layoutLinkUrl.getInputText());
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            UiHelper.hideKeyboard(this);
        }
    }


    // extract coordinates
    private static final String LINK_APPLE_MAPS = "maps.apple.com";
    private static final String LINK_GOOGLE_MAPS = "maps.google.com";
    private static final String LINK_OSM_AND = "osmand.net";
    private static final String LINK_OSM_ORG = "openstreetmap.org";

    private void tryToExtractCoordinates() {
        Double latitude = null, longitude = null;
        String errorMessage = null;

        String linkUrl = layoutLinkUrl.getInputText();
        if (TextUtils.isEmpty(linkUrl)) {
            errorMessage = getResources().getString(R.string.messageLinkUrlMissing);

        } else if (linkUrl.contains(LINK_APPLE_MAPS)) {

            Pattern appleMapsPattern = Pattern.compile(
                    "https?://maps\\.apple\\.com/?\\?.*ll=([0-9.]+),([0-9.]+)");
            Matcher appleMapsMatcher = appleMapsPattern.matcher(linkUrl);
            if (appleMapsMatcher.find()) {
                try {
                    latitude = Double.valueOf(appleMapsMatcher.group(1));
                    longitude = Double.valueOf(appleMapsMatcher.group(2));
                } catch (NumberFormatException e) {}
            } else {
                errorMessage = getResources().getString(R.string.messageAppleMapsUrlFormatUnknown);
            }

        } else if (linkUrl.contains(LINK_GOOGLE_MAPS)) {

            Pattern googleMapsPattern = Pattern.compile(
                    "https?://maps\\.google\\.com/maps\\?q=([0-9.]+)(%2[Cc]|,)([0-9.]+)");
            Matcher googleMapsMatcher = googleMapsPattern.matcher(linkUrl);
            if (googleMapsMatcher.find()) {
                try {
                    latitude = Double.valueOf(googleMapsMatcher.group(1));
                    longitude = Double.valueOf(googleMapsMatcher.group(3));
                } catch (NumberFormatException e) {}
            } else {
                errorMessage = getResources().getString(R.string.messageGoogleMapsUrlFormatUnknown);
            }

        } else if (linkUrl.contains(LINK_OSM_AND)) {

            Pattern osmAndPattern = Pattern.compile(
                    "https?://osmand\\.net/go\\?lat=([0-9.]+)&lon=([0-9.]+)");
            Matcher osmAndMatcher = osmAndPattern.matcher(linkUrl);
            if (osmAndMatcher.find()) {
                try {
                    latitude = Double.valueOf(osmAndMatcher.group(1));
                    longitude = Double.valueOf(osmAndMatcher.group(2));
                } catch (NumberFormatException e) {}
            } else {
                errorMessage = getResources().getString(R.string.messageOsmAndUrlFormatUnknown);
            }

        } else if (linkUrl.contains(LINK_OSM_ORG)) {

            Pattern osmOrgPattern = Pattern.compile(
                    "https?://www\\.openstreetmap\\.org/\\?mlat=([0-9.]+)&mlon=([0-9.]+)");
            Matcher osmOrgMatcher = osmOrgPattern.matcher(linkUrl);
            if (osmOrgMatcher.find()) {
                try {
                    latitude = Double.valueOf(osmOrgMatcher.group(1));
                    longitude = Double.valueOf(osmOrgMatcher.group(2));
                } catch (NumberFormatException e) {}
            } else {
                errorMessage = getResources().getString(R.string.messageOsmOrgUrlFormatUnknown);
            }

        } else {
            errorMessage = getResources().getString(R.string.messageLinkUrlFormatUnknown);
        }

        // try to build point
        GPS extractedCoordinates = null;
        if (latitude != null && longitude != null) {
            try {
                extractedCoordinates = new GPS.Builder(latitude, longitude).build();
            } catch (JSONException e) {
                errorMessage = getResources().getString(R.string.messageLinkContainsInvalidCoordinates);
            }
        }

        // return point or show error message
        if (extractedCoordinates != null) {
            Bundle result = new Bundle();
            result.putSerializable(EXTRA_COORDINATES, extractedCoordinates);
            getParentFragmentManager().setFragmentResult(REQUEST_FROM_COORDINATES_LINK, result);
            dismiss();
        } else if (errorMessage != null) {
            Toast.makeText(
                    getActivity(), errorMessage, Toast.LENGTH_LONG)
                .show();
        }
    }

}
