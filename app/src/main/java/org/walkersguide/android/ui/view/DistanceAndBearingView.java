package org.walkersguide.android.ui.view;

import org.walkersguide.android.util.Helper;
import android.os.Handler;
import android.os.Looper;
import org.walkersguide.android.sensor.bearing.AcceptNewBearing;
import org.walkersguide.android.sensor.position.AcceptNewPosition;
import android.view.accessibility.AccessibilityEvent;
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
import org.walkersguide.android.tts.TTSWrapper.MessageType;
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
import android.view.accessibility.AccessibilityNodeInfo;
import org.walkersguide.android.data.angle.RelativeBearing;
import org.walkersguide.android.data.angle.bearing.BearingSensorValue;
import androidx.appcompat.widget.AppCompatTextView;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import androidx.core.content.ContextCompat;


public class DistanceAndBearingView extends AppCompatTextView {

    private DeviceSensorManager deviceSensorManagerInstance;
    private PositionManager positionManagerInstance;
    private SettingsManager settingsManagerInstance;
    private TTSWrapper ttsWrapperInstance;

    private String prefix = null;
    private ObjectWithId objectWithId = null;
    private Bitmap bearingIndicator = null;

    public DistanceAndBearingView(Context context) {
        super(context);
        this.initUi(context);
    }

    public DistanceAndBearingView(Context context, String prefix) {
        super(context);
        this.prefix = prefix;
        this.initUi(context);
    }

    public DistanceAndBearingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // parse xml layout attributes
        TypedArray attributeArray = context.obtainStyledAttributes(
                attrs, R.styleable.ObjectWithIdAndProfileView);
        if (attributeArray != null) {
            this.prefix = attributeArray.getString(
                    R.styleable.ObjectWithIdAndProfileView_prefix);
            attributeArray.recycle();
        }

        this.initUi(context);
    }

    public void setObjectWithId(ObjectWithId object) {
        this.objectWithId = object;
        updateDistanceAndBearingLabelText();
    }

    private void initUi(Context context) {
        deviceSensorManagerInstance = DeviceSensorManager.getInstance();
        positionManagerInstance = PositionManager.getInstance();
        settingsManagerInstance = SettingsManager.getInstance();
        ttsWrapperInstance = TTSWrapper.getInstance();

        setAccessibilityDelegate(
                UiHelper.getAccessibilityDelegateToMuteContentChangedEventsWhileFocussed());
        bearingIndicator = BitmapFactory.decodeResource(
                context.getResources(), R.drawable.bearing_indicator);
        setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
    }


    /**
     * broadcasts
     */

    @Override public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Timber.d("onDetachedFromWindow: %1$s", objectWithId);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext())
            .unregisterReceiver(newLocationAndDirectionReceiver);
    }

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Timber.d("onAttachedToWindow: %1$s", objectWithId);

        IntentFilter filter = new IntentFilter();
        filter.addAction(PositionManager.ACTION_NEW_LOCATION);
        filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING);
        filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING_VALUE_FROM_SATELLITE);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext())
            .registerReceiver(newLocationAndDirectionReceiver, filter);

        // request current location to update the ui
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() {
                positionManagerInstance.requestCurrentLocation();
            }
        }, 200);
    }


    private BroadcastReceiver newLocationAndDirectionReceiver = new BroadcastReceiver() {
        private AcceptNewPosition acceptNewPosition = AcceptNewPosition.newInstanceForDistanceLabelUpdate();
        private AcceptNewPosition acceptNewPositionTts = AcceptNewPosition.newInstanceForTtsAnnouncement();
        private AcceptNewPosition acceptNewPositionTtsFocus = AcceptNewPosition.newInstanceForTtsAnnouncementOnFocus();

        private AcceptNewBearing acceptNewBearing = AcceptNewBearing.newInstanceForBearingLabelUpdate();
        private AcceptNewBearing acceptNewBearingTts = AcceptNewBearing.newInstanceForTtsAnnouncement();
        private RelativeBearing.Direction lastDirection = null;

        @Override public void onReceive(Context context, Intent intent) {
            if (objectWithId == null) return;

            if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)) {
                Point currentLocation = (Point) intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION);

                if (acceptNewPosition.updatePoint(
                            currentLocation, false, intent.getBooleanExtra(PositionManager.EXTRA_IS_IMPORTANT, false))) {
                    updateDistanceAndBearingLabelText();
                    updateBearingIndicator(context);
                }

                boolean announce = false;
                // no if/else, it's important, that both updatePoint functions are called to keep them up to date
                if (acceptNewPositionTts.updatePoint(currentLocation, false, false)) {
                    announce = true;
                }
                if (acceptNewPositionTtsFocus.updatePoint(currentLocation, false, false)
                        && isAccessibilityFocused()) {
                    announce = true;
                }
                if (announce) {
                    ttsWrapperInstance.announce(
                            objectWithId.formatDistanceAndRelativeBearingFromCurrentLocation(
                                R.plurals.meter, settingsManagerInstance.getShowPreciseBearingValues()),
                            MessageType.DISTANCE_OR_BEARING);
                }

            } else if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING)
                    || intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING_VALUE_FROM_SATELLITE)) {
                boolean announce = false;

                if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING)
                        && acceptNewBearing.updateBearing(
                            (Bearing) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING),
                            false, intent.getBooleanExtra(DeviceSensorManager.EXTRA_IS_IMPORTANT, false))) {
                    announce = isAccessibilityFocused();
                    updateDistanceAndBearingLabelText();
                    updateBearingIndicator(context);
                }
                if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING_VALUE_FROM_SATELLITE)
                        && acceptNewBearingTts.updateBearing(
                            (BearingSensorValue) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING),
                            false, intent.getBooleanExtra(DeviceSensorManager.EXTRA_IS_IMPORTANT, false))) {
                    announce = true;
                }

                RelativeBearing.Direction currentDirection = objectWithId
                    .relativeBearingFromCurrentLocation()
                    .getDirection();
                if (announce && currentDirection != lastDirection) {
                    ttsWrapperInstance.announce(
                            objectWithId.formatRelativeBearingFromCurrentLocation(
                                settingsManagerInstance.getShowPreciseBearingValues()),
                            MessageType.DISTANCE_OR_BEARING);
                    lastDirection = currentDirection;
                }
            }
        }
    };


    private void updateDistanceAndBearingLabelText() {
        String text = "";
        if (prefix != null) {
            text += String.format("%1$s: ", prefix);
        }
        if (objectWithId != null) {
            text += objectWithId.formatDistanceAndRelativeBearingFromCurrentLocation(
                    R.plurals.meter, settingsManagerInstance.getShowPreciseBearingValues());
        }
        setText(text);
    }

    private void updateBearingIndicator(Context context) {
        if (bearingIndicator == null
                || prefix != null
                || ! settingsManagerInstance.getShowBearingIndicator()) {
            return;
        }

        RelativeBearing relativeBearing = objectWithId.relativeBearingFromCurrentLocation();
        if (relativeBearing == null) {
            setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            return;
        }

        // rotate
        Bitmap rotatedBearingIndicator = UiHelper.rotateImage(
                bearingIndicator, relativeBearing.getDegree());
        Drawable drawableRotatedBearingIndicator = new BitmapDrawable(
                context.getResources(), rotatedBearingIndicator);

        // show it on the left side of the text view
        setCompoundDrawablesWithIntrinsicBounds(
                drawableRotatedBearingIndicator, null, null, null);
        invalidate();
    }

}
