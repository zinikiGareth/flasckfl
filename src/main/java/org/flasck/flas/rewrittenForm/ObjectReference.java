package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.NameOfThing;

// This should possibly be called "ClassReference"
// It is intended to be a holder for the "name" of a class (Card, Handler, etc) that can then be used in expressions later
// The example I see is in complexcard.fl/ready, and there it refers to a "nested" class?
public class ObjectReference implements Locatable, ExternalRef {
	public final InputPosition location;
	public final HandlerName handler;
	public final boolean fromHandler;

	public ObjectReference(InputPosition location, HandlerName handler) {
		this.location = location;
		this.handler = handler;
		this.fromHandler = false;
	}
	
	public ObjectReference(InputPosition location, ObjectReference inner, boolean fromHandler) {
		this.location = location;
		this.handler = inner.handler;
		this.fromHandler = fromHandler;
	}

	public InputPosition location() {
		return location;
	}

	@Override
	public NameOfThing myName() {
		return handler;
	}

	public String uniqueName() {
		return this.handler.uniqueName();
	}
	
	@Override
	public int compareTo(Object o) {
		return this.uniqueName().compareTo(((ExternalRef)o).uniqueName());
	}

	public boolean fromHandler() {
		return fromHandler;
	}
	
	@Override
	public String toString() {
		return this.handler.uniqueName();
	}
}
