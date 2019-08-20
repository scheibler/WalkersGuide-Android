package org.walkersguide.android.ui.adapter;

import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.R;


public class RouteIdAdapter extends ArrayAdapter<Integer> {

    private Context context;
    private AccessDatabase accessDatabaseInstance;
    private LayoutInflater inflater;
    private ArrayList<Integer> routeIdList;

    public RouteIdAdapter(Context context, ArrayList<Integer> routeIdList) {
        super(context, R.layout.layout_single_text_view);
        this.context = context;
        this.accessDatabaseInstance = AccessDatabase.getInstance(context);
        this.inflater = LayoutInflater.from(context);
        this.routeIdList = routeIdList;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        // load item layout
        EntryHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.layout_single_text_view, parent, false);
            holder = new EntryHolder();
            holder.label = (TextView) convertView.findViewById(R.id.label);
            convertView.setTag(holder);
        } else {
            holder = (EntryHolder) convertView.getTag();
        }
        Integer routeId = getItem(position);
        PointWrapper startPoint = accessDatabaseInstance.getStartPointOfRoute(routeId);
        PointWrapper destinationPoint = accessDatabaseInstance.getDestinationPointOfRoute(routeId);
        if (holder.label != null
                && startPoint != null
                && destinationPoint != null) {
            holder.label.setText(
                    String.format(
                        "%1$s: %2$s\n%3$s: %4$s",
                        context.getResources().getString(R.string.buttonStartPoint),
                        startPoint.getPoint().getName(),
                        context.getResources().getString(R.string.buttonDestinationPoint),
                        destinationPoint.getPoint().getName())
                    );
        }
        return convertView;
    }

    @Override public int getCount() {
        if (routeIdList != null)
            return routeIdList.size();
        return 0;
    }

    @Override public Integer getItem(int position) {
        return routeIdList.get(position);
    }


    private class EntryHolder {
        public TextView label;
    }

}
