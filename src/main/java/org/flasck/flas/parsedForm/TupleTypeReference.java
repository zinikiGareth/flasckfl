package org.flasck.flas.parsedForm;

import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class TupleTypeReference extends TypeReference {
	public final List<TypeReference> members;

	public TupleTypeReference(InputPosition location, TypeReference... members) {
		this(location, Arrays.asList(members));
	}

	public TupleTypeReference(InputPosition location, List<TypeReference> members) {
		super(location, "()", (List<TypeReference>)null);
		this.members = members;
	}

	@Override
	public String toString() {
		return "()" + members;
	}
}
