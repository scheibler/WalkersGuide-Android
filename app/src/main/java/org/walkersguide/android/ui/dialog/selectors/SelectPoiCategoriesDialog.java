package org.walkersguide.android.ui.dialog.selectors;

import org.walkersguide.android.R;
import org.walkersguide.android.server.poi.PoiCategory;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import org.walkersguide.android.server.util.ServerInstance;
import android.widget.Button;
import org.walkersguide.android.util.SettingsManager;

import java.util.concurrent.ExecutorService;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.walkersguide.android.server.util.ServerCommunicationException;
import org.walkersguide.android.server.util.ServerUtility;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;


public class SelectPoiCategoriesDialog extends DialogFragment {
    public static final String REQUEST_SELECT_POI_CATEGORIES = "selectPoiCategories";
    public static final String EXTRA_POI_CATEGORY_LIST = "poiCategoryList";


    // instance constructors

    public static SelectPoiCategoriesDialog newInstance(ArrayList<PoiCategory> selectedPoiCategoryList) {
        SelectPoiCategoriesDialog dialog= new SelectPoiCategoriesDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SELECTED_POI_CATEGORY_LIST, selectedPoiCategoryList);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_SELECTED_POI_CATEGORY_LIST = "selectedPoiCategoryList";

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Future getServerInstanceRequest;

    private ArrayList<PoiCategory> supportedPoiCategoryList, selectedPoiCategoryList;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        supportedPoiCategoryList = null;
        if(savedInstanceState != null) {
            selectedPoiCategoryList = (ArrayList<PoiCategory>) savedInstanceState.getSerializable(KEY_SELECTED_POI_CATEGORY_LIST);
        } else {
            selectedPoiCategoryList = (ArrayList<PoiCategory>) getArguments().getSerializable(KEY_SELECTED_POI_CATEGORY_LIST);
        }

        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectPoiCategoriesDialogTitle))
            .setMultiChoiceItems(
                    new String[]{getResources().getString(R.string.messagePleaseWait)},
                    null,
                    new DialogInterface.OnMultiChoiceClickListener() { 
                        @Override public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            PoiCategory selectedPoiCategory = null;
                            try {
                                selectedPoiCategory = supportedPoiCategoryList.get(which);
                            } catch (IndexOutOfBoundsException | NullPointerException e) {
                                selectedPoiCategory = null;
                            } finally {
                                if (selectedPoiCategory != null) {
                                    // add or remove
                                    if (selectedPoiCategoryList.contains(selectedPoiCategory)) {
                                        selectedPoiCategoryList.remove(selectedPoiCategory);
                                    } else {
                                        selectedPoiCategoryList.add(selectedPoiCategory);
                                    }
                                    updateNeutralButtonLabel();
                                }
                            }
                        }
                    }
                    )
            .setPositiveButton(
                    getResources().getString(R.string.dialogOK),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNeutralButton(
                    getResources().getString(R.string.dialogClear),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }
                    )
            .setNegativeButton(
                    getResources().getString(R.string.dialogCancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }
                    )
            .create();
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(KEY_SELECTED_POI_CATEGORY_LIST, selectedPoiCategoryList);
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            // hide positive and neutral buttons for now
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setVisibility(View.GONE);
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setVisibility(View.GONE);
        }

        // request server status
        if (getServerInstanceRequest == null || getServerInstanceRequest.isDone()) {
            getServerInstanceRequest = this.executorService.submit(() -> {
                try {
                    final ServerInstance serverInstance = ServerUtility.getServerInstance(
                                SettingsManager.getInstance().getServerURL());
                    handler.post(() -> {
                        initializeDialog(serverInstance);
                    });

                } catch (ServerCommunicationException e) {
                    final ServerCommunicationException scException = e;
                    handler.post(() -> {
                        SimpleMessageDialog.newInstance(
                                ServerUtility.getErrorMessageForReturnCode(scException.getReturnCode()))
                            .show(getChildFragmentManager(), "SimpleMessageDialog");
                    });
                }
            });
        }
    }

    @Override public void onStop() {
        super.onStop();
    }

    private void initializeDialog(ServerInstance serverInstance) {
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            supportedPoiCategoryList = serverInstance.getSupportedPoiCategoryList();

            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setVisibility(View.VISIBLE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    Bundle result = new Bundle();
                    result.putSerializable(EXTRA_POI_CATEGORY_LIST, selectedPoiCategoryList);
                    getParentFragmentManager().setFragmentResult(REQUEST_SELECT_POI_CATEGORIES, result);
                    dismiss();
                }
            });

            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setVisibility(View.VISIBLE);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    if (selectedPoiCategoryList.isEmpty()) {
                        selectedPoiCategoryList = new ArrayList<PoiCategory>(supportedPoiCategoryList);
                    } else {
                        selectedPoiCategoryList.clear();
                    }
                    updateListViewSelections();
                    updateNeutralButtonLabel();
                }
            });

            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                }
            });

            // listview
            ListView listViewItems = (ListView) dialog.getListView();
            listViewItems.setAdapter(
                    new ArrayAdapter<PoiCategory>(
                        SelectPoiCategoriesDialog.this.getContext(),
                        android.R.layout.select_dialog_multichoice,
                        supportedPoiCategoryList));
            updateListViewSelections();
            updateNeutralButtonLabel();
        }
    }

    private void updateListViewSelections() {
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            ListView listViewItems = (ListView) dialog.getListView();
            for (PoiCategory category : supportedPoiCategoryList) {
                listViewItems.setItemChecked(
                        supportedPoiCategoryList.indexOf(category),
                        selectedPoiCategoryList.contains(category));
            }
        }
    }

    private void updateNeutralButtonLabel() {
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (selectedPoiCategoryList.isEmpty()) {
                buttonNeutral.setText(
                        getResources().getString(R.string.dialogAll));
            } else {
                buttonNeutral.setText(
                        getResources().getString(R.string.dialogClear));
            }
        }
    }

}
