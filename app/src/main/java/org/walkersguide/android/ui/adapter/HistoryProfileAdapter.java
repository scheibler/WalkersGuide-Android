package org.walkersguide.android.ui.adapter;

import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import org.walkersguide.android.ui.view.ProfileView;
import org.walkersguide.android.database.profile.static_profile.HistoryProfile;
import android.widget.TextView;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;
import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import android.widget.BaseExpandableListAdapter;
import java.util.LinkedHashMap;


public class HistoryProfileAdapter extends BaseExpandableListAdapter {

    private enum Group {
        POINTS, ROUTES, OVERVIEW
    }

    private Context context;
    private HashMap<Group,ArrayList<HistoryProfile>> historyProfileMap;

    public HistoryProfileAdapter(Context context) {
        this.context = context;
        this.historyProfileMap = new LinkedHashMap<Group,ArrayList<HistoryProfile>>();
        this.historyProfileMap.put(
                Group.POINTS, HistoryProfile.getPointsHistoryProfileList());
        this.historyProfileMap.put(
                Group.ROUTES, HistoryProfile.getRoutesHistoryProfileList());
        this.historyProfileMap.put(
                Group.OVERVIEW, HistoryProfile.getOverviewHistoryProfileList());
    }

    @Override public View getGroupView(int groupPosition, boolean isExpanded,
            View convertView, ViewGroup parent) {
        EntryHolderParent holder;
        if (convertView == null) {
            holder = new EntryHolderParent();
            convertView = LayoutInflater.from(context).inflate(R.layout.layout_single_text_view_heading, parent, false);
            holder.label = (TextView) convertView.findViewById(R.id.labelHeading);
            convertView.setTag(holder);
        } else {
            holder = (EntryHolderParent) convertView.getTag();
        }

        String text = null;
        switch (getGroup(groupPosition)) {
            case POINTS:
                text = GlobalInstance.getStringResource(R.string.historyProfileGroupPoints);
                break;
            case ROUTES:
                text = GlobalInstance.getStringResource(R.string.historyProfileGroupRoutes);
                break;
            case OVERVIEW:
                text = GlobalInstance.getStringResource(R.string.historyProfileGroupOverview);
                break;
        }
        holder.label.setText(text);

        return convertView;
    }

    @Override public Group getGroup(int groupPosition) {
        return (new ArrayList<Group>(historyProfileMap.keySet())).get(groupPosition);
    }

    @Override public int getGroupCount() {
        return historyProfileMap.keySet().size();
    }

    @Override public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override public View getChildView(int groupPosition, int childPosition,
            boolean isLastChild, View convertView, ViewGroup parent) {
        ProfileView layoutHistoryProfile = null;
        if (convertView == null) {
            layoutHistoryProfile = new ProfileView(this.context);
            layoutHistoryProfile.setLayoutParams(
                    new LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        } else {
            layoutHistoryProfile = (ProfileView) convertView;
        }
        layoutHistoryProfile.configureAsListItem(getChild(groupPosition, childPosition), false, false);
        return layoutHistoryProfile;
    }

    @Override public HistoryProfile getChild(int groupPosition, int childPosition) {
        return historyProfileMap.get(getGroup(groupPosition)).get(childPosition);
    }

    @Override public int getChildrenCount(int groupPosition) {
        return historyProfileMap.get(getGroup(groupPosition)).size();
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


    private class EntryHolderParent {
        public TextView label;
    }

    private class EntryHolderChild {
        public ProfileView layoutHistoryProfile;
    }

}
