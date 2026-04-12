package org.walkersguide.android.ui.adapter;

import org.walkersguide.android.R;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import java.util.ArrayList;

import android.content.Context;
import android.widget.BaseAdapter;
import java.util.List;
import org.walkersguide.android.server.wg.p2p.WayClassWeightSettings;
import timber.log.Timber;
import android.widget.SpinnerAdapter;
import android.widget.CheckedTextView;
import androidx.annotation.Nullable;


public class WayClassWeightSettingsSpinnerAdapter extends BaseAdapter implements SpinnerAdapter {

    private Context context;
    private List<WayClassWeightSettings> items;
    private boolean showHintInDropdown;
    private String hintText;
    private LayoutInflater inflater;

    public WayClassWeightSettingsSpinnerAdapter(Context context,
            List<WayClassWeightSettings> items, String hintText, boolean showHintInDropdown) {
        this.context = context;
        this.items = createItemListWithLeadingNullValue(items);
        this.hintText = hintText;
        this.showHintInDropdown = showHintInDropdown;
        this.inflater = LayoutInflater.from(context);
    }

    public int indexOfItem(@Nullable WayClassWeightSettings item) {
        return this.items.indexOf(item);
    }

    public void updateItems(List<WayClassWeightSettings> newItems) {
        this.items = createItemListWithLeadingNullValue(newItems);
        notifyDataSetChanged();
    }

    @Override public int getCount() {
        return items.size();
    }

    @Override public WayClassWeightSettings getItem(int position) {
        return items.get(position);
    }

    @Override public long getItemId(int position) {
        return position;
    }

    @Override public boolean isEnabled(int position) {
        return true;
    }

    // Closed Spinner view (always shows hint if position 0)
    @Override public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);

        if (position == 0) {
            // Show hint
            textView.setText(hintText);
        } else {
            // Show actual item
            WayClassWeightSettings item = items.get(position);
            textView.setText(item.getName());
        }

        return convertView;
    }

    // Dropdown list view - uses custom layout with checkbox
    @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
        // Handle hint item based on configuration
        if (position == 0 && ! showHintInDropdown) {
            // Hide hint from dropdown
            View view = new View(context);
            view.setVisibility(View.GONE);
            Timber.d("hide hint / first item");
            return view;
        }

        if (convertView == null
                || convertView.getVisibility() == View.GONE
                || convertView.findViewById(R.id.label) == null) {
            Timber.d("inflate dropdown item");
            convertView = inflater.inflate(
                R.layout.layout_single_text_view_checkbox, parent, false);
        }

        CheckedTextView textView = convertView.findViewById(R.id.label);
        textView.setCheckMarkDrawable(android.R.drawable.btn_radio);

        WayClassWeightSettings item = items.get(position);
        textView.setText(item != null ? item.getName() : hintText);

        convertView.setVisibility(View.VISIBLE);
        return convertView;
    }

    private static List<WayClassWeightSettings> createItemListWithLeadingNullValue(List<WayClassWeightSettings> items) {
        List<WayClassWeightSettings> itemsWithLeadingNullValue = new ArrayList<>();
        itemsWithLeadingNullValue.add(null); // First item is always null (hint)
        itemsWithLeadingNullValue.addAll(items);
        return itemsWithLeadingNullValue;
    }

}
