package org.walkersguide.android.ui.fragment;


import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import org.walkersguide.android.data.ObjectWithId;
    import org.walkersguide.android.ui.view.TextViewAndActionButton;
import android.view.MenuItem;
import timber.log.Timber;

import android.os.Bundle;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ListView;
import android.widget.TextView;

import android.widget.ImageButton;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;
import android.app.Dialog;
import android.content.DialogInterface;
import android.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import android.os.Vibrator;
import java.util.ArrayList;
import android.content.BroadcastReceiver;
import android.content.Context;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import androidx.core.view.ViewCompat;
import android.widget.AbsListView;
import android.content.Intent;
import android.widget.BaseAdapter;
import java.util.Comparator;
import java.util.Collections;
import android.view.WindowManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.sensor.DeviceSensorManager;


public abstract class ObjectListFragment extends DialogFragment {
    public static final String REQUEST_SELECT_OBJECT = "selectObject";
    public static final String EXTRA_OBJECT_WITH_ID = "objectWithId";

    public abstract String getDialogTitle();
    public abstract int getPluralResourceId();

    public abstract void requestUiUpdate();
    public abstract void requestMoreResults();


    public enum DialogMode {
        DEFAULT, SELECT
    }

    public static Bundle createArgsBundle(DialogMode dialogMode) {
        Bundle args = new Bundle();
        args.putSerializable(KEY_DIALOG_MODE, dialogMode);
        return args;
    }


    // dialog
    private static final String KEY_DIALOG_MODE = "dialogMode";
    private static final String KEY_LIST_POSITION = "listPosition";

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setShowsDialog(getDialogMode() != null);
    }

    private DialogMode getDialogMode() {
        return (DialogMode) getArguments().getSerializable(KEY_DIALOG_MODE);
    }


    /**
     * create view
     */

    private int listPosition;

    private TextView labelHeading, labelEmptyListView, labelMoreResultsFooter;
    private ImageButton buttonRefresh;
	private ListView listViewObject;

    @Override public int getTheme() {
        return R.style.FullScreenDialog;
    }

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        if (getDialog() != null) {
            // fragment is a dialog
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            // fragment is embetted
		    return configureView(
                   inflater.inflate(R.layout.fragment_object_list, container, false),
                   savedInstanceState);
        }
	}

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = configureView(
                inflater.inflate(R.layout.fragment_object_list, nullParent),
                savedInstanceState);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getDialogTitle())
            .setView(view)
            .setNegativeButton(
                    getResources().getString(
                        getDialogMode() == DialogMode.SELECT
                        ? R.string.dialogCancel
                        : R.string.dialogClose),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    })
            .create();
    }

	public View configureView(View view, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
        } else {
            listPosition = 0;
        }

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                refreshButtonClicked();
            }
        });

        listViewObject = (ListView) view.findViewById(R.id.listView);
        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        labelEmptyListView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                requestMoreResults();
            }
        });
        listViewObject.setEmptyView(labelEmptyListView);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View footerView = inflater.inflate(R.layout.layout_single_text_view, listViewObject, false);
        labelMoreResultsFooter = (TextView) footerView.findViewById(R.id.label);
        labelMoreResultsFooter.setText(getResources().getString(R.string.labelMoreResults));
        labelMoreResultsFooter.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                requestMoreResults();
            }
        });

        // don't show keyboard automatically
        getActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        return view;
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(KEY_LIST_POSITION,  listPosition);
    }

    public void refreshButtonClicked() {
        Timber.d("refreshButtonClicked");
        requestUiUpdate();
    }

    public ObjectWithIdAdapter getListAdapter() {
        return (ObjectWithIdAdapter) listViewObject.getAdapter();
    }

    public String getEmptyObjectListMessage() {
        return "";
    }

    public void resetListPosition() {
        listPosition = 0;
    }


    /**
     * menu
     */

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItemJumpToTop) {
            listViewObject.setSelection(0);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        Timber.d("onPause");
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override public void onResume() {
        super.onResume();
        Timber.d("onResume");
        // broadcast filter
        LocalBroadcastManager
            .getInstance(getActivity())
            .registerReceiver(mMessageReceiver, getIntentFilter(true));
        // request ui update
        requestUiUpdate();
    }

    private IntentFilter getIntentFilter(boolean includeNewLocationAction) {
        IntentFilter filter = new IntentFilter();
        if (includeNewLocationAction) {
            filter.addAction(PositionManager.ACTION_NEW_LOCATION);
        }
        filter.addAction(PositionManager.ACTION_NEW_SIMULATED_LOCATION);
        filter.addAction(PositionManager.ACTION_LOCATION_SIMULATION_STATE_CHANGED);
        filter.addAction(DeviceSensorManager.ACTION_SHAKE_DETECTED);
        return filter;
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)
                    || intent.getAction().equals(PositionManager.ACTION_NEW_SIMULATED_LOCATION)
                    || intent.getAction().equals(PositionManager.ACTION_LOCATION_SIMULATION_STATE_CHANGED)
                    || intent.getAction().equals(DeviceSensorManager.ACTION_SHAKE_DETECTED)) {
                if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)) {
                    LocalBroadcastManager.getInstance(context).unregisterReceiver(mMessageReceiver);
                    LocalBroadcastManager
                        .getInstance(context)
                        .registerReceiver(mMessageReceiver, getIntentFilter(false));
                } else if (intent.getAction().equals(DeviceSensorManager.ACTION_SHAKE_DETECTED)) {
                    ((Vibrator) GlobalInstance.getContext().getSystemService(Context.VIBRATOR_SERVICE))
                        .vibrate(250);
                }
                requestUiUpdate();
            }
        }
    };


    /**
     * responses
     */

    public void prepareRequest() {
        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelHeading.setText(
                GlobalInstance.getPluralResource(getPluralResourceId(), 0));

        // list view
        listViewObject.setAdapter(null);
        listViewObject.setOnScrollListener(null);
        if (listViewObject.getFooterViewsCount() > 0) {
            listViewObject.removeFooterView(labelMoreResultsFooter);
        }

        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelEmptyListView.setClickable(false);
        labelEmptyListView.setText(
                GlobalInstance.getStringResource(R.string.messagePleaseWait));
    }

    public void populateUiAfterRequestWasSuccessful(String heading,
            ArrayList<? extends ObjectWithId> objectList, boolean showIsFavoriteIndicator) {
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
        updateHeadingText(heading);
        updateRefreshButton(false);

        listViewObject.setAdapter(
                new ObjectWithIdAdapter(
                    ObjectListFragment.this.getContext(), objectList, showIsFavoriteIndicator));
        labelEmptyListView.setText(getEmptyObjectListMessage());

        // list position
        listViewObject.setSelection(listPosition);
        listViewObject.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (listPosition != firstVisibleItem) {
                    listPosition = firstVisibleItem;
                }
            }
        });
    }

    public void populateUiAndShowMoreResultsFooterAfterRequestWasSuccessful(String heading,
            ArrayList<? extends ObjectWithId> objectList) {
        populateUiAfterRequestWasSuccessful(heading, objectList, true);

        // more results
        if (objectList.isEmpty()) {
            labelEmptyListView.setClickable(true);
            labelEmptyListView.setText(
                    GlobalInstance.getStringResource(R.string.labelMoreResults));
        } else {
            listViewObject.addFooterView(labelMoreResultsFooter, null, true);
        }
    }

    public void populateUiAfterRequestFailed(String message) {
        updateRefreshButton(false);

        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
        labelEmptyListView.setText(message);
    }

    public void updateHeadingText(String heading) {
        labelHeading.setText(heading);
    }

    public void updateRefreshButton(boolean requestInProgress) {
        if (requestInProgress) {
            buttonRefresh.setContentDescription(
                    GlobalInstance.getStringResource(R.string.buttonCancel));
            buttonRefresh.setImageResource(R.drawable.cancel);
        } else {
            buttonRefresh.setContentDescription(
                    GlobalInstance.getStringResource(R.string.buttonRefresh));
            buttonRefresh.setImageResource(R.drawable.refresh);
        }
    }


    /**
     * ObjectWithId adapter
     */

    public class ObjectWithIdAdapter extends BaseAdapter {

        private Context context;
        private ArrayList<? extends ObjectWithId> objectList;
        private boolean showIsFavoriteIndicator;

        public ObjectWithIdAdapter(Context context,
                ArrayList<? extends ObjectWithId> objectList, boolean showIsFavoriteIndicator) {
            this.context = context;
            this.objectList = objectList;
            this.showIsFavoriteIndicator = showIsFavoriteIndicator;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            TextViewAndActionButton layoutTextViewAndActionButton = null;
            if (convertView == null) {
                layoutTextViewAndActionButton = new TextViewAndActionButton(this.context);
                layoutTextViewAndActionButton.setLayoutParams(
                        new LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            } else {
                layoutTextViewAndActionButton = (TextViewAndActionButton) convertView;
            }

            if (getDialogMode() == DialogMode.SELECT) {
                layoutTextViewAndActionButton.setOnObjectDefaultActionListener(new TextViewAndActionButton.OnObjectDefaultActionListener() {
                    @Override public void onObjectDefaultAction(TextViewAndActionButton view) {
                        Bundle result = new Bundle();
                        result.putSerializable(EXTRA_OBJECT_WITH_ID, view.getObject());
                        getParentFragmentManager().setFragmentResult(REQUEST_SELECT_OBJECT, result);
                        dismiss();
                    }
                });
            }

            layoutTextViewAndActionButton.configureAsListItem(
                    getItem(position),
                    this.showIsFavoriteIndicator,
                    new TextViewAndActionButton.OnUpdateListRequestListener() {
                        @Override public void onUpdateListRequested(TextViewAndActionButton view) {
                            requestUiUpdate();
                        }
                    });
            return layoutTextViewAndActionButton;
        }

        @Override public int getCount() {
            if (this.objectList != null) {
                return this.objectList.size();
            }
            return 0;
        }

        @Override public ObjectWithId getItem(int position) {
            if (this.objectList != null) {
                return this.objectList.get(position);
            }
            return null;
        }

        @Override public long getItemId(int position) {
            return position;
        }

        public boolean sortObjectList(Comparator<ObjectWithId> comparator) {
            ObjectWithId lastListItemOnTop = getItem(0);
            // sort
            Collections.sort(objectList, comparator);
            notifyDataSetChanged();
            // return true, if the list element on top had changed
            return lastListItemOnTop != null && ! lastListItemOnTop.equals(getItem(0));
        }


        private class EntryHolder {
            public TextViewAndActionButton layoutTextViewAndActionButton;
        }
    }

}
