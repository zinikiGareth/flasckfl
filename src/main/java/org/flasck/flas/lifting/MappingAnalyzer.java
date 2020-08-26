package org.flasck.flas.lifting;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.LogicHolder;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.RepositoryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class MappingAnalyzer {
	public static final Logger logger = LoggerFactory.getLogger("Lifter");
	private final TypeBinder fn;
	private final MappingCollector collector;
	private final VarDependencyMapper dependencies;
	private FunctionName name;
	private FunctionIntro fi;
	private ObjectActionHandler meth;

	public MappingAnalyzer(TypeBinder fn, MappingCollector c, VarDependencyMapper dependencies) {
		this.fn = fn;
		this.collector = c;
		this.dependencies = dependencies;
	}

	public void visitFunctionIntro(FunctionIntro fi) {
		this.fi = fi;
		name = fi.name();
	}

	public void visitObjectMethod(ObjectActionHandler meth) {
		this.meth = meth;
		name = meth.name();
	}

	public void visitUnresolvedVar(UnresolvedVar vr) {
		RepositoryEntry defn = vr.defn();
		if (defn == null)
			throw new CantHappenException("should have a definition by this point");
		visitDefn(defn);
	}

	public void visitDefn(RepositoryEntry defn) {
		if (defn instanceof VarPattern) {
			VarPattern vp = (VarPattern) defn;
			if (vp.name().scope != name) {
				collector.recordNestedVar(fi, meth, vp);
				logger.debug("  uses var " + vp.name());
				dependencies.recordVarDependency(name, (FunctionName)vp.name().scope, collector);
				collector.recordDependency(vp.definedBy());
			}
		} else if (defn instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern) defn;
			if (tp.name().scope != name) {
				collector.recordNestedVar(fi, meth, tp);
				if (tp.definedBy() == null) {
					throw new CantHappenException("cannot depend on " + tp.var + " because its definedBy function has not been set");
				}
				logger.debug("  uses var " + tp.name());
				collector.recordDependency(tp.definedBy());
			}
		} else if (defn instanceof LogicHolder) {
			if (defn != fn)
				collector.recordDependency((LogicHolder) defn);
		} else if (defn instanceof HandlerImplements) {
//			throw new NotImplementedException("Cannot handle dependency on " + defn);
		}
	}
	

}
