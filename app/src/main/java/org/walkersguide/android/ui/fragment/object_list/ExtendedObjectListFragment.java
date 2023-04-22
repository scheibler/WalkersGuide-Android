package org.walkersguide.android.ui.fragment.object_list;

import org.walkersguide.android.ui.dialog.select.SelectProfileDialog;
import org.walkersguide.android.data.profile.Profile;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import org.walkersguide.android.data.profile.ProfileGroup;
import org.walkersguide.android.ui.UiHelper;
import org.walkersguide.android.ui.fragment.ObjectListFragment;
import org.walkersguide.android.data.ObjectWithId;

import java.util.ArrayList;

import android.os.Bundle;

import android.view.View;

import androidx.core.view.ViewCompat;

import org.walkersguide.android.R;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.Entrance;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.POI;
import org.walkersguide.android.data.object_with_id.point.PedestrianCrossing;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.Point;
import android.widget.Button;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.view.KeyEvent;
import org.walkersguide.android.ui.TextChangedListener;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import org.walkersguide.android.util.SettingsManager;
import android.widget.LinearLayout;
import org.walkersguide.android.ui.dialog.select.SelectPoiCategoriesDialog;
import timber.log.Timber;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;


public abstract class ExtendedObjectListFragment extends ObjectListFragment {

    public static class BundleBuilder extends ObjectListFragment.BundleBuilder {
        public BundleBuilder() {
            super();
            setProfileGroup(null);
        }
        public BundleBuilder setProfileGroup(ProfileGroup newProfileGroup) {
            bundle.putSerializable(KEY_GROUP, newProfileGroup);
            return this;
        }
    }


    // dialog
    private static final String KEY_GROUP = "group";

    public abstract Profile  getProfile();

    public ProfileGroup getProfileGroup() {
        return (ProfileGroup) getArguments().getSerializable(KEY_GROUP);
    }


    /**
     * create view
     */

    private Button buttonSelectProfile;
    private AutoCompleteTextView editSearch;
    private ImageButton buttonClearSearch;

	@Override public View configureView(View view, Bundle savedInstanceState) {
        view = super.configureView(view, savedInstanceState);

        buttonSelectProfile = (Button) view.findViewById(R.id.buttonSelectProfile);
        buttonSelectProfile.setVisibility(
                getProfileGroup() != null ? View.VISIBLE : View.GONE);
        buttonSelectProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (getProfileGroup() != null) {
                    SelectProfileDialog.newInstance(
                            getProfileGroup(), getProfile(), getProfileGroup().getCanCreateNewProfile())
                        .show(getChildFragmentManager(), "SelectProfileDialog");
                }
            }
        });
        buttonSelectProfile.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                if (getProfile() instanceof PoiProfile) {
                    SelectPoiCategoriesDialog.newInstance(
                            ((PoiProfile) getProfile()).getPoiCategoryList())
                        .show(getChildFragmentManager(), "SelectPoiCategoriesDialog");
                    return true;
                }
                return false;
            }
        });

        ((LinearLayout) view.findViewById(R.id.layoutSearch))
            .setVisibility(View.VISIBLE);

        editSearch = (AutoCompleteTextView) view.findViewById(R.id.editSearch);
        editSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (UiHelper.isDoSomeThingEditorAction(actionId, EditorInfo.IME_ACTION_SEARCH, event)) {
                    UiHelper.hideKeyboard(ExtendedObjectListFragment.this);
                    editSearch.dismissDropDown();
                    resetListPosition();
                    requestUiUpdate();
                    return true;
                }
                return false;
            }
        });
        editSearch.addTextChangedListener(new TextChangedListener<AutoCompleteTextView>(editSearch) {
            @Override public void onTextChanged(AutoCompleteTextView view, Editable s) {
                String newSearchTerm = view.getText().toString().trim();
                if (! TextUtils.isEmpty(newSearchTerm)) {
                    onSearchTermChanged(newSearchTerm);
                } else {
                    onSearchTermChanged(null);
                }
                showOrHideSearchFieldControls();
            }
        });
        // add auto complete suggestions
        editSearch.setAdapter(
                new ArrayAdapter<String>(
                    getActivity(),
                    android.R.layout.simple_dropdown_item_1line,
                    SettingsManager.getInstance().getSearchTermHistory()));

        buttonClearSearch = (ImageButton) view.findViewById(R.id.buttonClearSearch);
        buttonClearSearch.setContentDescription(GlobalInstance.getStringResource(R.string.buttonClearSearch));
        buttonClearSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // clear edit text
                updateSearchTerm(null);
                resetListPosition();
                requestUiUpdate();
            }
        });

        // don't show keyboard automatically
        getActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        return view;
    }

    private void showOrHideSearchFieldControls() {
        if (! TextUtils.isEmpty(editSearch.getText()) && buttonClearSearch.getVisibility() == View.GONE) {
            buttonClearSearch.setVisibility(View.VISIBLE);
        } else if (TextUtils.isEmpty(editSearch.getText()) && buttonClearSearch.getVisibility() == View.VISIBLE) {
            buttonClearSearch.setVisibility(View.GONE);
        }
    }


    /**
     * menu
     */

    @Override public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuItemJumpToTop = menu.findItem(R.id.menuItemJumpToTop);
        menuItemJumpToTop.setVisible(true);
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        UiHelper.hideKeyboard(this);
    }

    @Override public void onResume() {
        super.onResume();
    }

    @Override public void prepareRequest() {
        super.prepareRequest();

        // profile button
        buttonSelectProfile.setText(
                GlobalInstance.getStringResource(R.string.buttonSelectProfileNone));
        buttonSelectProfile.setContentDescription(null);
        if (getProfile() != null) {
            buttonSelectProfile.setText(
                    String.format(
                        GlobalInstance.getStringResource(R.string.buttonSelectProfile),
                        getProfile().getName())
                    );
            if (getProfile() instanceof PoiProfile) {
                buttonSelectProfile.setContentDescription(
                        String.format(
                            GlobalInstance.getStringResource(R.string.buttonSelectProfileWithPoiCategories),
                            getProfile().getName(),
                            TextUtils.join(
                                ", ", ((PoiProfile) getProfile()).getPoiCategoryList()))
                        );
            }
        }

        // search field
        showOrHideSearchFieldControls();
        SettingsManager.getInstance().addToSearchTermHistory(
                editSearch.getText().toString().trim());
    }

    public void updateSearchTerm(String searchTerm) {
        if (! TextUtils.isEmpty(searchTerm)) {
            editSearch.setText(searchTerm);
        } else {
            editSearch.setText("");
        }
    }

    public abstract void onSearchTermChanged(String newSearchTerm);

}
