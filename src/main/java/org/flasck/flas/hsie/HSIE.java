package org.flasck.flas.hsie;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.utils.StringComparator;

public class HSIE {
	static Logger logger = LoggerFactory.getLogger("HSIE");
	private final Map<String, HSIEForm> forms = new TreeMap<String, HSIEForm>(new StringComparator());

	public void createForms(Set<RWFunctionDefinition> d) {
		VarFactory vf = new NextVarFactory();
		GatherExternals ge = new GatherExternals();
		for (RWFunctionDefinition fn : d) {
			HSIEForm hf = new HSIEForm(fn.location, fn.fnName, fn.nargs(), fn.mytype, fn.inCard, vf);
			for (ScopedVar sv : fn.scopedVars) {
				if (sv.definedBy.equals(fn.fnName))
					hf.scopedDefinitions.add(sv);
				else
					hf.scoped.add(sv);
			}
			ge.process(hf, fn);
			forms.put(fn.fnName.uniqueName(), hf);
		}
	}

	// The process of "lambda lifting" means that we need to add additional lambda entries to each
	// form where it is automatically included in the parent.
	// We do this by allocating "scopedVars", which for any given form come directly from the function it
	// is mapping: so far, so good.
	// But here we need to look at where those lambdas are coming from and tell that form that it needs to
	// generate the appropriate closure for this form
	
	// Having done all this, I think it duplicates the over-eager definition in the creator above
	public void liftLambdas() {
		for (HSIEForm h : forms.values()) {
			for (ScopedVar sv : h.scoped) {
				if (!sv.definedBy.equals(h.funcName))
					forms.get(sv.definedBy.uniqueName()).scopedDefinitions.add(sv);
			}
		}
	}
	
	public Set<HSIEForm> orchard(Set<RWFunctionDefinition> d) {
		TreeMap<String, HSIEForm> ret = new TreeMap<String, HSIEForm>();
		for (RWFunctionDefinition fn : d) {
			ret.put(fn.fnName.uniqueName(), forms.get(fn.fnName.uniqueName()));
		}
//		GatherExternals.transitiveClosure(forms, ret.values());
		logger.info("HSIE transforming orchard in parallel: " + d);
		for (RWFunctionDefinition t : d) {
			handle(t);
		}
		return new TreeSet<HSIEForm>(ret.values());
	}

	private void handle(RWFunctionDefinition defn) {
	}

	public Collection<HSIEForm> allForms() {
		return forms.values();
	}

	public HSIEForm getForm(String name) {
		return forms.get(name);
	}
}
