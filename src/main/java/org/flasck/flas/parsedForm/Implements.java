package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class Implements {
	public final InputPosition typeLocation;
	public final String type;
	public final List<MethodDefinition> methods = new ArrayList<MethodDefinition>();

	public Implements(InputPosition location, String type) {
		this.typeLocation = location;
		this.type = type;
	}

	public void addMethod(MethodDefinition meth) {
		methods.add(meth);
	}

}
