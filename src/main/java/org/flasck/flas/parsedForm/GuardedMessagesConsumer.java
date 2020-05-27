package org.flasck.flas.parsedForm;

import org.flasck.flas.parsedForm.ut.GuardedMessages;

public interface GuardedMessagesConsumer {
	void guard(GuardedMessages gm);
}
