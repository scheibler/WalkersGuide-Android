package org.walkersguide.android.listener;

import org.walkersguide.android.data.basic.wrapper.PointWrapper;


public interface AddressListener {
	public void addressRequestFinished(int returnCode, String returnMessage, PointWrapper addressPoint);
}
