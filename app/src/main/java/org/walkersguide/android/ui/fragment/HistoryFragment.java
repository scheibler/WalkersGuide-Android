package org.walkersguide.android.ui.fragment;

import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import org.walkersguide.android.ui.view.ProfileView;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.database.DatabaseProfile.ForObjects;
import org.walkersguide.android.database.profile.static_profile.HistoryProfile;
import androidx.appcompat.app.AppCompatActivity;
import org.walkersguide.android.ui.fragment.TabLayoutFragment;
import org.walkersguide.android.ui.fragment.TabLayoutFragment.AbstractTabAdapter;
import android.text.format.DateFormat;
import android.widget.TextView;
import java.util.Date;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.server.wg.status.OSMMap;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.R;
import android.content.Context;


import android.os.Bundle;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;


import androidx.fragment.app.Fragment;
import org.walkersguide.android.util.SettingsManager;


import org.walkersguide.android.ui.dialog.SimpleMessageDialog;

import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.wg.status.ServerStatusTask;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.server.wg.WgException;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.Intent;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.fragment.app.FragmentActivity;
import timber.log.Timber;
import org.walkersguide.android.database.SortMethod;
import org.walkersguide.android.ui.fragment.object_list.extended.ObjectListFromDatabaseFragment;
import java.util.ArrayList;
import java.util.Arrays;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.MainActivityController;
import java.util.HashMap;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.BaseExpandableListAdapter;
import java.util.LinkedHashMap;


public class HistoryFragment extends RootFragment {

	public static HistoryFragment newInstance() {
		HistoryFragment fragment = new HistoryFragment();
		return fragment;
	}


	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    }


    /**
     * create view
     */
    private ExpandableListView listViewHistoryProfile;

    @Override public String getTitle() {
        return getResources().getString(R.string.fragmentHistoryName);
    }

    @Override public int getLayoutResourceId() {
        return R.layout.layout_single_expandable_list_view;
    }

	@Override public View configureView(View view, Bundle savedInstanceState) {
        HistoryProfileAdapter adapter = new HistoryProfileAdapter(getActivity());
        listViewHistoryProfile = (ExpandableListView) view.findViewById(R.id.expandableListView);
        listViewHistoryProfile.setAdapter(adapter);
        for (int i=0; i<adapter.getGroupCount(); i++) {
            listViewHistoryProfile.expandGroup(i);
        }

        return view;
    }


    public class HistoryProfileAdapter extends BaseExpandableListAdapter {

        private Context context;
        private HashMap<ForObjects,ArrayList<HistoryProfile>> historyProfileMap;

        public HistoryProfileAdapter(Context context) {
            this.context = context;
            this.historyProfileMap = new LinkedHashMap<ForObjects,ArrayList<HistoryProfile>>();

            for (HistoryProfile profile : HistoryProfile.getHistoryProfileList()) {
                ForObjects key = profile.getForObjects();
                // prepare list
                ArrayList<HistoryProfile> profileList = historyProfileMap.containsKey(key)
                    ? historyProfileMap.get(key) : new ArrayList<HistoryProfile>();
                profileList.add(profile);
                // add to map
                historyProfileMap.put(key, profileList);
            }
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
            holder.label.setText(getGroup(groupPosition).name);
            return convertView;
        }

        @Override public ForObjects getGroup(int groupPosition) {
            return (new ArrayList<ForObjects>(historyProfileMap.keySet())).get(groupPosition);
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

}
