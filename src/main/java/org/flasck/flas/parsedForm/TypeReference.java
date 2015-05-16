package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class TypeReference {
	public final String name;
	public final List<Object> args = new ArrayList<Object>();

	public TypeReference(String name) {
		this.name = name;
	}
}
