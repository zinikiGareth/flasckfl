package org.flasck.flas.parsedForm;

public class ObjectReference implements ExternalRef {
	public final String clzName;
	public final String handle;
	public final boolean fromHandler;

	public ObjectReference(String clzName, String handle) {
		this.clzName = clzName;
		this.handle = handle;
		this.fromHandler = false;
	}
	
	public ObjectReference(ObjectReference inner, boolean fromHandler) {
		this.clzName = inner.clzName;
		this.handle = inner.handle;
		this.fromHandler = fromHandler;
	}

	public String uniqueName() {
		return this.clzName + "." + this.handle;
	}

	@Override
	public int compareTo(Object o) {
		return this.toString().compareTo(o.toString());
	}

	public boolean fromHandler() {
		return fromHandler;
	}
	
	@Override
	public String toString() {
		return this.clzName + "." + this.handle;
	}
}
