package org.walkersguide.android.ui.activity;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;


public interface MainActivityController {
    public void configureToolbarTitle(String title);
    public void openMainMenu();
    public void closeMainMenu();
    public void openPlanRouteDialog(boolean startRouteCalculationImmediately);

    public void recreateActivity();
    public void displayRemainsActiveSettingChanged(boolean remainsActive);

    public void embeddFragmentIfPossibleElseOpenAsDialog(DialogFragment fragment);
    public void openDialog(DialogFragment dialog);
    public void closeAllOpenDialogs();
}
