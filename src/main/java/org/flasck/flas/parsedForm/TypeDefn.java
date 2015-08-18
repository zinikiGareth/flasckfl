package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class TypeDefn implements Serializable {
	public final transient boolean generate;
	public final TypeReference defining;
	public final List<TypeReference> cases = new ArrayList<TypeReference>();

	public TypeDefn(boolean generate, TypeReference defining) {
		this.generate = generate;
		this.defining = defining;
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
