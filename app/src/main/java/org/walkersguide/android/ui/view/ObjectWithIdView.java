package org.walkersguide.android.ui.view;

import org.walkersguide.android.ui.dialog.create.gpx.export.ExportRouteToGpxFileDialog;
import org.walkersguide.android.ui.dialog.edit.UserAnnotationForObjectWithIdDialog;
import org.walkersguide.android.ui.interfaces.ViewChangedListener;
import org.walkersguide.android.ui.fragment.tabs.ObjectDetailsTabLayoutFragment;
import org.walkersguide.android.ui.UiHelper;
import androidx.core.view.ViewCompat;

import org.walkersguide.android.sensor.bearing.AcceptNewBearing;
import org.walkersguide.android.sensor.position.AcceptNewPosition;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.MainActivityController;
import org.walkersguide.android.server.wg.p2p.P2pRouteRequest;
import androidx.appcompat.app.AppCompatActivity;
import org.walkersguide.android.data.ObjectWithId;
import android.view.MenuItem;
import timber.log.Timber;



import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import android.widget.ImageButton;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;
import android.text.TextUtils;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.data.object_with_id.Point;
import android.content.Context;
import android.widget.ImageView;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener;
import android.view.Menu;
import android.view.SubMenu;
import android.content.Intent;
import android.widget.LinearLayout;
import android.util.AttributeSet;
import android.view.Gravity;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.data.object_with_id.Segment;
import androidx.core.view.MenuCompat;
import android.content.BroadcastReceiver;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import org.walkersguide.android.data.object_with_id.Route;
import android.widget.Toast;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import android.content.res.TypedArray;
import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.poi.Station;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.POI;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.database.profile.StaticProfile;
import org.walkersguide.android.ui.dialog.template.EnterStringDialog;
import android.os.Bundle;
import android.app.Dialog;
import org.walkersguide.android.tts.TTSWrapper;
import android.graphics.Rect;
import android.view.TouchDelegate;
import java.lang.Runnable;
import org.json.JSONException;
import org.walkersguide.android.ui.dialog.select.SelectCollectionsDialog;
import java.util.ArrayList;
import org.walkersguide.android.database.profile.Collection;
import org.walkersguide.android.database.util.AccessDatabase;
import java.util.LinkedHashMap;
import java.util.Map;


public class ObjectWithIdView extends LinearLayout {

    public enum ShowIcon {
        NO, IN_DATABASE, ALWAYS
    }

    private MainActivityController mainActivityController;
    private DeviceSensorManager deviceSensorManagerInstance;
    private PositionManager positionManagerInstance;
    private SettingsManager settingsManagerInstance;

    private String prefix = null;
    private boolean compact = false;
    private boolean includeDistanceOrBearingInformation = true;

    private ObjectWithId objectWithId;
    private boolean autoUpdate;
    private ShowIcon showObjectIcon;
    private String emptyLabelText, staticLabelText;

    private ImageView imageViewObjectIcon;
    private TextView label;
    private ImageButton buttonActionFor;

    public ObjectWithIdView(Context context) {
        super(context);
        this.initUi(context);
    }

    public ObjectWithIdView(Context context, String prefix, boolean compact, boolean includeDistanceOrBearingInformation) {
        super(context);
        this.prefix = prefix;
        this.compact = compact;
        this.includeDistanceOrBearingInformation = includeDistanceOrBearingInformation;
        this.initUi(context);
    }

    public ObjectWithIdView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // parse xml layout attributes
        //
        // shared attributes between profile and object view
        TypedArray sharedAttributeArray = context.obtainStyledAttributes(
                attrs, R.styleable.ObjectWithIdAndProfileView);
        if (sharedAttributeArray != null) {
            this.prefix = sharedAttributeArray.getString(
                    R.styleable.ObjectWithIdAndProfileView_prefix);
            this.compact = sharedAttributeArray.getBoolean(
                    R.styleable.ObjectWithIdAndProfileView_compact, false);
            sharedAttributeArray.recycle();
        }

        // attributes for ObjectWithIdView
        TypedArray attributeArray = context.obtainStyledAttributes(
                attrs, R.styleable.ObjectWithIdView);
        if (attributeArray != null) {
            this.includeDistanceOrBearingInformation = attributeArray.getBoolean(
                    R.styleable.ObjectWithIdView_includeDistanceOrBearingInformation, false);
            attributeArray.recycle();
        }

        this.initUi(context);
    }

    private void initUi(Context context) {
        mainActivityController = context instanceof MainActivity ? (MainActivityController) context : null;
        deviceSensorManagerInstance = DeviceSensorManager.getInstance();
        positionManagerInstance = PositionManager.getInstance();
        settingsManagerInstance = SettingsManager.getInstance();

        // configure enclosing linear layout
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        View rootView = inflate(context, R.layout.layout_text_view_and_action_button, this);
        imageViewObjectIcon = (ImageView) rootView.findViewById(R.id.imageViewIcon);
        imageViewObjectIcon.setVisibility(View.GONE);

        label = (TextView) rootView.findViewById(R.id.label);
        if (compact) {
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setSingleLine();
        }
        label.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (onDefaultObjectActionListener != null) {
                    onDefaultObjectActionListener.onDefaultObjectActionClicked(
                            ObjectWithIdView.this, view, objectWithId);
                } else if (objectWithId != null) {
                    // open details
                    executeAccessibilityMenuAction(MENU_ITEM_DETAILS);
                }
            }
        });
        label.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View view) {
                if (objectWithId != null) {
                    showContextMenu(view, objectWithId);
                }
                return true;
            }
        });

        buttonActionFor = (ImageButton) rootView.findViewById(R.id.buttonActionFor);
        buttonActionFor.setContentDescription(null);
        buttonActionFor.setVisibility(View.GONE);
        buttonActionFor.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (objectWithId != null) {
                    showContextMenu(view, objectWithId);
                }
            }
        });

        this.autoUpdate = true;
        this.emptyLabelText = GlobalInstance.getStringResource(R.string.labelNothingSelected);
        this.onDefaultObjectActionListener = null;
        this.objectDetailsActionEnabled = false;
        this.onRemoveObjectActionListener = null;
        this.reset();
    }

    public ObjectWithId getObjectWithId() {
        return this.objectWithId;
    }


    // default action listener
    private OnDefaultObjectActionListener onDefaultObjectActionListener;
    private boolean objectDetailsActionEnabled;

    public interface OnDefaultObjectActionListener {
        public void onDefaultObjectActionClicked(ObjectWithIdView view, View subView, ObjectWithId objectWithId);
    }

    public void setOnDefaultObjectActionListener(OnDefaultObjectActionListener listener) {
        setOnDefaultObjectActionListener(listener, true);
    }

    public void setOnDefaultObjectActionListener(OnDefaultObjectActionListener listener, boolean enabled) {
        this.onDefaultObjectActionListener = listener;
        this.objectDetailsActionEnabled = listener != null ? enabled : false;
    }


    // remove object action listener
    private OnRemoveObjectActionListener onRemoveObjectActionListener;

    public interface OnRemoveObjectActionListener {
        public void onRemoveObjectActionClicked(ObjectWithId objectWithId);
    }

    public void setOnRemoveObjectActionListener(OnRemoveObjectActionListener listener) {
        this.onRemoveObjectActionListener = listener;
    }


    // convigure view

    public void setPrefix(String newPrefix) {
        this.prefix = newPrefix;
    }

    public void setAutoUpdate(boolean newState) {
        this.autoUpdate = newState;
    }

    public void setEmptyLabelText(String newText) {
        this.emptyLabelText = newText;
        updateLabelAndButtonText();
    }

    public void configureAsListItem(ObjectWithId object, ShowIcon showObjectIcon) {
        this.reset();
        if (mainActivityController != null && object != null) {
            this.objectWithId = object;
            this.showObjectIcon = showObjectIcon;
            updateLabelAndButtonText();
        }
    }

    public void configureAsListItem(ObjectWithId object, String staticLabelText) {
        this.reset();
        if (mainActivityController != null && object != null) {
            this.objectWithId = object;
            this.staticLabelText = staticLabelText;
            this.showObjectIcon = ShowIcon.NO;
            updateLabelAndButtonText();
        }
    }

    public void configureAsSingleObject(ObjectWithId object) {
        this.configureAsSingleObject(object, null);
    }

    public void configureAsSingleObject(ObjectWithId object, String staticLabelText) {
        this.reset();
        if (mainActivityController != null && object != null) {
            this.objectWithId = object;
            this.staticLabelText = staticLabelText;
            this.showObjectIcon = ShowIcon.NO;
            ViewCompat.setAccessibilityDelegate(
                    this.label, UiHelper.getAccessibilityDelegateViewClassButton());
            updateLabelAndButtonText();
        }
    }

    public void reset() {
        this.objectWithId = null;
        this.showObjectIcon = ShowIcon.NO;
        this.staticLabelText = null;

        // remove previously added accessibility actions
        for (Integer actionId : registeredAccessibilityActionIdList) {
            ViewCompat.removeAccessibilityAction(this.label, actionId);
        }
        registeredAccessibilityActionIdList.clear();

        updateLabelAndButtonText();
    }

    private void updateLabelAndButtonText() {
        String labelText = null, labelContentDescription = null;

        if (this.objectWithId != null) {
            if (this.staticLabelText != null) {
                labelText = this.staticLabelText;
            } else if (this.compact) {
                labelText = this.objectWithId.formatNameAndSubType();
            } else {
                labelText = this.objectWithId.toString();
            }
        } else {
            labelText = this.emptyLabelText;
        }

        if (this.prefix != null) {
            labelText = String.format(
                    "%1$s: %2$s", this.prefix, labelText);
        }

        if (this.objectWithId != null) {
            labelContentDescription = new String(labelText);

            if (this.includeDistanceOrBearingInformation) {
                String distanceAndBearing = this.objectWithId
                    .formatDistanceAndRelativeBearingFromCurrentLocation(R.plurals.inMeters);
                labelText += String.format("\n%1$s", distanceAndBearing);
                labelContentDescription += String.format(",\n%1$s", distanceAndBearing);
            }

            // object icon and label content description
            boolean iconVisible = false;
            if (this.showObjectIcon == ShowIcon.ALWAYS) {
                iconVisible = true;
            } else if (this.showObjectIcon == ShowIcon.IN_DATABASE
                    && this.objectWithId.isInDatabase()) {
                iconVisible = true;
            }
            if (iconVisible) {
                this.imageViewObjectIcon.setImageResource(this.objectWithId.getIcon().resId);
                this.imageViewObjectIcon.setVisibility(View.VISIBLE);
                labelContentDescription = String.format(
                        "%1$s: %2$s",
                        this.objectWithId.getIcon().name,
                        labelContentDescription);
            } else {
                this.imageViewObjectIcon.setVisibility(View.INVISIBLE);
            }

            // accessibility actions
            for (final Map.Entry<Integer,String> entry : getAccessibilityActionMenuItemMap().entrySet()) {
                int actionId = ViewCompat.addAccessibilityAction(
                        this.label,
                        entry.getValue(),
                        (actionView, arguments) -> {
                            executeAccessibilityMenuAction(entry.getKey());
                            return true;
                        });
                if (actionId != View.NO_ID) {
                    registeredAccessibilityActionIdList.add(actionId);
                }
            }

            // action button
            if (settingsManagerInstance.getShowActionButton()) {
                this.buttonActionFor.setContentDescription(
                        String.format(
                            "%1$s %2$s",
                            getResources().getString(R.string.buttonActionFor),
                            this.objectWithId.getName())
                        );
                this.buttonActionFor.setVisibility(View.VISIBLE);
            }
        }

        this.label.setText(labelText);
        this.label.setContentDescription(labelContentDescription);
    }


    /** accessibility actions
     */
    private ArrayList<Integer> registeredAccessibilityActionIdList = new ArrayList<Integer>();;

    private LinkedHashMap<Integer,String> getAccessibilityActionMenuItemMap() {
        LinkedHashMap<Integer,String> actionMap = new LinkedHashMap<Integer,String>();
        if (objectDetailsActionEnabled) {
            actionMap.put(
                    MENU_ITEM_DETAILS, GlobalInstance.getStringResource(R.string.contextMenuItemDetails));
        }
        if (objectWithId instanceof POI) {
            if (objectWithId instanceof Station) {
                actionMap.put(
                        MENU_ITEM_DEPARTURES, GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdDepartures));
            }
            if (((POI) objectWithId).hasEntrance()) {
                actionMap.put(
                        MENU_ITEM_ENTRANCES, GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdEntrances));
            }
        } else if (objectWithId instanceof Intersection) {
            if (((Intersection) objectWithId).hasPedestrianCrossings()) {
                actionMap.put(
                        MENU_ITEM_PEDESTRIAN_CROSSINGS, GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdPedestrianCrossings));
            }
        } else if (objectWithId instanceof IntersectionSegment) {
            actionMap.put(
                    MENU_ITEM_STREET_COURSE, GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdStreetCourse));
        }
        return actionMap;
    }

    private boolean executeAccessibilityMenuAction(int menuItemId) {
        if (menuItemId == MENU_ITEM_DETAILS) {
            mainActivityController.embeddFragmentIfPossibleElseOpenAsDialog(
                        ObjectDetailsTabLayoutFragment.details(this.objectWithId));
        } else if (menuItemId == MENU_ITEM_DEPARTURES) {
            mainActivityController.embeddFragmentIfPossibleElseOpenAsDialog(
                    ObjectDetailsTabLayoutFragment.departures((Station) this.objectWithId));
        } else if (menuItemId == MENU_ITEM_ENTRANCES) {
            mainActivityController.embeddFragmentIfPossibleElseOpenAsDialog(
                    ObjectDetailsTabLayoutFragment.entrances((POI) this.objectWithId));
        } else if (menuItemId == MENU_ITEM_PEDESTRIAN_CROSSINGS) {
            mainActivityController.embeddFragmentIfPossibleElseOpenAsDialog(
                    ObjectDetailsTabLayoutFragment.pedestrianCrossings((Intersection) this.objectWithId));
        } else if (menuItemId == MENU_ITEM_STREET_COURSE) {
            mainActivityController.embeddFragmentIfPossibleElseOpenAsDialog(
                    ObjectDetailsTabLayoutFragment.streetCourse((IntersectionSegment) this.objectWithId));
        } else {
            return false;
        }
        return true;
    }


    /**
     * broadcasts
     */

    @Override public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).unregisterReceiver(newLocationReceiver);
    }

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        // inner dialog results
        filter.addAction(RenameObjectWithIdDialog.ACTION_RENAME_OBJECT_WITH_ID_WAS_SUCCESSFUL);
        filter.addAction(UpdateObjectWithIdSelectedCollectionsDialog.ACTION_UPDATE_OBJECT_WITH_ID_SELECTED_COLLECTIONS_WAS_SUCCESSFUL);
        // new location and bearing values
        filter.addAction(PositionManager.ACTION_NEW_LOCATION);
        filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).registerReceiver(newLocationReceiver, filter);
    }

    private BroadcastReceiver newLocationReceiver = new BroadcastReceiver() {
        private AcceptNewPosition acceptNewPosition = new AcceptNewPosition(6, 4000l, null);
        private AcceptNewBearing acceptNewBearing = new AcceptNewBearing(30, 2000l);

        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(RenameObjectWithIdDialog.ACTION_RENAME_OBJECT_WITH_ID_WAS_SUCCESSFUL)) {
                updateLabelAndButtonText();

            } else if (intent.getAction().equals(UpdateObjectWithIdSelectedCollectionsDialog.ACTION_UPDATE_OBJECT_WITH_ID_SELECTED_COLLECTIONS_WAS_SUCCESSFUL)) {
                updateLabelAndButtonText();
                // update parent view
                ViewChangedListener.sendObjectWithIdListChangedBroadcast();

            } else if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)) {
                if (acceptNewPosition.updatePoint(
                            (Point) intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION),
                            false,      // se onAttachedToWindow
                            intent.getBooleanExtra(PositionManager.EXTRA_IS_IMPORTANT, false),
                            autoUpdate)) {
                    updateLabelAndButtonText();
                }

            } else if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING)) {
                if (acceptNewBearing.updateBearing(
                            (Bearing) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING),
                            false,      // se onAttachedToWindow
                            intent.getBooleanExtra(DeviceSensorManager.EXTRA_IS_IMPORTANT, false),
                            autoUpdate && includeDistanceOrBearingInformation)) {
                    updateLabelAndButtonText();
                }
            }
        }
    };


    /**
     * context menu
     */

    private static final int MENU_GROUP_1 = 1;
    private static final int MENU_GROUP_2 = 2;
    private static final int MENU_GROUP_3 = 3;
    private static final int MENU_GROUP_4 = 4;

    private static final int MENU_ITEM_DETAILS = 1;
    private static final int MENU_ITEM_DEPARTURES = 2;
    private static final int MENU_ITEM_ENTRANCES = 3;
    private static final int MENU_ITEM_PEDESTRIAN_CROSSINGS = 4;
    private static final int MENU_ITEM_LOAD_ROUTE = 5;
    private static final int MENU_ITEM_STREET_COURSE = 6;
    private static final int MENU_ITEM_OVERVIEW_PIN = 10;
    private static final int MENU_ITEM_OVERVIEW_TRACK = 11;
    private static final int MENU_ITEM_SIMULATE_LOCATION = 12;
    private static final int MENU_ITEM_SIMULATE_BEARING = 13;
    private static final int MENU_ITEM_EXCLUDE_FROM_ROUTING = 14;
    private static final int MENU_ITEM_NAVIGATE_TO_THIS_POINT = 15;
    private static final int MENU_ITEM_COLLECTIONS = 16;
    private static final int MENU_ITEM_REMOVE = 17;
    private static final int MENU_ITEM_EXPORT_TO_GPX_FILE = 18;

    private static final int MENU_ITEM_LOAD_ROUTE_CURRENT_DIRECTION = 50;
    private static final int MENU_ITEM_LOAD_ROUTE_OPPOSITE_DIRECTION = 51;

    private static final int MENU_ITEM_EDIT_USER_ANNOTATION = 70;
    private static final int MENU_ITEM_EDIT_RENAME = 71;

    private static final int MENU_ITEM_ROUTE_PLANNER_USE_AS_START_POINT = 90;
    private static final int MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_1 = 91;
    private static final int MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_2 = 92;
    private static final int MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_3 = 93;
    private static final int MENU_ITEM_ROUTE_PLANNER_USE_AS_DESTINATION_POINT = 94;


    public void showContextMenu(final View view, final ObjectWithId object) {
        PopupMenu contextMenu = new PopupMenu(view.getContext(), view);
        MenuCompat.setGroupDividerEnabled(contextMenu.getMenu(), true);
        int orderId = 0;

        // top items
        for (Map.Entry<Integer,String> entry : getAccessibilityActionMenuItemMap().entrySet()) {
            contextMenu.getMenu().add(
                    MENU_GROUP_1, entry.getKey(), orderId++, entry.getValue());
        }

        // load route
        if (object instanceof Route) {
            if (((Route) object).isReversable()) {
                SubMenu loadRouteSubMenu = contextMenu.getMenu().addSubMenu(
                        MENU_GROUP_1, Menu.NONE, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdLoadRoute));
                MenuItem menuItemLoadRouteCurrentDirection = loadRouteSubMenu.add(
                        Menu.NONE, MENU_ITEM_LOAD_ROUTE_CURRENT_DIRECTION, 0,
                        GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdLoadRouteCurrentDirection));
                MenuItem menuItemLoadRouteOppositeDirection = loadRouteSubMenu.add(
                        Menu.NONE, MENU_ITEM_LOAD_ROUTE_OPPOSITE_DIRECTION, 0,
                        GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdLoadRouteOppositeDirection));
            } else {
                contextMenu.getMenu().add(
                        MENU_GROUP_1, MENU_ITEM_LOAD_ROUTE_CURRENT_DIRECTION, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdLoadRoute));
            }
        }

        // pin
        MenuItem menuItemOverviewPin = contextMenu.getMenu().add(
                MENU_GROUP_2, MENU_ITEM_OVERVIEW_PIN, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemOverviewPin));
        menuItemOverviewPin.setCheckable(true);
        menuItemOverviewPin.setChecked(
                StaticProfile.pinnedObjectsWithId().containsObject(object));

        // track (only for points)
        if (object instanceof Point) {
            MenuItem menuItemOverviewTrack = contextMenu.getMenu().add(
                    MENU_GROUP_2, MENU_ITEM_OVERVIEW_TRACK, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemOverviewTrack));
            menuItemOverviewTrack.setCheckable(true);
            menuItemOverviewTrack.setChecked(
                    StaticProfile.trackedObjectsWithId().containsObject(object));
        }

        // simulation
        if (object instanceof Point) {
            MenuItem menuItemSimulateLocation = contextMenu.getMenu().add(
                    MENU_GROUP_2, MENU_ITEM_SIMULATE_LOCATION, orderId++,
                    GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdSimulateLocation));
            menuItemSimulateLocation.setCheckable(true);
            menuItemSimulateLocation.setChecked(
                       positionManagerInstance.getSimulationEnabled()
                    && ((Point) object).equals(positionManagerInstance.getSimulatedLocation()));
        } else if (object instanceof Segment) {
            MenuItem menuItemSimulateBearing = contextMenu.getMenu().add(
                    MENU_GROUP_2, MENU_ITEM_SIMULATE_BEARING, orderId++,
                    GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdSimulateBearing));
            menuItemSimulateBearing.setCheckable(true);
            menuItemSimulateBearing.setChecked(
                       deviceSensorManagerInstance.getSimulationEnabled()
                    && ((Segment) object).getBearing().equals(deviceSensorManagerInstance.getSimulatedBearing()));
        }

        // exclude from routing
        if (object.hasOsmId() && object instanceof Segment) {
            MenuItem menuItemExcludeFromRouting = contextMenu.getMenu().add(
                    MENU_GROUP_2, MENU_ITEM_EXCLUDE_FROM_ROUTING, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdExcludeFromRouting));
            menuItemExcludeFromRouting.setCheckable(true);
            menuItemExcludeFromRouting.setChecked(
                    StaticProfile.excludedRoutingSegments().containsObject(object));
        }

        // navigate to this point
        if (object instanceof Point) {
            contextMenu.getMenu().add(
                    MENU_GROUP_3, MENU_ITEM_NAVIGATE_TO_THIS_POINT, orderId++,
                    GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdNavigateToThisPoint));
        }

        // add to collections
        contextMenu.getMenu().add(
                MENU_GROUP_3, MENU_ITEM_COLLECTIONS, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdCollections));

        // edit subcommand: user annotation and rename
        SubMenu editSubMenu = contextMenu.getMenu().addSubMenu(
                MENU_GROUP_3, Menu.NONE, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemEdit));
        int editSubMenuOrder = 0;
        // user annotation
        editSubMenu.add(
                Menu.NONE, MENU_ITEM_EDIT_USER_ANNOTATION, editSubMenuOrder++,
                object.hasUserAnnotation()
                ? GlobalInstance.getStringResource(R.string.contextMenuItemUserAnnotationEdit)
                : GlobalInstance.getStringResource(R.string.contextMenuItemUserAnnotationAdd));
        editSubMenu.add(
                Menu.NONE, MENU_ITEM_EDIT_RENAME, editSubMenuOrder++,
                GlobalInstance.getStringResource(R.string.contextMenuItemRename));

        // remove
        if (onRemoveObjectActionListener != null) {
            contextMenu.getMenu().add(
                    MENU_GROUP_3, MENU_ITEM_REMOVE, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemRemove));
        }

        // route planner
        if (object instanceof Point) {
            P2pRouteRequest p2pRouteRequest = settingsManagerInstance.getP2pRouteRequest();
            SubMenu routePlannerSubMenu = contextMenu.getMenu().addSubMenu(
                    MENU_GROUP_4, Menu.NONE, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdRoutePlanner));
            int planRouteSubMenuOrder = 0;
            // start
            routePlannerSubMenu.add(
                    Menu.NONE, MENU_ITEM_ROUTE_PLANNER_USE_AS_START_POINT, planRouteSubMenuOrder++,
                    GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdUseAsStartPoint));
            // via point 1
            routePlannerSubMenu.add(
                    Menu.NONE, MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_1, planRouteSubMenuOrder++,
                    GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdUseAsViaPoint1));
            // via point 2
            if (p2pRouteRequest.getViaPoint1() != null) {
                routePlannerSubMenu.add(
                        Menu.NONE, MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_2, planRouteSubMenuOrder++,
                        GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdUseAsViaPoint2));
            }
            // via point 3
            if (p2pRouteRequest.getViaPoint2() != null) {
                routePlannerSubMenu.add(
                        Menu.NONE, MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_3, planRouteSubMenuOrder++,
                        GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdUseAsViaPoint3));
            }
            // destination
            routePlannerSubMenu.add(
                    Menu.NONE, MENU_ITEM_ROUTE_PLANNER_USE_AS_DESTINATION_POINT, planRouteSubMenuOrder++,
                    GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdUseAsDestinationPoint));
        }

        // share
        if (object instanceof Point) {
            SubMenu shareCoordinatesSubMenu = contextMenu.getMenu().addSubMenu(
                    MENU_GROUP_4, Menu.NONE, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdShareCoordinates));
            Point.populateShareCoordinatesSubMenuEntries(shareCoordinatesSubMenu);
        } else if (object instanceof Route) {
            contextMenu.getMenu().add(
                    MENU_GROUP_4, MENU_ITEM_EXPORT_TO_GPX_FILE, orderId++,
                    GlobalInstance.getStringResource(R.string.exportGpxFileDialogTitle));
        }

        contextMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {
                if (executeAccessibilityMenuAction(item.getItemId())) {
                    return true;
                } else if (executeObjectMenuAction(view.getContext(), object, item)) {
                    return true;
                } else if (object instanceof Point) {
                    return executePointMenuAction(view.getContext(), (Point) object, item);
                } else if (object instanceof Route) {
                    return executeRouteMenuAction(view.getContext(), (Route) object, item);
                } else if (object instanceof Segment) {
                    return executeSegmentMenuAction(view.getContext(), (Segment) object, item);
                } else {
                    return false;
                }
            }
        });

        contextMenu.show();
    }


    private boolean executeObjectMenuAction(Context context, ObjectWithId object, MenuItem item) {
        int menuItemId = item.getItemId();

        if (menuItemId == MENU_ITEM_OVERVIEW_PIN
                || menuItemId == MENU_ITEM_OVERVIEW_TRACK) {

            if (menuItemId == MENU_ITEM_OVERVIEW_PIN) {
                if (StaticProfile.pinnedObjectsWithId().containsObject(object)) {
                    StaticProfile.pinnedObjectsWithId().removeObject(object);
                } else {
                    StaticProfile.pinnedObjectsWithId().addObject(object);
                }

            } else if (menuItemId == MENU_ITEM_OVERVIEW_TRACK) {
                if (StaticProfile.trackedObjectsWithId().containsObject(object)) {
                    StaticProfile.trackedObjectsWithId().removeObject(object);
                } else {
                    StaticProfile.trackedObjectsWithId().addObject(object);
                }
            }

            // update parent view
            ViewChangedListener.sendProfileListChangedBroadcast();

        } else if (menuItemId == MENU_ITEM_COLLECTIONS) {
            mainActivityController.openDialog(
                    UpdateObjectWithIdSelectedCollectionsDialog.newInstance(object));

        } else if (menuItemId == MENU_ITEM_EDIT_USER_ANNOTATION) {
            mainActivityController.openDialog(
                    UserAnnotationForObjectWithIdDialog.newInstance(object));

        } else if (menuItemId == MENU_ITEM_EDIT_RENAME) {
            mainActivityController.openDialog(
                    RenameObjectWithIdDialog.newInstance(object));

        } else if (menuItemId == MENU_ITEM_REMOVE) {
            if (onRemoveObjectActionListener != null) {
                onRemoveObjectActionListener.onRemoveObjectActionClicked(object);
            }
            this.reset();

        } else {
            return false;
        }
        return true;
    }


    private boolean executePointMenuAction(Context context, Point point, MenuItem item) {
        int menuItemId = item.getItemId();

        if (menuItemId == MENU_ITEM_SIMULATE_LOCATION) {
            boolean enableSimulation = ! item.isChecked();
            if (enableSimulation) {
                positionManagerInstance.setSimulatedLocation(point);
            }
            positionManagerInstance.setSimulationEnabled(enableSimulation);

        } else if (menuItemId == MENU_ITEM_NAVIGATE_TO_THIS_POINT) {
            Point currentLocation = PositionManager.getInstance().getCurrentLocation();
            if (currentLocation == null) {
                Toast.makeText(
                        context,
                        GlobalInstance.getStringResource(R.string.errorNoLocationFound),
                        Toast.LENGTH_LONG).show();
                return true;
            }

            P2pRouteRequest p2pRouteRequest = P2pRouteRequest.getDefault();
            p2pRouteRequest.setStartPoint(currentLocation);
            p2pRouteRequest.setDestinationPoint(point);
            settingsManagerInstance.setP2pRouteRequest(p2pRouteRequest);
            mainActivityController.openPlanRouteDialog(true);

        } else if (menuItemId == MENU_ITEM_ROUTE_PLANNER_USE_AS_START_POINT
                || menuItemId == MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_1
                || menuItemId == MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_2
                || menuItemId == MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_3
                || menuItemId == MENU_ITEM_ROUTE_PLANNER_USE_AS_DESTINATION_POINT) {
            P2pRouteRequest p2pRouteRequest = settingsManagerInstance.getP2pRouteRequest();
            if (menuItemId == MENU_ITEM_ROUTE_PLANNER_USE_AS_START_POINT) {
                p2pRouteRequest.setStartPoint(point);
            } else if (menuItemId == MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_1) {
                p2pRouteRequest.setViaPoint1(point);
            } else if (menuItemId == MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_2) {
                p2pRouteRequest.setViaPoint2(point);
            } else if (menuItemId == MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_3) {
                p2pRouteRequest.setViaPoint3(point);
            } else if (menuItemId == MENU_ITEM_ROUTE_PLANNER_USE_AS_DESTINATION_POINT) {
                p2pRouteRequest.setDestinationPoint(point);
            }
            settingsManagerInstance.setP2pRouteRequest(p2pRouteRequest);
            // show plan route dialog
            mainActivityController.openPlanRouteDialog(false);

        } else if (menuItemId == Point.MENU_ITEM_SHARE_APPLE_MAPS_LINK) {
            point.startShareCoordinatesChooserActivity(
                    context, Point.SharingService.APPLE_MAPS);
        } else if (menuItemId == Point.MENU_ITEM_SHARE_GOOGLE_MAPS_LINK) {
            point.startShareCoordinatesChooserActivity(
                    context, Point.SharingService.GOOGLE_MAPS);
        } else if (menuItemId == Point.MENU_ITEM_SHARE_OSM_ORG_LINK) {
            point.startShareCoordinatesChooserActivity(
                    context, Point.SharingService.OSM_ORG);

        } else {
            return false;
        }
        return true;
    }


    private boolean executeRouteMenuAction(Context context, Route route, MenuItem item) {
        int menuItemId = item.getItemId();

        if (menuItemId == MENU_ITEM_LOAD_ROUTE_CURRENT_DIRECTION) {
            mainActivityController.closeAllOpenDialogs();
            MainActivity.loadRoute(context, route);

        } else if (menuItemId == MENU_ITEM_LOAD_ROUTE_OPPOSITE_DIRECTION) {
            Route reversedRoute = null;
            try {
                reversedRoute = Route.reverse(route);
            } catch (JSONException e) {
                Toast.makeText(
                        context,
                        GlobalInstance.getStringResource(R.string.messageCantReverseRoute),
                        Toast.LENGTH_LONG).show();
                return false;
            }
            mainActivityController.closeAllOpenDialogs();
            MainActivity.loadRoute(context, reversedRoute);

        } else if (menuItemId == MENU_ITEM_EXPORT_TO_GPX_FILE) {
            mainActivityController.openDialog(
                    ExportRouteToGpxFileDialog.newInstance(route));

        } else {
            return false;
        }
        return true;
    }


    private boolean executeSegmentMenuAction(Context context, Segment segment, MenuItem item) {
        int menuItemId = item.getItemId();

        if (menuItemId == MENU_ITEM_SIMULATE_BEARING) {
            boolean enableSimulation = ! item.isChecked();
            if (enableSimulation) {
                deviceSensorManagerInstance.setSimulatedBearing(segment.getBearing());
            }
            deviceSensorManagerInstance.setSimulationEnabled(enableSimulation);

        } else if (menuItemId == MENU_ITEM_EXCLUDE_FROM_ROUTING) {
            if (StaticProfile.excludedRoutingSegments().containsObject(segment)) {
                StaticProfile.excludedRoutingSegments().removeObject(segment);
            } else {
                StaticProfile.excludedRoutingSegments().addObject(segment);
            }
            // update parent view
            ViewChangedListener.sendObjectWithIdListChangedBroadcast();

        } else {
            return false;
        }
        return true;
    }


    /**
     * dialogs
     */

    public static class RenameObjectWithIdDialog extends EnterStringDialog {
        public static final String ACTION_RENAME_OBJECT_WITH_ID_WAS_SUCCESSFUL = "action.renameObjectWithIdWasSuccessful";

        public static RenameObjectWithIdDialog newInstance(ObjectWithId objectWithId) {
            RenameObjectWithIdDialog dialog = new RenameObjectWithIdDialog();
            Bundle args = new Bundle();
            args.putSerializable(KEY_OBJECT_WITH_ID, objectWithId);
            dialog.setArguments(args);
            return dialog;
        }

        // dialog
        private static final String KEY_OBJECT_WITH_ID = "objectWithId";

        private ObjectWithId objectWithId;

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            objectWithId = (ObjectWithId) getArguments().getSerializable(KEY_OBJECT_WITH_ID);
            if (objectWithId != null) {
                setInitialInput(
                        objectWithId.getName());
                setDialogTitle(
                        String.format(
                            getResources().getString(R.string.renameObjectWithIdDialogTitle),
                            objectWithId.getIcon().name)
                        );
                setMissingInputMessage(
                        getResources().getString(R.string.messageNameIsMissing));

                if (objectWithId.hasCustomNameInDatabase()) {
                    setNeutralButton(
                            getResources().getString(R.string.renameObjectWithIdDialogNeutralButton),
                            objectWithId.getOriginalName());
                }

                return super.onCreateDialog(savedInstanceState);
            }
            return null;
        }

        @Override public void execute(String input) {
            if (input.equals(objectWithId.getOriginalName())) input = null;
            if (objectWithId.setCustomNameInDatabase(input)) {
                Intent intent = new Intent(ACTION_RENAME_OBJECT_WITH_ID_WAS_SUCCESSFUL);
                LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
                dismiss();
            } else {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageRenameFailed),
                        Toast.LENGTH_LONG).show();
            }
        }
    }


    public static class UpdateObjectWithIdSelectedCollectionsDialog extends SelectCollectionsDialog {
        public static final String ACTION_UPDATE_OBJECT_WITH_ID_SELECTED_COLLECTIONS_WAS_SUCCESSFUL = "action.updateObjectWithIdSelectedCollectionsWasSuccessful";

        public static UpdateObjectWithIdSelectedCollectionsDialog newInstance(ObjectWithId object) {
            UpdateObjectWithIdSelectedCollectionsDialog dialog= new UpdateObjectWithIdSelectedCollectionsDialog();
            Bundle args = createInitialObjectListBundle(
                    AccessDatabase.getInstance().getCollectionList(), object.getSelectedCollectionList());
            args.putSerializable(KEY_OBJECT_WITH_ID, object);
            dialog.setArguments(args);
            return dialog;
        }

        // dialog
        private static final String KEY_OBJECT_WITH_ID = "objectWithId";

        private ObjectWithId objectWithId;

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            objectWithId = (ObjectWithId) getArguments().getSerializable(KEY_OBJECT_WITH_ID);
            if (objectWithId != null) {
                return super.onCreateDialog(savedInstanceState);
            }
            return null;
        }

        @Override public String getDialogTitle() {
            return getResources().getString(R.string.updateObjectWithIdSelectedCollectionsDialogTitle);
        }

        @Override public void execute(ArrayList<Collection> selectedCollectionList) {
            objectWithId.setSelectedCollectionList(selectedCollectionList);
            Intent intent = new Intent(ACTION_UPDATE_OBJECT_WITH_ID_SELECTED_COLLECTIONS_WAS_SUCCESSFUL);
            LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
            dismiss();
        }
    }

}
