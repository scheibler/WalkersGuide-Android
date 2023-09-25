package org.walkersguide.android.ui.fragment.object_list.simple;

import org.walkersguide.android.R;
import org.walkersguide.android.ui.fragment.object_list.SimpleObjectListFragment;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.Entrance;
import java.util.ArrayList;


public class EntranceListFragment extends SimpleObjectListFragment {


	public static EntranceListFragment newInstance(ArrayList<Entrance> entranceList) {
		EntranceListFragment fragment = new EntranceListFragment();
        fragment.setArguments(
                new SimpleObjectListFragment.BundleBuilder(entranceList).build());
		return fragment;
	}


    @Override public String getTitle() {
    	return getResources().getString(R.string.fragmentEntrancesName);
    }

    @Override public int getPluralResourceId() {
        return R.plurals.entrance;
    }

    @Override public void  successfulViewPopulationFinished() {
    }

}
