package org.walkersguide.android.data.server;

public class PublicTransportProvider {

    private String identifier, name;

    public PublicTransportProvider(String identifier, String name) {
        this.identifier = identifier;
        this.name = name;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public String getName() {
        return this.name;
    }

    @Override public String toString() {
        return this.name;
    }

	@Override public int hashCode() {
        return this.identifier.hashCode();
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
        return this.identifier.equals(other.getIdentifier());
    }

}
