package org.walkersguide.android.ui.view;

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
import org.walkersguide.android.data.object_with_id.point.Intersection;
import androidx.core.util.Pair;
import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.util.GlobalInstance;


public class IntersectionScheme extends View {

    private SelfVoicingTouchHelper mTouchHelper;
    private Intersection intersection;

    public IntersectionScheme(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Set up accessibility helper class.
        mTouchHelper = new SelfVoicingTouchHelper(this);
        ViewCompat.setAccessibilityDelegate(this, mTouchHelper);
    }

    public void configureView(Intersection intersection) {
        this.intersection = intersection;
        this.invalidate();
    }

    // draw

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Timber.d("onDraw: %1$s", intersection);
        if (intersection == null) {
            return;
        }
        final Paint paint = new Paint();
        final Pair<Float,Float> center = getCenterCoordinates();

        // intersection segments
        for (IntersectionSegment segment : intersection.getSegmentList()) {
            Pair<Float,Float> boundaryCoordinates = getBoundaryCoordinatesFor(segment.getBearing());
            Timber.d("Segment: name=%1$s   degree=%2$d   stopX=%3$.2f   stopY=%4$.2f",
                    segment.getName(), segment.getBearing().getDegree(), boundaryCoordinates.first, boundaryCoordinates.second);
            paint.setColor(Color.RED);
            canvas.drawLine(
                    center.first, center.second, boundaryCoordinates.first, boundaryCoordinates.second, paint);
        }

        // center
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(center.first, center.second, getCenterRadius(), paint);
    }

    private Pair<Float,Float> getBoundaryCoordinatesFor(Bearing bearing) {
        float scaledX = 0.0f, scaledY = 0.0f;
        if (bearing.withinRange(316, 45)) {
            scaledX = (float) Math.tan( Math.toRadians(bearing.getDegree()) );
            scaledY = 1.0f;
        } else if (bearing.withinRange(46, 135)) {
            scaledX = 1.0f;
            scaledY = (float) Math.tan( Math.toRadians(90-bearing.getDegree()) );
        } else if (bearing.withinRange(136, 225)) {
            scaledX = (float) Math.tan( Math.toRadians(-bearing.getDegree()) );
            scaledY = -1.0f;
        } else if (bearing.withinRange(226, 315)) {
            scaledX = -1.0f;
            scaledY = (float) Math.tan( Math.toRadians(bearing.getDegree()-90) );
        }

        return Pair.create(
                (scaledX * getHalfWidth()) + getHalfWidth(),
                getHalfHeight() - (scaledY * getHalfHeight()) );
    }

    private Pair<Float,Float> getCenterCoordinates() {
        return Pair.create(getHalfWidth(), getHalfHeight());
    }

    private float getCenterRadius() {
        return getHalfWidth() / 4;
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
        Timber.d("onTouchEvent: %1$.2f   %2$.2f", event.getX(), event.getY());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                if (isAtCenter(event.getX(), event.getY())) {
                    onCenterClicked();
                } else {
                    IntersectionSegment segment = getIntersectionSegmentUnder(event.getX(), event.getY());
                    if (segment != null) {
                        onIntersectionSegmentClicked(segment);
                    }
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private boolean isAtCenter(float x, float y) {
        final Pair<Float,Float> center = getCenterCoordinates();
        final float radius = getCenterRadius();
        final float distanceBetweenPointAndCenter =
              (x - center.first) * (x - center.first)
            + (y - center.second) * (y - center.second);
        return distanceBetweenPointAndCenter <= radius * radius;
    }

    private void onCenterClicked() {
        Toast.makeText(
                getContext(),
                intersection != null
                ? intersection.toString()
                : GlobalInstance.getStringResource(R.string.labelNothingSelected),
                Toast.LENGTH_LONG).show();
    }

    private IntersectionSegment getIntersectionSegmentUnder(float x, float y) {
        final float scaledX = (x - getHalfWidth()) / getHalfWidth();
        final float scaledY = (getHalfHeight() - y) / getHalfHeight();
        final int thresholdInDegree = 4;

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
        Timber.d("degree=%1$d   x=%2$.2f   y=%3$.2f", bearingFromCenter.getDegree(), scaledX, scaledY);

        int min = bearingFromCenter.getDegree() - thresholdInDegree;
        int max = bearingFromCenter.getDegree() + thresholdInDegree;
        for (IntersectionSegment segment : intersection.getSegmentList()) {
            if (segment.getBearing().withinRange(min, max)) {
                return segment;
            }
        }
        return null;
    }

    private void onIntersectionSegmentClicked(IntersectionSegment segment) {
        Toast.makeText(
                getContext(),
                segment.toString(),
                Toast.LENGTH_LONG).show();
    }

    // helpers

    private float getHalfWidth() {
        return getWidth() / 2;
    }

    private float getHalfHeight() {
        return getHeight() / 2;
    }


    private class SelfVoicingTouchHelper extends ExploreByTouchHelper {

        private TTSWrapper ttsWrapperInstance;
        private boolean atCenter;
        private IntersectionSegment lastAnnouncedSegment;

        public SelfVoicingTouchHelper(View forView) {
            super(forView);
            this.ttsWrapperInstance = TTSWrapper.getInstance();
            this.atCenter = false;
            this.lastAnnouncedSegment = null;
        }

        @Override protected int getVirtualViewAt(float x, float y) {
            if (isAtCenter(x, y)) {
                if (! this.atCenter) {
                    this.ttsWrapperInstance.announce("Kreuzungsmitte");
                }
                this.atCenter = true;
            } else {
                IntersectionSegment segment = getIntersectionSegmentUnder(x, y);
                if (segment != null) {
                    if (this.lastAnnouncedSegment == null
                            || ! this.lastAnnouncedSegment.equals(segment)) {
                        ttsWrapperInstance.announce(segment.getName());
                    }
                    Helper.vibrateOnce(50);
                    this.lastAnnouncedSegment = segment;
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
            node.setText("Kreuzungsschema");
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }

        @Override protected boolean onPerformActionForVirtualView(
                int virtualViewId, int action, Bundle arguments) {
            switch (action) {
                case AccessibilityNodeInfoCompat.ACTION_CLICK:
                    if (this.atCenter) {
                        onCenterClicked();
                    } else if (this.lastAnnouncedSegment != null) {
                        onIntersectionSegmentClicked(this.lastAnnouncedSegment);
                    }
                    return true;
            }
            return false;
        }
    }

}
