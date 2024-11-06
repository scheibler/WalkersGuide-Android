package org.walkersguide.android.ui.adapter;

import org.walkersguide.android.R;
import org.walkersguide.android.ui.view.ProfileView;
import org.walkersguide.android.ui.view.ProfileView.OnProfileDefaultActionListener;

import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;



import android.view.View;
import android.view.ViewGroup;


import java.util.ArrayList;
import android.content.Context;
import android.widget.BaseAdapter;
import org.walkersguide.android.data.Profile;
import org.walkersguide.android.data.profile.AnnouncementRadius;
import android.widget.ArrayAdapter;
import android.view.LayoutInflater;
import android.widget.CheckedTextView;
import java.lang.Class;


public class AnnouncementRadiusAdapter extends ArrayAdapter<AnnouncementRadius> {
    private static ArrayList<AnnouncementRadius> announcementRadiusList = AnnouncementRadius.values();

    private Context context;
    private LayoutInflater m_inflater;

    public AnnouncementRadiusAdapter(Context context) {
        super(context, R.layout.layout_single_text_view_checkbox);
        this.context = context;
        this.m_inflater = LayoutInflater.from(context);
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        return populateView(position, convertView, parent, false);
    }

    @Override public View getDropDownView(int position, View convertView,ViewGroup parent) {
        return populateView(position, convertView, parent, true);
    }

    private View populateView(int position, View convertView, ViewGroup parent, boolean isDropDown) {
        // load item layout
        EntryHolder holder;
        if (convertView == null) {
            LayoutInflater m_inflater = LayoutInflater.from(context);
            convertView = m_inflater.inflate(R.layout.layout_single_text_view_checkbox, parent, false);
            // find view
            holder = new EntryHolder();
            holder.labelRadius = (CheckedTextView) convertView.findViewById(R.id.label);
            convertView.setTag(holder);
        } else {
            holder = (EntryHolder) convertView.getTag();
        }

        final AnnouncementRadius radius = getItem(position);

        if (isDropDown) {
            holder.labelRadius.setCheckMarkDrawable(android.R.drawable.btn_radio);
            holder.labelRadius.setText(radius.longFormat());
            holder.labelRadius.setContentDescription(null);
        } else {
            holder.labelRadius.setCheckMarkDrawable(null);
            holder.labelRadius.setText(radius.shortFormat());
            holder.labelRadius.setContentDescription(radius.longFormat());
        }

        return convertView;
    }

    @Override public int getCount() {
        return this.announcementRadiusList.size();
    }

    @Override public AnnouncementRadius getItem(int position) {
        return this.announcementRadiusList.get(position);
    }

    @Override public long getItemId(int position) {
        return position;
    }

    public int getIndexOf(AnnouncementRadius radius) {
        int index = this.announcementRadiusList.indexOf(radius);
        if (index == -1) {
            index = 0;
        }
        return index;
    }


    private class EntryHolder {
        public CheckedTextView labelRadius;
    }


}
