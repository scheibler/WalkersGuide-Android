package org.walkersguide.android.ui.dialog.select;



import de.schildbach.pte.AbstractNetworkProvider;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

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
import org.walkersguide.android.server.pt.PtUtility;
import org.walkersguide.android.server.pt.PtUtility.Country;

import java.util.ArrayList;
import android.widget.ListView;
import android.widget.CheckedTextView;
import org.walkersguide.android.ui.dialog.SendFeedbackDialog;
import de.schildbach.pte.NetworkId;
import java.util.Locale;


public class SelectPublicTransportProviderDialog extends DialogFragment {
    public static final String REQUEST_SELECT_PT_PROVIDER = "selectPtProvider";
    public static final String EXTRA_NETWORK_ID = "networkId";


    // instance constructors

    public static SelectPublicTransportProviderDialog newInstance(NetworkId selectedId) {
        SelectPublicTransportProviderDialog dialog = new SelectPublicTransportProviderDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SELECTED_ID, selectedId);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_SELECTED_ID = "selectedId";
    private static final String KEY_LISTPOSITION = "listPosition";

    private NetworkId selectedId;
    private int listPosition;

    private ExpandableListView listViewNetworkProvider;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        selectedId = (NetworkId) getArguments().getSerializable(KEY_SELECTED_ID);
        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt(KEY_LISTPOSITION);
        } else {
            listPosition = 0;
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_single_expandable_list_view, nullParent);

        listViewNetworkProvider = (ExpandableListView) view.findViewById(R.id.expandableListView);
        listViewNetworkProvider.setOnChildClickListener(new OnChildClickListener() {
            @Override public boolean onChildClick(ExpandableListView parent, View v,
                    int groupPosition, int childPosition, long id) {
                AbstractNetworkProvider provider = ((NetworkProviderAdapter) parent.getExpandableListAdapter())
                    .getChild(groupPosition, childPosition);
                if (provider != null) {
                    Bundle result = new Bundle();
                    result.putSerializable(EXTRA_NETWORK_ID, provider.id());
                    getParentFragmentManager().setFragmentResult(REQUEST_SELECT_PT_PROVIDER, result);
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
                            SendFeedbackDialog.FeedbackToken.PT_PROVIDER_REQUEST)
                        .show(getChildFragmentManager(), "SendFeedbackDialog");
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
                adapter.getIndexOfNetworkProvider(selectedId), true);
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
        savedInstanceState.putInt(KEY_LISTPOSITION,  listPosition);
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
                convertView = LayoutInflater.from(context).inflate(R.layout.layout_single_text_view_heading, parent, false);
                holder.labelCountry = (TextView) convertView.findViewById(R.id.labelHeading);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolderParent) convertView.getTag();
            }
            holder.labelCountry.setText(
                    String.format(
                        Locale.getDefault(),
                        "%1$s (%2$d)",
                        getGroup(groupPosition).toString(),
                        getChildrenCount(groupPosition)));
            return convertView;
        }

        @Override public Country getGroup(int groupPosition) {
            return (new ArrayList<Country>(PtUtility.supportedNetworkProviderMap.keySet())).get(groupPosition);
        }

        @Override public int getGroupCount() {
            return PtUtility.supportedNetworkProviderMap.keySet().size();
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
                convertView = LayoutInflater.from(context).inflate(R.layout.layout_single_text_view_checked, parent, false);
                holder.labelProvider = (CheckedTextView) convertView.findViewById(R.id.label);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolderChild) convertView.getTag();
            }
            holder.labelProvider.setPadding(
                    context.getResources().getDimensionPixelOffset(R.dimen.smallPadding), 0, 0, 0);
            holder.labelProvider.setText(
                    PtUtility.getNameForNetworkId(
                        getChild(groupPosition, childPosition).id()));
            return convertView;
        }

        @Override public AbstractNetworkProvider getChild(int groupPosition, int childPosition) {
            return PtUtility.supportedNetworkProviderMap.get(getGroup(groupPosition)).get(childPosition);
        }

        @Override public int getChildrenCount(int groupPosition) {
            return PtUtility.supportedNetworkProviderMap.get(getGroup(groupPosition)).size();
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

        public int getIndexOfNetworkProvider(NetworkId selectedId) {
            int index = 0;
            for (Country country : PtUtility.supportedNetworkProviderMap.keySet()) { 
                index += 1;
                for (AbstractNetworkProvider provider : PtUtility.supportedNetworkProviderMap.get(country)) {
                    if (selectedId == provider.id()) {
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
