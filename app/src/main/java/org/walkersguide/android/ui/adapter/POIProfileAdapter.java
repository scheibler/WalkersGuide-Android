package org.walkersguide.android.ui.adapter;

import android.content.Context;

import android.support.v7.app.AppCompatActivity;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

import java.util.ArrayList;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.ui.dialog.CreateOrEditPOIProfileDialog;
import org.walkersguide.android.ui.dialog.RemovePOIProfileDialog;
import org.walkersguide.android.util.SettingsManager;


public class POIProfileAdapter extends ArrayAdapter<Integer> {

    private Context context;
    private LayoutInflater inflater;
    private ArrayList<Integer> poiProfileIdList;

    public POIProfileAdapter(Context context, ArrayList<Integer> poiProfileIdList) {
        super(context, R.layout.layout_poi_profile_list_entry);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.poiProfileIdList = poiProfileIdList;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        // load item layout
        EntryHolder holder;
        if (convertView == null) {
            convertView = this.inflater.inflate(R.layout.layout_poi_profile_list_entry, parent, false);
            holder = new EntryHolder();
            holder.labelPOIProfileName = (TextView) convertView.findViewById(R.id.labelPOIProfileName);
            holder.buttonActionForPOIProfile = (ImageButton) convertView.findViewById(R.id.buttonActionForPOIProfile);
            convertView.setTag(holder);
        } else {
            holder = (EntryHolder) convertView.getTag();
        }

        Integer poiProfileId = getItem(position);
        String poiProfileName = AccessDatabase.getInstance(context).getPOIProfileMap().get(poiProfileId);

        // label
        if (holder.labelPOIProfileName != null) {
            if (poiProfileId == SettingsManager.getInstance(context).getPOISettings().getSelectedPOIProfileId()) {
                holder.labelPOIProfileName.setText(StringUtility.boldAndRed(poiProfileName));
                holder.labelPOIProfileName.setContentDescription(
                        String.format(
                            "%1$s: %2$s",
                            context.getResources().getString(R.string.contentDescriptionSelected),
                            poiProfileName)
                        );
            } else {
                holder.labelPOIProfileName.setText(poiProfileName);
                holder.labelPOIProfileName.setContentDescription(poiProfileName);
            }
        }

        // action button
        if (holder.buttonActionForPOIProfile != null) {
            holder.buttonActionForPOIProfile.setContentDescription(
                    String.format(
                        "%1$s %2$s",
                        context.getResources().getString(R.string.buttonActionForPOIProfile),
                        poiProfileName)
                    );
            holder.buttonActionForPOIProfile.setTag(poiProfileId);
            holder.buttonActionForPOIProfile.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    showActionsMenu(view, (Integer) view.getTag());
                }
            });
        }

        return convertView;
    }

    @Override public int getCount() {
        if (this.poiProfileIdList != null) {
            return this.poiProfileIdList.size();
        }
        return 0;
    }

    @Override public Integer getItem(int position) {
        return this.poiProfileIdList.get(position);
    }

    @Override public void notifyDataSetChanged() {
        this.poiProfileIdList = new ArrayList(AccessDatabase.getInstance(context).getPOIProfileMap().keySet());
        super.notifyDataSetChanged();
    }

    private void showActionsMenu(View view, final int poiProfileId) {
        PopupMenu popupActionMenu = new PopupMenu(context, view);
        popupActionMenu.inflate(R.menu.menu_adapter_poi_profile_actions);
        popupActionMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menuItemEditProfile:
                        CreateOrEditPOIProfileDialog.newInstance(poiProfileId)
                            .show(((AppCompatActivity) context).getSupportFragmentManager(), "CreateOrEditPOIProfileDialog");
                        return true;
                    case R.id.menuItemRemoveProfile:
                        RemovePOIProfileDialog.newInstance(poiProfileId)
                            .show(((AppCompatActivity) context).getSupportFragmentManager(), "RemovePOIProfileDialog");
                        return true;
                    default:
                        return false;
                }
            }
        });
        popupActionMenu.show();
    }


    private class EntryHolder {
        public TextView labelPOIProfileName;
        public ImageButton buttonActionForPOIProfile;
    }

}
