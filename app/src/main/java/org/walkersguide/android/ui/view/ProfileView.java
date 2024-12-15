        package org.walkersguide.android.ui.view;

import org.walkersguide.android.ui.dialog.gpx.export.ExportDatabaseProfileToGpxFileDialog;
import org.walkersguide.android.ui.interfaces.ViewChangedListener;
import org.walkersguide.android.ui.dialog.template.SelectMultipleObjectsFromListDialog;
import org.walkersguide.android.ui.dialog.select.SelectCollectionsDialog;
import org.walkersguide.android.ui.dialog.template.EnterStringDialog;
import org.walkersguide.android.data.profile.MutableProfile;
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
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.data.Profile;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import androidx.fragment.app.Fragment;
import org.walkersguide.android.ui.fragment.object_list.extended.ObjectListFromDatabaseFragment;
import org.walkersguide.android.ui.fragment.object_list.extended.PoiListFromServerFragment;
import androidx.fragment.app.DialogFragment;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentResultListener;
import android.os.Bundle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.Lifecycle;
import androidx.fragment.app.FragmentManager;
import android.app.Dialog;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import org.walkersguide.android.shortcut.PinnedShortcutUtility;
import java.util.ArrayList;
import org.walkersguide.android.server.wg.poi.PoiCategory;
import org.walkersguide.android.database.profile.Collection;
import org.walkersguide.android.ui.dialog.select.SelectPoiCategoriesDialog;
import org.walkersguide.android.database.util.AccessDatabase;


public class ProfileView extends LinearLayout {

    private MainActivityController mainActivityController;
    private SettingsManager settingsManagerInstance;

    private String prefix = null;
    private boolean compact = false;

    private Profile profile;
    private boolean showProfileIcon, showContextMenuItemRemove;

    private ImageView imageViewProfileIcon;
    private TextView label;
    private ImageButton buttonActionFor;

    public ProfileView(Context context) {
        super(context);
        this.initUi(context);
    }

    public ProfileView(Context context, String prefix, boolean compact) {
        super(context);
        this.prefix = prefix;
        this.compact = compact;
        this.initUi(context);
    }

    public ProfileView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // parse xml layout attributes
        TypedArray sharedAttributeArray = context.obtainStyledAttributes(
                attrs, R.styleable.ObjectWithIdAndProfileView);
        if (sharedAttributeArray != null) {
            this.prefix = sharedAttributeArray.getString(
                    R.styleable.ObjectWithIdAndProfileView_prefix);
            this.compact = sharedAttributeArray.getBoolean(
                    R.styleable.ObjectWithIdAndProfileView_compact, false);
            sharedAttributeArray.recycle();
        }

        this.initUi(context);
    }

    private void initUi(Context context) {
        mainActivityController = context instanceof MainActivity ? (MainActivityController) context : null;
        settingsManagerInstance = SettingsManager.getInstance();

        // configure enclosing linear layout
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        View rootView = inflate(context, R.layout.layout_text_view_and_action_button, this);
        imageViewProfileIcon = (ImageView) rootView.findViewById(R.id.imageViewIcon);
        imageViewProfileIcon.setVisibility(View.GONE);

        label = (TextView) rootView.findViewById(R.id.label);
        if (compact) {
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setSingleLine();
        }
        label.setContentDescription(null);
        label.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (onProfileDefaultActionListener != null) {
                    onProfileDefaultActionListener.onProfileDefaultActionClicked(profile);
                } else if (profile != null) {
                    executeProfileMenuAction(
                            view.getContext(), profile, MENU_ITEM_DETAILS);
                }
            }
        });
        label.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View view) {
                if (profile != null) {
                    showContextMenu(view, profile);
                }
                return true;
            }
        });

        buttonActionFor = (ImageButton) rootView.findViewById(R.id.buttonActionFor);
        buttonActionFor.setContentDescription(null);
        buttonActionFor.setVisibility(View.GONE);
        buttonActionFor.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (profile != null) {
                    showContextMenu(view, profile);
                }
            }
        });

        this.reset();
    }

    public Profile getProfile() {
        return this.profile;
    }

    private void reset() {
        this.profile = null;
        this.showProfileIcon = false;
        this.showContextMenuItemRemove = false;
        updateLabelAndButtonText();
    }


    // default action listener
    private OnProfileDefaultActionListener onProfileDefaultActionListener = null;

    public interface OnProfileDefaultActionListener {
        public void onProfileDefaultActionClicked(Profile profile);
    }

    public void setOnProfileDefaultActionListener(OnProfileDefaultActionListener listener) {
        this.onProfileDefaultActionListener = listener;
    }


    // convigure view

    public void configureAsSingleObject(Profile profile) {
        configure(profile, false, false);
        ViewCompat.setAccessibilityDelegate(
                this.label, UiHelper.getAccessibilityDelegateViewClassButton());
    }

    public void configureAsListItem(Profile profile, boolean showProfileIcon, boolean showContextMenuItemRemove) {
        configure(profile, showProfileIcon, showContextMenuItemRemove);
    }

    private void configure(Profile profile, boolean showProfileIcon, boolean showContextMenuItemRemove) {
        this.reset();
        if (mainActivityController != null && profile != null) {
            this.profile = profile;
            this.showProfileIcon = showProfileIcon;
            this.showContextMenuItemRemove = showContextMenuItemRemove;
            updateLabelAndButtonText();
        }
    }

    private void updateLabelAndButtonText() {
        String labelText = null;
        if (this.profile != null) {
            labelText = this.compact ? this.profile.getName() : this.profile.toString();
        } else {
            labelText = GlobalInstance.getStringResource(R.string.labelNothingSelected);
        }

        // prepare complete label text
        if (this.prefix != null) {
            labelText = String.format(
                    "%1$s: %2$s", this.prefix, labelText);
        }
        this.label.setText(labelText);

        if (this.profile != null) {

            // profile icon and label content description
            if (this.showProfileIcon) {
                this.imageViewProfileIcon.setImageResource(this.profile.getIcon().resId);
                this.imageViewProfileIcon.setVisibility(View.VISIBLE);
                this.label.setContentDescription(
                        String.format(
                            "%1$s: %2$s",
                            this.profile.getIcon().name,
                            labelText));
            }

            // action button
            if (settingsManagerInstance.getShowActionButton()) {
                this.buttonActionFor.setContentDescription(
                        String.format(
                            "%1$s %2$s",
                            getResources().getString(R.string.buttonActionFor),
                            this.profile.getName())
                        );
                this.buttonActionFor.setVisibility(View.VISIBLE);
            }
        }
    }


    /**
     * broadcasts
     */

    @Override public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).unregisterReceiver(profileViewReceiver);
    }

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UpdatePoiProfileSelectedCollectionsDialog.ACTION_UPDATE_COLLECTIONS_WAS_SUCCESSFUL);
        filter.addAction(UpdatePoiProfileSelectedPoiCategoriesDialog.ACTION_UPDATE_POI_CATEGORIES_WAS_SUCCESSFUL);
        filter.addAction(RenameProfileDialog.ACTION_RENAME_PROFILE_WAS_SUCCESSFUL);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).registerReceiver(profileViewReceiver, filter);

        // check, if profile was removed in the background
        if (this.profile instanceof MutableProfile
                && ((MutableProfile) this.profile).profileWasRemoved()) {
            reset();
        }
    }

    private BroadcastReceiver profileViewReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UpdatePoiProfileSelectedCollectionsDialog.ACTION_UPDATE_COLLECTIONS_WAS_SUCCESSFUL)) {
                updateLabelAndButtonText();
            } else if (intent.getAction().equals(UpdatePoiProfileSelectedPoiCategoriesDialog.ACTION_UPDATE_POI_CATEGORIES_WAS_SUCCESSFUL)) {
                updateLabelAndButtonText();
            } else if (intent.getAction().equals(RenameProfileDialog.ACTION_RENAME_PROFILE_WAS_SUCCESSFUL)) {
                updateLabelAndButtonText();
            }
        }
    };


    /**
     * context menu
     */

    private static final int MENU_ITEM_DETAILS = 1;
    private static final int MENU_ITEM_OVERVIEW_PIN = 2;
    private static final int MENU_ITEM_OVERVIEW_TRACK = 3;
    private static final int MENU_ITEM_POI_CATEGORIES = 4;
    private static final int MENU_ITEM_COLLECTIONS = 5;
    private static final int MENU_ITEM_PIN_SHORTCUT = 6;
    private static final int MENU_ITEM_EXPORT_TO_GPX_FILE = 7;
    private static final int MENU_ITEM_RENAME = 8;
    private static final int MENU_ITEM_REMOVE = 9;


    public void showContextMenu(final View view, final Profile profile) {
        PopupMenu contextMenu = new PopupMenu(view.getContext(), view);
        int orderId = 0;

        if (onProfileDefaultActionListener != null) {
            contextMenu.getMenu().add(
                    Menu.NONE, MENU_ITEM_DETAILS, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemDetails));
        }

        if (profile instanceof MutableProfile) {
            MutableProfile mutableProfile = (MutableProfile) profile;

            // pin
            MenuItem menuItemOverviewPin = contextMenu.getMenu().add(
                    Menu.NONE, MENU_ITEM_OVERVIEW_PIN, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemOverviewPin));
            menuItemOverviewPin.setCheckable(true);
            menuItemOverviewPin.setChecked(mutableProfile.isPinned());
            // track
            MenuItem menuItemOverviewTrack = contextMenu.getMenu().add(
                    Menu.NONE, MENU_ITEM_OVERVIEW_TRACK, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemOverviewTrack));
            menuItemOverviewTrack.setCheckable(true);
            menuItemOverviewTrack.setChecked(mutableProfile.isTracked());

            // poi profile specific: select poi categories and collections
            if (profile instanceof PoiProfile) {
                contextMenu.getMenu().add(
                        Menu.NONE, MENU_ITEM_POI_CATEGORIES, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemProfilePoiCategories));
                contextMenu.getMenu().add(
                        Menu.NONE, MENU_ITEM_COLLECTIONS, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemProfileCollections));
            }

            // poi profile specific: add to home screen
            if (profile instanceof PoiProfile
                    && PinnedShortcutUtility.isPinShortcutsSupported()) {
                contextMenu.getMenu().add(
                        Menu.NONE, MENU_ITEM_PIN_SHORTCUT, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemProfilePinShortcut));
            }

            // gpx export
            if (profile instanceof DatabaseProfile) {
                contextMenu.getMenu().add(
                        Menu.NONE, MENU_ITEM_EXPORT_TO_GPX_FILE, orderId++,
                        GlobalInstance.getStringResource(R.string.exportGpxFileDialogTitle));
            }

            // rename and remove profile

            contextMenu.getMenu().add(
                    Menu.NONE, MENU_ITEM_RENAME, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemRename));

            if (this.showContextMenuItemRemove) {
                contextMenu.getMenu().add(
                        Menu.NONE, MENU_ITEM_REMOVE, orderId++, GlobalInstance.getStringResource(R.string.contextMenuItemRemove));
            }
        }

        contextMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {
                Timber.d("onMenuItemClick: %1$d", item.getItemId());
                if (executeProfileMenuAction(view.getContext(), profile, item.getItemId())) {
                    return true;
                } else {
                    return false;
                }
            }
        });

        contextMenu.show();
    }

    private boolean executeProfileMenuAction(Context context, final Profile selectedProfile, final int menuItemId) {
        if (menuItemId == MENU_ITEM_DETAILS) {
            DialogFragment profileDetailsFragment = null;
            if (selectedProfile instanceof DatabaseProfile) {
                profileDetailsFragment = ObjectListFromDatabaseFragment.newInstance((DatabaseProfile) selectedProfile);
            } else if (selectedProfile instanceof PoiProfile) {
                settingsManagerInstance.setSelectedPoiProfile((PoiProfile) selectedProfile);
                profileDetailsFragment = PoiListFromServerFragment.newInstance((PoiProfile) selectedProfile);
            }
            if (profileDetailsFragment != null) {
                mainActivityController.embeddFragmentIfPossibleElseOpenAsDialog(profileDetailsFragment);
            }

        } else if (menuItemId == MENU_ITEM_OVERVIEW_PIN
                || menuItemId == MENU_ITEM_OVERVIEW_TRACK) {
            MutableProfile mutableProfile = (MutableProfile) selectedProfile;
            if (menuItemId == MENU_ITEM_OVERVIEW_PIN) {
                mutableProfile.setPinned(! mutableProfile.isPinned());
            } else if (menuItemId == MENU_ITEM_OVERVIEW_TRACK) {
                mutableProfile.setTracked(! mutableProfile.isTracked());
            }
            // update parent view
            ViewChangedListener.sendProfileListChangedBroadcast();

        } else if (menuItemId == MENU_ITEM_POI_CATEGORIES) {
            mainActivityController.openDialog(
                    UpdatePoiProfileSelectedPoiCategoriesDialog.newInstance((PoiProfile) selectedProfile));

        } else if (menuItemId == MENU_ITEM_COLLECTIONS) {
            mainActivityController.openDialog(
                    UpdatePoiProfileSelectedCollectionsDialog.newInstance((PoiProfile) selectedProfile));

        } else if (menuItemId == MENU_ITEM_PIN_SHORTCUT) {
            mainActivityController.openDialog(
                    AddPoiProfileToHomeScreenDialog.newInstance((PoiProfile) selectedProfile));

        } else if (menuItemId == MENU_ITEM_EXPORT_TO_GPX_FILE) {
            mainActivityController.openDialog(
                    ExportDatabaseProfileToGpxFileDialog.newInstance((DatabaseProfile) selectedProfile));

        } else if (menuItemId == MENU_ITEM_RENAME) {
            mainActivityController.openDialog(
                    RenameProfileDialog.newInstance((MutableProfile) selectedProfile));

        } else if (menuItemId == MENU_ITEM_REMOVE) {
            new AlertDialog.Builder(context)
                .setMessage(
                        String.format(
                            getResources().getString(R.string.removeProfileDialogTitle),
                            selectedProfile.getName())
                        )
                .setPositiveButton(
                        getResources().getString(R.string.dialogYes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (((MutableProfile) selectedProfile).remove()) {
                                    reset();
                                    ViewChangedListener.sendProfileListChangedBroadcast();
                                    dialog.dismiss();
                                }
                            }
                        })
                .setNegativeButton(
                        getResources().getString(R.string.dialogNo),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                .create()
                .show();

        } else {
            return false;
        }
        return true;
    }


    /**
     * dialogs
     */

    public static class RenameProfileDialog extends EnterStringDialog {
        public static final String ACTION_RENAME_PROFILE_WAS_SUCCESSFUL = "action.renameProfileWasSuccessful";

        public static RenameProfileDialog newInstance(MutableProfile profile) {
            RenameProfileDialog dialog = new RenameProfileDialog();
            Bundle args = new Bundle();
            args.putSerializable(KEY_PROFILE, profile);
            dialog.setArguments(args);
            return dialog;
        }

        // dialog
        private static final String KEY_PROFILE = "profile";

        private MutableProfile profile;

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            profile = (MutableProfile) getArguments().getSerializable(KEY_PROFILE);
            if (profile != null) {
                setInitialInput(
                        profile.getName());
                setDialogTitle(
                        getResources().getString(R.string.renameProfileDialogTitle));
                setMissingInputMessage(
                        getResources().getString(R.string.messageNameIsMissing));

                return super.onCreateDialog(savedInstanceState);
            }
            return null;
        }

        @Override public void execute(String input) {
            if (profile.rename(input)) {
                Intent intent = new Intent(ACTION_RENAME_PROFILE_WAS_SUCCESSFUL);
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


    public static class UpdatePoiProfileSelectedCollectionsDialog extends SelectCollectionsDialog {
        public static final String ACTION_UPDATE_COLLECTIONS_WAS_SUCCESSFUL = "action.updateCollectionsWasSuccessful";

        public static UpdatePoiProfileSelectedCollectionsDialog newInstance(PoiProfile poiProfile) {
            UpdatePoiProfileSelectedCollectionsDialog dialog= new UpdatePoiProfileSelectedCollectionsDialog();
            Bundle args = createInitialObjectListBundle(
                    AccessDatabase.getInstance().getCollectionList(), poiProfile.getCollectionList());
            args.putSerializable(KEY_POI_PROFILE, poiProfile);
            dialog.setArguments(args);
            return dialog;
        }

        // dialog
        private static final String KEY_POI_PROFILE = "poiProfile";

        private PoiProfile poiProfile;

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            poiProfile = (PoiProfile) getArguments().getSerializable(KEY_POI_PROFILE);
            if (poiProfile != null) {
                return super.onCreateDialog(savedInstanceState);
            }
            return null;
        }

        @Override public String getDialogTitle() {
            return poiProfile != null
                ? String.format(
                        "%1$s: %2$s", poiProfile.getName(), super.getDialogTitle())
                : super.getDialogTitle();
        }

        @Override public void execute(ArrayList<Collection> selectedCollectionList) {
            if (poiProfile.setCollectionList(selectedCollectionList)) {
                Intent intent = new Intent(ACTION_UPDATE_COLLECTIONS_WAS_SUCCESSFUL);
                LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
                dismiss();
            } else {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageUpdateCollectionsFailed),
                        Toast.LENGTH_LONG).show();
            }
        }
    }


    public static class UpdatePoiProfileSelectedPoiCategoriesDialog extends SelectPoiCategoriesDialog {
        public static final String ACTION_UPDATE_POI_CATEGORIES_WAS_SUCCESSFUL = "action.updatePoiCategoriesWasSuccessful";

        public static UpdatePoiProfileSelectedPoiCategoriesDialog newInstance(PoiProfile poiProfile) {
            UpdatePoiProfileSelectedPoiCategoriesDialog dialog= new UpdatePoiProfileSelectedPoiCategoriesDialog();
            Bundle args = createInitialObjectListBundle(
                    null, poiProfile.getPoiCategoryList());
            args.putSerializable(KEY_POI_PROFILE, poiProfile);
            dialog.setArguments(args);
            return dialog;
        }

        // dialog
        private static final String KEY_POI_PROFILE = "poiProfile";

        private PoiProfile poiProfile;

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            poiProfile = (PoiProfile) getArguments().getSerializable(KEY_POI_PROFILE);
            if (poiProfile != null) {
                return super.onCreateDialog(savedInstanceState);
            }
            return null;
        }

        @Override public String getDialogTitle() {
            return poiProfile != null
                ? String.format(
                        "%1$s: %2$s", poiProfile.getName(), super.getDialogTitle())
                : super.getDialogTitle();
        }

        @Override public void execute(ArrayList<PoiCategory> selectedPoiCategoryList) {
            if (poiProfile.setPoiCategoryList(selectedPoiCategoryList)) {
                Intent intent = new Intent(ACTION_UPDATE_POI_CATEGORIES_WAS_SUCCESSFUL);
                LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
                dismiss();
            } else {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageUpdatePoiCategoriesFailed),
                        Toast.LENGTH_LONG).show();
            }
        }
    }


    public static class AddPoiProfileToHomeScreenDialog extends EnterStringDialog {

        public static AddPoiProfileToHomeScreenDialog newInstance(PoiProfile poiProfile) {
            AddPoiProfileToHomeScreenDialog dialog = new AddPoiProfileToHomeScreenDialog();
            Bundle args = new Bundle();
            args.putSerializable(KEY_POI_PROFILE, poiProfile);
            dialog.setArguments(args);
            return dialog;
        }

        // dialog
        private static final String KEY_POI_PROFILE = "poiProfile";

        private PoiProfile poiProfile;

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            poiProfile = (PoiProfile) getArguments().getSerializable(KEY_POI_PROFILE);
            if (poiProfile != null) {
                setInitialInput(
                        poiProfile.getName());
                setDialogTitle(
                        getResources().getString(R.string.addPoiProfileToHomeScreenDialogTitle));
                setMissingInputMessage(
                        getResources().getString(R.string.messageShortcutNameMissing));

                return super.onCreateDialog(savedInstanceState);
            }
            return null;
        }

        @Override public void execute(String input) {
            PinnedShortcutUtility.addPinnedShortcutForPoiProfile(poiProfile.getId(), input);
            dismiss();
        }
    }

}
