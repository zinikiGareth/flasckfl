package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class TypeReference implements Serializable {
	public final String var;
	public final String name;
	public final transient InputPosition location;
	public final List<TypeReference> args = new ArrayList<TypeReference>();

	public TypeReference(InputPosition location, String name, String var) {
		this.location = location;
		this.name = name;
		this.var = var;
	}
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append(name != null ? name : var);
		if (!args.isEmpty()) {
			String sep = "";
			ret.append("[");
			for (Object o : args) {
				ret.append(sep);
				sep = ",";
				ret.append(o.toString());
			}
			ret.append("]");
		}
		return ret.toString();
	}

	public TypeReference with(TypeReference arg) {
		args.add(arg);
		return this;
	}
}
