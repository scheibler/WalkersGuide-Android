        package org.walkersguide.android.ui.view;

import org.walkersguide.android.ui.UiHelper;
import timber.log.Timber;



import android.view.View;
import android.view.View.BaseSavedState;

import android.widget.TextView;

import android.widget.ImageButton;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;
import android.text.TextUtils;
import android.content.Context;
import android.widget.LinearLayout;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.EditText;
import org.walkersguide.android.ui.TextChangedListener;
import android.text.Editable;
import android.view.KeyEvent;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.Parcelable.ClassLoaderCreator;
import android.util.SparseArray;
import android.os.Parcel;
import android.os.Build;
import java.lang.ClassLoader;
import androidx.annotation.RequiresApi;
import android.content.res.TypedArray;
import android.annotation.TargetApi;
import android.view.inputmethod.InputMethodManager;


public class EditTextAndClearInputButton extends LinearLayout {

    public interface OnSelectedActionClickListener {
        public void onSelectedActionClicked();
    }


    private TextView label;
    private EditText editInput;
    private ImageButton buttonClearInput;

    public EditTextAndClearInputButton(Context context) {
        super(context);
        init(context, null);
    }

    public EditTextAndClearInputButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        boolean labelAbove = false;

        // parse layout attributes
        TypedArray attributeArray = context.obtainStyledAttributes(
                attrs, R.styleable.EditTextAndClearInputButton);
        if (attributeArray != null) {
            labelAbove = attributeArray.getBoolean(
                    R.styleable.EditTextAndClearInputButton_labelAbove, false);
            attributeArray.recycle();
            Timber.d("labelAbove: %1$s", labelAbove);
        }

        View view = null;
        if (labelAbove) {
            view = inflate(context, R.layout.layout_edit_text_and_clear_input_button_label_above, this);
            setOrientation(LinearLayout.VERTICAL);
        } else {
            view = inflate(context, R.layout.layout_edit_text_and_clear_input_button, this);
            setOrientation(LinearLayout.HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
        }

        label = (TextView) view.findViewById(R.id.label);
        label.setText("");
        label.setVisibility(View.GONE);

        editInput = (EditText) view.findViewById(R.id.editInput);
        editInput.addTextChangedListener(new TextChangedListener<EditText>(editInput) {
            @Override public void onTextChanged(EditText view, Editable s) {
                if (! TextUtils.isEmpty(editInput.getText()) && buttonClearInput.getVisibility() == View.GONE) {
                    buttonClearInput.setVisibility(View.VISIBLE);
                } else if (TextUtils.isEmpty(editInput.getText()) && buttonClearInput.getVisibility() == View.VISIBLE) {
                    buttonClearInput.setVisibility(View.GONE);
                }
            }
        });

        buttonClearInput = (ImageButton) view.findViewById(R.id.buttonClearInput);
        buttonClearInput.setContentDescription(
                GlobalInstance.getStringResource(R.string.buttonClearInput));
        buttonClearInput.setVisibility(View.GONE);
        buttonClearInput.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setInputText("");
            }
        });
    }

    public String getInputText() {
        return editInput.getText().toString();
    }

    public void setHint(String hint) {
        this.editInput.setHint(hint);
    }

    public void setInputText(String inputText) {
        this.editInput.setText(inputText);
    }

    public void setLabelText(String labelText) {
        this.label.setText(labelText);
        this.label.setVisibility(View.VISIBLE);
        this.buttonClearInput.setContentDescription(
                String.format(
                    GlobalInstance.getStringResource(R.string.buttonClearInputWithLabel),
                    labelText)
                );
    }

    public void setInputType(int inputType) {
        editInput.setInputType(inputType);
    }

    public void setEditorAction(final int imeAction, final OnSelectedActionClickListener listener) {
        editInput.setImeOptions(imeAction);
        editInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (UiHelper.isDoSomeThingEditorAction(actionId, imeAction, event)) {
                    if (listener != null) {
                        listener.onSelectedActionClicked();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    public void showKeyboard() {
        editInput.requestFocus();
        editInput.postDelayed(new Runnable() {
            @Override public void run() {
                InputMethodManager imm = (InputMethodManager) GlobalInstance.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editInput, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 500);
    }


    /**
     * if more than one EditTextAndClearInputButton view is included in a layout
     */

    @Override public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.inputText = this.getInputText();
        return ss;
    }

    @Override protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    @Override public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setInputText(ss.inputText);
    }

    @Override protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }


    static class SavedState extends BaseSavedState {
        String inputText;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.inputText = in.readString();
        }

        @RequiresApi(Build.VERSION_CODES.N)
        SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            this.inputText = in.readString();
        }

        @Override public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(this.inputText);
        }

        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            @Override public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? new SavedState(in, loader) : new SavedState(in);
            }

            @TargetApi(Build.VERSION_CODES.N)
            @Override public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

}
