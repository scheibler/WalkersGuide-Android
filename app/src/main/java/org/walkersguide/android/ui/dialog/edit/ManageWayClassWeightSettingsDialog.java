package org.walkersguide.android.ui.dialog.edit;

import org.walkersguide.android.ui.adapter.WayClassWeightSettingsSpinnerAdapter;
import android.widget.Toast;
import org.walkersguide.android.server.wg.p2p.WayClassWeightSettings;
import org.walkersguide.android.server.wg.p2p.wayclass.WayClassType;
import org.walkersguide.android.server.wg.p2p.wayclass.WayClassWeight;

import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.View;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import org.walkersguide.android.R;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.Spinner;
import java.util.Arrays;
import android.widget.ArrayAdapter;
import org.walkersguide.android.ui.view.builder.TextViewBuilder;
import android.widget.TableRow;
import android.widget.AdapterView;
import androidx.appcompat.widget.PopupMenu;
import android.view.Menu;
import org.walkersguide.android.util.GlobalInstance;
import android.view.MenuItem;
import org.walkersguide.android.util.SettingsManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.Nullable;
import java.util.List;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import org.walkersguide.android.ui.dialog.template.EnterStringDialog;
import androidx.annotation.NonNull;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import timber.log.Timber;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.Window;
import android.text.TextUtils;
import androidx.recyclerview.widget.ItemTouchHelper;
import java.util.Collections;
import androidx.core.view.ViewCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;



public class ManageWayClassWeightSettingsDialog extends DialogFragment implements FragmentResultListener {

    public static ManageWayClassWeightSettingsDialog newInstance() {
        return new ManageWayClassWeightSettingsDialog();
    }

    private SettingsManager settingsManagerInstance;

    private Spinner spinnerSelectDefaultWayClassWeightSettings;
    private RecyclerView recyclerViewAvailableWayClassWeightSettings;
    private LinearLayout layoutEmptyView;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsManagerInstance = SettingsManager.getInstance();

        getChildFragmentManager()
            .setFragmentResultListener(
                    EnterWayClassWeightSettingsNameDialog.REQUEST_ENTER_WAY_CLASS_WEIGHT_SETTINGS_NAME, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    ConfigureWayClassWeightsDialog.REQUEST_WAY_CLASS_WEIGHTS_CHANGED, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(EnterWayClassWeightSettingsNameDialog.REQUEST_ENTER_WAY_CLASS_WEIGHT_SETTINGS_NAME)) {
            String newSettingsName = bundle.getString(EnterWayClassWeightSettingsNameDialog.EXTRA_NAME);
            if (! TextUtils.isEmpty(newSettingsName)) {
                ConfigureWayClassWeightsDialog.newInstance(
                        new WayClassWeightSettings(newSettingsName))
                    .show(getChildFragmentManager(), "ConfigureWayClassWeightsDialog");
            }

        } else if (requestKey.equals(ConfigureWayClassWeightsDialog.REQUEST_WAY_CLASS_WEIGHTS_CHANGED)) {
            WayClassWeightSettings settings = (WayClassWeightSettings) bundle.getSerializable(ConfigureWayClassWeightsDialog.EXTRA_SETTINGS);
            WayClassWeightSettingsAdapter settingsAdapter = (WayClassWeightSettingsAdapter) recyclerViewAvailableWayClassWeightSettings.getAdapter();
            boolean settingsIsNew = ! settingsManagerInstance.containsWayClassWeightSettings(settings);
            if (settings != null && settingsAdapter != null) {
                settingsManagerInstance.addOrUpdateWayClassWeightSettings(settings);
                settingsAdapter.addOrUpdateItem(settings);
                if (settingsIsNew) updateDefaultWayClassWeightSettingsUi();
            }
        }
    }

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_manage_way_class_weight_settings, nullParent);

        spinnerSelectDefaultWayClassWeightSettings = view.findViewById(R.id.spinnerSelectDefaultWayClassWeightSettings);
        WayClassWeightSettingsSpinnerAdapter spinnerAdapter = new WayClassWeightSettingsSpinnerAdapter(
                requireContext(),
                settingsManagerInstance.getWayClassWeightSettingsList(),
                getResources().getString(R.string.labelDefaultWayClassWeightSettingsAlwaysAsk),
                true);
        spinnerSelectDefaultWayClassWeightSettings.setAdapter(spinnerAdapter);
        spinnerSelectDefaultWayClassWeightSettings.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView parent) {
                }
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                WayClassWeightSettings newSettings = (WayClassWeightSettings) parent.getItemAtPosition(position);
                Timber.d("onItemSelected: %1$d, %2$s", position, newSettings);
                if (newSettings != null) {
                    settingsManagerInstance.setDefaultWayClassWeightSettings(newSettings);
                } else {
                    settingsManagerInstance.clearDefaultWayClassWeightSettings();
                }
            }
        });

        TextView labelAvailableWayClassWeightSettingsHeading = view.findViewById(R.id.labelHeading);
        labelAvailableWayClassWeightSettingsHeading.setText(
                R.string.labelAvailableWayClassWeightSettingsHeading);

        ImageButton buttonAddWayClassWeightSettings = view.findViewById(R.id.buttonAdd);
        buttonAddWayClassWeightSettings.setContentDescription(
                getResources().getString(R.string.buttonAddWayClassWeightSettings));
        buttonAddWayClassWeightSettings.setOnClickListener(v -> {
            EnterWayClassWeightSettingsNameDialog.newInstance()
                .show(getChildFragmentManager(), "EnterWayClassWeightSettingsNameDialog");
        });
        buttonAddWayClassWeightSettings.setVisibility(View.VISIBLE);

        recyclerViewAvailableWayClassWeightSettings = view.findViewById(R.id.recyclerViewAvailableWayClassWeightSettings);
        recyclerViewAvailableWayClassWeightSettings.setLayoutManager(new LinearLayoutManager(requireContext()));

        layoutEmptyView = view.findViewById(R.id.layoutEmptyView);

        Button buttonRestoreDefaultWayClassWeightSettingsList = view.findViewById(R.id.buttonRestoreDefaultWayClassWeightSettingsList);
        buttonRestoreDefaultWayClassWeightSettingsList.setOnClickListener(v -> {
            settingsManagerInstance.restoreWayClassWeightSettingsListToDefaults();
            updateWayClassWeightSettingsListUi();
        });

        return new AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton(R.string.dialogClose, null)
            .create();
    }

    @Override public void onStart() {
        super.onStart();
        updateWayClassWeightSettingsListUi();
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            // add a single route profile back if the list is empty and the dialog closing
            if (settingsManagerInstance.getWayClassWeightSettingsList().isEmpty()) {
                settingsManagerInstance.addOrUpdateWayClassWeightSettings(
                        WayClassWeightSettings.createShortestRoute());
                settingsManagerInstance.clearDefaultWayClassWeightSettings();
            }
        }
    }

    private void updateDefaultWayClassWeightSettingsUi() {
        WayClassWeightSettingsSpinnerAdapter spinnerAdapter = (WayClassWeightSettingsSpinnerAdapter) spinnerSelectDefaultWayClassWeightSettings.getAdapter();
        if (spinnerAdapter == null) return;

        // remember previous spinner position
        int currentWayClassWeightSettingsSpinnerPosition = spinnerSelectDefaultWayClassWeightSettings.getSelectedItemPosition();

        spinnerAdapter.updateItems(
                settingsManagerInstance.getWayClassWeightSettingsList());

        // select item if selection index changed
        int newWayClassWeightSettingsSpinnerPosition = spinnerAdapter.indexOfItem(
                settingsManagerInstance.getDefaultWayClassWeightSettings());
        Timber.d("current: %1$d, new: %2$d", currentWayClassWeightSettingsSpinnerPosition, newWayClassWeightSettingsSpinnerPosition);
        if (currentWayClassWeightSettingsSpinnerPosition != newWayClassWeightSettingsSpinnerPosition) {
            spinnerSelectDefaultWayClassWeightSettings.setSelection(newWayClassWeightSettingsSpinnerPosition);
        }
    }

    private void updateWayClassWeightSettingsListUi() {
        WayClassWeightSettingsAdapter.OnSettingClickListener settingsListener = new WayClassWeightSettingsAdapter.OnSettingClickListener() {

            @Override public void onSettingsClick(WayClassWeightSettings settings) {
                ConfigureWayClassWeightsDialog.newInstance(settings)
                    .show(getChildFragmentManager(), "ConfigureWayClassWeightsDialog");
            }

            @Override public void onMoveClick(int from, int to) {
                WayClassWeightSettingsAdapter settingsAdapter = (WayClassWeightSettingsAdapter) recyclerViewAvailableWayClassWeightSettings.getAdapter();
                if (settingsAdapter != null && from >= 0 && to < settingsAdapter.getItemCount()) {
                    WayClassWeightSettings settingsToMove = settingsAdapter.getItem(from);
                    Timber.d("move %1$s from %2$d to %3$d", settingsToMove, from, to);
                    settingsManagerInstance.moveWayClassWeightSettings(from, to);
                    settingsAdapter.moveItem(from, to);
                    updateDefaultWayClassWeightSettingsUi();

                    recyclerViewAvailableWayClassWeightSettings.announceForAccessibility(
                            String.format(
                                getResources().getString(R.string.labelWayClassWeightSettingsMoved),
                                settingsToMove, (from+1), (to+1)));
                }
            }

            @Override public void onDeleteClick(WayClassWeightSettings settings) {
                WayClassWeightSettingsAdapter settingsAdapter = (WayClassWeightSettingsAdapter) recyclerViewAvailableWayClassWeightSettings.getAdapter();
                if (settings != null && settingsAdapter != null) {
                    settingsManagerInstance.removeWayClassWeightSettings(settings);
                    settingsAdapter.removeItem(settings);
                    updateDefaultWayClassWeightSettingsUi();
                }
            }
        };

        // to show or hide the empty view
        RecyclerView.AdapterDataObserver adapterDataObserver = new RecyclerView.AdapterDataObserver() {
            @Override public void onChanged() {
                showOrHideEmptyView();
            }
            @Override public void onItemRangeInserted(int positionStart, int itemCount) {
                showOrHideEmptyView();
            }
            @Override public void onItemRangeRemoved(int positionStart, int itemCount) {
                showOrHideEmptyView();
            }

            public void showOrHideEmptyView() {
                showOrHideEmptyWayClassWeightSettingsListView();
            }
        };

        // add adapter to recycler view
        WayClassWeightSettingsAdapter adapter = new WayClassWeightSettingsAdapter(
                requireContext(), settingsManagerInstance.getWayClassWeightSettingsList(), settingsListener);
        adapter.registerAdapterDataObserver(adapterDataObserver);
        recyclerViewAvailableWayClassWeightSettings.setAdapter(adapter);

        // drag and drop
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
            new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                @Override public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder vh, RecyclerView.ViewHolder target) {
                    settingsListener.onMoveClick(
                            vh.getAdapterPosition(), target.getAdapterPosition());
                    return true;
                }

                @Override public void onSwiped(RecyclerView.ViewHolder vh, int direction) {
                }
            });
        itemTouchHelper.attachToRecyclerView(recyclerViewAvailableWayClassWeightSettings);

        updateDefaultWayClassWeightSettingsUi();
        showOrHideEmptyWayClassWeightSettingsListView();
    }

    private void showOrHideEmptyWayClassWeightSettingsListView() {
        layoutEmptyView.setVisibility(
                settingsManagerInstance.getWayClassWeightSettingsList().isEmpty() ? View.VISIBLE : View.GONE);
    }


    public static class WayClassWeightSettingsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public interface OnSettingClickListener {
            public void onSettingsClick(WayClassWeightSettings settings);
            public void onMoveClick(int from, int to);
            public void onDeleteClick(WayClassWeightSettings settings);
        }

        private List<WayClassWeightSettings> settingsList;
        private OnSettingClickListener listener;
        private Context context;

        public WayClassWeightSettingsAdapter(Context context, List<WayClassWeightSettings> settingsList, OnSettingClickListener listener) {
            this.context = context;
            this.settingsList = settingsList;
            this.listener = listener;
        }

        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_way_class_weight_settings, parent, false);
            return new SettingViewHolder(view);
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof SettingViewHolder) {
                WayClassWeightSettings settings = settingsList.get(position);
                ((SettingViewHolder) holder).bind(settings);
            }
        }

        @Override public int getItemCount() {
            return settingsList.size();
        }

        public WayClassWeightSettings getItem(int position) {
           return position >= 0 && position < settingsList.size()
               ? settingsList.get(position) : null;
        }

        public void addOrUpdateItem(WayClassWeightSettings settings) {
            int index = settingsList.indexOf(settings);
            if (index >= 0) {
                settingsList.set(index, settings);
                notifyItemChanged(index);
            } else {
                settingsList.add(settings);
                notifyItemInserted(settingsList.size()-1);
            }
        }

        public void moveItem(int from, int to) {
            Collections.swap(settingsList, from, to);
            notifyItemMoved(from, to);
        }

        public void removeItem(WayClassWeightSettings settings) {
            int index = settingsList.indexOf(settings);
            if (index >= 0) {
                settingsList.remove(index);
                notifyItemRemoved(index);
            }
        }


        class SettingViewHolder extends RecyclerView.ViewHolder {
            TextView labelSettingName;
            ImageButton buttonDelete;

            SettingViewHolder(View itemView) {
                super(itemView);

                itemView.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && pos < settingsList.size() && listener != null) {
                        listener.onSettingsClick(settingsList.get(pos));
                    }
                });

                ViewCompat.setAccessibilityDelegate(itemView, new AccessibilityDelegateCompat() {
                    @Override public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                        super.onInitializeAccessibilityNodeInfo(host, info);

                        int position = getAdapterPosition();
                        if (position == RecyclerView.NO_POSITION) return;

                        if (position > 0) {
                            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                        R.id.action_move_up,
                                        context.getResources().getString(R.string.actionMoveUp)));
                        }

                        if (position < getItemCount() - 1) {
                            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                        R.id.action_move_down,
                                        context.getResources().getString(R.string.actionMoveDown)));
                        }
                    }

                    @Override public boolean performAccessibilityAction(View host, int action, Bundle args) {
                        int position = getAdapterPosition();

                        if (action == R.id.action_move_up && position > 0 && listener != null) {
                            listener.onMoveClick(position, position - 1);
                            return true;
                        } else if (action == R.id.action_move_down && position < getItemCount() - 1 && listener != null) {
                            listener.onMoveClick(position, position + 1);
                            return true;
                        }

                        return super.performAccessibilityAction(host, action, args);
                    }
                });

                // text label
                labelSettingName = itemView.findViewById(R.id.labelSettingName);

                // delete button
                buttonDelete = itemView.findViewById(R.id.buttonDelete);
                buttonDelete.setOnClickListener(v -> {
                    int pos = getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && pos < settingsList.size() && listener != null) {
                        listener.onDeleteClick(settingsList.get(pos));
                    }
                });
            }

            void bind(WayClassWeightSettings setting) {
                labelSettingName.setText(setting.getName());
                labelSettingName.setContentDescription(
                        String.format(
                            "%1$s.\n%2$s",
                            setting.getName(),
                            setting.formatWayClassWeightsForContentDescription()));

                buttonDelete.setContentDescription(
                        String.format(
                            GlobalInstance.getStringResource(R.string.buttonRemoveWayClassWeightSettings),
                            setting.getName()));
            }
        }
    }


    public static class EnterWayClassWeightSettingsNameDialog extends EnterStringDialog {
        public static final String REQUEST_ENTER_WAY_CLASS_WEIGHT_SETTINGS_NAME = "requestEnterWayClassWeightSettingsName";
        public static final String EXTRA_NAME = "extraName";

        public static EnterWayClassWeightSettingsNameDialog newInstance() {
            return new EnterWayClassWeightSettingsNameDialog();
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            setDialogTitle(
                    getResources().getString(R.string.enterWayClassWeightSettingsNameDialogTitle));
            setMissingInputMessage(
                    getResources().getString(R.string.messageWayClassWeightSettingsNameIsMissing));
            return super.onCreateDialog(savedInstanceState);
        }

        @Override public void execute(String input) {
            Bundle result = new Bundle();
            result.putSerializable(EXTRA_NAME, input);
            getParentFragmentManager().setFragmentResult(REQUEST_ENTER_WAY_CLASS_WEIGHT_SETTINGS_NAME, result);
            dismiss();
        }
    }


    public static class ConfigureWayClassWeightsDialog extends DialogFragment implements AdapterView.OnItemSelectedListener {
        public static final String REQUEST_WAY_CLASS_WEIGHTS_CHANGED = "wayClassWeightsChanged";
        public static final String EXTRA_SETTINGS = "extra.wayClassWeightSettings";

        public static ConfigureWayClassWeightsDialog newInstance(WayClassWeightSettings settings) {
            ConfigureWayClassWeightsDialog dialog = new ConfigureWayClassWeightsDialog();
            Bundle args = new Bundle();
            args.putSerializable(KEY_SETTINGS, settings);
            dialog.setArguments(args);
            return dialog;
        }

        // dialog
        private static final String KEY_SETTINGS = "wayClassWeightSettings";

        private WayClassWeightSettings wayClassWeightSettings;
        private TableLayout tableLayout;

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            wayClassWeightSettings = savedInstanceState != null
                ? (WayClassWeightSettings) savedInstanceState.getSerializable(KEY_SETTINGS)
                : (WayClassWeightSettings) getArguments().getSerializable(KEY_SETTINGS);

            tableLayout = new TableLayout(
                    ConfigureWayClassWeightsDialog.this.getContext());
            tableLayout.setLayoutParams(
                    new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
            tableLayout.setColumnStretchable(1, true);

            ScrollView scrollView = new ScrollView(
                    ConfigureWayClassWeightsDialog.this.getContext());
            scrollView.setLayoutParams(
                    new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            scrollView.addView(tableLayout);

            // create dialog
            return new AlertDialog.Builder(requireContext())
                .setTitle(wayClassWeightSettings.getName())
                .setView(scrollView)
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
            final AlertDialog dialog = (AlertDialog) getDialog();
            if (dialog != null) {

                // positive button
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        Bundle result = new Bundle();
                        result.putSerializable(EXTRA_SETTINGS, wayClassWeightSettings);
                        getParentFragmentManager().setFragmentResult(REQUEST_WAY_CLASS_WEIGHTS_CHANGED, result);
                        dialog.dismiss();
                    }
                });

                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }

            populateTableLayout();
        }

        private void populateTableLayout() {
            final TableRow.LayoutParams lpTableRowChild = new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
            tableLayout.removeAllViews();

            for (WayClassType type : WayClassType.values()) {

                Spinner spinner = new Spinner(
                        ConfigureWayClassWeightsDialog.this.getContext(),
                        Spinner.MODE_DIALOG);
                spinner.setId(type.ordinal());
                spinner.setLayoutParams(lpTableRowChild);
                spinner.setTag(type);
                spinner.setPrompt(type.toString());
                spinner.setOnItemSelectedListener(this);

                ArrayList<WayClassWeight> wayClassWeightList =
                    new ArrayList<WayClassWeight>(
                            Arrays.asList(WayClassWeight.values()));
                // create weight adapter
                ArrayAdapter<WayClassWeight> wayClassWeightAdapter = new ArrayAdapter<WayClassWeight>(
                        ConfigureWayClassWeightsDialog.this.getContext(),
                        android.R.layout.select_dialog_singlechoice,
                        wayClassWeightList);
                wayClassWeightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                // set adapter
                spinner.setAdapter(wayClassWeightAdapter);
                // set weight selection
                spinner.setSelection(
                        wayClassWeightList.indexOf(
                            wayClassWeightSettings.getWeightFor(type)));

                TextView label = new TextViewBuilder(
                        ConfigureWayClassWeightsDialog.this.getContext(),
                        type.toString(),
                        lpTableRowChild)
                    .setId(WayClassType.values().length + spinner.getId() + 1)
                    .isLabelFor(spinner.getId())
                    .centerTextVertically()
                    .create();

                TableRow tableRow = new TableRow(
                        ConfigureWayClassWeightsDialog.this.getContext());
                tableRow.setLayoutParams(
                        new TableRow.LayoutParams(
                            TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tableRow.addView(label);
                tableRow.addView(spinner);

                tableLayout.addView(tableRow);
            }
        }

        @Override public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            savedInstanceState.putSerializable(KEY_SETTINGS, wayClassWeightSettings);
        }

        @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            wayClassWeightSettings.setWeightFor(
                    (WayClassType) parent.getTag(),
                    (WayClassWeight) parent.getItemAtPosition(position));
        }

        @Override public void onNothingSelected(AdapterView parent) {
            }
    }

}
