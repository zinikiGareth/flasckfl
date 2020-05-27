package org.flasck.flas.parsedForm.ut;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.parsedForm.ObjectMessagesHolder;

public class GuardedMessages extends ObjectMessagesHolder implements Locatable {
	private final InputPosition location;
	public final Expr guard;

	public GuardedMessages(InputPosition location, Expr guard) {
		this.location = location;
		this.guard = guard;
	}

	@Override
	public InputPosition location() {
		return location;
	}
}
