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
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.hsie.VarFactory;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWMethodDefinition;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.types.Type;
import org.slf4j.Logger;
import org.zinutils.collections.SetMap;
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
public class HSIEForm extends HSIEBlock implements Comparable<HSIEForm> {
	public enum CodeType {
		FUNCTION,	// standalone, package-scoped function 
		CARD, 		// card-scoped function (method)
		DECL,		// method on a contract declaration (cannot be generated)
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

	public final CardName inCardName;
	public final FunctionName funcName;
	public final int nformal;
	public final CodeType mytype;
	private final VarFactory vf;
	public final List<Var> vars = new ArrayList<Var>();
	private final Map<Var, ClosureCmd> closures = new HashMap<Var, ClosureCmd>();
	
	// This is a set of vars which are defined in our nested scope that we actually use
	public final Set<ScopedVar> scopedDefinitions = new TreeSet<ScopedVar>();
	
	// The names of things outside of us that we reference
	public final Set<String> externals = new TreeSet<String>();
	
	// Variables defined in an enclosing scope which we reference
	public final Set<ScopedVar> scoped = new TreeSet<ScopedVar>();
	public final SetMap<VarInSource, Type> varConstraints = new SetMap<>();

	public HSIEForm(InputPosition nameLoc, FunctionName name, int nformal, CodeType mytype, CardName inCard, VarFactory vf) {
		super(nameLoc);
		this.inCardName = inCard;
		this.vf = vf;
		if (mytype == null) throw new UtilException("Null mytype");
		this.mytype = mytype;
		this.funcName = name;
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
		vars.add(ret);
		return ret;
	}

	public ClosureCmd createClosure(InputPosition loc) {
		Var var = allocateVar();
		ClosureCmd ret = new ClosureCmd(loc, var);
		closures.put(var, ret);
		return ret;
	}

	// This is still annoyingly used by tests, in "thingy()"
	@Deprecated
	public ClosureCmd closure(InputPosition loc, Var var) {
		ClosureCmd ret = new ClosureCmd(loc, var);
		closures.put(var, ret);
		return ret;
	}

	public ClosureCmd getClosure(Var v) {
		return closures.get(v);
	}

	public void dump(Logger logTo) {
		if (logTo == null)
			logTo = logger;
		logTo.info(asString(0));
		logTo.info("#Args: " + nformal + " #bound: " + (vars.size()-nformal));
		logTo.info("    externals: " + externals);
		logTo.info("    scoped = " + justNames(scoped));
		logTo.info("    scopedDefns = " + justNames(scopedDefinitions));
		logTo.info("    all vars = " + vars);
		dump(logTo, 0);
		for (HSIEBlock c : closures.values())
			c.dumpOne(logTo, 0);
	}

	public void dump(PrintWriter pw) {
		pw.println(asString(0));
		pw.println("#Args: " + nformal + " #bound: " + (vars.size()-nformal));
		pw.println("    externals: " + externals);
		pw.println("    scoped = " + justNames(scoped));
		pw.println("    scopedDefns = " + justNames(scopedDefinitions));
		pw.println("    all vars = " + vars);
		dump(pw, 0);
		for (HSIEBlock c : closures.values())
			c.dumpOne(pw, 0);
		pw.flush();
	}

	private <T extends ExternalRef> Set<String> justNames(Set<T> list) {
		Set<String> ret = new TreeSet<>();
		for (T er : list)
			ret.add(er.uniqueName());
		return ret;
	}

	public void definesScoped(ScopedVar vn) {
		scopedDefinitions.add(vn);
	}

	public boolean dependsOn(Object ref) {
		String name;
		if (ref instanceof String)
			name = (String) ref;
		else if (ref instanceof ExternalRef)
			name = ((ExternalRef)ref).uniqueName();
		else
			throw new UtilException("Cannot pass in: " + ref + " " + (ref!=null?ref.getClass():""));
		if (name.equals(this.funcName.uniqueName()))
			return false; // we don't reference ourselves and this is not new

		if (ref instanceof ScopedVar) {
			ScopedVar vn = (ScopedVar) ref;
			// Try and eliminate the case where we have our own local vars, masquerading as scoped vars by other people ...
			if (isDefinedByMe(vn))
				return false;
			return scoped.add((ScopedVar)ref);
		} else
			return externals.add(name);
	}

	public boolean isDefinedByMe(ScopedVar vn) {
		if (scopedDefinitions.contains(vn))
			return true;
		if (vn.defn instanceof LocalVar) {
			LocalVar lv = (LocalVar) vn.defn;
			int idx = lv.caseName.jsName().lastIndexOf("_"); // TODO: refactor this ...
			if (funcName.jsName().indexOf(".", idx) == -1)
				return true;
		} else if (vn.defn instanceof RWFunctionDefinition) {
		} else if (vn.defn instanceof RWHandlerImplements) {
			RWHandlerImplements hi = (RWHandlerImplements) vn.defn;
			int idx = hi.hiName.lastIndexOf("_");
			if (funcName.jsName().indexOf(".", idx) == -1)
				return true;
		} else if (vn.defn instanceof RWMethodDefinition) {
		} else { // if (vn.defn instanceof HandlerLambda) {
			throw new UtilException("Unexpected class for vn defn: " + vn.defn.getClass());
		}

		return false;
	}

	public Collection<ClosureCmd> closures() {
		return closures.values();
	}

	@Override
	public String toString() {
		return "HSIE for " + funcName.jsName() + "/" + nformal;
	}

	public boolean isMethod() {
		return mytype == CodeType.CARD || mytype == CodeType.CONTRACT || mytype == CodeType.SERVICE || mytype == CodeType.HANDLER || mytype == CodeType.EVENTHANDLER || mytype == CodeType.AREA;
	}

	@Override
	public int compareTo(HSIEForm o) {
		return this.funcName.jsName().compareTo(o.funcName.jsName());
	}
}
