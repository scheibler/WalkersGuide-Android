package org.walkersguide.android.ui.adapter;

import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.walkersguide.android.R;
import org.walkersguide.android.util.SettingsManager;


public class POIProfilePointAdapter extends ArrayAdapter<PointProfileObject> {

    private Context context;
    private LayoutInflater inflater;
    private ArrayList<PointProfileObject> pointProfileObjectList;

    public POIProfilePointAdapter(Context context, ArrayList<PointProfileObject> pointProfileObjectList) {
        super(context, R.layout.layout_poi_profile_point_list_entry);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.pointProfileObjectList = pointProfileObjectList;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        // load item layout
        EntryHolder holder;
        if (convertView == null) {
            convertView = this.inflater.inflate(R.layout.layout_poi_profile_point_list_entry, parent, false);
            holder = new EntryHolder();
            holder.imagePointIsFavorite = (ImageView) convertView.findViewById(R.id.imagePointIsFavorite);
            holder.labelPOIProfilePoint = (TextView) convertView.findViewById(R.id.labelPOIProfilePoint);
            convertView.setTag(holder);
        } else {
            holder = (EntryHolder) convertView.getTag();
        }
        PointProfileObject pointProfileObject = getItem(position);
        // favorite image view
        if (holder.imagePointIsFavorite != null) {
            int selectedPOIProfileId = SettingsManager.getInstance(context).getPOISettings().getSelectedPOIProfileId();
            if (AccessDatabase.getInstance(context).getCheckedProfileIdsForFavoritePoint(pointProfileObject, false).contains(selectedPOIProfileId)) {
                holder.imagePointIsFavorite.setVisibility(View.VISIBLE);
            } else {
                holder.imagePointIsFavorite.setVisibility(View.INVISIBLE);
            }
        }
        // label
        if (holder.labelPOIProfilePoint != null) {
            holder.labelPOIProfilePoint.setText(pointProfileObject.toString());
        }
        return convertView;
    }

    @Override public int getCount() {
        if (this.pointProfileObjectList != null) {
            return this.pointProfileObjectList.size();
        }
        return 0;
    }

    @Override public PointProfileObject getItem(int position) {
        if (this.pointProfileObjectList != null) {
            return this.pointProfileObjectList.get(position);
        }
        return null;
    }


    private class EntryHolder {
        public ImageView imagePointIsFavorite;
        public TextView labelPOIProfilePoint;
    }

}
