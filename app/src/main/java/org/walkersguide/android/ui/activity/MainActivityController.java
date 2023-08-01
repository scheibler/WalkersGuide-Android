package org.walkersguide.android.ui.activity;

import androidx.fragment.app.DialogFragment;


public interface MainActivityController {
    public void configureToolbarTitle(String title);
    public void displayRemainsActiveSettingChanged(boolean remainsActive);
    public void openPlanRouteDialog();
    public void addFragment(DialogFragment fragment);
}
