package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.collections.CollectionUtils;

public class FunctionTypeReference extends TypeReference {
	public final List<TypeReference> args;

	public FunctionTypeReference(InputPosition location, TypeReference... args) {
		this(location, CollectionUtils.listOf(args));
	}

	public FunctionTypeReference(InputPosition location, List<TypeReference> args) {
		super(location, "->", (List<TypeReference>)null);
		this.args = args;
	}

	@Override
	public String toString() {
		return "->" + args;
	}
}
