        package org.walkersguide.android.ui.view;

import org.walkersguide.android.database.profiles.DatabaseRouteProfile;
import org.walkersguide.android.database.profiles.DatabaseSegmentProfile;
import androidx.appcompat.app.AppCompatActivity;
import org.walkersguide.android.ui.dialog.creators.RenameObjectDialog;
import org.walkersguide.android.ui.dialog.creators.RenameObjectDialog.RenameObjectListener;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.profiles.DatabasePointProfile;
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
import org.walkersguide.android.data.basic.point.Point;
import org.walkersguide.android.ui.activity.toolbar.tabs.PointDetailsActivity;
import java.util.ArrayList;
import android.content.Context;
import android.widget.ImageView;
import org.walkersguide.android.util.Constants;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener;
import android.view.Menu;
import android.view.SubMenu;
import android.content.Intent;
import android.net.Uri;
import java.util.Locale;
import android.widget.LinearLayout;
import android.util.AttributeSet;
import android.view.Gravity;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.data.basic.segment.Segment;
import org.walkersguide.android.ui.activity.toolbar.tabs.SegmentDetailsActivity;
import androidx.core.view.MenuCompat;
import org.walkersguide.android.data.sensor.Direction;
import android.content.BroadcastReceiver;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import org.walkersguide.android.data.sensor.attribute.NewLocationAttributes;
import org.walkersguide.android.data.sensor.threshold.DistanceThreshold;
import org.walkersguide.android.data.route.Route;
import org.walkersguide.android.data.sensor.attribute.NewDirectionAttributes;
import org.walkersguide.android.data.sensor.threshold.BearingThreshold;


public class TextViewAndActionButton extends LinearLayout implements RenameObjectListener {


    /**
     * label config
     */

    public static class LabelTextConfig {

        public static LabelTextConfig empty(boolean useObjectName) {
            return new LabelTextConfig("", useObjectName);
        }
        public static LabelTextConfig start(boolean useObjectName) {
            return new LabelTextConfig(
                    GlobalInstance.getStringResource(R.string.labelPrefixStart), useObjectName);
        }
        public static LabelTextConfig via(int number, boolean useObjectName) {
            return new LabelTextConfig(
                    String.format(GlobalInstance.getStringResource(R.string.labelPrefixVia), number), useObjectName);
        }
        public static LabelTextConfig destination(boolean useObjectName) {
            return new LabelTextConfig(
                    GlobalInstance.getStringResource(R.string.labelPrefixDestination), useObjectName);
        }
        public static LabelTextConfig simulation(boolean useObjectName) {
            return new LabelTextConfig(
                    GlobalInstance.getStringResource(R.string.labelPrefixSimulation), useObjectName);
        }

        public String prefix;
        public boolean useObjectName;
        private LabelTextConfig(String prefix, boolean useObjectName) {
            this.prefix = prefix;
            this.useObjectName = useObjectName;
        }
    }


    /**
     * listener
     */

    public interface OnLabelClickListener {
        public void onLabelClick(TextViewAndActionButton view);
    }

    public interface OnMenuItemRemoveClickListener {
        public void onMenuItemRemoveClick(TextViewAndActionButton view);
    }

    public interface NotifyDataSetChangedListener {
        public void notifyDataSetChanged(TextViewAndActionButton view);
    }


    private OnLabelClickListener onLabelClickListener;
    private OnMenuItemRemoveClickListener onMenuItemRemoveClickListener;
    private NotifyDataSetChangedListener notifyDataSetChangedListener;

    public void setOnLabelClickListener(OnLabelClickListener listener) {
        onLabelClickListener = listener;
    }

    public void setOnMenuItemRemoveClickListener(OnMenuItemRemoveClickListener listener) {
        onMenuItemRemoveClickListener = listener;
    }

    public void setOnNotifyDataSetChangedListener(NotifyDataSetChangedListener listener) {
        notifyDataSetChangedListener = listener;
    }


    /**
     * initialize
     */

    private AccessDatabase accessDatabaseInstance;
    private DirectionManager directionManagerInstance;
    private PositionManager positionManagerInstance;

    private ObjectWithId objectWithId;
    private LabelTextConfig labelTextConfig;
    private boolean showIsFavoriteIndicator, showMenuItemRemove;

    private View rootView;
    private ImageView imageViewIsFavorite;
    private TextView label;
    private ImageButton buttonActionFor;


    public TextViewAndActionButton(Context context) {
        super(context);
        init(context);
    }

    public TextViewAndActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        objectWithId = null;
        labelTextConfig = null;
        showIsFavoriteIndicator = true;
        showMenuItemRemove = false;

        accessDatabaseInstance = AccessDatabase.getInstance();
        directionManagerInstance = DirectionManager.getInstance();
        positionManagerInstance = PositionManager.getInstance();

        rootView = inflate(context, R.layout.layout_text_view_and_action_button, this);
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        imageViewIsFavorite = (ImageView) rootView.findViewById(R.id.imageViewIsFavorite);
        imageViewIsFavorite.setVisibility(View.GONE);

        label = (TextView) rootView.findViewById(R.id.label);
        label.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Timber.d("label onClick: %1$s", onLabelClickListener);
                if (onLabelClickListener != null) {
                    onLabelClickListener.onLabelClick(TextViewAndActionButton.this);
                } else {
                    // default action: show details
                    if (objectWithId instanceof Point) {
                        PointDetailsActivity.start(view.getContext(), (Point) objectWithId);
                    } else if (objectWithId instanceof Segment) {
                        SegmentDetailsActivity.start(view.getContext(), (Segment) objectWithId);
                    }
                }
            }
        });
        label.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View view) {
                Timber.d("label onLongClick");
                showContextMenu(view);
                return true;
            }
        });

        buttonActionFor = (ImageButton) rootView.findViewById(R.id.buttonActionFor);
        buttonActionFor.setVisibility(View.GONE);
        buttonActionFor.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                Timber.d("button clicked");
                showContextMenu(view);
            }
        });
    }

    public void configureView(ObjectWithId objectWithId) {
        this.objectWithId = objectWithId;
        this.labelTextConfig = null;
        this.showIsFavoriteIndicator = true;
        this.showMenuItemRemove = false;
        updateUI();
    }

    public void configureView(ObjectWithId objectWithId, LabelTextConfig labelTextConfig) {
        this.objectWithId = objectWithId;
        this.labelTextConfig = labelTextConfig;
        this.showIsFavoriteIndicator = true;
        this.showMenuItemRemove = false;
        updateUI();
    }

    public void configureView(ObjectWithId objectWithId, LabelTextConfig labelTextConfig, boolean showIsFavoriteIndicator, boolean showMenuItemRemove) {
        this.objectWithId = objectWithId;
        this.labelTextConfig = labelTextConfig;
        this.showIsFavoriteIndicator = showIsFavoriteIndicator;
        this.showMenuItemRemove = showMenuItemRemove;
        updateUI();
    }

    public ObjectWithId getObject() {
        return this.objectWithId;
    }

    public boolean hasObject() {
        return this.objectWithId != null;
    }

    public void setLabelText(String text) {
        label.setText(text);
    }

    private void updateUI() {
        // label
        if (labelTextConfig != null) {
            String labelText;
            if (objectWithId == null) {
                labelText = GlobalInstance.getStringResource(R.string.labelNothingSelected);
            } else {
                if (labelTextConfig.useObjectName) {
                    labelText = objectWithId.getName();
                } else {
                    labelText = objectWithId.toString();
                }
            }
            if (! TextUtils.isEmpty(labelTextConfig.prefix)) {
                labelText = String.format("%1$s: %2$s", labelTextConfig.prefix, labelText);
            }
            label.setText(labelText);
            // content description
            if (showIsFavoriteIndicator
                    && objectWithId instanceof Point
                    && ((Point) objectWithId).isFavorite()) {
                if (! TextUtils.isEmpty(labelTextConfig.prefix)) {
                    label.setContentDescription(
                            String.format(
                                "%1$s\n%2$s",
                                labelText,
                                GlobalInstance.getStringResource(R.string.labelIsFavoriteLong))
                            );
                } else {
                    label.setContentDescription(
                            String.format(
                                "%1$s: %2$s",
                                GlobalInstance.getStringResource(R.string.labelIsFavoriteShort),
                                labelText)
                            );
                }
            } else {
                label.setContentDescription(labelText);
            }
        } else if (notifyDataSetChangedListener != null) {
            notifyDataSetChangedListener.notifyDataSetChanged(TextViewAndActionButton.this);
        }

        // others
        if (objectWithId != null) {
            if (showIsFavoriteIndicator && objectWithId instanceof Point) {
                imageViewIsFavorite.setVisibility(
                        ((Point) objectWithId).isFavorite() ? View.VISIBLE : View.INVISIBLE);
            }

            if (SettingsManager.getInstance().getShowActionButton()) {
                buttonActionFor.setContentDescription(
                        String.format(
                            "%1$s %2$s",
                            getResources().getString(R.string.buttonActionFor),
                            objectWithId.getName())
                        );
                buttonActionFor.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override public void renameObjectSuccessful() {
        updateUI();
    }

    @Override public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Timber.d("onDetachedFromWindow");
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).unregisterReceiver(mMessageReceiver);
    }

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Timber.d("onAttachedToWindow");
        // listen for new location broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_DIRECTION);
        filter.addAction(Constants.ACTION_NEW_LOCATION);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).registerReceiver(mMessageReceiver, filter);
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)) {
                NewDirectionAttributes newDirectionAttributes = NewDirectionAttributes.fromString(
                        context, intent.getStringExtra(Constants.ACTION_NEW_DIRECTION_ATTRIBUTES));
                if (newDirectionAttributes != null
                        && newDirectionAttributes.getAggregatingBearingThreshold().isAtLeast(BearingThreshold.TWENTY_DEGREES)) {
                    updateUI();
                }

            } else if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)) {
                NewLocationAttributes newLocationAttributes = NewLocationAttributes.fromString(
                        intent.getStringExtra(Constants.ACTION_NEW_LOCATION_ATTRIBUTES));
                if (newLocationAttributes != null
                        && newLocationAttributes.getAggregatingDistanceThreshold().isAtLeast(DistanceThreshold.TEN_METERS)
                        && labelTextConfig != null) {
                    updateUI();
                }
            }
        }
    };


    /**
     * context menu
     */

    // group ids (only to create a devider in the menu)
    private static final int MENU_GROUP_1 = 1;
    private static final int MENU_GROUP_2 = 2;

    // item ids for point and segment menu
    private static final int MENU_ITEM_ADD_TO_FAVORITES = 1;
    private static final int MENU_ITEM_REMOVE_FROM_FAVORITES = 2;
    private static final int MENU_ITEM_ADD_TO_EXCLUDED_FROM_ROUTING = 3;
    private static final int MENU_ITEM_REMOVE_EXCLUDE_FROM_ROUTING = 4;
    private static final int MENU_ITEM_START_SIMULATION = 5;
    private static final int MENU_ITEM_END_SIMULATION = 6;
    private static final int MENU_ITEM_RENAME = 7;
    private static final int MENU_ITEM_REMOVE = 8;
    private static final int MENU_ITEM_DETAILS = 9;
    private static final int MENU_ITEM_ROUTE_PLANNER = 10;
    private static final int MENU_ITEM_OPEN_ON_OSM_ORG = 11;

    private void showContextMenu(View view) {
        if (objectWithId instanceof Point) {
            showPointContextMenu(view, (Point) objectWithId);
        } else if (objectWithId instanceof Route) {
            showRouteContextMenu(view, (Route) objectWithId);
        } else if (objectWithId instanceof Segment) {
            showSegmentContextMenu(view, (Segment) objectWithId);
        }
    }


    // point context menu
    private static final String OSM_NODE_URL = "https://www.openstreetmap.org/node/%1$d/";

    // route planner submenu
    private static final int MENU_ITEM_ROUTE_PLANNER_USE_AS_START_POINT = 100;
    private static final int MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_1 = 101;
    private static final int MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_2 = 102;
    private static final int MENU_ITEM_ROUTE_PLANNER_USE_AS_VIA_POINT_3 = 103;
    private static final int MENU_ITEM_ROUTE_PLANNER_USE_AS_DESTINATION_POINT = 104;

    private void showPointContextMenu(final View view, final Point point) {
        PopupMenu pointContextMenu = new PopupMenu(view.getContext(), view);
        MenuCompat.setGroupDividerEnabled(pointContextMenu.getMenu(), true);
        int orderId = 0;

        if (point != null) {

            // favorite
            if (point.isFavorite()) {
                pointContextMenu.getMenu().add(
                        MENU_GROUP_1, MENU_ITEM_REMOVE_FROM_FAVORITES, orderId++, GlobalInstance.getStringResource(R.string.menuItemRemoveFromFavorites));
            } else {
                pointContextMenu.getMenu().add(
                        MENU_GROUP_1, MENU_ITEM_ADD_TO_FAVORITES, orderId++, GlobalInstance.getStringResource(R.string.menuItemAddToFavorites));
            }

            // simulation
            if (positionManagerInstance.getSimulationEnabled()
                    && point.equals(positionManagerInstance.getCurrentLocation())) {
                pointContextMenu.getMenu().add(
                        MENU_GROUP_1, MENU_ITEM_END_SIMULATION, orderId++, GlobalInstance.getStringResource(R.string.menuItemEndSimulation));
            } else {
                pointContextMenu.getMenu().add(
                        MENU_GROUP_1, MENU_ITEM_START_SIMULATION, orderId++, GlobalInstance.getStringResource(R.string.menuItemStartSimulation));
            }

            // rename and remove
            pointContextMenu.getMenu().add(
                    MENU_GROUP_1, MENU_ITEM_RENAME, orderId++, GlobalInstance.getStringResource(R.string.menuItemRename));
            if (showMenuItemRemove) {
                pointContextMenu.getMenu().add(
                        MENU_GROUP_1, MENU_ITEM_REMOVE, orderId++, GlobalInstance.getStringResource(R.string.menuItemRemove));
            }

            // details
            pointContextMenu.getMenu().add(
                    MENU_GROUP_2, MENU_ITEM_DETAILS, orderId++, GlobalInstance.getStringResource(R.string.menuItemDetails));

            SubMenu routePlannerSubMenu = pointContextMenu.getMenu().addSubMenu(
                    MENU_GROUP_2, Menu.NONE, orderId++, GlobalInstance.getStringResource(R.string.menuItemRoutePlanner));
            routePlannerSubMenu.add(
                    Menu.NONE, MENU_ITEM_ROUTE_PLANNER_USE_AS_START_POINT, 1, GlobalInstance.getStringResource(R.string.menuItemUseAsStartPoint));
            routePlannerSubMenu.add(
                    Menu.NONE, MENU_ITEM_ROUTE_PLANNER_USE_AS_DESTINATION_POINT, 2, GlobalInstance.getStringResource(R.string.menuItemUseAsDestinationPoint));

            // osm id
            if (point.getOsmId() != null) {
                pointContextMenu.getMenu().add(
                        MENU_GROUP_2, MENU_ITEM_OPEN_ON_OSM_ORG, orderId++, GlobalInstance.getStringResource(R.string.menuItemOpenStreetMap));
            }
        }

        pointContextMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {
                Timber.d("onMenuItemClick: %1$d", item.getItemId());
                switch (item.getItemId()) {

                    case MENU_ITEM_ADD_TO_FAVORITES:
                        accessDatabaseInstance.addObjectToDatabaseProfile(
                                point, DatabasePointProfile.FAVORITES);
                        updateUI();
                        break;
                    case MENU_ITEM_REMOVE_FROM_FAVORITES:
                        accessDatabaseInstance.removeObjectFromDatabaseProfile(
                                point, DatabasePointProfile.FAVORITES);
                        updateUI();
                        break;

                    case MENU_ITEM_START_SIMULATION:
                        positionManagerInstance.setSimulatedLocation(point);
                        positionManagerInstance.setSimulationEnabled(true);
                        updateUI();
                        break;
                    case MENU_ITEM_END_SIMULATION:
                        positionManagerInstance.setSimulationEnabled(false);
                        updateUI();
                        break;

                    case MENU_ITEM_RENAME:
                        RenameObjectDialog roDialog = RenameObjectDialog.newInstance(point);
                        roDialog.setRenameObjectListener(TextViewAndActionButton.this);
                        if (getContext() instanceof AppCompatActivity) {
                            roDialog.show(
                                    ((AppCompatActivity)getContext()).getSupportFragmentManager(), "RenameObjectDialog");
                        }
                        break;
                    case MENU_ITEM_REMOVE:
                        if (onMenuItemRemoveClickListener != null) {
                            onMenuItemRemoveClickListener.onMenuItemRemoveClick(TextViewAndActionButton.this);
                        }
                        break;

                    case MENU_ITEM_DETAILS:
                        PointDetailsActivity.start(view.getContext(), point);
                        break;
                    case MENU_ITEM_OPEN_ON_OSM_ORG:
                        Intent openBrowserIntent = new Intent(Intent.ACTION_VIEW);
                        openBrowserIntent.setData(
                                Uri.parse(
                                    String.format(Locale.ROOT, OSM_NODE_URL, point.getOsmId())));
                        view.getContext().startActivity(openBrowserIntent);
                        break;

                    default:
                        return false;
                }
                return true;
            }
        });

        pointContextMenu.show();
    }

                    /*
                    PopupMenu popupAddToRoute = new PopupMenu(PointDetailsActivity.this, view);
                    popupAddToRoute.setOnMenuItemClickListener(PointDetailsActivity.this);
                    // start point
                    popupAddToRoute.getMenu().add(
                            Menu.NONE,
                            Constants.POINT_PUT_INTO.START,
                            1,
                            getResources().getString(R.string.menuItemAsRouteStartPoint));
                    // via points
                    ArrayList<PointWrapper> viaPointList = SettingsManager.getInstance(PointDetailsActivity.this).getRouteSettings().getViaPointList();
                    for (int viaPointIndex=0; viaPointIndex<viaPointList.size(); viaPointIndex++) {;
                        popupAddToRoute.getMenu().add(
                                Menu.NONE,
                                viaPointIndex+Constants.POINT_PUT_INTO.VIA,
                                viaPointIndex+2,
                                String.format(
                                    getResources().getString(R.string.menuItemAsRouteViaPoint),
                                    viaPointIndex+1));
                    }
                    // destination point
                    popupAddToRoute.getMenu().add(
                            Menu.NONE,
                            Constants.POINT_PUT_INTO.DESTINATION,
                            viaPointList.size()+2,
                            getResources().getString(R.string.menuItemAsRouteDestinationPoint));
                    popupAddToRoute.show();
                    */


    // route context menu

    private void showRouteContextMenu(final View view, final Route route) {
        PopupMenu routeContextMenu = new PopupMenu(view.getContext(), view);
        MenuCompat.setGroupDividerEnabled(routeContextMenu.getMenu(), true);
        int orderId = 0;

        if (route != null) {
            // favorite
            if (route.isFavorite()) {
                routeContextMenu.getMenu().add(
                        MENU_GROUP_1, MENU_ITEM_REMOVE_FROM_FAVORITES, orderId++, GlobalInstance.getStringResource(R.string.menuItemRemoveFromFavorites));
            } else {
                routeContextMenu.getMenu().add(
                        MENU_GROUP_1, MENU_ITEM_ADD_TO_FAVORITES, orderId++, GlobalInstance.getStringResource(R.string.menuItemAddToFavorites));
            }

            // remove
            if (showMenuItemRemove) {
                routeContextMenu.getMenu().add(
                        MENU_GROUP_1, MENU_ITEM_REMOVE, orderId++, GlobalInstance.getStringResource(R.string.menuItemRemove));
            }

            // details
            routeContextMenu.getMenu().add(
                    MENU_GROUP_2, MENU_ITEM_DETAILS, orderId++, GlobalInstance.getStringResource(R.string.menuItemDetails));
        }

        routeContextMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    case MENU_ITEM_ADD_TO_FAVORITES:
                        accessDatabaseInstance.addObjectToDatabaseProfile(
                                route, DatabaseRouteProfile.FAVORITES);
                        updateUI();
                        break;
                    case MENU_ITEM_REMOVE_FROM_FAVORITES:
                        accessDatabaseInstance.removeObjectFromDatabaseProfile(
                                route, DatabaseRouteProfile.FAVORITES);
                        updateUI();
                        break;

                    case MENU_ITEM_REMOVE:
                        if (onMenuItemRemoveClickListener != null) {
                            onMenuItemRemoveClickListener.onMenuItemRemoveClick(TextViewAndActionButton.this);
                        }
                        break;

                    case MENU_ITEM_DETAILS:
                        break;

                    default:
                        return false;
                }
                return true;
            }
        });
        routeContextMenu.show();
    }


    // segment context menu
    private static final String OSM_WAY_URL = "https://www.openstreetmap.org/way/%1$d/";

    private void showSegmentContextMenu(final View view, final Segment segment) {
        PopupMenu segmentContextMenu = new PopupMenu(view.getContext(), view);
        MenuCompat.setGroupDividerEnabled(segmentContextMenu.getMenu(), true);
        int orderId = 0;

        if (segment != null) {
            if (segment.isExcludedFromRouting()) {
                segmentContextMenu.getMenu().add(
                        MENU_GROUP_1, MENU_ITEM_REMOVE_EXCLUDE_FROM_ROUTING, orderId++, GlobalInstance.getStringResource(R.string.menuItemRemoveExcludeFromRouting));
            } else {
                segmentContextMenu.getMenu().add(
                        MENU_GROUP_1, MENU_ITEM_ADD_TO_EXCLUDED_FROM_ROUTING, orderId++, GlobalInstance.getStringResource(R.string.menuItemAddToExcludeFromRouting));
            }

            // simulation
            if (directionManagerInstance.getSimulationEnabled()
                    && directionManagerInstance.getCurrentDirection() != null
                    && directionManagerInstance.getCurrentDirection().getBearing() == segment.getBearing()) {
                segmentContextMenu.getMenu().add(
                        MENU_GROUP_1, MENU_ITEM_END_SIMULATION, orderId++, GlobalInstance.getStringResource(R.string.menuItemEndSimulation));
            } else {
                segmentContextMenu.getMenu().add(
                        MENU_GROUP_1, MENU_ITEM_START_SIMULATION, orderId++, GlobalInstance.getStringResource(R.string.menuItemStartSimulation));
            }

            // rename and remove
            segmentContextMenu.getMenu().add(
                    MENU_GROUP_1, MENU_ITEM_RENAME, orderId++, GlobalInstance.getStringResource(R.string.menuItemRename));
            if (showMenuItemRemove) {
                segmentContextMenu.getMenu().add(
                        MENU_GROUP_1, MENU_ITEM_REMOVE, orderId++, GlobalInstance.getStringResource(R.string.menuItemRemove));
            }

            // details
            segmentContextMenu.getMenu().add(
                    MENU_GROUP_2, MENU_ITEM_DETAILS, orderId++, GlobalInstance.getStringResource(R.string.menuItemDetails));
            // osm id
            if (segment.getOsmId() != null) {
                segmentContextMenu.getMenu().add(
                        MENU_GROUP_2, MENU_ITEM_OPEN_ON_OSM_ORG, orderId++, GlobalInstance.getStringResource(R.string.menuItemOpenStreetMap));
            }
        }

        segmentContextMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    case MENU_ITEM_ADD_TO_EXCLUDED_FROM_ROUTING:
                        accessDatabaseInstance.addObjectToDatabaseProfile(
                                segment, DatabaseSegmentProfile.EXCLUDED_FROM_ROUTING);
                        updateUI();
                        break;
                    case MENU_ITEM_REMOVE_EXCLUDE_FROM_ROUTING:
                        accessDatabaseInstance.removeObjectFromDatabaseProfile(
                                segment, DatabaseSegmentProfile.EXCLUDED_FROM_ROUTING);
                        updateUI();
                        break;

                    case MENU_ITEM_START_SIMULATION:
                        directionManagerInstance.setSimulatedDirection(
                                new Direction.Builder(
                                    GlobalInstance.getContext(), segment.getBearing())
                                .build());
                        directionManagerInstance.setSimulationEnabled(true);
                        updateUI();
                        break;
                    case MENU_ITEM_END_SIMULATION:
                        directionManagerInstance.setSimulationEnabled(false);
                        updateUI();
                        break;

                    case MENU_ITEM_RENAME:
                        RenameObjectDialog roDialog = RenameObjectDialog.newInstance(segment);
                        roDialog.setRenameObjectListener(TextViewAndActionButton.this);
                        if (getContext() instanceof AppCompatActivity) {
                            roDialog.show(
                                    ((AppCompatActivity)getContext()).getSupportFragmentManager(), "RenameObjectDialog");
                        }
                        break;
                    case MENU_ITEM_REMOVE:
                        if (onMenuItemRemoveClickListener != null) {
                            onMenuItemRemoveClickListener.onMenuItemRemoveClick(TextViewAndActionButton.this);
                        }
                        break;

                    case MENU_ITEM_DETAILS:
                        SegmentDetailsActivity.start(view.getContext(), segment);
                        break;
                    case MENU_ITEM_OPEN_ON_OSM_ORG:
                        Intent openBrowserIntent = new Intent(Intent.ACTION_VIEW);
                        openBrowserIntent.setData(
                                Uri.parse(
                                    String.format(Locale.ROOT, OSM_WAY_URL, segment.getOsmId())));
                        view.getContext().startActivity(openBrowserIntent);
                        break;

                    default:
                        return false;
                }
                return true;
            }
        });
        segmentContextMenu.show();
    }

}
