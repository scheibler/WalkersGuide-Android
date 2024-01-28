package org.walkersguide.android.ui.fragment.object_list;

import org.walkersguide.android.ui.interfaces.ViewChangedListener;
import org.walkersguide.android.ui.UiHelper;
import org.walkersguide.android.ui.fragment.ObjectListFragment;


import android.os.Bundle;

import android.view.View;


import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.view.KeyEvent;
import org.walkersguide.android.ui.interfaces.TextChangedListener;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import org.walkersguide.android.util.SettingsManager;
import android.widget.LinearLayout;
import android.view.WindowManager;
import android.view.Menu;
import androidx.annotation.NonNull;
import android.view.MenuItem;
import android.content.BroadcastReceiver;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.Context;


public abstract class ExtendedObjectListFragment extends ObjectListFragment implements ViewChangedListener {


    /**
     * menu
     */

    @Override public void onPrepareMenu(@NonNull Menu menu) {
        super.onPrepareMenu(menu);
        MenuItem menuItemJumpToTop = menu.findItem(R.id.menuItemJumpToTop);
        menuItemJumpToTop.setVisible(getListPosition() > 0);
    }


    /**
     * create view
     */
    private AutoCompleteTextView editSearch;
    private ImageButton buttonClearSearch;

    @Override public View configureView(View view, Bundle savedInstanceState) {
        view = super.configureView(view, savedInstanceState);

        // show search field
        ((LinearLayout) view.findViewById(R.id.layoutExtendedObjectListFragment))
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
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        unregisterViewChangedBroadcastReceiver(viewChangedBroadcastReceiver);
        UiHelper.hideKeyboard(this);
    }

    @Override public void onResume() {
        super.onResume();
        registerViewChangedBroadcastReceiver(viewChangedBroadcastReceiver);
    }

    private BroadcastReceiver viewChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ViewChangedListener.ACTION_OBJECT_WITH_ID_LIST_CHANGED)) {
                requestUiUpdate();
            }
        }
    };


    @Override public void prepareRequest() {
        super.prepareRequest();

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
