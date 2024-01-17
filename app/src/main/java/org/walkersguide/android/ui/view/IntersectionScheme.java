package org.walkersguide.android.ui.view;

import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.MainActivityController;
import org.walkersguide.android.ui.fragment.tabs.ObjectDetailsTabLayoutFragment;
import org.walkersguide.android.R;
import org.walkersguide.android.util.Helper;
import android.widget.Toast;
import org.walkersguide.android.tts.TTSWrapper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.os.Bundle;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.lang.Math;
import org.walkersguide.android.data.angle.Bearing;
import timber.log.Timber;
import android.os.VibrationEffect;
import androidx.core.util.Pair;
import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.util.GlobalInstance;
import android.text.TextUtils;
import org.walkersguide.android.data.Angle;
import java.util.Map;
import java.util.LinkedHashMap;
import org.walkersguide.android.data.angle.RelativeBearing;


public class IntersectionScheme extends View {

    private SelfVoicingTouchHelper mTouchHelper;
    private String intersectionName;
    private LinkedHashMap<RelativeBearing,IntersectionSegment> intersectionSegmentRelativeToInstructionMap;

    public IntersectionScheme(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Set up accessibility helper class.
        mTouchHelper = new SelfVoicingTouchHelper(this);
        ViewCompat.setAccessibilityDelegate(this, mTouchHelper);
    }

    public void configureView(String intersectionName,
            LinkedHashMap<RelativeBearing,IntersectionSegment> intersectionSegmentRelativeToInstructionMap) {
        this.intersectionName = intersectionName;
        this.intersectionSegmentRelativeToInstructionMap = intersectionSegmentRelativeToInstructionMap;
        this.invalidate();
    }

    // draw

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.intersectionSegmentRelativeToInstructionMap == null) {
            return;
        }
        final Paint paint = new Paint();
        paint.setStrokeWidth(20);
        final Pair<Float,Float> center = getCenterCoordinates();

        // intersection segments
        for (Map.Entry<RelativeBearing,IntersectionSegment> entry : this.intersectionSegmentRelativeToInstructionMap.entrySet()) {
            Pair<Float,Float> boundaryCoordinates = getBoundaryCoordinatesFor(entry.getKey());
            IntersectionSegment segment = entry.getValue();
            // draw street line
            paint.setColor(
                    segment.isPartOfNextRouteSegment() ?  Color.RED : Color.BLACK);
            canvas.drawLine(
                    center.first, center.second,
                    boundaryCoordinates.first, boundaryCoordinates.second,
                    paint);
            //Timber.d("Segment: name=%1$s   degree=%2$d   stopX=%3$.2f   stopY=%4$.2f",
            //        segment.getName(), entry.getKey().getDegree(), boundaryCoordinates.first, boundaryCoordinates.second);
        }

        // center
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(center.first, center.second, getCenterRadius(), paint);
    }

    private Pair<Float,Float> getBoundaryCoordinatesFor(Angle angle) {
        Pair<Float,Float> scaledBoundaryCoordinates = Helper.getScaledEndCoordinatesForLineWithAngle(angle);
        return Pair.create(
                (scaledBoundaryCoordinates.first * getHalfWidth()) + getHalfWidth(),
                getHalfHeight() - (scaledBoundaryCoordinates.second * getHalfHeight()) );
    }

    // hover and click

    @Override public boolean dispatchHoverEvent(MotionEvent event) {
        // Always attempt to dispatch hover events to accessibility first.
        if (mTouchHelper.dispatchHoverEvent(event)) {
            return true;
        }
        return super.dispatchHoverEvent(event);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        //Timber.d("onTouchEvent: %1$.2f   %2$.2f", event.getX(), event.getY());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                if (isAtCenter(event.getX(), event.getY(), getCenterRadius())) {
                    onCenterClicked();
                } else {
                    Map.Entry<RelativeBearing,IntersectionSegment> relativeBearingAndIntersectionSegmentMapEntry =
                        getIntersectionSegmentUnder(event.getX(), event.getY());
                    if (relativeBearingAndIntersectionSegmentMapEntry != null) {
                        onIntersectionSegmentClicked(
                                relativeBearingAndIntersectionSegmentMapEntry.getValue());
                    }
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private boolean isAtCenter(float x, float y, float radius) {
        final Pair<Float,Float> center = getCenterCoordinates();
        final float distanceBetweenPointAndCenter =
              (x - center.first) * (x - center.first)
            + (y - center.second) * (y - center.second);
        return distanceBetweenPointAndCenter <= radius * radius;
    }

    private void onCenterClicked() {
        Toast.makeText(
                getContext(), this.intersectionName, Toast.LENGTH_LONG)
            .show();
    }

    private Map.Entry<RelativeBearing,IntersectionSegment> getIntersectionSegmentUnder(float x, float y) {
        final float scaledX = (x - getHalfWidth()) / getHalfWidth();
        final float scaledY = (getHalfHeight() - y) / getHalfHeight();
        final int thresholdInDegree = 5;

        // alpha = arc tangent ( opposite / adjacent )
        // special case for y = 0 (devide by 0)
        int degree = scaledY != 0
            ? (int) Math.toDegrees(Math.atan(scaledX / scaledY))
            : 90;
        // for q2 and q3 shift by 180°
        if (scaledY < 0) {
            degree += 180;
        }
        final Bearing bearingFromCenter = new Bearing(degree);
        //Timber.d("degree=%1$d   x=%2$.2f   y=%3$.2f", bearingFromCenter.getDegree(), scaledX, scaledY);

        int min = bearingFromCenter.getDegree() - thresholdInDegree;
        int max = bearingFromCenter.getDegree() + thresholdInDegree;
        for (Map.Entry<RelativeBearing,IntersectionSegment> entry : this.intersectionSegmentRelativeToInstructionMap.entrySet()) {
            if (entry.getKey().withinRange(min, max)) {
                return entry;
            }
        }

        return null;
    }

    private void onIntersectionSegmentClicked(IntersectionSegment segment) {
        if (getContext() instanceof MainActivity) {
            ((MainActivityController) getContext())
                .addFragment(
                        ObjectDetailsTabLayoutFragment.details(segment));
        }
    }

    // helpers

    private float getHalfWidth() {
        return getWidth() / 2;
    }

    private float getHalfHeight() {
        return getHeight() / 2;
    }

    private float getCenterRadius() {
        return getHalfWidth() / 6;
    }

    private float getA11yCenterRadius() {
        return getHalfWidth() / 4;
    }

    private Pair<Float,Float> getCenterCoordinates() {
        return Pair.create(getHalfWidth(), getHalfHeight());
    }


    private class SelfVoicingTouchHelper extends ExploreByTouchHelper {

        private TTSWrapper ttsWrapperInstance;
        private boolean atCenter, atSegment;
        private IntersectionSegment lastAnnouncedSegment;

        public SelfVoicingTouchHelper(View forView) {
            super(forView);
            this.ttsWrapperInstance = TTSWrapper.getInstance();
            this.atCenter = false;
            this.atSegment = false;
            this.lastAnnouncedSegment = null;
        }

        @Override protected int getVirtualViewAt(float x, float y) {
            if (isAtCenter(x, y, getA11yCenterRadius())) {
                if (! this.atCenter) {
                    this.ttsWrapperInstance.announce(
                            String.format(
                                "%1$s, %2$s",
                                GlobalInstance.getStringResource(R.string.labelIntersectionCenter),
                                GlobalInstance.getPluralResource(
                                    R.plurals.turning,
                                    intersectionSegmentRelativeToInstructionMap.size())
                                )
                            );
                    Helper.vibrateOnce(
                            100, Helper.VIBRATION_INTENSITY_WEAK);
                    this.lastAnnouncedSegment = null;
                    this.atCenter = true;
                }
                this.atSegment = false;
            } else {
                Map.Entry<RelativeBearing,IntersectionSegment> relativeBearingAndIntersectionSegmentMapEntry =
                    getIntersectionSegmentUnder(x, y);
                if (relativeBearingAndIntersectionSegmentMapEntry != null) {
                    IntersectionSegment segment = relativeBearingAndIntersectionSegmentMapEntry.getValue();
                    if (       ! this.atSegment
                            || ! segment.equals(this.lastAnnouncedSegment)) {
                        String announcement = String.format(
                                "%1$s, %2$s",
                                segment.formatNameAndSubType(),
                                relativeBearingAndIntersectionSegmentMapEntry.getKey().getDirection().toString());
                        if (segment.isPartOfPreviousRouteSegment()) {
                            announcement += String.format(
                                    ", %1$s", GlobalInstance.getStringResource(R.string.labelPartOfPreviousRouteSegment));
                        } else if (segment.isPartOfNextRouteSegment()) {
                            announcement += String.format(
                                    ", %1$s", GlobalInstance.getStringResource(R.string.labelPartOfNextRouteSegment));
                        }
                        ttsWrapperInstance.announce(announcement);
                        this.lastAnnouncedSegment = segment;
                        this.atSegment = true;
                    }
                    Helper.vibrateOnce(
                            Helper.VIBRATION_DURATION_SHORT, Helper.VIBRATION_INTENSITY_WEAK);
                } else {
                    this.atSegment = false;
                }
                this.atCenter = false;
            }
            return 0;
        }

        @Override protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            virtualViewIds.add(0);
        }

        @Override protected void onPopulateNodeForVirtualView(
                int virtualViewId, AccessibilityNodeInfoCompat node) {
            node.setBoundsInParent(
                    new Rect(0, 0, getWidth(), getHeight()));
            node.setText(
                    GlobalInstance.getStringResource(R.string.labelIntersectionScheme));
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }

        @Override protected boolean onPerformActionForVirtualView(
                int virtualViewId, int action, Bundle arguments) {
            if (action == AccessibilityNodeInfoCompat.ACTION_CLICK) {
                if (this.atCenter) {
                    onCenterClicked();
                    return true;
                } else if (this.lastAnnouncedSegment != null) {
                    onIntersectionSegmentClicked(lastAnnouncedSegment);
                    return true;
                }
            }
            return false;
        }
    }

}
