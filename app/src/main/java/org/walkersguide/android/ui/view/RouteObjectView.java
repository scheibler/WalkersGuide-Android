        package org.walkersguide.android.ui.view;

import org.walkersguide.android.BuildConfig;
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
import org.walkersguide.android.ui.interfaces.TextChangedListener;
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

    private TextView labelSelectedRouteObject;
    private TextViewAndActionButton layoutRouteSegment;
    private TextViewAndActionButton layoutRoutePoint;

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
        labelSelectedRouteObject = (TextView) view.findViewById(R.id.labelSelectedRouteObject);
        layoutRouteSegment = (TextViewAndActionButton) view.findViewById(R.id.layoutRouteSegment);
        layoutRoutePoint = (TextViewAndActionButton) view.findViewById(R.id.layoutRoutePoint);

        reset();
    }

    public RouteObject getRouteObject() {
        return this.routeObject;
    }

    public void reset() {
        this.routeObject = null;
        this.labelSelectedRouteObject.setVisibility(View.GONE);
        this.layoutRouteSegment.reset();
        this.layoutRouteSegment.setVisibility(View.GONE);
        this.layoutRoutePoint.reset();
    }

    public void configureAsListItem(RouteObject object, boolean isSelected) {
        this.reset();
        if (object != null) {
            this.routeObject = object;
            if (isSelected) {
                this.labelSelectedRouteObject.setVisibility(View.VISIBLE);
            }
            this.configureRouteObjectView();
        }
    }

    public void configureAsSingleObject(RouteObject object) {
        this.reset();
        if (object != null) {
            this.routeObject = object;
            this.configureRouteObjectView();
        }
    }

    private void configureRouteObjectView() {
        if (! this.routeObject.getIsFirstRouteObject()) {
            this.layoutRouteSegment.configureAsSingleObject(
                    this.routeObject.getSegment(), this.routeObject.formatSegmentInstruction());
            this.layoutRouteSegment.setVisibility(View.VISIBLE);
        }
        this.layoutRoutePoint.configureAsSingleObject(
                this.routeObject.getPoint(),
                this.routeObject.formatPointInstruction());
    }

}
