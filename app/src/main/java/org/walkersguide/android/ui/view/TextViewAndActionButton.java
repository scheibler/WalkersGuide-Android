        package org.walkersguide.android.ui.view;

import org.walkersguide.android.sensor.bearing.AcceptNewBearing;
import org.walkersguide.android.sensor.position.AcceptNewPosition;
import org.walkersguide.android.ui.activity.toolbar.tabs.MainActivity;
import org.walkersguide.android.server.wg.p2p.P2pRouteRequest;
import androidx.appcompat.app.AppCompatActivity;
import org.walkersguide.android.ui.dialog.edit.RenameObjectDialog;
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
import org.walkersguide.android.ui.activity.toolbar.tabs.PointDetailsActivity;
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
import org.walkersguide.android.ui.activity.toolbar.tabs.SegmentDetailsActivity;
import androidx.core.view.MenuCompat;
import android.content.BroadcastReceiver;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import org.walkersguide.android.data.object_with_id.HikingTrail;
import org.walkersguide.android.data.object_with_id.Route;
import android.widget.Toast;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import org.walkersguide.android.ui.activity.toolbar.FragmentContainerActivity;
import android.content.res.TypedArray;
import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.poi.Station;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.POI;
import org.walkersguide.android.data.angle.Bearing;


public class TextViewAndActionButton extends LinearLayout {


    /**
     * interfaces
     */

    // default action

    public interface OnObjectDefaultActionListener {
        public void onObjectDefaultAction(TextViewAndActionButton view);
    }

    private boolean showDetailsAction;
    private OnObjectDefaultActionListener onObjectDefaultActionListener;

    public void setOnObjectDefaultActionListener(OnObjectDefaultActionListener listener) {
        setOnObjectDefaultActionListener(listener, true);
    }

    public void setOnObjectDefaultActionListener(OnObjectDefaultActionListener listener, boolean showDetailsAction) {
        this.showDetailsAction = showDetailsAction;
        this.onObjectDefaultActionListener = listener;
    }

    // parent list update request

    public interface OnUpdateListRequestListener {
        public void onUpdateListRequested(TextViewAndActionButton view);
    }

    private OnUpdateListRequestListener onUpdateListRequestListener;

    // reset layout action

    public interface OnLayoutResetListener {
        public void onLayoutReset(TextViewAndActionButton view);
    }

    private OnLayoutResetListener onLayoutResetListener;


    /**
     * initialize
     */

    private DeviceSensorManager deviceSensorManagerInstance;
    private PositionManager positionManagerInstance;
    private SettingsManager settingsManagerInstance;

    private String prefix = null;
    private boolean includeDistanceOrBearingInformation = false;
    private boolean compact = false;

    private ObjectWithId objectWithId;
    private int isFavoriteModeHide, isFavoriteModeVisible;
    private boolean autoUpdate;

    private ImageView imageViewIsFavorite;
    private TextView label;
    private ImageButton buttonActionFor;

    public TextViewAndActionButton(Context context) {
        super(context);
        this.initUi(context);
    }

    public TextViewAndActionButton(Context context, boolean includeDistanceOrBearingInformation) {
        super(context);
        this.includeDistanceOrBearingInformation = includeDistanceOrBearingInformation;
        this.initUi(context);
    }

    public TextViewAndActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        // parse xml layout attributes
        TypedArray attributeArray = context.obtainStyledAttributes(
                attrs, R.styleable.TextViewAndActionButton);
        if (attributeArray != null) {
            this.prefix = attributeArray.getString(
                    R.styleable.TextViewAndActionButton_prefix);
            this.includeDistanceOrBearingInformation = attributeArray.getBoolean(
                    R.styleable.TextViewAndActionButton_includeDistanceOrBearingInformation, false);
            this.compact = attributeArray.getBoolean(
                    R.styleable.TextViewAndActionButton_compact, false);
            attributeArray.recycle();
        }

        this.initUi(context);
    }

    private void initUi(Context context) {
        deviceSensorManagerInstance = DeviceSensorManager.getInstance();
        positionManagerInstance = PositionManager.getInstance();
        settingsManagerInstance = SettingsManager.getInstance();

        // configure enclosing linear layout
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        View rootView = inflate(context, R.layout.layout_text_view_and_action_button, this);
        imageViewIsFavorite = (ImageView) rootView.findViewById(R.id.imageViewIsFavorite);

        label = (TextView) rootView.findViewById(R.id.label);
        if (compact) {
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setSingleLine();
        }
        label.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (onObjectDefaultActionListener != null) {
                    onObjectDefaultActionListener.onObjectDefaultAction(TextViewAndActionButton.this);
                } else if (objectWithId != null) {
                    executeObjectMenuAction(
                            view.getContext(), objectWithId, MENU_ITEM_DETAILS);
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
        buttonActionFor.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (objectWithId != null) {
                    showContextMenu(view, objectWithId);
                }
            }
        });

        this.onObjectDefaultActionListener = null;
        this.showDetailsAction = false;
        this.autoUpdate = false;
        this.reset();
    }

    public ObjectWithId getObject() {
        return this.objectWithId;
    }

    public void reset() {
        this.objectWithId = null;
        this.isFavoriteModeHide = View.GONE;
        this.isFavoriteModeVisible = View.GONE;
        this.onUpdateListRequestListener = null;
        this.onLayoutResetListener = null;
        this.setLabelAndButtonText(
                GlobalInstance.getStringResource(R.string.labelNothingSelected));
    }

    public void setAutoUpdate(boolean newState) {
        this.autoUpdate = newState;
    }

    public void configureAsListItem(ObjectWithId object, boolean showIsFavoriteIndicator, OnUpdateListRequestListener listener) {
        this.reset();
        if (object != null) {
            this.objectWithId = object;
            if (showIsFavoriteIndicator) {
                this.isFavoriteModeVisible = View.VISIBLE;
                this.isFavoriteModeHide = View.INVISIBLE;
            }
            this.onUpdateListRequestListener = listener;
            this.setLabelAndButtonText(object.toString());
        }
    }

    public void configureAsSingleObject(ObjectWithId object) {
        this.configureAsSingleObject(object, object != null ? object.toString() : null);
    }

    public void configureAsSingleObject(ObjectWithId object, String labelText) {
        this.configureAsSingleObject(object, labelText, null);
    }

    public void configureAsSingleObject(ObjectWithId object, String labelText, OnLayoutResetListener listener) {
        this.reset();
        if (labelText != null) {
            if (object != null) {
                this.objectWithId = object;
                this.isFavoriteModeVisible = View.VISIBLE;
                this.onLayoutResetListener = listener;
            }
            this.setLabelAndButtonText(labelText);
        }
    }

    private void setLabelAndButtonText(String labelText) {
        this.label.setTag(labelText);

        // prepare complete label text
        if (this.prefix != null) {
            labelText = String.format(
                    "%1$s%2$s%3$s",
                    this.prefix,
                    this.compact ? ": " : ":\n",
                    labelText);
        }
        if (this.includeDistanceOrBearingInformation) {
            String distanceOrBearing = null;
            if (this.objectWithId instanceof Point) {
                distanceOrBearing = ((Point) this.objectWithId).formatDistanceAndRelativeBearingFromCurrentLocation();
                if (! TextUtils.isEmpty(distanceOrBearing)) {
                    labelText = String.format(
                            "%1$s\n%2$s", labelText, distanceOrBearing);
                }
            } else if (this.objectWithId instanceof IntersectionSegment) {
                distanceOrBearing = ((IntersectionSegment) this.objectWithId).formatRelativeBearingFromCurrentLocation();
                if (! TextUtils.isEmpty(distanceOrBearing)) {
                    labelText = String.format(
                            "%1$s: %2$s", distanceOrBearing, labelText);
                }
            }
        }
        this.label.setText(labelText);

        // favorite indicator
        updateFavoriteIndicator();

        // action button
        if (this.objectWithId != null
                && settingsManagerInstance.getShowActionButton()) {
            this.buttonActionFor.setContentDescription(
                    String.format(
                        "%1$s %2$s",
                        getResources().getString(R.string.buttonActionFor),
                        this.objectWithId.getName())
                    );
            this.buttonActionFor.setVisibility(View.VISIBLE);
        } else {
            this.buttonActionFor.setVisibility(View.GONE);
        }
    }

    private void updateFavoriteIndicator() {
        int favoriteIndicatorVisibilityMode = isFavoriteModeHide;
        if (this.objectWithId != null && this.objectWithId.isFavorite()) {
            favoriteIndicatorVisibilityMode = isFavoriteModeVisible;
        }
        this.imageViewIsFavorite.setVisibility(favoriteIndicatorVisibilityMode);

        // content description
        if (favoriteIndicatorVisibilityMode == View.VISIBLE) {
            this.label.setContentDescription(
                    String.format(
                        "%1$s: %2$s",
                        GlobalInstance.getStringResource(R.string.labelIsFavorite),
                        this.label.getText().toString())
                    );
        } else {
            this.label.setContentDescription(null);
        }
    }


    /**
     * auto update distance and bearing
     */

    @Override public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).unregisterReceiver(newLocationReceiver);
    }

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.autoUpdate && this.includeDistanceOrBearingInformation) {
            Timber.d("onAttachedToWindow");
            IntentFilter filter = new IntentFilter();
            filter.addAction(PositionManager.ACTION_NEW_LOCATION);
            filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING);
            LocalBroadcastManager.getInstance(GlobalInstance.getContext()).registerReceiver(newLocationReceiver, filter);
        }
    }

    private BroadcastReceiver newLocationReceiver = new BroadcastReceiver() {
        private AcceptNewPosition acceptNewPosition = AcceptNewPosition.newInstanceForTextViewAndActionButtonUpdate();
        private AcceptNewBearing acceptNewBearing = AcceptNewBearing.newInstanceForTextViewAndActionButtonUpdate();

        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)) {
                Point currentLocation = (Point) intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION);
                if (currentLocation != null
                        && (
                               intent.getBooleanExtra(PositionManager.EXTRA_IS_IMPORTANT, false)
                            || acceptNewPosition.updatePoint(currentLocation))) {
                    setLabelAndButtonText((String) label.getTag());
                }

            } else if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING)) {
                Bearing currentBearing = (Bearing) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING);
                if (currentBearing != null
                        && acceptNewBearing.updateBearing(currentBearing)) {
                    setLabelAndButtonText((String) label.getTag());
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

    private static final int MENU_ITEM_DETAILS = 1;
    private static final int MENU_ITEM_DEPARTURES = 2;
    private static final int MENU_ITEM_ENTRANCES = 3;
    private static final int MENU_ITEM_LOAD = 4;
    private static final int MENU_ITEM_STREET_COURSE = 5;
    private static final int MENU_ITEM_ADD_TO_FAVORITES = 10;
    private static final int MENU_ITEM_REMOVE_FROM_FAVORITES = 11;
    private static final int MENU_ITEM_START_LOCATION_SIMULATION = 12;
    private static final int MENU_ITEM_END_LOCATION_SIMULATION = 13;
    private static final int MENU_ITEM_START_BEARING_SIMULATION = 14;
    private static final int MENU_ITEM_END_BEARING_SIMULATION = 15;
    private static final int MENU_ITEM_ADD_TO_EXCLUDED_FROM_ROUTING = 16;
    private static final int MENU_ITEM_REMOVE_EXCLUDE_FROM_ROUTING = 17;
    private static final int MENU_ITEM_RENAME = 18;
    private static final int MENU_ITEM_RESET_LAYOUT = 19;
    private static final int MENU_ITEM_ROUTE_PLANNER = 20;
    private static final int MENU_ITEM_SHARE_COORDINATES = 21;

    private static final int MENU_ITEM_ROUTE_PLANNER_USE_AS_START_POINT = 100;
    private static final int MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_1 = 101;
    private static final int MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_2 = 102;
    private static final int MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_3 = 103;
    private static final int MENU_ITEM_ROUTE_PLANNER_USE_AS_DESTINATION_POINT = 104;


    private void showContextMenu(final View view, final ObjectWithId object) {
        PopupMenu contextMenu = new PopupMenu(view.getContext(), view);
        MenuCompat.setGroupDividerEnabled(contextMenu.getMenu(), true);
        int orderId = 0;

        // top items
        if (onObjectDefaultActionListener != null && showDetailsAction) {
            contextMenu.getMenu().add(
                    MENU_GROUP_1, MENU_ITEM_DETAILS, orderId++, GlobalInstance.getStringResource(R.string.objectMenuItemDetails));
        }
        if (object instanceof POI) {
            if (object instanceof Station) {
                contextMenu.getMenu().add(
                        MENU_GROUP_1, MENU_ITEM_DEPARTURES, orderId++, GlobalInstance.getStringResource(R.string.objectMenuItemDepartures));
            }
            if (((POI) object).hasEntrance()) {
                contextMenu.getMenu().add(
                        MENU_GROUP_1, MENU_ITEM_ENTRANCES, orderId++, GlobalInstance.getStringResource(R.string.objectMenuItemEntrances));
            }
        } else if (object instanceof Route
                && ! ((Route) object).equals(settingsManagerInstance.getSelectedRoute())) {
            contextMenu.getMenu().add(
                    MENU_GROUP_1, MENU_ITEM_LOAD, orderId++, GlobalInstance.getStringResource(R.string.objectMenuItemLoad));
        } else if (object instanceof IntersectionSegment) {
            contextMenu.getMenu().add(
                    MENU_GROUP_1, MENU_ITEM_STREET_COURSE, orderId++, GlobalInstance.getStringResource(R.string.objectMenuItemStreetCourse));
        }

        // favorite
        if (object.hasDefaultFavoritesProfile()) {
            if (object.isFavorite()) {
                contextMenu.getMenu().add(
                        MENU_GROUP_2, MENU_ITEM_REMOVE_FROM_FAVORITES, orderId++, GlobalInstance.getStringResource(R.string.objectMenuItemRemoveFromFavorites));
            } else {
                contextMenu.getMenu().add(
                        MENU_GROUP_2, MENU_ITEM_ADD_TO_FAVORITES, orderId++, GlobalInstance.getStringResource(R.string.objectMenuItemAddToFavorites));
            }
        }

        // simulation
        if (object instanceof Point) {
            if (positionManagerInstance.getSimulationEnabled()
                    && ((Point) object).equals(positionManagerInstance.getSimulatedLocation())) {
                contextMenu.getMenu().add(
                        MENU_GROUP_2, MENU_ITEM_END_LOCATION_SIMULATION, orderId++, GlobalInstance.getStringResource(R.string.objectMenuItemEndSimulation));
            } else {
                contextMenu.getMenu().add(
                        MENU_GROUP_2, MENU_ITEM_START_LOCATION_SIMULATION, orderId++, GlobalInstance.getStringResource(R.string.objectMenuItemStartLocationSimulation));
            }
        } else if (object instanceof Segment) {
            if (deviceSensorManagerInstance.getSimulationEnabled()
                    && ((Segment) object).equals(deviceSensorManagerInstance.getSimulatedBearing())) {
                contextMenu.getMenu().add(
                        MENU_GROUP_2, MENU_ITEM_END_BEARING_SIMULATION, orderId++, GlobalInstance.getStringResource(R.string.objectMenuItemEndSimulation));
            } else {
                contextMenu.getMenu().add(
                        MENU_GROUP_2, MENU_ITEM_START_BEARING_SIMULATION, orderId++, GlobalInstance.getStringResource(R.string.objectMenuItemStartBearingSimulation));
            }
        }

        // exclude from routing
        if (object instanceof Segment) {
            if (((Segment) object).isExcludedFromRouting()) {
                contextMenu.getMenu().add(
                        MENU_GROUP_2, MENU_ITEM_REMOVE_EXCLUDE_FROM_ROUTING, orderId++, GlobalInstance.getStringResource(R.string.objectMenuItemRemoveExcludeFromRouting));
            } else {
                contextMenu.getMenu().add(
                        MENU_GROUP_2, MENU_ITEM_ADD_TO_EXCLUDED_FROM_ROUTING, orderId++, GlobalInstance.getStringResource(R.string.objectMenuItemAddToExcludeFromRouting));
            }
        }

        // rename and reset
        if (object instanceof Point
                || object instanceof Route
                || object instanceof Segment) {
            contextMenu.getMenu().add(
                    MENU_GROUP_2, MENU_ITEM_RENAME, orderId++, GlobalInstance.getStringResource(R.string.objectMenuItemRename));
        }
        if (onLayoutResetListener != null) {
            contextMenu.getMenu().add(
                    MENU_GROUP_2, MENU_ITEM_RESET_LAYOUT, orderId++, GlobalInstance.getStringResource(R.string.objectMenuItemResetLayout));
        }

        // route planner
        if (object instanceof Point) {
            P2pRouteRequest p2pRouteRequest = settingsManagerInstance.getP2pRouteRequest();
            SubMenu routePlannerSubMenu = contextMenu.getMenu().addSubMenu(
                    MENU_GROUP_3, Menu.NONE, orderId++, GlobalInstance.getStringResource(R.string.objectMenuItemRoutePlanner));
            int planRouteSubMenuOrder = 0;
            // start
            routePlannerSubMenu.add(
                    Menu.NONE, MENU_ITEM_ROUTE_PLANNER_USE_AS_START_POINT, planRouteSubMenuOrder++,
                    GlobalInstance.getStringResource(R.string.objectMenuItemUseAsStartPoint));
            // via point 1
            routePlannerSubMenu.add(
                    Menu.NONE, MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_1, planRouteSubMenuOrder++,
                    GlobalInstance.getStringResource(R.string.objectMenuItemUseAsViaPoint1));
            // via point 2
            if (p2pRouteRequest.getViaPoint1() != null) {
                routePlannerSubMenu.add(
                        Menu.NONE, MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_2, planRouteSubMenuOrder++,
                        GlobalInstance.getStringResource(R.string.objectMenuItemUseAsViaPoint2));
            }
            // via point 3
            if (p2pRouteRequest.getViaPoint2() != null) {
                routePlannerSubMenu.add(
                        Menu.NONE, MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_3, planRouteSubMenuOrder++,
                        GlobalInstance.getStringResource(R.string.objectMenuItemUseAsViaPoint3));
            }
            // destination
            routePlannerSubMenu.add(
                    Menu.NONE, MENU_ITEM_ROUTE_PLANNER_USE_AS_DESTINATION_POINT, planRouteSubMenuOrder++,
                    GlobalInstance.getStringResource(R.string.objectMenuItemUseAsDestinationPoint));
        }

        // share
        if (object instanceof Point) {
            SubMenu shareCoordinatesSubMenu = contextMenu.getMenu().addSubMenu(
                    MENU_GROUP_3, Menu.NONE, orderId++, GlobalInstance.getStringResource(R.string.objectMenuItemShareCoordinates));
            Point.populateShareCoordinatesSubMenuEntries(shareCoordinatesSubMenu);
        }

        contextMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {
                Timber.d("onMenuItemClick: %1$d", item.getItemId());
                if (executeObjectMenuAction(view.getContext(), object, item.getItemId())) {
                    return true;
                } else if (object instanceof Point) {
                    return executePointMenuAction(
                            view.getContext(), (Point) object, item.getItemId());
                } else if (object instanceof HikingTrail) {
                    return executeHikingTrailMenuAction(
                            view.getContext(), (HikingTrail) object, item.getItemId());
                } else if (object instanceof Route) {
                    return executeRouteMenuAction(
                            view.getContext(), (Route) object, item.getItemId());
                } else if (object instanceof Segment) {
                    return executeSegmentMenuAction(
                            view.getContext(), (Segment) object, item.getItemId());
                } else {
                    return false;
                }
            }
        });

        contextMenu.show();
    }


    private boolean executeObjectMenuAction(Context context, ObjectWithId object, int menuItemId) {
        if (menuItemId == MENU_ITEM_DETAILS) {
            if (object instanceof Point) {
                PointDetailsActivity.start(context, (Point) object);
            } else if (object instanceof HikingTrail) {
                FragmentContainerActivity.showDetailsForObjectWithId(context, (HikingTrail) object);
            } else if (object instanceof Route) {
                FragmentContainerActivity.showDetailsForObjectWithId(context, (Route) object);
            } else if (object instanceof Segment) {
                SegmentDetailsActivity.start(context, (Segment) object);
            }

        } else if (menuItemId == MENU_ITEM_ADD_TO_FAVORITES
                || menuItemId == MENU_ITEM_REMOVE_FROM_FAVORITES) {
            if (menuItemId == MENU_ITEM_ADD_TO_FAVORITES) {
                object.addToFavorites();
            } else {
                object.removeFromFavorites();
                // notify parent list update
                if (this.onUpdateListRequestListener != null) {
                    this.onUpdateListRequestListener.onUpdateListRequested(TextViewAndActionButton.this);
                }
            }
            updateFavoriteIndicator();

        } else if (menuItemId == MENU_ITEM_RENAME) {
            RenameObjectDialog roDialog = RenameObjectDialog.newInstance(object);
            if (context instanceof AppCompatActivity) {
                roDialog.show(
                        ((AppCompatActivity) context).getSupportFragmentManager(), "RenameObjectDialog");
            }

        } else if (menuItemId == MENU_ITEM_RESET_LAYOUT) {
            this.reset();
            if (onLayoutResetListener != null) {
                onLayoutResetListener.onLayoutReset(TextViewAndActionButton.this);
            }

        } else {
            return false;
        }
        return true;
    }


    private boolean executePointMenuAction(Context context, Point point, int menuItemId) {
        if (menuItemId == MENU_ITEM_DEPARTURES) {
            if (point instanceof Station) {
                PointDetailsActivity.startAtTab(
                        context, (Station) point, PointDetailsActivity.Tab.DEPARTURES);
            }

        } else if (menuItemId == MENU_ITEM_ENTRANCES) {
            if (point instanceof POI) {
                PointDetailsActivity.startAtTab(
                        context, (POI) point, PointDetailsActivity.Tab.ENTRANCES);
            }

        } else if (menuItemId == MENU_ITEM_START_LOCATION_SIMULATION) {
            positionManagerInstance.setSimulatedLocation(point);
            positionManagerInstance.setSimulationEnabled(true);
        } else if (menuItemId == MENU_ITEM_END_LOCATION_SIMULATION) {
            positionManagerInstance.setSimulationEnabled(false);

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
            PlanRouteDialog prDialog = PlanRouteDialog.newInstance();
            if (context instanceof AppCompatActivity) {
                prDialog.show(
                        ((AppCompatActivity) context).getSupportFragmentManager(), "PlanRouteDialog");
            }

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


    private boolean executeHikingTrailMenuAction(Context context, HikingTrail hikingTrail, int menuItemId) {
        return false;
    }


    private boolean executeRouteMenuAction(Context context, Route route, int menuItemId) {
        if (menuItemId == MENU_ITEM_LOAD) {
            MainActivity.loadRoute(context, route);

        } else {
            return false;
        }
        return true;
    }


    private boolean executeSegmentMenuAction(Context context, Segment segment, int menuItemId) {
        if (menuItemId == MENU_ITEM_STREET_COURSE) {
            if (segment instanceof IntersectionSegment) {
                SegmentDetailsActivity.startAtTab(
                        context, (IntersectionSegment) segment, SegmentDetailsActivity.Tab.STREET_COURSE);
            }

        } else if (menuItemId == MENU_ITEM_START_BEARING_SIMULATION) {
            deviceSensorManagerInstance.setSimulatedBearing(segment.getBearing());
            deviceSensorManagerInstance.setSimulationEnabled(true);
        } else if (menuItemId == MENU_ITEM_END_BEARING_SIMULATION) {
            deviceSensorManagerInstance.setSimulationEnabled(false);

        } else if (menuItemId == MENU_ITEM_ADD_TO_EXCLUDED_FROM_ROUTING) {
            segment.excludeFromRouting();
        } else if (menuItemId == MENU_ITEM_REMOVE_EXCLUDE_FROM_ROUTING) {
            segment.includeIntoRouting();
            // notify parent list update
            if (this.onUpdateListRequestListener != null) {
                this.onUpdateListRequestListener.onUpdateListRequested(TextViewAndActionButton.this);
            }

        } else {
            return false;
        }
        return true;
    }

}
