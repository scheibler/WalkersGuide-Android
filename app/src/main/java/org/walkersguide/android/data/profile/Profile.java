package org.walkersguide.android.data.profile;

import java.io.Serializable;


public abstract class Profile implements Serializable {
    private static final long serialVersionUID = 1l;

    public abstract boolean isModifiable();


    private long id;
    private String name;

    public Profile(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return this.id;
    }

    public  String getName() {
        return this.name;
    }

    protected  void setName(String newName) {
        this.name = newName;
    }

    @Override public String toString() {
        return this.name;
    }

	@Override public int hashCode() {
        return new Long(this.id).hashCode();
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof Profile)) {
			return false;
        }
		Profile other = (Profile) obj;
        return this.id == other.getId();
    }

}
