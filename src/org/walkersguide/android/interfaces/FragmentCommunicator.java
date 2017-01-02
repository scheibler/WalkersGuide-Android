package org.walkersguide.android.interfaces;

/**
 * Interface for communication between the parent activity and it's attached
 * fragments The fragments must implement the defined functions
 **/

public interface FragmentCommunicator {
	public void onFragmentEnabled();
	public void onFragmentDisabled();
}
