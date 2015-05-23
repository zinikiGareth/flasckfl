package org.flasck.flas.parsedForm;

import java.util.List;

public class MethodMessage {
	public final List<String> slot;
	public final Object expr;

	public MethodMessage(List<String> slot, Object expr) {
		this.slot = slot;
		this.expr = expr;
	}
}
