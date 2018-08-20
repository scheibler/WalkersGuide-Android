package org.walkersguide.android.data.server;

import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;

import android.content.Context;


public class AddressProvider {

    private String id, name;

    public AddressProvider(Context context, String id) {
        this.id = id;
        // determine name from id
        if (id.equals(Constants.ADDRESS_PROVIDER.GOOGLE)) {
            this.name = context.getResources().getString(R.string.addressProviderGoogle);
        } else if (id.equals(Constants.ADDRESS_PROVIDER.OSM)) {
            this.name = context.getResources().getString(R.string.addressProviderOSM);
        } else {
            this.name = id;
        }
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    @Override public String toString() {
        return this.name;
    }

	@Override public int hashCode() {
        return this.id.hashCode();
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof AddressProvider)) {
			return false;
        }
		AddressProvider other = (AddressProvider) obj;
        return this.id.equals(other.getId());
    }

}
