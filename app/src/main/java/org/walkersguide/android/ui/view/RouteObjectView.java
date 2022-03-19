        package org.walkersguide.android.ui.view;

import org.walkersguide.android.ui.activity.toolbar.tabs.PointDetailsActivity;
    import org.walkersguide.android.data.object_with_id.route.RouteObject;
import timber.log.Timber;



import android.view.View;
import android.view.View.BaseSavedState;

import android.widget.TextView;

import android.widget.ImageButton;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;
import android.text.TextUtils;
import android.content.Context;
import android.widget.LinearLayout;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.EditText;
import org.walkersguide.android.ui.TextChangedListener;
import android.text.Editable;
import android.view.KeyEvent;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.Parcelable.ClassLoaderCreator;
import android.util.SparseArray;
import android.os.Parcel;
import android.os.Build;
import java.lang.ClassLoader;
import androidx.annotation.RequiresApi;
import android.content.res.TypedArray;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import org.walkersguide.android.data.object_with_id.Point;


public class RouteObjectView extends LinearLayout {


    private RouteObject routeObject;

    private TextViewAndActionButton layoutRouteSegment, layoutRouteSegmentSelected;
    private TextViewAndActionButton layoutRoutePoint;
    private TextView labelRoutePointOptionalDetails;

    public RouteObjectView(Context context) {
        super(context);
        init(context);
    }

    public RouteObjectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setOrientation(LinearLayout.VERTICAL);

        View view = inflate(context, R.layout.layout_route_object_view, this);
        layoutRouteSegment = (TextViewAndActionButton) view.findViewById(R.id.layoutRouteSegment);
        layoutRouteSegmentSelected = (TextViewAndActionButton) view.findViewById(R.id.layoutRouteSegmentSelected);
        layoutRoutePoint = (TextViewAndActionButton) view.findViewById(R.id.layoutRoutePoint);
        labelRoutePointOptionalDetails = (TextView) view.findViewById(R.id.labelRoutePointOptionalDetails);

        reset();
    }

    public RouteObject getRouteObject() {
        return this.routeObject;
    }

    public void reset() {
        this.routeObject = null;
        this.layoutRouteSegment.reset();
        this.layoutRouteSegment.setVisibility(View.GONE);
        this.layoutRouteSegmentSelected.reset();
        this.layoutRouteSegmentSelected.setVisibility(View.GONE);
        this.layoutRoutePoint.reset();
        this.labelRoutePointOptionalDetails.setVisibility(View.GONE);
    }

    public void configureAsListItem(RouteObject object, boolean isSelected) {
        this.reset();
        if (object != null) {
            this.routeObject = object;
            this.configureRouteObjectView(isSelected);
        }
    }

    public void configureAsSingleObject(RouteObject object) {
        this.reset();
        if (object != null) {
            this.routeObject = object;
            this.configureRouteObjectView(false);

            String optionalPointDetails = object.formatOptionalPointDetails();
            if (! TextUtils.isEmpty(optionalPointDetails)) {
                labelRoutePointOptionalDetails.setText(optionalPointDetails);
                labelRoutePointOptionalDetails.setVisibility(View.VISIBLE);
            }
        }
    }

    private void configureRouteObjectView(boolean isSelected) {
        if (! this.routeObject.getIsFirstRouteObject()) {
            if (isSelected) {
                this.layoutRouteSegmentSelected.configureAsSingleObject(
                        this.routeObject.getSegment(), this.routeObject.formatSegmentInstruction());
                this.layoutRouteSegmentSelected.setVisibility(View.VISIBLE);
            } else {
                this.layoutRouteSegment.configureAsSingleObject(
                        this.routeObject.getSegment(), this.routeObject.formatSegmentInstruction());
                this.layoutRouteSegment.setVisibility(View.VISIBLE);
            }
        }
        this.layoutRoutePoint.configureAsSingleObject(
                this.routeObject.getPoint(),
                this.routeObject.formatPointInstruction());
    }

}
