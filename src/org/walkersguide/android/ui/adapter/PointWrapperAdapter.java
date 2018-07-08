package org.walkersguide.android.ui.adapter;

import java.util.ArrayList;
import java.util.Collections;

import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.basic.wrapper.PointWrapper.SortByDistanceFromCurrentPosition;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PointWrapperAdapter extends ArrayAdapter<PointWrapper> {

    private LayoutInflater m_inflater;
    private ArrayList<PointWrapper> pointWrapperList;

    public PointWrapperAdapter(Context context, ArrayList<PointWrapper> pointWrapperList) {
        super(context, R.layout.layout_single_text_view);
        m_inflater = LayoutInflater.from(context);
        this.pointWrapperList = pointWrapperList;
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
        holder.label.setText(getItem(position).toString());
        return convertView;
    }

    @Override public int getCount() {
        if (pointWrapperList != null)
            return pointWrapperList.size();
        return 0;
    }

    @Override public PointWrapper getItem(int position) {
        return pointWrapperList.get(position);
    }

    @Override public void notifyDataSetChanged() {
        Collections.sort(pointWrapperList, new SortByDistanceFromCurrentPosition());
        super.notifyDataSetChanged();
    }

    private class EntryHolder {
        public TextView label;
    }

}
