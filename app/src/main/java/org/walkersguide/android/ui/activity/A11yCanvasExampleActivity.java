package org.walkersguide.android.ui.activity;

import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.view.IntersectionScheme;
import org.walkersguide.android.R;

import android.app.Activity;
import android.os.Bundle;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import android.view.View;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import org.walkersguide.android.data.object_with_id.Point;


public class A11yCanvasExampleActivity extends Activity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_a11y_canvas_example);
        final IntersectionScheme intersectionScheme = (IntersectionScheme) findViewById(R.id.intersectionScheme);

        Point point = PositionManager.getInstance().getSimulatedLocation();
        if (point != null
                && point instanceof Intersection) {
            intersectionScheme.configureView((Intersection) point);
        }
    }

}
