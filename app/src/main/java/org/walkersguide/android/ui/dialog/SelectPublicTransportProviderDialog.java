package org.walkersguide.android.ui.dialog;

import android.os.Build;
import android.graphics.Color;
import android.graphics.Typeface;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import android.support.v4.content.LocalBroadcastManager;
import de.schildbach.pte.AbstractNetworkProvider;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;

import android.support.v4.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;

import org.walkersguide.android.R;
import org.walkersguide.android.pt.PTHelper;
import org.walkersguide.android.pt.PTHelper.Country;
import org.walkersguide.android.util.SettingsManager;

import java.util.ArrayList;
import android.widget.ListView;
import android.widget.CheckedTextView;


public class SelectPublicTransportProviderDialog extends DialogFragment {

    public static final String NEW_NETWORK_PROVIDER = "org.walkersguide.android.intent.new_network_provider";

    // Store instance variables
    private SettingsManager settingsManagerInstance;
    private int listPosition;

    // ui components
    private ExpandableListView listViewNetworkProvider;

    public static SelectPublicTransportProviderDialog newInstance() {
        SelectPublicTransportProviderDialog selectPublicTransportProviderDialogInstance = new SelectPublicTransportProviderDialog();
        return selectPublicTransportProviderDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt("listPosition");
        } else {
            listPosition = -1;
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_select_network_provider, nullParent);
        listViewNetworkProvider = (ExpandableListView) view.findViewById(R.id.expandableListView);
        listViewNetworkProvider.setOnChildClickListener(new OnChildClickListener() {
            @Override public boolean onChildClick(ExpandableListView parent, View v,
                    int groupPosition, int childPosition, long id) {
                AbstractNetworkProvider provider = ((NetworkProviderAdapter) parent.getExpandableListAdapter())
                    .getChild(groupPosition, childPosition);
                if (provider != null) {
                    settingsManagerInstance.getServerSettings().setSelectedPublicTransportProvider(provider);
                    Intent intent = new Intent(NEW_NETWORK_PROVIDER);
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectPublicTransportProviderDialogTitle))
            .setView(view)
            .setNeutralButton(
                    getResources().getString(R.string.dialogSomethingMissing),
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

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {
            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    SendFeedbackDialog.newInstance(
                            SendFeedbackDialog.Token.PT_PROVIDER_REQUEST)
                        .show(getActivity().getSupportFragmentManager(), "SendFeedbackDialog");
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
        // fill list
        NetworkProviderAdapter adapter = new NetworkProviderAdapter(getActivity());
        listViewNetworkProvider.setAdapter(adapter);
        for (int i=0; i<adapter.getGroupCount(); i++) {
            listViewNetworkProvider.expandGroup(i);
        }
        // list selection
        listViewNetworkProvider.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listViewNetworkProvider.setItemChecked(
                adapter.getIndexOfNetworkProvider(
                    settingsManagerInstance.getServerSettings().getSelectedPublicTransportProvider()), true);
        // list position
        listViewNetworkProvider.setSelection(listPosition);
        listViewNetworkProvider.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (listPosition != firstVisibleItem) {
                    listPosition = firstVisibleItem;
                }
            }
        });
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("listPosition",  listPosition);
    }


    /**
     * provider adapter
     */

    public class NetworkProviderAdapter extends BaseExpandableListAdapter {

        private Context context;

        public NetworkProviderAdapter(Context context) {
            this.context = context;
        }

        @Override public View getGroupView(int groupPosition, boolean isExpanded,
                View convertView, ViewGroup parent) {
            EntryHolderParent holder;
            if (convertView == null) {
                holder = new EntryHolderParent();
                convertView = LayoutInflater.from(context).inflate(R.layout.layout_single_text_view, parent, false);
                holder.labelCountry = (TextView) convertView.findViewById(R.id.label);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    holder.labelCountry.setAccessibilityHeading(true);
                }
                convertView.setTag(holder);
            } else {
                holder = (EntryHolderParent) convertView.getTag();
            }

            SpannableString countryString = null;
            // id -> name
            Country country = getGroup(groupPosition);
            switch (country) {
                case EUROPE:
                    countryString = new SpannableString(
                            context.getResources().getString(R.string.countryEurope));
                    break;
                case GERMANY:
                    countryString = new SpannableString(
                            context.getResources().getString(R.string.countryGermany));
                    break;
                case SWITZERLAND:
                    countryString = new SpannableString(
                            context.getResources().getString(R.string.countrySwitzerland));
                    break;
                default:
                    countryString = new SpannableString(country.name());
                    break;
            }
            // bold and red
            countryString.setSpan(
                    new StyleSpan(Typeface.BOLD), 0, countryString.length(), 0);
            countryString.setSpan(
                    new ForegroundColorSpan(Color.rgb(215, 0, 0)), 0, countryString.length(), 0);

            holder.labelCountry.setText(
                    String.format(
                        "%1$s (%2$d)", countryString, getChildrenCount(groupPosition)));
            return convertView;
        }

        @Override public Country getGroup(int groupPosition) {
            return (new ArrayList<Country>(PTHelper.supportedNetworkProviderMap.keySet())).get(groupPosition);
        }

        @Override public int getGroupCount() {
            return PTHelper.supportedNetworkProviderMap.keySet().size();
        }

        @Override public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override public View getChildView(int groupPosition, int childPosition,
                boolean isLastChild, View convertView, ViewGroup parent) {
            // load item layout
            EntryHolderChild holder;
            if (convertView == null) {
                holder = new EntryHolderChild();
                convertView = LayoutInflater.from(context).inflate(R.layout.layout_single_checked_text_view, parent, false);
                holder.labelProvider = (CheckedTextView) convertView.findViewById(R.id.label);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolderChild) convertView.getTag();
            }
            holder.labelProvider.setPadding(
                    context.getResources().getDimensionPixelOffset(R.dimen.smallPadding), 0, 0, 0);
            holder.labelProvider.setText(
                    PTHelper.getNetworkProviderName(
                        context, getChild(groupPosition, childPosition)));
            return convertView;
        }

        @Override public AbstractNetworkProvider getChild(int groupPosition, int childPosition) {
            return PTHelper.supportedNetworkProviderMap.get(getGroup(groupPosition)).get(childPosition);
        }

        @Override public int getChildrenCount(int groupPosition) {
            return PTHelper.supportedNetworkProviderMap.get(getGroup(groupPosition)).size();
        }

        @Override public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override public boolean hasStableIds() {
            return true;
        }

        @Override public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public int getIndexOfNetworkProvider(AbstractNetworkProvider selectedProvider) {
            int index = 0;
            for (Country country : PTHelper.supportedNetworkProviderMap.keySet()) { 
                index += 1;
                for (AbstractNetworkProvider provider : PTHelper.supportedNetworkProviderMap.get(country)) {
                    if (provider.equals(selectedProvider)) {
                        return index;
                    }
                    index += 1;
                }
            }
            return -1;
        }

        private class EntryHolderParent {
            public TextView labelCountry;
        }

        private class EntryHolderChild {
            public CheckedTextView labelProvider;
        }
    }

}
