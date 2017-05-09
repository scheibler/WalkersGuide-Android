package org.walkersguide.android.data.server;

public class Map {

    private String name, url;
    private int version;
    private long created;

    public Map(String name, String url) {
        this.name = name;
        this.url = url;
        this.version = 0;
        this.created = 0l;
    }

    public Map(String name, String url, int version, long created) {
        this.name = name;
        this.url = url;
        this.version = version;
        this.created = created;
    }

    public String getName() {
        return this.name;
    }

    public String getURL() {
        return this.url;
    }

    public int getVersion() {
        return this.version;
    }

    public long getCreated() {
        return this.created;
    }

    @Override public String toString() {
        return this.name;
    }

	@Override public int hashCode() {
        return this.name.hashCode();
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof Map)) {
			return false;
        }
		Map other = (Map) obj;
        return this.name.equals(other.getName());
    }

}
