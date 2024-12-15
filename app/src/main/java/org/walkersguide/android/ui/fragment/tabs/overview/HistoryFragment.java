package org.walkersguide.android.ui.fragment.tabs.overview;

import org.walkersguide.android.ui.adapter.HistoryProfileAdapter;
import org.walkersguide.android.R;


import android.os.Bundle;



import android.view.View;






import android.widget.ExpandableListView;
import org.walkersguide.android.tts.TTSWrapper;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.ui.fragment.RootFragment;


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
        final HistoryProfileAdapter adapter = new HistoryProfileAdapter(getActivity());

        listViewHistoryProfile = (ExpandableListView) view.findViewById(R.id.expandableListView);
        listViewHistoryProfile .setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override public boolean onGroupClick(ExpandableListView expandableListView, View view, int groupPosition, long l) {
                // negate that boolean, cause the isGroupExpanded function returns the state before click / toggle
                boolean expanded = ! expandableListView.isGroupExpanded(groupPosition);
                HistoryProfileAdapter.Group group = adapter.getGroup(groupPosition);
                if (group != null) {
                    TTSWrapper.getInstance().screenReader(
                            String.format(
                                "%1$s %2$s",
                                group.toString(),
                                expanded
                                ? getResources().getString(R.string.stateExpanded)
                                : getResources().getString(R.string.stateCollapsed))
                            );
                    Helper.vibrateOnce(
                            Helper.VIBRATION_DURATION_SHORT, Helper.VIBRATION_INTENSITY_WEAK);
                }
                return false;
            }
        });

        listViewHistoryProfile.setAdapter(adapter);
        for (int i=0; i<adapter.getGroupCount(); i++) {
            listViewHistoryProfile.expandGroup(i);
        }

        return view;
    }

}
