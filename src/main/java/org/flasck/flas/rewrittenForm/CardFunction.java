package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;

public class CardFunction implements ExternalRef {
	public final InputPosition location;
	public final String clzName;
	public final String function;
	public final boolean fromHandler;

	public CardFunction(InputPosition location, String clzName, String function) {
		this.location = location;
		this.clzName = clzName;
		this.function = function;
		this.fromHandler = false;
	}
	
	public CardFunction(InputPosition location, CardFunction inner, boolean fromHandler) {
		this.location = location;
		this.clzName = inner.clzName;
		this.function = inner.function;
		this.fromHandler = fromHandler;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public String uniqueName() {
		return this.clzName + "." + this.function;
	}

	public boolean fromHandler() {
		return fromHandler;
	}
	
	@Override
	public String toString() {
		return this.clzName + "." + this.function;
	}

	@Override
	public int compareTo(Object o) {
		return this.uniqueName().compareTo(((ExternalRef)o).uniqueName());
	}
}
