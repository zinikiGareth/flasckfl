package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class ObjectReference implements ExternalRef {
	public final InputPosition location;
	public final String clzName;
	public final String handle;
	public final boolean fromHandler;

	public ObjectReference(InputPosition location, String clzName, String handle) {
		this.location = location;
		this.clzName = clzName;
		this.handle = handle;
		this.fromHandler = false;
	}
	
	public ObjectReference(InputPosition location, ObjectReference inner, boolean fromHandler) {
		this.location = location;
		this.clzName = inner.clzName;
		this.handle = inner.handle;
		this.fromHandler = fromHandler;
	}

	public InputPosition location() {
		return location;
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
