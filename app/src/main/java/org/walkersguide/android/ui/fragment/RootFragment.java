package org.walkersguide.android.ui.fragment;

import org.walkersguide.android.R;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.MainActivityController;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import androidx.fragment.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.app.Dialog;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import timber.log.Timber;


public abstract class RootFragment extends DialogFragment {

    public abstract String getTitle();
    public abstract int getLayoutResourceId();
	public abstract View configureView(View view, Bundle savedInstanceState);

    public String getDialogTitle() {
        return getTitle();
    }

    public String getDialogButtonText() {
        return getResources().getString(R.string.dialogClose);
    }

    @Override public int getTheme() {
        return R.style.FullScreenDialog;
    }

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getDialog() != null) {
            Timber.d("im a dialog");
            // fragment is a dialog
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            Timber.d("im embedded");
            // fragment is embetted
		    return configureView(
                   inflater.inflate(getLayoutResourceId(), container, false),
                   savedInstanceState);
        }
	}

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = configureView(
                inflater.inflate(getLayoutResourceId(), nullParent),
                savedInstanceState);

        // create dialog
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity())
            .setView(view)
            .setNegativeButton(
                    getDialogButtonText(),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    });
        if (getDialogTitle() != null) {
            dialogBuilder.setTitle(getDialogTitle());
        }
        return dialogBuilder.create();
    }


    protected MainActivityController mainActivityController;

    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            if (activity instanceof MainActivity) {
                mainActivityController = (MainActivityController) ((MainActivity) activity);
            }
        }
    }

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        if (getDialog() == null) {
            updateToolbarTitle();
        }
    }

    public void updateToolbarTitle() {
        String toolbarTitle = getTitle();
        if (toolbarTitle != null) {
            mainActivityController.configureToolbarTitle(toolbarTitle);
        }
    }

}
