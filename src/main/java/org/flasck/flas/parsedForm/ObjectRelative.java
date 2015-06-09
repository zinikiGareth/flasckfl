package org.flasck.flas.parsedForm;

public class ObjectRelative implements ExternalRef {
	public final String clzName;
	public final String handle;

	public ObjectRelative(String clzName, String handle) {
		this.clzName = clzName;
		this.handle = handle;
	}
	
	public String uniqueName() {
		return this.clzName + "." + this.handle;
	}

	@Override
	public int compareTo(Object o) {
		return this.toString().compareTo(o.toString());
	}

	@Override
	public String toString() {
		return this.clzName + "." + this.handle;
	}
}
