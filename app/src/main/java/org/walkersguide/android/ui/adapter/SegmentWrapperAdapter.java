package org.walkersguide.android.ui.adapter;

import android.content.Context;

import android.text.TextUtils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import org.walkersguide.android.data.basic.segment.Footway;
import org.walkersguide.android.data.basic.wrapper.SegmentWrapper;
import org.walkersguide.android.R;


public class SegmentWrapperAdapter extends ArrayAdapter<SegmentWrapper> {

    private LayoutInflater m_inflater;
    private ArrayList<SegmentWrapper> segmentWrapperList;

    public SegmentWrapperAdapter(Context context, ArrayList<SegmentWrapper> segmentWrapperList) {
        super(context, R.layout.layout_single_text_view);
        m_inflater = LayoutInflater.from(context);
        this.segmentWrapperList = segmentWrapperList;
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
        // fill label
        SegmentWrapper segmentWrapper = getItem(position);
        String segmentWrapperString = segmentWrapper.toString();
        if (segmentWrapper.getSegment() instanceof Footway) {
            Footway footway = (Footway) segmentWrapper.getSegment();
            if (! TextUtils.isEmpty(footway.getUserDescription())) {
                segmentWrapperString = String.format(
                        "%1$s\n%2$s", footway.toString(), footway.getUserDescription());
            }
        }
        holder.label.setText(segmentWrapperString);
        return convertView;
    }

    @Override public int getCount() {
        if (segmentWrapperList != null)
            return segmentWrapperList.size();
        return 0;
    }

    @Override public SegmentWrapper getItem(int position) {
        return segmentWrapperList.get(position);
    }

    @Override public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    private class EntryHolder {
        public TextView label;
    }

}
