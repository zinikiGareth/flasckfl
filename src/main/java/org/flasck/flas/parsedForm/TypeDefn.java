package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class TypeDefn implements Serializable, Locatable {
	public final transient boolean generate;
	public final InputPosition location;
	public final TypeReference defining;
	public final List<TypeReference> cases = new ArrayList<TypeReference>();

	public TypeDefn(InputPosition location, boolean generate, TypeReference defining) {
		this.location = location;
		this.generate = generate;
		this.defining = defining;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}

	public TypeDefn addCase(TypeReference tr) {
		this.cases.add(tr);
		return this;
	}
	
	@Override
	public String toString() {
		return defining.toString();
	}
}
