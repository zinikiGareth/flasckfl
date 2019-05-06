package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.Pattern;

public class ObjectCtor implements Locatable/*, MessageMethodConsumer */{
	private final InputPosition location;
	private final String name;
	private final List<Pattern> args;

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
}
