package org.flasck.flas.droidgen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.UtilException;

public class VarHolder {
	final Map<String, Var> svars = new HashMap<String, Var>();
	final Map<org.flasck.flas.vcode.hsieForm.Var, Var> vars = new HashMap<org.flasck.flas.vcode.hsieForm.Var, Var>();

	// Useful for tests, but not much else ...
	public VarHolder() {
	}

	public VarHolder(HSIEForm form, List<PendingVar> pendingVars) {
		int j = 0;
		for (ScopedVar s : form.scoped) {
			svars.put(s.uniqueName(), pendingVars.get(j).getVar());
			j++;
		}
		for (int i=0;i<form.nformal;i++)
			vars.put(form.vars.get(i), pendingVars.get(i+j).getVar());
	}
	
	public boolean has(org.flasck.flas.vcode.hsieForm.Var var) {
		return vars.containsKey(var);
	}

	public Var get(org.flasck.flas.vcode.hsieForm.Var v) {
		if (!vars.containsKey(v))
			throw new UtilException("Do not have the variable " + v);
		return vars.get(v);
	}

	public void put(org.flasck.flas.vcode.hsieForm.Var var, Var hv) {
		vars.put(var, hv);
	}

	public Var getScoped(String svar) {
		if (!svars.containsKey(svar))
			throw new UtilException("ScopedVar not in scope: " + svar);
		return svars.get(svar);
	}

}
