package org.walkersguide.android.ui.adapter;

import org.walkersguide.android.R;
import org.walkersguide.android.ui.interfaces.ViewChangedListener;

import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import org.walkersguide.android.data.ObjectWithId;
    import org.walkersguide.android.ui.view.ObjectWithIdView;
    import org.walkersguide.android.ui.view.ObjectWithIdView.OnDefaultObjectActionListener;



import android.view.View;
import android.view.ViewGroup;


import java.util.ArrayList;
import android.content.Context;
import android.widget.BaseAdapter;
import org.walkersguide.android.data.Profile;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.database.profile.StaticProfile;
import org.walkersguide.android.database.profile.Collection;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import org.walkersguide.android.util.Helper;


public class ObjectWithIdAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<? extends ObjectWithId> objectList, filteredObjectList;
    private OnDefaultObjectActionListener onDefaultObjectActionListener;
    private Profile profile;
    private boolean autoUpdate, viewingDirectionFilter, alwaysShowIcon;

    public ObjectWithIdAdapter(Context context, ArrayList<? extends ObjectWithId> objectList,
            OnDefaultObjectActionListener listener, Profile profile,
            boolean autoUpdate, boolean viewingDirectionFilter) {

        this.context = context;
        this.objectList = objectList;
        this.onDefaultObjectActionListener = listener;
        this.profile = profile;
        this.autoUpdate = autoUpdate;
        this.viewingDirectionFilter = viewingDirectionFilter;
        this.alwaysShowIcon = false;

        // must come after setting viewingDirectionFilter
        this.filteredObjectList = populateFilteredObjectList();
    }

    public void setAlwaysShowIcon() {
        this.alwaysShowIcon = true;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        ObjectWithIdView layoutObject = null;
        if (convertView == null) {
            layoutObject = new ObjectWithIdView(this.context);
            layoutObject.setLayoutParams(
                    new LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        } else {
            layoutObject = (ObjectWithIdView) convertView;
        }

        layoutObject.setAutoUpdate(this.autoUpdate);
        layoutObject.setOnDefaultObjectActionListener(this.onDefaultObjectActionListener);

        if (this.profile != null && this.profile instanceof DatabaseProfile) {
            final DatabaseProfile profileToBeRemovedFrom = (DatabaseProfile) this.profile;
            if (       profileToBeRemovedFrom.equals(StaticProfile.excludedRoutingSegments())
                    || profileToBeRemovedFrom.equals(StaticProfile.recordedRoutes())
                    || profileToBeRemovedFrom instanceof Collection) {
                layoutObject.setOnRemoveObjectActionListener(new ObjectWithIdView.OnRemoveObjectActionListener() {
                    @Override public void onRemoveObjectActionClicked(ObjectWithId objectWithId) {
                        profileToBeRemovedFrom.removeObject(objectWithId);
                        // update parent view
                        ViewChangedListener.sendObjectWithIdListChangedBroadcast();
                    }
                });
            }
        }

        ObjectWithId objectWithId = getItem(position);
        boolean showIcon = false;
        if (this.alwaysShowIcon) {
            showIcon = true;
        } else if (this.profile != null && this.profile instanceof DatabaseProfile) {
            if (((DatabaseProfile) this.profile).getPluralResId() == R.plurals.pointAndRoute) {
                showIcon = true;
            }
        } else if (this.profile != null && this.profile instanceof PoiProfile) {
            for (Collection collection : ((PoiProfile) this.profile).getCollectionList()) {
                if (collection.containsObject(objectWithId)) {
                    showIcon = true;
                    break;
                }
            }
        }


        layoutObject.configureAsListItem(objectWithId, showIcon);
        return layoutObject;
    }

    @Override public int getCount() {
        return this.filteredObjectList.size();
    }

    @Override public ObjectWithId getItem(int position) {
        return this.filteredObjectList.get(position);
    }

    @Override public long getItemId(int position) {
        return position;
    }

    @Override public void notifyDataSetChanged() {
        this.filteredObjectList = populateFilteredObjectList();
        // the following must be put after the object list was sorted and updated
        super.notifyDataSetChanged();
    }

    public Context getContext() {
        return this.context;
    }

    public boolean isEmpty() {
        return this.filteredObjectList.isEmpty();
    }

    public ArrayList<? extends ObjectWithId> getObjectList() {
        return this.objectList;
    }

    protected void updateObjectList(ArrayList<? extends ObjectWithId> updatedObjectList) {
        this.objectList = updatedObjectList;
    }

    private ArrayList<? extends ObjectWithId> populateFilteredObjectList() {
        if (this.objectList == null) {
            return new ArrayList<ObjectWithId>();
        } else if (viewingDirectionFilter) {
            return Helper.filterObjectWithIdListByViewingDirection(this.objectList, 315, 45);
        } else {
            return this.objectList;
        }
    }


    private class EntryHolder {
        public ObjectWithIdView layoutObject;
    }

}
