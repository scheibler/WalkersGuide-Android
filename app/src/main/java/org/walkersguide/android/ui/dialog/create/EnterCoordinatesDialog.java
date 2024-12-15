package org.walkersguide.android.ui.dialog.create;

import org.walkersguide.android.ui.interfaces.TextChangedListener;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.database.profile.static_profile.HistoryProfile;
import org.walkersguide.android.ui.UiHelper;

import org.walkersguide.android.ui.view.EditTextAndClearInputButton;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

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
import org.walkersguide.android.R;
import android.text.InputType;
import org.json.JSONException;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.view.KeyEvent;
import timber.log.Timber;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.widget.RadioGroup;
import android.text.Editable;


public class EnterCoordinatesDialog extends DialogFragment {
    public static final String REQUEST_ENTER_COORDINATES = "enterCoordinates";
    public static final String EXTRA_COORDINATES = "coordinates";


    // instance constructors

    public static EnterCoordinatesDialog newInstance() {
        EnterCoordinatesDialog dialog = new EnterCoordinatesDialog();
        return dialog;
    }


    // dialog
    private static final String KEY_SELECTED_RADIO_BUTTON_ID = "selectedRadioButtonId";

    private int selectedRadioButtonId;

    private RadioGroup radioGroupCoordinatesInputFormat;
    private EditText editLatitude, editLongitude;
    private EditTextAndClearInputButton layoutOptionalName;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        selectedRadioButtonId = savedInstanceState != null
            ? savedInstanceState.getInt(KEY_SELECTED_RADIO_BUTTON_ID)
            : R.id.radioButtonCoordinatesInputFormatDecimal;

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_enter_coordinates, nullParent);

        radioGroupCoordinatesInputFormat = (RadioGroup) view.findViewById(R.id.radioGroupCoordinatesInputFormat);
        radioGroupCoordinatesInputFormat.check(selectedRadioButtonId);
        radioGroupCoordinatesInputFormat.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId != selectedRadioButtonId) {
                    selectedRadioButtonId = checkedId;
                    editLatitude.setText("");
                    editLongitude.setText("");
                    updateEditTextInputType();
                }
            }
        });

        editLatitude = (EditText) view.findViewById(R.id.editLatitude);
        editLatitude.addTextChangedListener(new TextChangedListener<EditText>(editLatitude) {
            boolean wasEmpty = true;
            @Override public void onTextChanged(EditText view, Editable s) {
                if (wasEmpty != view.getText().toString().isEmpty()) {
                    updateEditTextHint();
                    wasEmpty = ! wasEmpty;
                }
            }
        });
        editLatitude.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (UiHelper.isDoSomeThingEditorAction(actionId, EditorInfo.IME_ACTION_NEXT, event)) {
                    editLongitude.requestFocus();
                    return true;
                }
                return false;
            }
        });

        editLongitude = (EditText) view.findViewById(R.id.editLongitude);
        editLongitude.addTextChangedListener(new TextChangedListener<EditText>(editLongitude) {
            boolean wasEmpty = true;
            @Override public void onTextChanged(EditText view, Editable s) {
                if (wasEmpty != view.getText().toString().isEmpty()) {
                    updateEditTextHint();
                    wasEmpty = ! wasEmpty;
                }
            }
        });
        editLongitude.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (UiHelper.isDoSomeThingEditorAction(actionId, EditorInfo.IME_ACTION_NEXT, event)) {
                    layoutOptionalName.requestFocus();
                    return true;
                }
                return false;
            }
        });

        layoutOptionalName = (EditTextAndClearInputButton) view.findViewById(R.id.layoutOptionalName);
        layoutOptionalName.setLabelText(
                getResources().getString(R.string.labelOptionalName));
        layoutOptionalName.setEditorAction(
                EditorInfo.IME_ACTION_DONE,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        tryToCreateGpsPoint();
                    }
                });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.enterCoordinatesDialogName))
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
        if(dialog == null) return;

        // positive button
        Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        buttonPositive.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                tryToCreateGpsPoint();
            }
        });

        // negative button
        Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        buttonNegative.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                dismiss();
            }
        });

        updateEditTextInputType();
        updateEditTextHint();
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(KEY_SELECTED_RADIO_BUTTON_ID, selectedRadioButtonId);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            UiHelper.hideKeyboard(this);
        }
    }

    private void tryToCreateGpsPoint() {
        Double latitude = convertStringLatitudeToDouble(
                editLatitude.getText().toString());
        if (latitude == null) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageLatitudeMissing),
                    Toast.LENGTH_LONG).show();
            return;
        }

        Double longitude = convertStringLongitudeToDouble(
                editLongitude.getText().toString());
        if (longitude == null) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageLongitudeMissing),
                    Toast.LENGTH_LONG).show();
            return;
        }

        Point newLocation = null;
        try {
            newLocation = new Point.Builder(
                    Point.Type.POINT, layoutOptionalName.getInputText(), latitude, longitude)
                .build();
        } catch (JSONException e) {}
        if (newLocation != null
                && HistoryProfile.allPoints().addObject(newLocation)) {
            // push results and dismiss dialog
            Bundle result = new Bundle();
            result.putSerializable(EXTRA_COORDINATES, newLocation);
            getParentFragmentManager().setFragmentResult(REQUEST_ENTER_COORDINATES, result);
            dismiss();
        }
    }

    public static Double convertStringLatitudeToDouble(String strLatitude) {
        String strLatitudeSanitized = sanitizeCoordinateInputText(strLatitude);
        Double latitude = null;
        try {
            // first try the decimal format
            latitude = Double.valueOf(strLatitudeSanitized);
        } catch (NumberFormatException e) {
            latitude = convertDmsToDecimal(strLatitudeSanitized, PATTERN_DIRECTION_LATITUDE);
        }
        if (latitude == null || latitude < -90.0 || latitude > 90.0) {
            return null;
        }
        return latitude;
    }

    public static Double convertStringLongitudeToDouble(String strLongitude) {
        String strLongitudeSanitized = sanitizeCoordinateInputText(strLongitude);
        Double longitude = null;
        try {
            // first try the decimal format
            longitude = Double.valueOf(strLongitudeSanitized);
        } catch (NumberFormatException e) {
            longitude = convertDmsToDecimal(strLongitudeSanitized, PATTERN_DIRECTION_LONGITUDE);
        }
        if (longitude == null || longitude < -180.0 || longitude > 180.0) {
            return null;
        }
        return longitude;
    }

    private static String sanitizeCoordinateInputText(String input) {
        String output = new String(input);
        output = output.replace(",", ".");
        // In German the direction East sometimes is abbreviated with O for "Osten"
        output = output.replaceAll("^[oO]", "E");
        output = output.replaceAll("[oO]$", "E");
        return output;
    }


    // latitude / longitude input formats
    private static final int INPUT_TYPE_DECIMAL = InputType.TYPE_CLASS_NUMBER
        | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED;
    private static final int INPUT_TYPE_DMS = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    private static final int INPUT_TYPE_DEFAULT = InputType.TYPE_CLASS_TEXT;

    private void updateEditTextInputType() {
        if (selectedRadioButtonId == R.id.radioButtonCoordinatesInputFormatDecimal) {
            editLatitude.setInputType(INPUT_TYPE_DECIMAL);
            editLongitude.setInputType(INPUT_TYPE_DECIMAL);
        } else if (selectedRadioButtonId == R.id.radioButtonCoordinatesInputFormatDms) {
            editLatitude.setInputType(INPUT_TYPE_DMS);
            editLongitude.setInputType(INPUT_TYPE_DMS);
        } else {
            editLatitude.setInputType(INPUT_TYPE_DEFAULT);
            editLongitude.setInputType(INPUT_TYPE_DEFAULT);
        }
    }

    private void updateEditTextHint() {
        editLatitude.setHint(null);
        if (editLatitude.getText().toString().isEmpty()) {
            if (selectedRadioButtonId == R.id.radioButtonCoordinatesInputFormatDecimal) {
                editLatitude.setHint("-6.54321");
            } else if (selectedRadioButtonId == R.id.radioButtonCoordinatesInputFormatDms) {
                editLatitude.setHint("6°5\"4.321'S");
            }
        }

        editLongitude.setHint(null);
        if (editLongitude.getText().toString().isEmpty()) {
            if (selectedRadioButtonId == R.id.radioButtonCoordinatesInputFormatDecimal) {
                editLongitude.setHint("123.45678");
            } else if (selectedRadioButtonId == R.id.radioButtonCoordinatesInputFormatDms) {
                editLongitude.setHint("123°45\"67.89'E");
            }
        }
    }


    // convert from degrees/minutes/seconds to decimal latitude / longitude coordinates
    private final static String PATTERN_DMS_COORDINATES_WITHOUT_DIRECTION =
        "([0-9]+)[° ]([0-9]+)[′\"’ ]([0-9]+\\.[0-9]+|[0-9]+)(?:[″']| ?)";
    private static final String PATTERN_DIRECTION_LATITUDE = "([nNsS])";
    private static final String PATTERN_DIRECTION_LONGITUDE = "([eEwW])";

    private static Double convertDmsToDecimal(String coordinate, String patternDirection) {
        Matcher matcher = null;
        Timber.d("coordinate to convert: %1$s", coordinate);

        matcher = Pattern.compile(
                String.format("^%1$s%2$s$", PATTERN_DMS_COORDINATES_WITHOUT_DIRECTION, patternDirection))
            .matcher(coordinate);
        if (matcher.find()) {
            Timber.d("match direction suffix");
            return convertDmsToDecimal(
                    matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
        }

        matcher = Pattern.compile(
                String.format("^%1$s%2$s$", patternDirection, PATTERN_DMS_COORDINATES_WITHOUT_DIRECTION))
            .matcher(coordinate);
        if (matcher.find()) {
            Timber.d("match direction prefix");
            return convertDmsToDecimal(
                    matcher.group(2), matcher.group(3), matcher.group(4), matcher.group(1));
        }

        return null;
    }

    private static Double convertDmsToDecimal(String degrees, String minutes, String seconds, String direction) {
        Timber.d("cast: %1$s %2$s %3$s %4$s", degrees, minutes, seconds, direction);
        try {
            return convertDmsToDecimal(
                    Integer.valueOf(degrees),
                    Integer.valueOf(minutes),
                    Double.valueOf(seconds),
                    direction.charAt(0));
        } catch (Exception e) {}
        return null;
    }

    private static Double convertDmsToDecimal(int degrees, int minutes, double seconds, char direction) {
        // check boundaries of degrees, minutes and seconds
        // the distinktion between the upper boundary for latitude / longitude is checked above
        if (degrees < 0 || degrees > 180) return null;
        if (minutes < 0 || minutes >= 60) return null;
        if (seconds < 0.0 || seconds >= 60.0) return null;
        // convert to decimal format
        double decimalDegrees = degrees + (minutes / 60.0) + (seconds / 3600.0);
        // If the direction is South or West, the decimal degrees should be negative
        if (       direction == 's' || direction == 'S'
                || direction == 'w' || direction == 'W') {
            decimalDegrees *= -1;
        }
        Timber.d("decimalDegrees: %1$f = %2$d %3$d %4$f %5$s", decimalDegrees, degrees, minutes, seconds, direction);
        return Double.valueOf(decimalDegrees);
    }

}
