package org.flasck.flas.vcode.hsieForm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.hsie.SubstExpr;

public class HSIEForm extends HSIEBlock {
	public final String fnName;
	public final int alreadyUsed;
	public final int nformal;
	public final List<Var> vars = new ArrayList<Var>();
	public final Set<String> externals = new TreeSet<String>();
	private final Map<Var, HSIEBlock> closures = new HashMap<Var, HSIEBlock>();
	public final List<SubstExpr> exprs = new ArrayList<SubstExpr>();

	// This constructor is the one for real code
	public HSIEForm(String name, int alreadyUsed, Map<String, Var> map, int nformal) {
		this.fnName = name;
		this.alreadyUsed = alreadyUsed;
		for (int i=0;i<alreadyUsed;i++)
			vars.add(null);
		for (Var v : map.values())
			vars.set(v.idx, v);
		this.nformal = nformal;
	}

	// This is the copy/rewrite constructor
	public HSIEForm(String name, int alreadyUsed, int nformal, List<Var> vars, Collection<String> externals) {
		this.fnName = name;
		this.alreadyUsed = alreadyUsed;
		this.nformal = nformal;
		this.vars.addAll(vars);
		this.externals.addAll(externals);
	}

	// This constructor is for testing
	public HSIEForm(String name, int alreadyUsed, int nformal, int nbound, Collection<String> dependsOn) {
		fnName = name;
		this.alreadyUsed = alreadyUsed;
		this.nformal = nformal;
		for (int i=0;i<alreadyUsed;i++)
			vars.add(new Var(i));
		for (int i=0;i<nformal;i++)
			vars.add(new Var(alreadyUsed + i));
		for (int i=0;i<nbound;i++)
			vars.add(new Var(alreadyUsed + nformal + i));
		this.externals.addAll(dependsOn);
	}

	public Var var(int v) {
		return vars.get(v);
	}

	public HSIEBlock closure(Var var) {
		ClosureCmd ret = new ClosureCmd(var);
		closures.put(var, ret);
		return ret;
	}

	public HSIEBlock getClosure(Var v) {
		return closures.get(v);
	}

	public void dump() {
		System.out.println("#Args: " + nformal + " #bound: " + (vars.size()-nformal) + " externals: " + externals);
		System.out.println("Vars = " + vars);
		dump(0);
		for (HSIEBlock c : closures.values())
			c.dumpOne(0);
	}

	public void dependsOn(String text) {
		if (!text.equals(this.fnName))
			externals.add(text);
	}

	public Collection<HSIEBlock> closures() {
		return closures.values();
	}

	// Get the vars which are bound in for ExprN
	public Map<String, Var> varsFor(int eN) {
		return exprs.get(eN).substs;
	}
	
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

}
