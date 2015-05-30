package org.flasck.flas.typechecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeDefn;
import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.ClosureCmd;
import org.flasck.flas.vcode.hsieForm.ErrorCmd;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Head;
import org.flasck.flas.vcode.hsieForm.IFCmd;
import org.flasck.flas.vcode.hsieForm.PushCmd;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.ReturnCmd;
import org.flasck.flas.vcode.hsieForm.Switch;
import org.flasck.flas.vcode.hsieForm.Var;
import org.zinutils.exceptions.UtilException;

public class TypeChecker {
	public final ErrorResult errors = new ErrorResult();
	private final VariableFactory factory = new VariableFactory();
	final Map<String, Type> knowledge = new HashMap<String, Type>();
	final Map<String, StructDefn> structs = new HashMap<String, StructDefn>();
	final Map<String, TypeDefn> types = new HashMap<String, TypeDefn>();

	public TypeChecker() {
	}

	public void addStructDefn(StructDefn structDefn) {
		structs.put(structDefn.typename, structDefn);
	}

	public void addTypeDefn(TypeDefn typeDefn) {
		types.put(typeDefn.defining.name, typeDefn);
	}

	public void addExternal(String name, Type type) {
		knowledge.put(name, type);
	}

	public void typecheck(Set<HSIEForm> functionsToCheck) {
		TypeEnvironment gamma = new TypeEnvironment(); // should be based on everything we already know
		PhiSolution phi = new PhiSolution(errors);
		// Before we begin, we want to define "local knowledge" with all the "alleged" types of the things we're defining
		// so that they can be accessed recursively
		Map<String, Object> localKnowledge = new HashMap<String, Object>();
		Map<String, HSIEForm> rewritten = new HashMap<String, HSIEForm>();
		int from = 201; // the actual number doesn't matter but might make debugging easier
		for (HSIEForm hsie : functionsToCheck) {
			localKnowledge.put(hsie.fnName, factory.next());
			System.out.println("Allocating tv " + localKnowledge.get(hsie.fnName) + " for " + hsie.fnName);
			rewritten.put(hsie.fnName, rewriteWithFreshVars(hsie, from));
			from += hsie.vars.size();
		}
		Map<String, Object> actualTypes = new HashMap<String, Object>();
		for (HSIEForm hsie : rewritten.values()) {
			Object te = checkHSIE(localKnowledge, phi, gamma, hsie);
			if (te == null)
				return;
			actualTypes.put(hsie.fnName, te);
		}
		for (HSIEForm f : rewritten.values()) {
			Object rwt = phi.unify(localKnowledge.get(f.fnName), actualTypes.get(f.fnName));
			actualTypes.put(f.fnName, phi.subst(rwt)); 
		}
		for (HSIEForm f : rewritten.values()) {
			TypeExpr subst = (TypeExpr) phi.subst(actualTypes.get(f.fnName));
			knowledge.put(f.fnName, subst.asType(this));
		}
	}

	private HSIEForm rewriteWithFreshVars(HSIEForm hsie, int from) {
		Map<Var, Var> mapping = new HashMap<Var, Var>();
		List<Var> vars = new ArrayList<Var>();
		for (Var v : hsie.vars) {
			Var newVar = new Var(from++);
			mapping.put(v, newVar);
			vars.add(newVar);
		}
		HSIEForm ret = new HSIEForm(hsie.fnName, hsie.nformal, vars, hsie.externals);
		mapBlock(ret, hsie, mapping);
		for (HSIEBlock b : hsie.closures()) {
			HSIEBlock closure = ret.closure(mapping.get(((ClosureCmd)b).var));
			mapBlock(closure, b, mapping);
		}
		ret.dump();
		return ret;
	}

	private void mapBlock(HSIEBlock ret, HSIEBlock hsie, Map<Var, Var> mapping) {
		for (HSIEBlock b : hsie.nestedCommands()) {
			if (b instanceof Head) {
				ret.head(mapping.get(((Head)b).v));
			} else if (b instanceof Switch) {
				Switch sc = (Switch)b;
				HSIEBlock ib = ret.switchCmd(mapping.get(sc.var), sc.ctor);
				mapBlock(ib, sc, mapping);
			} else if (b instanceof IFCmd) {
				IFCmd ic = (IFCmd) b;
				HSIEBlock ib = ret.ifCmd(mapping.get(ic.var), ic.value);
				mapBlock(ib, ic, mapping);
			} else if (b instanceof BindCmd) {
				BindCmd bc = (BindCmd) b;
				ret.bindCmd(mapping.get(bc.bind), mapping.get(bc.from), bc.field);
			} else if (b instanceof ReturnCmd) {
				ReturnCmd rc = (ReturnCmd)b;
				List<Var> deps = rewriteList(mapping, rc.deps);
				if (rc.var != null)
					ret.doReturn(mapping.get(rc.var), deps);
				else if (rc.ival != null)
					ret.doReturn(rc.ival, deps);
				else if (rc.fn != null)
					ret.doReturn(rc.fn, deps);
				else
					throw new UtilException("Unhandled");
			} else if (b instanceof PushCmd) {
				PushCmd pc = (PushCmd) b;
				if (pc.var != null)
					ret.push(mapping.get(pc.var));
				else if (pc.ival != null)
					ret.push(pc.ival);
				else if (pc.fn != null)
					ret.push(pc.fn);
				else
					throw new UtilException("Unhandled");
			} else if (b instanceof ErrorCmd)
				ret.caseError();
			else
				throw new UtilException("Unhandled " + b.getClass());
		}
	}

	private List<Var> rewriteList(Map<Var, Var> mapping, List<Var> deps) {
		if (deps == null)
			return null;
		List<Var> ret = new ArrayList<Var>();
		for (Var var : deps)
			ret.add(mapping.get(var));
		return ret;
	}

	Object checkHSIE(Map<String, Object> localKnowledge, PhiSolution phi, TypeEnvironment gamma, HSIEForm hsie) {
		List<TypeVar> vars = new ArrayList<TypeVar>();
		for (int i=0;i<hsie.nformal;i++) {
			TypeVar tv = factory.next();
			System.out.println("Allocating " + tv + " for " + hsie.fnName + " arg " + i);
			gamma = gamma.bind(hsie.vars.get(i), new TypeScheme(null, tv));
			vars.add(tv);
		}
		System.out.println(gamma);
		// what we need to do is to apply tcExpr to the right hand side with the new gamma
		Object rhs = checkBlock(new SFTypes(null), localKnowledge, phi, gamma, hsie, hsie);
		if (rhs == null)
			return null;
		// then we need to build an expr tv0 -> tv1 -> tv2 -> E with all the vars substituted
		for (int i=vars.size()-1;i>=0;i--)
			rhs = new TypeExpr("->", phi.meaning(vars.get(i)), rhs);
		return rhs;
	}

	private Object checkBlock(SFTypes sft, Map<String, Object> localKnowledge, PhiSolution phi, TypeEnvironment gamma, HSIEForm form, HSIEBlock hsie) {
		List<Object> returns = new ArrayList<Object>();
		for (HSIEBlock o : hsie.nestedCommands()) {
			if (o instanceof ReturnCmd) {
				System.out.println("Checking expr " + o);
				Object ret = checkExpr(localKnowledge, phi, gamma, form, o);
				System.out.println("Checked expr " + o + " as " + ret);
				return ret;
			}
			else if (o instanceof Head)
				;
			else if (o instanceof Switch) {
				Switch s = (Switch) o;
				StructDefn sd = structs.get(s.ctor);
				if (sd == null) {
					errors.message((Block)null, "there is no definition for struct " + s.ctor);
					return null;
				}
				TypeScheme valueOf = gamma.valueOf(s.var);
				System.out.println(valueOf);
				List<Object> targs = new ArrayList<Object>();
				Map<String, TypeVar> polys = new HashMap<String, TypeVar>();
				// we need a complex map of form var -> ctor -> field -> type
				// and type needs to be cunningly constructed from TypeReference
				for (String x : sd.args) {
					TypeVar tv = factory.next();
					targs.add(tv);
					polys.put(x, tv);
				}
				System.out.println(polys);
				SFTypes inner = new SFTypes(sft);
				for (StructField x : sd.fields) {
					System.out.println("field " + x.name + " has " + x.type);
					Object fr = TypeExpr.fromReference(x.type, polys);
					inner.put(s.var, x.name, fr);
					System.out.println(fr);
				}
				phi.unify(valueOf.typeExpr, new TypeExpr(s.ctor, targs));
				returns.add(checkBlock(inner, localKnowledge, phi, gamma, form, s));
			} else if (o instanceof IFCmd) {
				IFCmd ic = (IFCmd) o;
				// Since we have to have done a SWITCH before we get here, this gives us no new information
				returns.add(checkBlock(sft, localKnowledge, phi, gamma, form, ic));
			} else if (o instanceof BindCmd) {
				BindCmd bc = (BindCmd) o;
				TypeVar tv = factory.next();
				phi.unify(tv, sft.get(bc.from, bc.field));
				System.out.println("binding " + bc.bind + " to " + tv);
				gamma = gamma.bind(bc.bind, new TypeScheme(null, tv));
			} else if (o instanceof ErrorCmd) {
				// nothing really to do here ...
			} else
				throw new UtilException("Missing cases " + o.getClass());
		}
		Object t1 = returns.get(0);
		for (int i=1;i<returns.size();i++) {
			if (t1 == null || returns.get(i) == null)
				return null;
			System.out.println("Attempting to unify return values " + t1 + " and " + returns.get(i));
			t1 = phi.unify(t1, returns.get(i));
		}
		if (t1 == null)
			return null;
		return phi.subst(t1);
	}

	Object checkExpr(Map<String, Object> localKnowledge, PhiSolution phi, TypeEnvironment gamma, HSIEForm form, HSIEBlock cmd) {
		if (cmd instanceof PushReturn) {
			PushReturn r = (PushReturn) cmd;
			if (r.ival != null)
				return new TypeExpr("Number");
			else if (r.var != null) {
				HSIEBlock c = form.getClosure(r.var);
				if (c == null) {
					// phi is not updated
					// assume it must be a bound var; we will fail to get the existing type scheme if not
					TypeScheme old = gamma.valueOf(r.var);
					PhiSolution temp = new PhiSolution(errors);
					for (TypeVar tv : old.schematicVars) {
						temp.bind(tv, factory.next());
						System.out.println("Allocating tv " + temp.meaning(tv) + " for " + tv + " when instantiating typescheme");
					}
					return temp.subst(old.typeExpr);
				} else {
					// c is a closure, which must be a function application
					List<Object> args = new ArrayList<Object>();
					for (HSIEBlock b : c.nestedCommands()) {
						Object te = checkExpr(localKnowledge, phi, gamma, form, b);
						if (te == null)
							return null;
						args.add(te);
					}
					Object Tf = args.get(0);
					for (int i=1;i<args.size();i++)
						Tf = checkSingleApplication(phi, Tf, args.get(i));
					return Tf;
				}
			} else if (r.fn != null) {
				// phi is not updated
				// I am going to say that by getting here, we know that it must be an external
				// all lambdas should be variables by now
				Object te = localKnowledge.get(r.fn);
				if (te != null)
					return te;
				te = knowledge.get(r.fn);
				if (te == null) {
					// This is probably a failure on our part rather than user error
					// We should not be able to get here if r.fn is not already an external which has been resolved
					errors.message((Block)null, "There is no type for " + r.fn); // We need some way to report error location
					return null;
				} else {
					System.out.print("Replacing vars in " + r.fn +": ");
					return freshVarsIn(te);
				}
			} else
				throw new UtilException("What are you returning?");
		} else
			throw new UtilException("Missing cases");
	}

	private Object checkSingleApplication(PhiSolution phi, Object Tf, Object Tx) {
		TypeVar Tr = factory.next();
		System.out.println("Allocating " + Tr + " for new application of " + Tf + " to " + Tx);
		TypeExpr Tf2 = new TypeExpr("->", Tx, Tr);
		phi.unify(Tf, Tf2);
		if (errors.hasErrors())
			return null;
		return phi.meaning(Tr);
	}

	private Object freshVarsIn(Object te) {
		if (te instanceof Type) {
			Object ret = ((Type)te).asExpr(factory);
			System.out.println(ret);
			return ret;
		}
		Set<TypeVar> vs = new HashSet<TypeVar>();
		findVarsIn(vs, te);
		Map<TypeVar, TypeVar> map = new HashMap<TypeVar, TypeVar>();
		for (TypeVar tv : vs)
			map.put(tv, factory.next());
		System.out.println(map);
		return substVars(map, te);
	}

	private void findVarsIn(Set<TypeVar> vs, Object te) {
		if (te instanceof TypeVar)
			vs.add((TypeVar) te);
		else if (te instanceof TypeExpr) {
			TypeExpr te2 = (TypeExpr) te;
			for (Object o : te2.args)
				findVarsIn(vs, o);
		} else
			throw new UtilException("case not handled " + te.getClass());
	}

	private Object substVars(Map<TypeVar, TypeVar> map, Object te) {
		if (te instanceof TypeVar)
			return map.get(te);
		else {
			TypeExpr te2 = (TypeExpr) te;
			List<Object> newArgs = new ArrayList<Object>();
			for (Object o : te2.args)
				newArgs.add(substVars(map, o));
			return new TypeExpr(te2.type, newArgs);
		}
	}

}
