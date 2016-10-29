package org.flasck.flas.vcode.hsieForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.hsie.SubstExpr;
import org.flasck.flas.hsie.VarFactory;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.VarNestedFromOuterFunctionScope;
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
		FUNCTION,	// standalone, package-scoped function 
		CARD, 		// card-scoped function (method)
		CONTRACT,	// method on a contract impl
		SERVICE,	// method on a service impl
		HANDLER,	// method on a handler impl
		EVENTHANDLER, // an event handler on a card
		STANDALONE,	// how does this differ from FUNCTION?
		AREA;		// a method on an area

		public boolean isHandler() {
			return this == CONTRACT || this == SERVICE || this == HANDLER || this == AREA;
		}
	}

	public final CodeType mytype;
	public final String fnName;
	public final int nformal;
	public final List<Var> vars = new ArrayList<Var>();
	public final Set<Object> externals = new TreeSet<Object>();
	public final Set<String> scoped = new TreeSet<String>();
	private final Map<Var, ClosureCmd> closures = new HashMap<Var, ClosureCmd>();
	public final List<SubstExpr> exprs = new ArrayList<SubstExpr>();
	private /* final */ VarFactory vf;

	// This constructor is the one for real code
	public HSIEForm(VarFactory vf, CodeType mytype, String name, InputPosition nameLoc, Map<String, CreationOfVar> map, int nformal) {
		super(nameLoc);
		this.vf = vf;
		if (mytype == null) throw new UtilException("Null mytype");
		this.mytype = mytype;
		this.fnName = name;
		for (CreationOfVar v : map.values())
			vars.set(v.var.idx, v.var);
		this.nformal = nformal;
	}

	public Var var(int v) {
		for (Var vi : vars)
			if (vi.idx == v)
				return vi;
		throw new UtilException("There is no var with idx " + v);
	}

	public Var allocateVar() {
		Var ret = vf.nextVar();
//		System.out.println("Allocating var " + ret);
		vars.add(ret);
		return ret;
	}

	public ClosureCmd closure(Var var) {
		ClosureCmd ret = new ClosureCmd(null, var);
		closures.put(var, ret);
		return ret;
	}

	public ClosureCmd getClosure(Var v) {
		return closures.get(v);
	}

	public void dump(Logger logTo) {
		if (logTo == null)
			logTo = logger;
		logTo.debug("HSIE for " + fnName);
		logTo.debug("#Args: " + nformal + " #bound: " + (vars.size()-nformal));
		logTo.debug("    externals: " + externals + " scoped = " + scoped);
		logTo.debug("    all vars = " + vars);
		dump(logTo, 0);
		for (HSIEBlock c : closures.values())
			c.dumpOne(logTo, 0);
	}

	public void dump(PrintWriter pw) {
		pw.println("HSIE for " + fnName);
		pw.println("#Args: " + nformal + " #bound: " + (vars.size()-nformal));
		pw.println("    externals: " + externals + " scoped = " + scoped);
		pw.println("    all vars = " + vars);
		dump(pw, 0);
		for (HSIEBlock c : closures.values())
			c.dumpOne(pw, 0);
		pw.flush();
	}

	public void dependsOn(Object ref) {
		String name;
		if (ref instanceof String)
			name = (String) ref;
		else if (ref instanceof ExternalRef)
			name = ((ExternalRef)ref).uniqueName();
		else
			throw new UtilException("Cannot pass in: " + ref + " " + (ref!=null?ref.getClass():""));
		if (!name.equals(this.fnName)) {
			if (ref instanceof VarNestedFromOuterFunctionScope)
				scoped.add(name);
			else
				externals.add(name);
		}
	}

	public Collection<ClosureCmd> closures() {
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
