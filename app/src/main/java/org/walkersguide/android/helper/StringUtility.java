package org.walkersguide.android.helper;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

public class StringUtility {

    public static int getDirectionConstant(int direction) {
        if (direction < 0) {
            direction += 360;
        }
        if ((direction >= 0) && (direction < 23)) {
            return Constants.DIRECTION.NORTH;
        } else if ((direction >= 23) && (direction < 68)) {
            return Constants.DIRECTION.NORTH_EAST;
        } else if ((direction >= 68) && (direction < 113)) {
            return Constants.DIRECTION.EAST;
        } else if ((direction >= 113) && (direction < 158)) {
            return Constants.DIRECTION.SOUTH_EAST;
        } else if ((direction >= 158) && (direction < 203)) {
            return Constants.DIRECTION.SOUTH;
        } else if ((direction >= 203) && (direction < 248)) {
            return Constants.DIRECTION.SOUTH_WEST;
        } else if ((direction >= 248) && (direction < 293)) {
            return Constants.DIRECTION.WEST;
        } else if ((direction >= 293) && (direction < 338)) {
            return Constants.DIRECTION.NORTH_WEST;
        } else {
            return Constants.DIRECTION.NORTH;
        }
    }

    public static String formatInstructionDirection(Context context, int direction) {
        if (direction < 0) {
            direction += 360;
        }
        switch (getDirectionConstant(direction)) {
            case Constants.DIRECTION.NORTH:
                return context.getResources().getString(R.string.directionStraightforward);
            case Constants.DIRECTION.NORTH_EAST:
                return context.getResources().getString(R.string.directionTurnRightSlightly);
            case Constants.DIRECTION.EAST:
                return context.getResources().getString(R.string.directionTurnRight);
            case Constants.DIRECTION.SOUTH_EAST:
                return context.getResources().getString(R.string.directionTurnRightStrongly);
            case Constants.DIRECTION.SOUTH:
                return context.getResources().getString(R.string.directionBehindYou);
            case Constants.DIRECTION.SOUTH_WEST:
                return context.getResources().getString(R.string.directionTurnLeftStrongly);
            case Constants.DIRECTION.WEST:
                return context.getResources().getString(R.string.directionTurnLeft);
            case Constants.DIRECTION.NORTH_WEST:
                return context.getResources().getString(R.string.directionTurnLeftSlightly);
            default:
                return context.getResources().getString(R.string.directionStraightforward);
        }
    }

    public static String formatGeographicDirection(Context context, int direction) {
        if (direction < 0) {
            direction += 360;
        }
        switch (getDirectionConstant(direction)) {
            case Constants.DIRECTION.NORTH:
                return context.getResources().getString(R.string.directionNorth);
            case Constants.DIRECTION.NORTH_EAST:
                return context.getResources().getString(R.string.directionNorthEast);
            case Constants.DIRECTION.EAST:
                return context.getResources().getString(R.string.directionEast);
            case Constants.DIRECTION.SOUTH_EAST:
                return context.getResources().getString(R.string.directionSouthEast);
            case Constants.DIRECTION.SOUTH:
                return context.getResources().getString(R.string.directionSouth);
            case Constants.DIRECTION.SOUTH_WEST:
                return context.getResources().getString(R.string.directionSouthWest);
            case Constants.DIRECTION.WEST:
                return context.getResources().getString(R.string.directionWest);
            case Constants.DIRECTION.NORTH_WEST:
                return context.getResources().getString(R.string.directionNorthWest);
            default:
                return context.getResources().getString(R.string.directionNorth);
        }
    }

    public static String formatHoursMinutes(long timestamp) {
        SimpleDateFormat hoursMinutesFormat = new SimpleDateFormat("HH:mm");
        return hoursMinutesFormat.format(new Date(timestamp));
    }

    public static String formatProfileSortCriteria(Context context, int sortCriteria) {
        switch (sortCriteria) {
            case Constants.SORT_CRITERIA.NAME_ASC:
                return context.getResources().getString(R.string.radioButtonSortNameAsc);
            case Constants.SORT_CRITERIA.NAME_DESC:
                return context.getResources().getString(R.string.radioButtonSortNameDesc);
            case Constants.SORT_CRITERIA.DISTANCE_ASC:
                return context.getResources().getString(R.string.radioButtonSortDistanceAsc);
            case Constants.SORT_CRITERIA.DISTANCE_DESC:
                return context.getResources().getString(R.string.radioButtonSortDistanceDesc);
            case Constants.SORT_CRITERIA.ORDER_ASC:
                return context.getResources().getString(R.string.radioButtonSortOrderAsc);
            case Constants.SORT_CRITERIA.ORDER_DESC:
                return context.getResources().getString(R.string.radioButtonSortOrderDesc);
            default:
                return "";
        }
    }

    public static SpannableString boldAndRed(String text) {
        SpannableString spanString = new SpannableString(text);
        spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
        spanString.setSpan(
                new ForegroundColorSpan(Color.rgb(215, 0, 0)), 0, spanString.length(), 0);
        return spanString;
    }

}
