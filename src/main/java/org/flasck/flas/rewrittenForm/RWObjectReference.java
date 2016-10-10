package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;

// This should possibly be called "ClassReference"
// It is intended to be a holder for the "name" of a class (Card, Handler, etc) that can then be used in expressions later 
public class RWObjectReference implements ExternalRef {
	public final InputPosition location;
	public final String clzName;
	public final String handle;
	public final boolean fromHandler;

	public RWObjectReference(InputPosition location, String clzName, String handle) {
		this.location = location;
		this.clzName = clzName;
		this.handle = handle;
		this.fromHandler = false;
	}
	
	public RWObjectReference(InputPosition location, RWObjectReference inner, boolean fromHandler) {
		this.location = location;
		this.clzName = inner.clzName;
		this.handle = inner.handle;
		this.fromHandler = fromHandler;
	}

	public InputPosition location() {
		return location;
	}

	public String uniqueName() {
		return this.handle;
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
