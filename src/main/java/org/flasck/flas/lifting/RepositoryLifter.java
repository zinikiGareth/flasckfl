package org.flasck.flas.lifting;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.Repository;

public class RepositoryLifter extends LeafAdapter implements Lifter {
	private MappingStore ms;
	private MappingAnalyzer ma;

	@Override
	public void lift(Repository r) {
		r.traverse(this);
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		ms = new MappingStore();
		ma = new MappingAnalyzer(fn, ms);
	}

	@Override
	public void visitFunctionIntro(FunctionIntro fi) {
		ma.visitFunctionIntro(fi);
	}

	@Override
	public void visitUnresolvedVar(UnresolvedVar vr, int nargs) {
		ma.visitUnresolvedVar(vr);
	}

	@Override
	public void leaveFunction(FunctionDefinition fn) {
		if (ms.isInteresting())
			fn.nestedVars(ms);
		ms = null;
	}
}
