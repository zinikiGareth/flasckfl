package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.SendMessage;

public interface MethodMessagesConsumer {
	void sendMessage(SendMessage message);
	void assignMessage(AssignMessage message);
}
