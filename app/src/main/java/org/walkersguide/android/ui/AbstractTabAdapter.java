package org.walkersguide.android.ui;

import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.fragment.app.FragmentActivity;
import java.util.ArrayList;
import timber.log.Timber;
import androidx.fragment.app.Fragment;


public abstract class AbstractTabAdapter extends FragmentStateAdapter {
    private ArrayList<? extends Enum> tabList;

    public AbstractTabAdapter(FragmentActivity activity, ArrayList<? extends Enum> tabList) {
        super(activity);
        this.tabList = tabList;
    }

    public AbstractTabAdapter(Fragment fragment, ArrayList<? extends Enum> tabList) {
        super(fragment);
        this.tabList = tabList;
    }

    public abstract String getFragmentName(int position);

    public <T extends Enum> T getTab(int index) {
        if (index >= 0 && index < this.tabList.size()) {
            return (T) this.tabList.get(index);
        }
        return (T) this.tabList.get(0);
    }

    public int getTabIndex(Enum<?> tab) {
        int tabIndex = this.tabList.indexOf(tab);
        Timber.d("getTabIndex: tabIndex=%1$d", tabIndex);
        if (tabIndex >= 0) {
            return tabIndex;
        }
        return 0;
    }

	@Override public int getItemCount() {
		return this.tabList.size();
    }

}
