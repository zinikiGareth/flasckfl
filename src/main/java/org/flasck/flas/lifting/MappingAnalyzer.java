package org.flasck.flas.lifting;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.RepositoryEntry;

public class MappingAnalyzer {

	private final FunctionDefinition fn;
	private final MappingCollector collector;

	public MappingAnalyzer(FunctionDefinition fn, MappingCollector c) {
		this.fn = fn;
		this.collector = c;
	}

	public void visitFunctionIntro(FunctionIntro fi) {
		// TODO Auto-generated method stub
		
	}

	public void visitUnresolvedVar(UnresolvedVar vr) {
		RepositoryEntry defn = vr.defn();
		if (defn instanceof VarPattern) {
			VarPattern vp = (VarPattern) defn;
			if (vp.name().scope != fn.name())
				collector.recordNestedVar(fn, vp);
		} else if (defn instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern) defn;
			if (tp.name().scope != fn.name())
				collector.recordNestedVar(fn, tp);
		}
	}

}
