package org.walkersguide.android.ui.dialog.gpx;

import org.walkersguide.android.ui.dialog.template.FilePickerDialog;
import org.walkersguide.android.util.gpx.GpxFileWriter;
import androidx.core.view.ViewCompat;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;



import org.walkersguide.android.R;

import android.content.Intent;
import android.net.Uri;
import android.widget.TextView;
import org.walkersguide.android.util.GlobalInstance;
import java.io.IOException;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

import org.walkersguide.android.util.FileUtility;
import java.util.Locale;


public abstract class ExportGpxFileDialog extends FilePickerDialog {

    public abstract String getFileNameSuggestion();
    public abstract void createGpxFileInBackground(GpxFileWriter gpxFileWriter) throws IOException;

    private TextView labelExportResult;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_single_text_view, nullParent);
        labelExportResult = (TextView) view.findViewById(R.id.label);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.exportGpxFileDialogTitle))
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.dialogOK),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNeutralButton(
                    getResources().getString(R.string.dialogShare),
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
        buttonPositive.setVisibility(View.VISIBLE);
        buttonPositive.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                dismiss();
            }
        });

        // neutral button
        Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        buttonNeutral.setVisibility(View.GONE);
        buttonNeutral.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                Uri selectedUri = getSelectedUri();
                if (selectedUri == null) return;
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType(FilePickerDialog.MIME_TYPE_APPLICATION_ALL);
                shareIntent.putExtra(Intent.EXTRA_STREAM, selectedUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(
                        Intent.createChooser(
                            shareIntent,
                            GlobalInstance.getStringResource(R.string.shareGpxFileDialogTitle)));
            }
        });
    }

    @Override public void prepareForFilePicking() {
        ViewCompat.setAccessibilityLiveRegion(
                labelExportResult, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelExportResult.setText(
                getResources().getString(R.string.messagePleaseWait));
        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
    }

    @Override public Intent getFilePickingIntent() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(FilePickerDialog.MIME_TYPE_APPLICATION_ALL);
        intent.putExtra(
                Intent.EXTRA_TITLE,
                String.format(Locale.ROOT, "%1$s.gpx", getFileNameSuggestion()));
        return intent;
    }

    @Override public void fileSelectionSuccessful() {
        ViewCompat.setAccessibilityLiveRegion(
                labelExportResult, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);

        final String extractedFileName = FileUtility.extractFileNameFrom(getSelectedUri());
        if (extractedFileName == null) {
            labelExportResult.setText(
                    GlobalInstance.getStringResource(R.string.messageExtractGpxFileNameFailed));
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            GpxFileWriter gpxFileWriter =
                new GpxFileWriter(getSelectedUri(), extractedFileName.replaceAll("(?i)\\.gpx$", ""));

            try {
                createGpxFileInBackground(gpxFileWriter);
                (new Handler(Looper.getMainLooper())).post(() -> {
                    if (isAdded()) {
                        this.labelExportResult.setText(
                                String.format(
                                    GlobalInstance.getStringResource(R.string.messageGpxFileExportSuccessful),
                                    extractedFileName));
                        if (dialog != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
                    }
                });

            } catch (IOException e) {
                gpxFileWriter.cleanupOnFailure();
                (new Handler(Looper.getMainLooper())).post(() -> {
                    if (isAdded()) {
                        this.labelExportResult.setText(
                                String.format(
                                    GlobalInstance.getStringResource(R.string.messageGpxFileExportFailed),
                                    extractedFileName));
                    }
                });
            }
        });
    }

}
