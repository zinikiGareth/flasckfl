package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.CardName;

// This should possibly be called "ClassReference"
// It is intended to be a holder for the "name" of a class (Card, Handler, etc) that can then be used in expressions later 
public class ObjectReference implements Locatable, ExternalRef {
	public final InputPosition location;
	public final CardName clzName;
	public final String handle;
	public final boolean fromHandler;

	public ObjectReference(InputPosition location, CardName clzName, String handle) {
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
		return this.handle;
	}
	
	@Override
	public int compareTo(Object o) {
		return this.handle.compareTo(((ExternalRef)o).uniqueName());
	}

	public boolean fromHandler() {
		return fromHandler;
	}
	
	@Override
	public String toString() {
		return this.clzName.jsName() + "." + this.handle;
	}
}
