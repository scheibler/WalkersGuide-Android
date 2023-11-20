package org.walkersguide.android.ui.adapter;

import org.walkersguide.android.ui.view.ProfileView;
import org.walkersguide.android.ui.view.ProfileView.OnProfileDefaultActionListener;

import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;



import android.view.View;
import android.view.ViewGroup;


import java.util.ArrayList;
import android.content.Context;
import android.widget.BaseAdapter;
import org.walkersguide.android.data.Profile;


public class ProfileAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<? extends Profile> profileList;
    private OnProfileDefaultActionListener onProfileDefaultActionListener;
    private boolean showContextMenuItemRemove;

    public ProfileAdapter(Context context, ArrayList<? extends Profile> profileList,
            OnProfileDefaultActionListener onProfileDefaultActionListener,
            boolean showContextMenuItemRemove) {
        this.context = context;
        this.profileList = profileList;
        this.onProfileDefaultActionListener = onProfileDefaultActionListener;
        this.showContextMenuItemRemove = showContextMenuItemRemove;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        ProfileView layoutProfileView = null;
        if (convertView == null) {
            layoutProfileView = new ProfileView(this.context);
            layoutProfileView.setLayoutParams(
                    new LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        } else {
            layoutProfileView = (ProfileView) convertView;
        }

        layoutProfileView.setOnProfileDefaultActionListener(onProfileDefaultActionListener);
        layoutProfileView.configureAsListItem(
                getItem(position), false, showContextMenuItemRemove);
        return layoutProfileView;
    }

    @Override public int getCount() {
        return this.profileList.size();
    }

    @Override public Profile getItem(int position) {
        return this.profileList.get(position);
    }

    @Override public long getItemId(int position) {
        return position;
    }

    public boolean isEmpty() {
        return this.profileList.isEmpty();
    }


    private class EntryHolder {
        public ProfileView layoutProfileView;
    }
}
