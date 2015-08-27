package org.flasck.flas.parsedForm;

import java.util.List;

public class MethodMessage {
	public final List<Locatable> slot;
	public final Object expr;

	public MethodMessage(List<Locatable> slot, Object expr) {
		this.slot = slot;
		this.expr = expr;
	}
}
