package org.flasck.flas.hsie;

import java.util.HashMap;
import java.util.Map;

import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Var;
import org.flasck.flas.vcode.hsieForm.VarInSource;
import org.zinutils.exceptions.UtilException;

public class CurrentFunction {
	public final HSIEForm form;
	public final Map<String, VarInSource> substs = new HashMap<String, VarInSource>();
	public final Branching branching;
	public final Expressions expressions;

	public CurrentFunction(HSIEForm form) {
		this.form = form;
		this.branching = new Branching(this);
		this.expressions = new Expressions(form);
	}
	
	public Var allocateVar() {
		return form.allocateVar();
	}
	
	public void subst(String varToSubst, VarInSource var) {
		if (substs.containsKey(varToSubst))
			throw new HSIEException(var.loc, "duplicate var in patterns: " + varToSubst);
		Branching.logger.info("Defining " + varToSubst + " as " + var);
		substs.put(varToSubst, var);
	}

	public void mapVar(String id, VarInSource cov) {
		substs.put(id, cov);
	}

	public Object getSubst(String uniqueName) {
		if (!substs.containsKey(uniqueName))
			throw new UtilException("There is no var for " + uniqueName);
		return substs.get(uniqueName);
	}
}
