package org.walkersguide.android.ui.fragment.object_list.simple;

import org.walkersguide.android.R;
import org.walkersguide.android.ui.fragment.object_list.SimpleObjectListFragment;
import java.util.ArrayList;
import org.walkersguide.android.data.object_with_id.point.PedestrianCrossing;
import org.walkersguide.android.data.ObjectWithId;


public class PedestrianCrossingListFragment extends SimpleObjectListFragment {

	public static PedestrianCrossingListFragment embedded(ArrayList<PedestrianCrossing> pedestrianCrossingList) {
		PedestrianCrossingListFragment fragment = new PedestrianCrossingListFragment();
        fragment.setArguments(
                new SimpleObjectListFragment.BundleBuilder(pedestrianCrossingList)
                    .setIsEmbedded(true)
                    .build());
		return fragment;
	}


    @Override public String getTitle() {
        return getSelectObjectWithId()
            ? getResources().getString(R.string.labelPleaseSelect)
    	    : getResources().getString(R.string.fragmentPedestrianCrossingsName);
    }

    @Override public int getPluralResourceId() {
        return R.plurals.crossing;
    }

    @Override public boolean  showObjectList() {
        return false;
    }

}
