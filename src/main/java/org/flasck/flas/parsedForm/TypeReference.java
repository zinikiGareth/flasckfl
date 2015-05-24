package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class TypeReference {
	public final String name;
	public final List<Object> args = new ArrayList<Object>();

	public TypeReference(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append(name);
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
}
