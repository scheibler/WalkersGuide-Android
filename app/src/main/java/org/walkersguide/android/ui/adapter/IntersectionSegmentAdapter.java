package org.walkersguide.android.ui.adapter;

import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import org.walkersguide.android.data.basic.segment.IntersectionSegment;
import org.walkersguide.android.data.basic.segment.IntersectionSegment.SortByBearingFromCurrentDirection;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.R;


public class IntersectionSegmentAdapter extends ArrayAdapter<IntersectionSegment> {

    private Context context;
    private LayoutInflater m_inflater;
    private ArrayList<IntersectionSegment> intersectionSegmentList;

    public IntersectionSegmentAdapter(Context context, ArrayList<IntersectionSegment> intersectionSegmentList) {
        super(context, R.layout.layout_single_text_view);
        this.context = context;
        m_inflater = LayoutInflater.from(context);
        this.intersectionSegmentList = intersectionSegmentList;
        notifyDataSetChanged();
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        // load item layout
        EntryHolder holder;
        if (convertView == null) {
            convertView = m_inflater.inflate(R.layout.layout_single_text_view, parent, false);
            holder = new EntryHolder();
            holder.label = (TextView) convertView.findViewById(R.id.label);
            convertView.setTag(holder);
        } else {
            holder = (EntryHolder) convertView.getTag();
        }

        IntersectionSegment segment = getItem(position);
        if (holder.label != null) {
            if (segment.isPartOfPreviousRouteSegment()) {
                holder.label.setText(segment.toString());
                holder.label.setContentDescription(
                        String.format(
                            "%1$s,\n%2$s",
                            segment.toString(),
                            context.getResources().getString(R.string.labelPartOfPreviousRouteSegment))
                        );
            } else if (segment.isPartOfNextRouteSegment()) {
                holder.label.setText(
                        StringUtility.boldAndRed(context, segment.toString()));
                holder.label.setContentDescription(
                        String.format(
                            "%1$s,\n%2$s",
                            segment.toString(),
                            context.getResources().getString(R.string.labelPartOfNextRouteSegment))
                        );
            } else {
                holder.label.setText(segment.toString());
                holder.label.setContentDescription(segment.toString());
            }
        }
        return convertView;
    }

    @Override public int getCount() {
        if (intersectionSegmentList != null)
            return intersectionSegmentList.size();
        return 0;
    }

    @Override public IntersectionSegment getItem(int position) {
        return intersectionSegmentList.get(position);
    }

    @Override public void notifyDataSetChanged() {
        Collections.sort(intersectionSegmentList, new SortByBearingFromCurrentDirection(68));
        super.notifyDataSetChanged();
    }

    private class EntryHolder {
        public TextView label;
    }

}
