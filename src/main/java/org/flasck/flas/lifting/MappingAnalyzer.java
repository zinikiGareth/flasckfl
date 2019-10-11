package org.flasck.flas.lifting;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.RepositoryEntry;

public class MappingAnalyzer {

	private final FunctionDefinition fn;
	private final MappingCollector collector;
	// TODO: this should be the "case" name with _1 or whatever
	private FunctionName name;
	private FunctionIntro fi;

	public MappingAnalyzer(FunctionDefinition fn, MappingCollector c) {
		this.fn = fn;
		this.collector = c;
	}

	public void visitFunctionIntro(FunctionIntro fi) {
		this.fi = fi;
		name = fi.name();
	}

	public void visitUnresolvedVar(UnresolvedVar vr) {
		RepositoryEntry defn = vr.defn();
		if (defn instanceof VarPattern) {
			VarPattern vp = (VarPattern) defn;
			if (vp.name().scope != name)
				collector.recordNestedVar(fi, vp);
		} else if (defn instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern) defn;
			if (tp.name().scope != name)
				collector.recordNestedVar(fi, tp);
		} else if (defn instanceof FunctionDefinition) {
			if (defn != fn)
				collector.recordDependency((FunctionDefinition) defn);
		}
	}

}
