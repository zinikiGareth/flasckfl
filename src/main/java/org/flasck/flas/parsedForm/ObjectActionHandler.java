package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parser.MethodMessagesConsumer;

public class ObjectActionHandler implements Locatable, MethodMessagesConsumer {
	private final InputPosition location;
	private final FunctionName name;
	private final List<Pattern> args;
	private final List<ActionMessage> messages = new ArrayList<>();

	public ObjectActionHandler(InputPosition location, FunctionName name, List<Pattern> args) {
		this.location = location;
		this.name = name;
		this.args = args;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}
	
	public FunctionName name() {
		return name;
	}

	public List<Pattern> args() {
		return args;
	}

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
}