        package org.walkersguide.android.ui.view;

import androidx.core.view.ViewCompat;
import org.walkersguide.android.ui.dialog.edit.UserAnnotationForObjectWithIdDialog;
import timber.log.Timber;
import org.walkersguide.android.R;
import org.walkersguide.android.ui.view.builder.TextViewBuilder;
import android.widget.LinearLayout;
import android.content.Context;
import android.util.AttributeSet;
import android.os.Bundle;
import android.content.IntentFilter;
import org.walkersguide.android.server.ServerTaskExecutor;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.address.ResolveCoordinatesTask;
import android.content.BroadcastReceiver;
import android.content.Intent;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.server.address.AddressException;
import org.walkersguide.android.sensor.position.AcceptNewPosition;
import org.walkersguide.android.data.ObjectWithId;
import android.widget.TextView;
import android.view.View;
import org.walkersguide.android.ui.activity.MainActivityController;
import org.walkersguide.android.ui.UiHelper;
import org.walkersguide.android.ui.activity.MainActivity;


public class UserAnnotationView extends LinearLayout {

    private MainActivityController mainActivityController;
    private ObjectWithId objectWithId;

    private TextView labelUserAnnotation;

    public UserAnnotationView(Context context) {
        super(context);
        this.initUi(context);
    }

    public UserAnnotationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.initUi(context);
    }

    private void initUi(Context context) {
        mainActivityController = context instanceof MainActivity ? (MainActivityController) context : null;

        setOrientation(LinearLayout.VERTICAL);
        setLayoutParams(
                new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        View rootView = inflate(context, R.layout.layout_user_annotation, this);
        labelUserAnnotation = (TextView) rootView.findViewById(R.id.labelUserAnnotation);
        labelUserAnnotation.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (mainActivityController != null && objectWithId != null) {
                    UserAnnotationForObjectWithIdDialog.newInstance(objectWithId)
                        .show(mainActivityController.getFragmentManagerInstance(), "UserAnnotationForObjectWithIdDialog");
                }
            }
        });
        ViewCompat.setAccessibilityDelegate(
                labelUserAnnotation, UiHelper.getAccessibilityDelegateViewClassButton());

        updateUserAnnotationLabel();
    }

    public void setObjectWithId(ObjectWithId newObject) {
        this.objectWithId = newObject;
        this.updateUserAnnotationLabel();
    }

    private void updateUserAnnotationLabel() {
        if (objectWithId != null && objectWithId.hasUserAnnotation()) {
            labelUserAnnotation.setText(objectWithId.getUserAnnotation());
            setVisibility(View.VISIBLE);
        } else {
            labelUserAnnotation.setText("");
            setVisibility(View.GONE);
        }
    }

    @Override public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).unregisterReceiver(localIntentReceiver);
    }

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(UserAnnotationForObjectWithIdDialog.ACTION_USER_ANNOTATION_FOR_OBJECT_WITH_ID_WAS_SUCCESSFUL);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).registerReceiver(localIntentReceiver, localIntentFilter);
    }

    private BroadcastReceiver localIntentReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UserAnnotationForObjectWithIdDialog.ACTION_USER_ANNOTATION_FOR_OBJECT_WITH_ID_WAS_SUCCESSFUL)) {
                updateUserAnnotationLabel();
            }
        }
    };

}
