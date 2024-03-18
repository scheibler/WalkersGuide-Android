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
import android.widget.ImageView;
import java.util.Locale;


public class RouteObjectView extends LinearLayout {


    private RouteObject routeObject;

    private ImageView imageViewSelected;
    private ObjectWithIdView layoutRouteSegment;
    private ObjectWithIdView layoutRoutePoint;

    public RouteObjectView(Context context) {
        super(context);
        init(context);
    }

    public RouteObjectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        // configure enclosing linear layout
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        View view = inflate(context, R.layout.layout_route_object_view, this);
        imageViewSelected = (ImageView) view.findViewById(R.id.imageViewSelected);
        layoutRouteSegment = (ObjectWithIdView) view.findViewById(R.id.layoutRouteSegment);
        layoutRoutePoint = (ObjectWithIdView) view.findViewById(R.id.layoutRoutePoint);

        reset();
    }

    public RouteObject getRouteObject() {
        return this.routeObject;
    }

    private void reset() {
        this.routeObject = null;
        this.imageViewSelected.setVisibility(View.GONE);
        this.layoutRouteSegment.reset();
        this.layoutRouteSegment.setVisibility(View.GONE);
        this.layoutRoutePoint.reset();
    }

    public void configureAsListItem(RouteObject object, int instructionNumber, boolean isSelected) {
        this.reset();
        if (object != null) {
            this.routeObject = object;
            this.imageViewSelected.setVisibility(
                    isSelected ? View.VISIBLE : View.INVISIBLE);
            this.configureRouteObjectView(instructionNumber, isSelected);
        }
    }

    public void configureAsSingleObject(RouteObject object) {
        this.reset();
        if (object != null) {
            this.routeObject = object;
            this.configureRouteObjectView(-1, false);
        }
    }

    private void configureRouteObjectView(int instructionNumber, boolean isSelected) {
        // route segment
        if (! this.routeObject.getIsFirstRouteObject()) {
            if (isSelected) {
                this.layoutRouteSegment.setPrefix(
                        GlobalInstance.getStringResource(R.string.labelIndexSelected));
            }
            this.layoutRouteSegment.configureAsSingleObject(
                    this.routeObject.getSegment(), this.routeObject.formatSegmentInstruction());
            this.layoutRouteSegment.setVisibility(View.VISIBLE);
        }

        // route point
        if (instructionNumber > -1) {
            this.layoutRoutePoint.setPrefix(
                    String.format(Locale.getDefault(), "%1$d.", instructionNumber));
        }

        String routePointInstruction = this.routeObject.formatPointInstruction();
        if (this.routeObject.getPoint() instanceof Intersection
                && ((Intersection) this.routeObject.getPoint()).hasPedestrianCrossings()) {
            routePointInstruction += System.lineSeparator();
            routePointInstruction += ((Intersection) this.routeObject.getPoint()).formatNumberOfCrossingsNearby();
        }

        this.layoutRoutePoint.configureAsSingleObject(
                this.routeObject.getPoint(), routePointInstruction);
    }

}
