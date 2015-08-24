package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.exceptions.UtilException;

public class HandlerLambda implements ExternalRef {
	public final InputPosition location;
	public final String hi;
	public final String var;

	public HandlerLambda(InputPosition location, String hi, String var) {
		this.location = location;
		this.hi = hi;
		this.var = var;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public String uniqueName() {
		return hi + "." + var;
	}
	
	@Override
	public int compareTo(Object o) {
		return this.toString().compareTo(o.toString());
	}
	
	public boolean fromHandler() {
		throw new UtilException("This is not available");
	}

	@Override
	public String toString() {
		return "HL[" + uniqueName() + "]";
	}
}
