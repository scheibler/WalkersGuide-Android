package org.walkersguide.android.ui.view;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.GridLayout;


public class CheckBoxGroupView extends GridLayout {

    private ArrayList<CheckBox> checkBoxList = new ArrayList<CheckBox>();

    public CheckBoxGroupView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        for(CheckBox c : checkBoxList) {
            addView(c);
        }
        invalidate();
        requestLayout();
    }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void put(CheckBox checkBox) {
        checkBoxList.add( checkBox);
        invalidate();
        requestLayout();
    }

    public ArrayList<CheckBox> getCheckBoxList() {
        return this.checkBoxList;
    }

    public ArrayList<CheckBox> getCheckedCheckBoxList() {
        ArrayList<CheckBox> checkedCheckBoxList = new ArrayList<CheckBox>();
        for (CheckBox c : checkBoxList){
            if(c.isChecked()) {
                checkedCheckBoxList.add(c);
            }
        }
        return checkedCheckBoxList;
    }

    public boolean allChecked() {
        for (CheckBox c : checkBoxList){
            if(! c.isChecked()) {
                return false;
            }
        }
        return true;
    }

    public void checkAll() {
        for (CheckBox c : checkBoxList){
            if(! c.isChecked()) {
                c.setChecked(true);
            }
        }
        invalidate();
        requestLayout();
    }

    public boolean nothingChecked() {
        for (CheckBox c : checkBoxList){
            if(c.isChecked()) {
                return false;
            }
        }
        return true;
    }

    public void uncheckAll() {
        for (CheckBox c : checkBoxList){
            if(c.isChecked()) {
                c.setChecked(false);
            }
        }
        invalidate();
        requestLayout();
    }

}
