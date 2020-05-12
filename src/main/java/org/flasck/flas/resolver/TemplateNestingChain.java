package org.flasck.flas.resolver;

import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.RepositoryEntry;

public class TemplateNestingChain implements NestingChain {
	private final StructDefn ty;

	public TemplateNestingChain(StructDefn ty) {
		this.ty = ty;
	}

	@Override
	public RepositoryEntry resolve(UnresolvedVar var) {
		return ty.findField(var.var);
	}

}
