package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.LocatedToken;

public class MethodMessage {
	public final List<LocatedToken> slot;
	public final Object expr;

	public MethodMessage(List<LocatedToken> slot, Object expr) {
		this.slot = slot;
		this.expr = expr;
	}
}
