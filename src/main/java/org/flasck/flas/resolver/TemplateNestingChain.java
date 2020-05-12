package org.flasck.flas.resolver;

import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TemplateNestedField;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;

public class TemplateNestingChain implements NestingChain {
	private final StructDefn ty;

	public TemplateNestingChain(StructDefn ty) {
		this.ty = ty;
	}

	@Override
	public NamedType type() {
		return ty;
	}
	
	@Override
	public RepositoryEntry resolve(UnresolvedVar var) {
		StructField field = ty.findField(var.var);
		if (field == null)
			return null;
		return new TemplateNestedField(var.location, "_expr1", field);
	}

}
