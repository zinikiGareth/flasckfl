package org.flasck.flas.lifting;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.RepositoryEntry;

public class MappingAnalyzer {
	private final TypeBinder fn;
	private final MappingCollector collector;
	private final VarDependencyMapper dependencies;
	private FunctionName name;
	private FunctionIntro fi;

	public MappingAnalyzer(TypeBinder fn, MappingCollector c, VarDependencyMapper dependencies) {
		this.fn = fn;
		this.collector = c;
		this.dependencies = dependencies;
	}

	public void visitFunctionIntro(FunctionIntro fi) {
		this.fi = fi;
		name = fi.name();
	}

	public void visitUnresolvedVar(UnresolvedVar vr) {
		RepositoryEntry defn = vr.defn();
		if (defn instanceof VarPattern) {
			VarPattern vp = (VarPattern) defn;
			if (vp.name().scope != name) {
				collector.recordNestedVar(fi, vp);
				dependencies.recordVarDependency(name, (FunctionName)vp.name().scope, collector);
			}
		} else if (defn instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern) defn;
			if (tp.name().scope != name) {
				collector.recordNestedVar(fi, tp);
			}
		} else if (defn instanceof FunctionDefinition) {
			if (defn != fn)
				collector.recordDependency((FunctionDefinition) defn);
		} else if (defn instanceof StandaloneMethod) {
			if (defn != fn)
				collector.recordDependency((StandaloneMethod) defn);
		}
	}

}
