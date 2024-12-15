package org.walkersguide.android.ui.dialog.gpx;

import org.walkersguide.android.ui.dialog.create.CreateOrSelectCollectionDialog;
import org.walkersguide.android.util.gpx.GpxFileReader;
import org.walkersguide.android.util.gpx.GpxFileReader.GpxFileParseResult;
import org.walkersguide.android.util.gpx.GpxFileReader.GpxFileParseException;
import androidx.core.view.ViewCompat;
import org.walkersguide.android.database.profile.Collection;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;



import org.walkersguide.android.R;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;

import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;

import android.content.Intent;
import android.net.Uri;
import android.widget.TextView;
import org.walkersguide.android.util.GlobalInstance;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.ui.dialog.template.FilePickerDialog;
import android.widget.ListView;
import android.widget.ImageButton;
import org.walkersguide.android.ui.adapter.ObjectWithIdAdapter;


public class OpenGpxFileDialog extends FilePickerDialog implements FragmentResultListener {
    public static final String REQUEST_IMPORT_INTO_COLLECTION_SUCCESSFUL = "requestImportIntoCollectionSuccessful";
    public static final String EXTRA_COLLECTION = "collection";


    // instance constructors

    public static OpenGpxFileDialog newInstance(Uri uri) {
        OpenGpxFileDialog dialog = new OpenGpxFileDialog();
        dialog.setArguments(new FilePickerDialog.BundleBuilder(uri).build());
        return dialog;
    }


    // dialog
    private static final String KEY_PARSE_ReSULT = "parseResult";

    private GpxFileParseResult parseResult;

    private TextView labelHeading;
    private ListView listViewObjectList;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseResult = savedInstanceState != null
            ? (GpxFileParseResult) savedInstanceState.getSerializable(KEY_PARSE_ReSULT)
            : null;

        getChildFragmentManager()
            .setFragmentResultListener(
                    CreateOrSelectCollectionDialog.REQUEST_SUCCESSFUL, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SimpleMessageDialog.REQUEST_DIALOG_CLOSED, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(CreateOrSelectCollectionDialog.REQUEST_SUCCESSFUL)) {
            Collection selectedCollection = (Collection) bundle.getSerializable(CreateOrSelectCollectionDialog.EXTRA_SELECTED_COLLECTION);
            if (selectedCollection instanceof Collection) {
                addObjectsFromGpxFileToSelectedCollectionAndDismiss(selectedCollection);
            }
        } else if (requestKey.equals(SimpleMessageDialog.REQUEST_DIALOG_CLOSED)) {
            dismiss();
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_heading_and_list_view, nullParent);

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        ImageButton buttonAddObjectWithId = (ImageButton) view.findViewById(R.id.buttonAdd);
        buttonAddObjectWithId.setVisibility(View.GONE);

        listViewObjectList = (ListView) view.findViewById(R.id.listView);
        TextView labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        labelEmptyListView.setText(null);
        listViewObjectList.setEmptyView(labelEmptyListView);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.openGpxFileDialogTitle))
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.labelPrefixAddTo),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNegativeButton(
                    getResources().getString(R.string.dialogClose),
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
                if (parseResult != null) {
                    CreateOrSelectCollectionDialog.newInstance(parseResult.getCollectionName(), false)
                        .show(getChildFragmentManager(), "CreateOrSelectCollectionDialog");
                }
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

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(KEY_PARSE_ReSULT, this.parseResult);
    }

    @Override public void prepareForFilePicking() {
        labelHeading.setText(
                GlobalInstance.getPluralResource(R.plurals.pointAndRoute, 0));
        listViewObjectList.setAdapter(null);
        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
    }

    @Override public Intent getFilePickingIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(FilePickerDialog.MIME_TYPE_APPLICATION_ALL);
        return intent;
    }

    @Override public void fileSelectionSuccessful() {
        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) dialog.setTitle(getFileNameOfSelectedUri());

        if (parseResult != null) {
            gpxFileParsedSuccessfully(parseResult);
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            GpxFileReader gpxFileReader = new GpxFileReader(getSelectedUri());

            try {
                final GpxFileParseResult parseResult = gpxFileReader.read();
                (new Handler(Looper.getMainLooper())).post(() -> {
                    if (isAdded()) {
                        gpxFileParsedSuccessfully(parseResult);
                    }
                });

            } catch (GpxFileParseException e) {
                final GpxFileParseException parseException = e;
                (new Handler(Looper.getMainLooper())).post(() -> {
                    if (isAdded()) {
                        SimpleMessageDialog.newInstance(parseException.getMessage())
                            .show(getChildFragmentManager(), "SimpleMessageDialog");
                    }
                });
            }
        });
    }

    private void gpxFileParsedSuccessfully(GpxFileParseResult result) {
        labelHeading.setText(
                GlobalInstance.getPluralResource(
                    R.plurals.pointAndRoute, result.objectList.size()));
        ObjectWithIdAdapter adapter = new ObjectWithIdAdapter(
                OpenGpxFileDialog.this.getContext(), result.objectList, null, null, false, false);
        adapter.setAlwaysShowIcon();
        listViewObjectList.setAdapter(adapter);
        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
        this.parseResult = result;
    }

    private void addObjectsFromGpxFileToSelectedCollectionAndDismiss(final Collection collectionToAddObjectsTo) {
        if (collectionToAddObjectsTo == null || parseResult == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            for (ObjectWithId objectToAdd : parseResult.objectList) {
                collectionToAddObjectsTo.addObject(objectToAdd);
            }
            (new Handler(Looper.getMainLooper())).post(() -> {
                if (isAdded()) {
                    dismiss();
                    Bundle result = new Bundle();
                    result.putSerializable(EXTRA_COLLECTION, collectionToAddObjectsTo);
                    getParentFragmentManager().setFragmentResult(REQUEST_IMPORT_INTO_COLLECTION_SUCCESSFUL, result);
                }
            });
        });
    }

}
