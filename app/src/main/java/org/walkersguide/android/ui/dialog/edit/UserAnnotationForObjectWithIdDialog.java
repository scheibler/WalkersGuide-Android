package org.walkersguide.android.ui.dialog.edit;

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


public class UserAnnotationForObjectWithIdDialog extends EnterStringDialog {
    public static final String ACTION_USER_ANNOTATION_FOR_OBJECT_WITH_ID_WAS_SUCCESSFUL = "action.userAnnotationForObjectWithIdWasSuccessful";

    public static UserAnnotationForObjectWithIdDialog newInstance(ObjectWithId objectWithId) {
        UserAnnotationForObjectWithIdDialog dialog = new UserAnnotationForObjectWithIdDialog();
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
                    objectWithId.getUserAnnotation());
            setMultiLine(true);
            setDialogTitle(
                    String.format(
                        getResources().getString(R.string.userAnnotationForObjectWithIdDialogTitle),
                        objectWithId.getName())
                    );

            return super.onCreateDialog(savedInstanceState);
        }
        return null;
    }

    @Override public void execute(String input) {
        if (objectWithId.setUserAnnotation(input)) {
            Intent intent = new Intent(ACTION_USER_ANNOTATION_FOR_OBJECT_WITH_ID_WAS_SUCCESSFUL);
            LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
            dismiss();
        } else {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageSetUserAnnotationFailed),
                    Toast.LENGTH_LONG).show();
        }
    }

}
