package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class ObjectDefn implements AsString, Serializable, Locatable {
	private final InputPosition location;
	public final String typename;
	public final List<String> args = new ArrayList<String>();
	public final List<ObjectMethod> methods = new ArrayList<ObjectMethod>();
	public final transient boolean generate;

	public ObjectDefn(InputPosition location, String tn, boolean generate) {
		this.location = location;
		this.typename = tn;
		this.generate = generate;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public ObjectDefn add(String ta) {
		args.add(ta);
		return this;
	}
	
	public ObjectDefn addMethod(ObjectMethod m) {
		methods.add(m);
		return this;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(typename);
		if (!args.isEmpty()) {
			sb.append(args);
		}
		return sb.toString();
	}

	public String asString() {
		StringBuilder sb = new StringBuilder(typename);
		if (!args.isEmpty()) {
			sb.append(args);
		}
		return sb.toString();
	}
}
