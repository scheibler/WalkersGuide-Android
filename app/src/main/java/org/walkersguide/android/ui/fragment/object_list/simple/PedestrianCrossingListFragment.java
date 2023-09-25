package org.walkersguide.android.ui.fragment.object_list.simple;

import org.walkersguide.android.R;
import org.walkersguide.android.ui.fragment.object_list.SimpleObjectListFragment;
import java.util.ArrayList;
import org.walkersguide.android.data.object_with_id.point.PedestrianCrossing;


public class PedestrianCrossingListFragment extends SimpleObjectListFragment {

	public static PedestrianCrossingListFragment newInstance(ArrayList<PedestrianCrossing> pedestrianCrossingList) {
		PedestrianCrossingListFragment fragment = new PedestrianCrossingListFragment();
        fragment.setArguments(
                new SimpleObjectListFragment.BundleBuilder(pedestrianCrossingList).build());
		return fragment;
	}


    @Override public String getTitle() {
    	return getResources().getString(R.string.fragmentPedestrianCrossingsName);
    }

    @Override public int getPluralResourceId() {
        return R.plurals.crossing;
    }

    @Override public void  successfulViewPopulationFinished() {
    }

}
