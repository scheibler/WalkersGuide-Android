package org.walkersguide.android.ui.activity;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;


public interface MainActivityController {
    public void recreateActivity();

    public void configureToolbarTitle(String title);
    public void displayRemainsActiveSettingChanged(boolean remainsActive);
    public void openPlanRouteDialog();

    public FragmentManager getFragmentManagerInstance();
    public void addFragment(DialogFragment fragment);
}
