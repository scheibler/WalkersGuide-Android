package org.walkersguide.android.ui.fragment;

import org.walkersguide.android.ui.adapter.HistoryProfileAdapter;
import org.walkersguide.android.R;


import android.os.Bundle;



import android.view.View;






import android.widget.ExpandableListView;


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

}
