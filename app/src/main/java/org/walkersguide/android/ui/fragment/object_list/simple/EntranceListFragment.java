package org.walkersguide.android.ui.fragment.object_list.simple;

import org.walkersguide.android.R;
import org.walkersguide.android.ui.fragment.object_list.SimpleObjectListFragment;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.Entrance;
import java.util.ArrayList;
import org.walkersguide.android.data.ObjectWithId;


public class EntranceListFragment extends SimpleObjectListFragment {


    public static EntranceListFragment embedded(ArrayList<Entrance> entranceList) {
        EntranceListFragment fragment = new EntranceListFragment();
        fragment.setArguments(
                new SimpleObjectListFragment.BundleBuilder(entranceList)
                    .setIsEmbedded(true)
                    .build());
        return fragment;
    }


    @Override public String getTitle() {
        return getSelectObjectWithId()
            ? getResources().getString(R.string.labelPleaseSelect)
            : getResources().getString(R.string.fragmentEntrancesName);
    }

    @Override public int getPluralResourceId() {
        return R.plurals.entrance;
    }

    @Override public boolean  showObjectList() {
        return false;
    }

}
