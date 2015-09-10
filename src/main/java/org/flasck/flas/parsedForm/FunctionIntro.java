package org.flasck.flas.parsedForm;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.ConstructorMatch.Field;
import org.flasck.flas.rewriter.ResolutionException;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewriter.Rewriter.NamingContext;
import org.flasck.flas.typechecker.Type;
import org.zinutils.exceptions.UtilException;

public class FunctionIntro {
	public final InputPosition location;
	public final String name;
	public final List<Object> args;

	public FunctionIntro(InputPosition location, String name, List<Object> args) {
		this.location = location;
		this.name = name;
		this.args = args;
	}
	
	public Map<String, LocalVar> allVars(ErrorResult errors, Rewriter rewriter, Rewriter.NamingContext cx, String definedBy) {
		Map<String, LocalVar> ret = new TreeMap<>();
		gatherVars(errors, rewriter, cx, definedBy, ret);
		return ret;
	}
	
	public void gatherVars(ErrorResult errors, Rewriter rewriter, Rewriter.NamingContext cx, String definedBy, Map<String, LocalVar> into) {
		for (int i=0;i<args.size();i++) {
			Object arg = args.get(i);
			if (arg instanceof VarPattern) {
				VarPattern vp = (VarPattern)arg;
				into.put(vp.var, new LocalVar(definedBy, vp.varLoc, vp.var, null, null));
			} else if (arg instanceof ConstructorMatch)
				gatherCtor(errors, cx, definedBy, into, (ConstructorMatch) arg);
			else if (arg instanceof ConstPattern)
				;
			else if (arg instanceof TypedPattern) {
				TypedPattern tp = (TypedPattern)arg;
				Type t = null;
				if (cx != null) { // the DependencyAnalyzer can pass in null for the NamingContext 'coz it only wants the var names
					try {
						t = rewriter.rewrite(cx, tp.type, false);
					} catch (ResolutionException ex) {
						throw new UtilException("Need to consider if " + tp.type + " might be a polymorphic var");
					}
				}
				into.put(tp.var, new LocalVar(definedBy, tp.varLocation, tp.var, tp.typeLocation, t));
			} else
				throw new UtilException("Not gathering vars from " + arg.getClass());
		}
	}

	private void gatherCtor(ErrorResult errors, NamingContext cx, String definedBy, Map<String, LocalVar> into, ConstructorMatch cm) {
		// NOTE: I am deliberately NOT returning any errors here because I figure this should already have been checked for validity somewhere else
		// But this (albeit, defensively) assumes that cm.ctor is a struct defn and that it has the defined fields 
		for (Field x : cm.args) {
			if (x.patt instanceof VarPattern) {
				VarPattern vp = (VarPattern)x.patt;
				// TODO: it should theoretically be possible to infer the type of this field by looking at the StructField associated with the StructDefn associated with cm.ctor, and we have a resolving context
				Type t = null;
				if (cx != null) {
					Object sd = cx.resolve(cm.location, cm.ctor);
					if (sd instanceof AbsoluteVar && ((AbsoluteVar)sd).defn instanceof StructDefn) {
						StructDefn sdf = (StructDefn) ((AbsoluteVar)sd).defn;
						StructField sf = sdf.findField(x.field);
						if (sf != null) {
							t = sf.type;
						}
					}
				}
				into.put(vp.var, new LocalVar(definedBy, vp.varLoc, vp.var, vp.varLoc, t));
			} else if (x.patt instanceof ConstructorMatch)
				gatherCtor(errors, cx, definedBy, into, (ConstructorMatch)x.patt);
			else if (x.patt instanceof ConstPattern)
				;
			else
				throw new UtilException("Not gathering vars from " + x.patt.getClass());
		}
	}
	
	@Override
	public String toString() {
		return "FI[" + name + "/" + args.size() + "]";
	}
}
