package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parser.MethodMessagesConsumer;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;

public class ObjectActionHandler implements Locatable, MethodMessagesConsumer, RepositoryEntry {
	private final InputPosition location;
	private final FunctionName name;
	private final List<Pattern> args;
	private final List<ActionMessage> messages = new ArrayList<>();
	private Type type;

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

	public void bindType(Type ty) {
		if (this.type != null)
			throw new RuntimeException("Cannot bind type more than once");
		this.type = ty;
	}

	public Type type() {
		if (this.type == null)
			throw new RuntimeException("Type not bound");
		return this.type;
	}

	@Override
	public void sendMessage(SendMessage message) {
		messages.add(message);
	}

	@Override
	public void assignMessage(AssignMessage message) {
		messages.add(message);
	}

	@Override
	public void done() {
	}

	public List<ActionMessage> messages() {
		return messages;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("ObjectCtor[" + toString() + "]");
	}
}
