package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parser.MethodMessagesConsumer;

public abstract class ObjectMessagesHolder implements MethodMessagesConsumer {

	protected final List<ActionMessage> messages = new ArrayList<>();

	@Override
	public void sendMessage(SendMessage message) {
		messages.add(message);
	}

	@Override
	public void assignMessage(AssignMessage message) {
		messages.add(message);
	}

	public List<ActionMessage> messages() {
		return messages;
	}

	@Override
	public void done() {
	}

}
