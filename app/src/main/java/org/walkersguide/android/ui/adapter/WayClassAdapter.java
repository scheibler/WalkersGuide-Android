package org.walkersguide.android.ui.adapter;

import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import org.walkersguide.android.server.route.WayClass;
import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;


public class WayClassAdapter extends ArrayAdapter<WayClass> {

    private Context context;
    private LayoutInflater inflater;
    private ArrayList<WayClass> wayClassList;

    public WayClassAdapter(Context context, ArrayList<WayClass> wayClassList) {
        super(context, R.layout.layout_way_class_list_entry);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.wayClassList = wayClassList;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        // load item layout
        EntryHolder holder;
        if (convertView == null) {
            convertView = this.inflater.inflate(R.layout.layout_way_class_list_entry, parent, false);
            holder = new EntryHolder();
            holder.labelWayClassName = (TextView) convertView.findViewById(R.id.labelWayClassName);
            holder.spinnerWayClassWeight = (Spinner) convertView.findViewById(R.id.spinnerWayClassWeight);
            convertView.setTag(holder);
        } else {
            holder = (EntryHolder) convertView.getTag();
        }
        WayClass wayClass = getItem(position);

        // label
        if (holder.labelWayClassName != null) {
            holder.labelWayClassName.setText(
                    WayClass.idToString(context, wayClass.getId()));
        }

        // spinner
        if (holder.spinnerWayClassWeight != null) {
            // fill spinner
            ArrayList<String> weightToStringList = new ArrayList<String>();
            for (int i=0; i<Constants.RoutingWayClassWeightValueArray.length; i++) {
                weightToStringList.add(
                        WayClass.weightToString(context, Constants.RoutingWayClassWeightValueArray[i]));
            }
            ArrayAdapter<String> wayClassWeightAdapter = new ArrayAdapter<String>(
                    context, android.R.layout.simple_spinner_item, weightToStringList);
            wayClassWeightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.spinnerWayClassWeight.setAdapter(wayClassWeightAdapter);
            // listener
            holder.spinnerWayClassWeight.setTag(wayClass);
            holder.spinnerWayClassWeight.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    WayClass associatedWayClass = (WayClass) parent.getTag();
                    if (associatedWayClass != null
                            && associatedWayClass.getWeight() != Constants.RoutingWayClassWeightValueArray[pos]) {
                        wayClassList.set(
                                wayClassList.indexOf(associatedWayClass),
                                new WayClass(
                                    associatedWayClass.getId(), Constants.RoutingWayClassWeightValueArray[pos]));
                        notifyDataSetChanged();
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
            // select weight by index
            for (int i=0; i<Constants.RoutingWayClassWeightValueArray.length; i++) {
                if (Constants.RoutingWayClassWeightValueArray[i] == wayClass.getWeight()) {
                    holder.spinnerWayClassWeight.setSelection(i);
                    break;
                }
            }
        }

        return convertView;
    }

    @Override public int getCount() {
        if (this.wayClassList != null) {
            return this.wayClassList.size();
        }
        return 0;
    }

    @Override public WayClass getItem(int position) {
        return this.wayClassList.get(position);
    }

    public ArrayList<WayClass> getWayClassList() {
        return this.wayClassList;
    }

    public void resetToDefaults() {
        this.wayClassList = new ArrayList<WayClass>();
        for (int i=0; i<Constants.RoutingWayClassIdValueArray.length; i++) {
            String wayClassId = Constants.RoutingWayClassIdValueArray[i];
            double wayClassWeight = WayClass.defaultWeightForWayClass(wayClassId);
            this.wayClassList.add(new WayClass(wayClassId, wayClassWeight));
        }
        notifyDataSetChanged();
    }


    private class EntryHolder {
        public TextView labelWayClassName;
        public Spinner spinnerWayClassWeight;
    }

}
