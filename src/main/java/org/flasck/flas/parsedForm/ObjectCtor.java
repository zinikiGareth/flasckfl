package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.parser.MethodMessagesConsumer;

public class ObjectCtor implements Locatable, MethodMessagesConsumer{
	private final InputPosition location;
	private final String name;
	private final List<Pattern> args;
	private final List<ActionMessage> messages = new ArrayList<>();

	public ObjectCtor(InputPosition location, String name, List<Pattern> args) {
		this.location = location;
		this.name = name;
		this.args = args;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}
	
	public String name() {
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
}
