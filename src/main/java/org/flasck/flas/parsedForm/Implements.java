package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class Implements {
	public final String type;
	public final List<MethodDefinition> methods = new ArrayList<MethodDefinition>();

	public Implements(String type) {
		this.type = type;
	}

	public void addMethod(MethodDefinition meth) {
		methods.add(meth);
	}

}
