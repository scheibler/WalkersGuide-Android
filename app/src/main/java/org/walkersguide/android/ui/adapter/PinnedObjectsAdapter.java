package org.walkersguide.android.ui.adapter;

import android.view.ViewGroup.MarginLayoutParams;
import org.walkersguide.android.ui.view.ProfileView;
import org.walkersguide.android.database.profile.StaticProfile;
import android.widget.LinearLayout.LayoutParams;
import org.walkersguide.android.R;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import android.widget.ImageButton;
import org.walkersguide.android.data.ObjectWithId;
import java.util.ArrayList;
import org.walkersguide.android.util.GlobalInstance;
import android.widget.LinearLayout;

import android.content.Context;
import org.walkersguide.android.data.Profile;
import android.widget.BaseExpandableListAdapter;
import org.walkersguide.android.ui.view.TextViewAndActionButton;


public class PinnedObjectsAdapter extends BaseExpandableListAdapter {
    private static final int ID_PROFILES = 0;
    private static final int ID_OBJECTS = 1;

    public interface OnAddButtonClick {
        public void onAddPinnedProfileButtonClicked(View view);
        public void onAddPinnedObjectButtonClicked(View view);
    }


    private Context context;
    private OnAddButtonClick onAddButtonClick;
    private ArrayList<Profile> profileList;
    private ArrayList<ObjectWithId> objectList;

    public PinnedObjectsAdapter(Context context, OnAddButtonClick onAddButtonClick,
            ArrayList<Profile> profileList, ArrayList<ObjectWithId> objectList) {
        this.context = context;
        this.onAddButtonClick = onAddButtonClick;
        this.profileList = profileList;
        this.objectList = objectList;
    }

    @Override public View getGroupView(int groupPosition, boolean isExpanded,
            View convertView, ViewGroup parent) {
        EntryHolderParent holder;
        if (convertView == null) {
            holder = new EntryHolderParent();
            convertView = LayoutInflater.from(context).inflate(R.layout.layout_heading_and_add_button, parent, false);
            holder.layoutHeadingAndAddButton = (LinearLayout) convertView.findViewById(R.id.layoutHeadingAndAddButton);
            holder.labelHeading = (TextView) convertView.findViewById(R.id.labelHeading);
            holder.buttonAdd = (ImageButton) convertView.findViewById(R.id.buttonAdd);
            // set top margin
            MarginLayoutParams lp = (MarginLayoutParams) holder.layoutHeadingAndAddButton.getLayoutParams();
            lp.topMargin = (int) (10 * GlobalInstance.getContext().getResources().getDisplayMetrics().density);
            holder.layoutHeadingAndAddButton.setLayoutParams(lp);
            convertView.setTag(holder);
        } else {
            holder = (EntryHolderParent) convertView.getTag();
        }

        String quantityString = null;
        switch (groupPosition) {
            case ID_PROFILES:
                quantityString = GlobalInstance.getPluralResource(
                        R.plurals.profile, profileList.size());
                holder.buttonAdd.setContentDescription(
                        context.getResources().getString(R.string.buttonAddPinnedProfile));
                holder.buttonAdd.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        onAddButtonClick.onAddPinnedProfileButtonClicked(view);
                    }
                });
                break;

            case ID_OBJECTS:
                quantityString = GlobalInstance.getPluralResource(
                        StaticProfile.pinnedPointsAndRoutes().getPluralResId(), objectList.size());
                holder.buttonAdd.setContentDescription(
                        context.getResources().getString(R.string.buttonAddPinnedPointOrRoute));
                holder.buttonAdd.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        onAddButtonClick.onAddPinnedObjectButtonClicked(view);
                    }
                });
                break;
        }

        if (quantityString != null) {
            holder.labelHeading.setText(
                    String.format(
                        context.getResources().getString(R.string.labelSomethingPinned),
                        quantityString)
                    );
            holder.buttonAdd.setVisibility(View.VISIBLE);
        }
        return convertView;
    }

    @Override public Integer getGroup(int groupPosition) {
        return Integer.valueOf(groupPosition);
    }

    @Override public int getGroupCount() {
        return 2;
    }

    @Override public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override public View getChildView(int groupPosition, int childPosition,
            boolean isLastChild, View convertView, ViewGroup parent) {
        Object childObject = getChild(groupPosition, childPosition);

        if (childObject instanceof Profile) {
            ProfileView profileView = new ProfileView(this.context);
            profileView.setLayoutParams(
                    new LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            profileView.configure((Profile) childObject, true, false);
            return profileView;

        } else if (childObject instanceof ObjectWithId) {
            TextViewAndActionButton objectView = new TextViewAndActionButton(this.context, null, true);
            objectView.setLayoutParams(
                    new LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            objectView.configureAsListItem((ObjectWithId) childObject, true, null);
            objectView.setAutoUpdate(true);
            return objectView;

        } else {
            return convertView;
        }
    }

    @Override public Object getChild(int groupPosition, int childPosition) {
        switch (groupPosition) {
            case ID_PROFILES:
                return this.profileList.get(childPosition);
            case ID_OBJECTS:
                return this.objectList.get(childPosition);
        }
        return null;
    }

    @Override public int getChildrenCount(int groupPosition) {
        switch (groupPosition) {
            case ID_PROFILES:
                return this.profileList.size();
            case ID_OBJECTS:
                return this.objectList.size();
        }
        return 0;
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
        public LinearLayout layoutHeadingAndAddButton;
        public TextView labelHeading;
        public ImageButton buttonAdd;
    }

}
