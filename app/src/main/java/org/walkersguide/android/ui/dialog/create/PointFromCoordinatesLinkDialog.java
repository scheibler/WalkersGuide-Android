package org.walkersguide.android.ui.dialog.create;

import java.util.concurrent.Executors;
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
import org.walkersguide.android.database.DatabaseProfile;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import timber.log.Timber;
import java.io.IOException;
import android.os.Handler;
import android.os.Looper;
import org.walkersguide.android.server.address.AddressException;
import org.walkersguide.android.server.ServerUtility;
import org.json.JSONObject;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;

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
                    if (Pattern.compile("^https?://").matcher(clipText).find()) {
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
    private final static String PATTERN_FLOATING_POINT_NUMBER = "(-?[0-9]+\\.[0-9]+)";

    private void tryToExtractCoordinates() {
        String linkUrlFromEditText = layoutLinkUrl.getInputText();
        if (linkUrlFromEditText != null) {
            try {
                linkUrlFromEditText = URLDecoder.decode(linkUrlFromEditText, "UTF-8");
            } catch (UnsupportedEncodingException e) {}
        }
        final String linkUrl = linkUrlFromEditText;

        if (TextUtils.isEmpty(linkUrl)) {
            showErrorMessage(
                    getResources().getString(R.string.messageLinkUrlMissing));

        } else if (linkUrl.contains("goo.gl")) {
            // resolve shortened google url
            Executors.newSingleThreadExecutor().execute(() -> {
                final String resolvedGoogleMapsUrl = resolveShortenedUrl(linkUrl);
                Timber.d("resolvedGoogleMapsUrl: %1$s", resolvedGoogleMapsUrl);
                (new Handler(Looper.getMainLooper())).post(() -> {
                    if (isAdded()) {
                        if (! TextUtils.isEmpty(resolvedGoogleMapsUrl)) {
                            tryToExtractCoordinatesFromGoogleMapsUrl(resolvedGoogleMapsUrl);
                        } else {
                            showErrorMessage(
                                    getResources().getString(R.string.messageCouldNotResolveShortenedUrl));
                        }
                    }
                });
            });

        } else if (linkUrl.contains("apple") && linkUrl.contains("maps")) {
            tryToExtractCoordinatesFromAppleMapsUrl(linkUrl);
        } else if (linkUrl.contains("google") && linkUrl.contains("maps")) {
            tryToExtractCoordinatesFromGoogleMapsUrl(linkUrl);
        } else if (linkUrl.contains("osmand.net")) {
            tryToExtractCoordinatesFromOsmAndUrl(linkUrl);
        } else if (linkUrl.contains("openstreetmap.org")) {
            tryToExtractCoordinatesFromOsmOrgUrl(linkUrl);

        } else {
            showErrorMessage(
                    getResources().getString(R.string.messageLinkUrlFormatUnknown));
        }
    }

    private void tryToExtractCoordinatesFromAppleMapsUrl(String coordinatesUrl) {
        Pattern patternCoordinatesApple = Pattern.compile(
                String.format("ll=%1$s,%1$s", PATTERN_FLOATING_POINT_NUMBER));
        Matcher matcherCoordinatesApple = patternCoordinatesApple.matcher(coordinatesUrl);
        Matcher matcherCoordinatesDefault = getDefaultCoordinatesMatcher(coordinatesUrl);
        if (matcherCoordinatesApple.find()) {
            parseCoordinatesAndCreatePointAndDismissDialogOrShowErrorMessage(matcherCoordinatesApple);
        } else if (matcherCoordinatesDefault.find()) {
            parseCoordinatesAndCreatePointAndDismissDialogOrShowErrorMessage(matcherCoordinatesDefault);
        } else {
            showErrorMessage(
                    getResources().getString(R.string.messageAppleMapsUrlFormatUnknown));
        }
    }

    private void tryToExtractCoordinatesFromGoogleMapsUrl(String coordinatesUrl) {
        Pattern patternCoordinatesGoogle = Pattern.compile(
                String.format("@%1$s,%1$s", PATTERN_FLOATING_POINT_NUMBER));
        Matcher matcherCoordinatesGoogle = patternCoordinatesGoogle.matcher(coordinatesUrl);
        Matcher matcherCoordinatesDefault = getDefaultCoordinatesMatcher(coordinatesUrl);
        if (matcherCoordinatesGoogle.find()) {
            parseCoordinatesAndCreatePointAndDismissDialogOrShowErrorMessage(matcherCoordinatesGoogle);
        } else if (matcherCoordinatesDefault.find()) {
            parseCoordinatesAndCreatePointAndDismissDialogOrShowErrorMessage(matcherCoordinatesDefault);
        } else {
            showErrorMessage(
                    getResources().getString(R.string.messageGoogleMapsUrlFormatUnknown));
        }
    }

    private void tryToExtractCoordinatesFromOsmAndUrl(String coordinatesUrl) {
        Pattern patternCoordinatesOsmAnd = Pattern.compile(
                String.format("lat=%1$s&lon=%1$s", PATTERN_FLOATING_POINT_NUMBER));
        Matcher matcherCoordinatesOsmAnd = patternCoordinatesOsmAnd.matcher(coordinatesUrl);
        Matcher matcherCoordinatesDefault = getDefaultCoordinatesMatcher(coordinatesUrl);
        if (matcherCoordinatesOsmAnd.find()) {
            parseCoordinatesAndCreatePointAndDismissDialogOrShowErrorMessage(matcherCoordinatesOsmAnd);
        } else if (matcherCoordinatesDefault.find()) {
            parseCoordinatesAndCreatePointAndDismissDialogOrShowErrorMessage(matcherCoordinatesDefault);
        } else {
            showErrorMessage(
                    getResources().getString(R.string.messageOsmAndUrlFormatUnknown));
        }
    }

    private void tryToExtractCoordinatesFromOsmOrgUrl(String coordinatesUrl) {
        Pattern patternNodeId = Pattern.compile(
                "node/([0-9]+)");
        Pattern patternCoordinatesOsmOrg = Pattern.compile(
                String.format("mlat=%1$s&mlon=%1$s", PATTERN_FLOATING_POINT_NUMBER));
        Matcher matcherNodeId = patternNodeId.matcher(coordinatesUrl);
        Matcher matcherCoordinatesOsmOrg = patternCoordinatesOsmOrg.matcher(coordinatesUrl);
        Matcher matcherCoordinatesDefault = getDefaultCoordinatesMatcher(coordinatesUrl);

        if (matcherNodeId.find()) {
            final String nodeId = matcherNodeId.group(1);
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    JSONObject nodeInformation = ServerUtility.performRequestAndReturnJsonObject(
                            "https://overpass-api.de/api/interpreter",
                            String.format("[out:json];node(%1$s);out;", nodeId),
                            AddressException.class)
                        .getJSONArray("elements")
                        .getJSONObject(0);
                    final double latitude = nodeInformation.getDouble("lat");
                    final double longitude = nodeInformation.getDouble("lon");
                    (new Handler(Looper.getMainLooper())).post(() -> {
                        if (isAdded()) {
                            createPointAndDismissDialogOrShowErrorMessage(latitude, longitude);
                        }
                    });

                } catch (Exception e) {
                    (new Handler(Looper.getMainLooper())).post(() -> {
                        if (isAdded()) {
                            showErrorMessage(
                                    getResources().getString(R.string.messageCouldNotObtainCoordinatesForOsmOrgNodeId));
                        }
                    });
                }
            });

        } else if (matcherCoordinatesOsmOrg.find()) {
            parseCoordinatesAndCreatePointAndDismissDialogOrShowErrorMessage(matcherCoordinatesOsmOrg);
        } else if (matcherCoordinatesDefault.find()) {
            parseCoordinatesAndCreatePointAndDismissDialogOrShowErrorMessage(matcherCoordinatesDefault);
        } else {
            showErrorMessage(
                    getResources().getString(R.string.messageOsmOrgUrlFormatUnknown));
        }
    }

    // helpers

    private void parseCoordinatesAndCreatePointAndDismissDialogOrShowErrorMessage(Matcher matcher) {
        Double latitude = null, longitude = null;
        try {
            latitude = Double.valueOf(matcher.group(1));
            longitude = Double.valueOf(matcher.group(2));
        } catch (Exception e) {}
        if (latitude != null && longitude != null) {
            createPointAndDismissDialogOrShowErrorMessage(latitude, longitude);
        } else {
            showErrorMessage(
                    getResources().getString(R.string.messageLinkContainsInvalidCoordinates));
        }
    }

    private void createPointAndDismissDialogOrShowErrorMessage(double latitude, double longitude) {
        GPS extractedCoordinates = null;
        try {
            extractedCoordinates = new GPS.Builder(latitude, longitude).build();
        } catch (JSONException e) {}
        if (extractedCoordinates != null
                && DatabaseProfile.allPoints().add(extractedCoordinates)) {
            Bundle result = new Bundle();
            result.putSerializable(EXTRA_COORDINATES, extractedCoordinates);
            getParentFragmentManager().setFragmentResult(REQUEST_FROM_COORDINATES_LINK, result);
            dismiss();
        } else {
            showErrorMessage(
                    getResources().getString(R.string.messageLinkContainsInvalidCoordinates));
        }
    }

    private void showErrorMessage(String errorMessage) {
        Toast.makeText(
                getActivity(), errorMessage, Toast.LENGTH_LONG)
            .show();
    }

    private String resolveShortenedUrl(String shortenedUrl) {
        HttpsURLConnection connection = null;
        String resolvedUrl = null;
        try {
            URL url = new URL(shortenedUrl);
            if (ServerUtility.isInternetAvailable()) {
                connection = (HttpsURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.connect();
                resolvedUrl = connection.getHeaderField("location");
            }
        } catch (IOException e) {
            Timber.e("error: %1$s", e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return resolvedUrl;
    }

    private Matcher getDefaultCoordinatesMatcher(String coordinatesUrl) {
        Pattern pattern = Pattern.compile(
                String.format("%1$s[^0-9.-]+%1$s", PATTERN_FLOATING_POINT_NUMBER));
        return pattern.matcher(coordinatesUrl);
    }

}
