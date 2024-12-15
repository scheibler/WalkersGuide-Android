package org.walkersguide.android.ui.dialog.template;



import android.os.Bundle;

import androidx.fragment.app.DialogFragment;








import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;
import org.walkersguide.android.util.FileUtility;



public abstract class FilePickerDialog extends DialogFragment {
    public static final String MIME_TYPE_APPLICATION_ALL = "application/*";
    public static final int PICK_FILE = 14;

    public abstract Intent getFilePickingIntent();
    public abstract void prepareForFilePicking();
    public abstract void fileSelectionSuccessful();

    public static class BundleBuilder {
        protected Bundle bundle = new Bundle();
        public BundleBuilder(Uri uri) {
            bundle.putParcelable(KEY_SELECTED_URI, uri);
        }
        public Bundle build() {
            return bundle;
        }
    }


    private static final String KEY_FILE_PICKING_IN_PROGRESS = "filePickingInProgress";
    private static final String KEY_SELECTED_URI = "selectedUri";

    private boolean filePickingInProgress;
    private Uri selectedUri;

    protected Uri getSelectedUri() {
        return this.selectedUri;
    }

    protected String getFileNameOfSelectedUri() {
        return FileUtility.extractFileNameFrom(this.selectedUri);
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            filePickingInProgress = savedInstanceState.getBoolean(KEY_FILE_PICKING_IN_PROGRESS);
            selectedUri = (Uri) savedInstanceState.getParcelable(KEY_SELECTED_URI);
        } else {
            filePickingInProgress = false;
            selectedUri = (Uri) getArguments().getParcelable(KEY_SELECTED_URI);
        }
    }

    @Override public void onStart() {
        super.onStart();
        if (filePickingInProgress) return;

        if (selectedUri != null) {
            fileSelectionSuccessful();

        } else {
            filePickingInProgress = true;
            prepareForFilePicking();
            startActivityForResult(getFilePickingIntent(), PICK_FILE);
        }
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode != PICK_FILE
                || resultCode != AppCompatActivity.RESULT_OK
                || resultData == null) {
            dismiss();
        } else {
            selectedUri = resultData.getData();
            filePickingInProgress = false;
            fileSelectionSuccessful();
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(KEY_FILE_PICKING_IN_PROGRESS, this.filePickingInProgress);
        savedInstanceState.putParcelable(KEY_SELECTED_URI, selectedUri);
    }

}
