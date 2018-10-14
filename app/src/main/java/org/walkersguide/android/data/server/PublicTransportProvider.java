package org.walkersguide.android.data.server;

import android.content.Context;

import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;


public class PublicTransportProvider {

    private String id, name;

    public PublicTransportProvider(Context context, String id) {
        this.id = id;
        // determine name from id
        if (id.equals(Constants.PUBLIC_TRANSPORT_PROVIDER.DB)) {
            this.name = context.getResources().getString(R.string.publicTransportProviderDB);
        } else if (id.equals(Constants.PUBLIC_TRANSPORT_PROVIDER.VBB)) {
            this.name = context.getResources().getString(R.string.publicTransportProviderVBB);
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
        } else if (! (obj instanceof PublicTransportProvider)) {
			return false;
        }
		PublicTransportProvider other = (PublicTransportProvider) obj;
        return this.id.equals(other.getId());
    }

}
