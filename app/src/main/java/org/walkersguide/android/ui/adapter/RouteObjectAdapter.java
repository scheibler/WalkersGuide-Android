package org.walkersguide.android.ui.adapter;

import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import org.walkersguide.android.data.route.RouteObject;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.R;


public class RouteObjectAdapter extends ArrayAdapter<RouteObject> {

    private Context context;
    private LayoutInflater inflater;
    private ArrayList<RouteObject> routeObjectList;
    private RouteObject selectedRouteObject;

    public RouteObjectAdapter(Context context, ArrayList<RouteObject> routeObjectList, RouteObject selectedRouteObject) {
        super(context, R.layout.layout_single_text_view);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.routeObjectList = routeObjectList;
        this.selectedRouteObject = selectedRouteObject;
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
        RouteObject routeObject = getItem(position);
        if (holder.label != null) {
            if (routeObject.equals(selectedRouteObject)) {
                holder.label.setText(
                        StringUtility.boldAndRed(routeObject.toString()));
                holder.label.setContentDescription(
                        String.format(
                            "%1$s: %2$s",
                            context.getResources().getString(R.string.contentDescriptionSelected),
                            routeObject.toString())
                        );
            } else {
                holder.label.setText(routeObject.toString());
                holder.label.setContentDescription(routeObject.toString());
            }
        }
        return convertView;
    }

    @Override public int getCount() {
        if (this.routeObjectList != null) {
            return this.routeObjectList.size();
        }
        return 0;
    }

    @Override public RouteObject getItem(int position) {
        return this.routeObjectList.get(position);
    }


    private class EntryHolder {
        public TextView label;
    }

}
