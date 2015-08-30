package org.flasck.flas.vcode.hsieForm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.hsie.SubstExpr;
import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.ExternalRef;
import org.slf4j.Logger;
import org.zinutils.exceptions.UtilException;

// So, basically an HSIE definition consists of
// Fn "name" [formal-args] [bound-vars] [external-vars]
//   HEAD var
//   SWITCH var Type/Constructor|Type|Type/Constructor
//     BIND new-var var "field"
//     IF boolean-expr
//       EVAL En
//   Er
// If there is no general case, then add "E?" to indicate an error in switching

// There is no notion of "Else", you just drop down to the next statement at a not-indented level and pick up from there.

// Each of the Expressions En is modified to be just a simple apply-tree
public class HSIEForm extends HSIEBlock {
	public enum CodeType {
		FUNCTION, CARD, CONTRACT, SERVICE, HANDLER, EVENTHANDLER
	}

	public final CodeType mytype;
	public final String fnName;
	public final int alreadyUsed;
	public final int nformal;
	public final List<Var> vars = new ArrayList<Var>();
	public final Set<Object> externals = new TreeSet<Object>();
	private final Map<Var, HSIEBlock> closures = new HashMap<Var, HSIEBlock>();
	public final List<SubstExpr> exprs = new ArrayList<SubstExpr>();

	// This constructor is the one for real code
	public HSIEForm(CodeType mytype, String name, int alreadyUsed, Map<String, CreationOfVar> map, int nformal) {
		if (mytype == null) throw new UtilException("Null mytype");
		this.mytype = mytype;
		this.fnName = name;
		this.alreadyUsed = alreadyUsed;
		for (int i=0;i<alreadyUsed;i++)
			vars.add(null);
		for (CreationOfVar v : map.values())
			vars.set(v.var.idx, v.var);
		this.nformal = nformal;
	}

	// This is the copy/rewrite constructor
	public HSIEForm(CodeType mytype, String name, int alreadyUsed, int nformal, List<Var> vars, Collection<Object> externals) {
		if (mytype == null) throw new UtilException("Null mytype");
		this.mytype = mytype;
		this.fnName = name;
		this.alreadyUsed = alreadyUsed;
		this.nformal = nformal;
		this.vars.addAll(vars);
		this.externals.addAll(externals);
	}

	// This constructor is for testing
	public HSIEForm(CodeType mytype, String name, int alreadyUsed, int nformal, int nbound, Collection<String> dependsOn) {
		if (mytype == null) throw new UtilException("Null mytype");
		this.mytype = mytype;
		fnName = name;
		this.alreadyUsed = alreadyUsed;
		this.nformal = nformal;
		for (int i=0;i<alreadyUsed;i++)
			vars.add(new Var(i));
		for (int i=0;i<nformal;i++)
			vars.add(new Var(alreadyUsed + i));
		for (int i=0;i<nbound;i++)
			vars.add(new Var(alreadyUsed + nformal + i));
		for (String s : dependsOn)
			this.externals.add(new AbsoluteVar(null, s, null));
	}

	public Var var(int v) {
		return vars.get(v);
	}

	public Var allocateVar() {
		Var ret = new Var(vars.size());
//		System.out.println("Allocating var " + ret);
		vars.add(ret);
		return ret;
	}

	public HSIEBlock closure(Var var) {
		ClosureCmd ret = new ClosureCmd(var);
		closures.put(var, ret);
		return ret;
	}

	public HSIEBlock getClosure(Var v) {
		return closures.get(v);
	}

	public void dump(Logger logTo) {
		if (logTo == null)
			logTo = logger;
		logTo.info("#Args: " + nformal + " #bound: " + (vars.size()-nformal) + " externals: " + externals);
		logTo.info("Vars = " + vars);
		dump(logTo, 0);
		for (HSIEBlock c : closures.values())
			c.dumpOne(logTo, 0);
	}

	public void dependsOn(Object ref) {
		if (!(ref instanceof ExternalRef))
			throw new UtilException("Cannot pass in a string var: " + ref);
		String name = ((ExternalRef)ref).uniqueName();
		if (!name.equals(this.fnName))
			externals.add(ref);
	}

	public Collection<HSIEBlock> closures() {
		return closures.values();
	}

	// Get the vars which are bound in for ExprN
	public Map<String, CreationOfVar> varsFor(int eN) {
		return exprs.get(eN).substs;
	}

	@Override
	public String toString() {
		return fnName + "/" + nformal;
	}

	public boolean isMethod() {
		return mytype == CodeType.CARD || mytype == CodeType.CONTRACT || mytype == CodeType.SERVICE || mytype == CodeType.HANDLER || mytype == CodeType.EVENTHANDLER;
	}
}
