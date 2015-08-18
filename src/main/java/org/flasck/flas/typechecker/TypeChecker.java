package org.flasck.flas.typechecker;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.CardGrouping;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.rewriter.Rewriter;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;
import org.zinutils.graphs.Node;
import org.zinutils.graphs.Orchard;
import org.zinutils.graphs.Tree;

public class TypeChecker {
	public final static Logger logger = LoggerFactory.getLogger("TypeChecker");
	public final ErrorResult errors;
	private final VariableFactory factory = new VariableFactory();
	final Map<String, Type> knowledge = new TreeMap<String, Type>();
	final Map<String, StructDefn> structs = new TreeMap<String, StructDefn>();
	final Map<String, TypeDefn> types = new TreeMap<String, TypeDefn>();
	final Map<String, StructDefn> cards = new TreeMap<String, StructDefn>();
	// TODO: should have contracts
	
	public TypeChecker(ErrorResult errors) {
		this.errors = errors;
	}

	public void populateTypes(Rewriter rewriter) {
		for (Entry<String, StructDefn> d : rewriter.structs.entrySet())
			structs.put(d.getKey(), d.getValue());
//		System.out.println("structs: " + structs);
		for (Entry<String, TypeDefn> d : rewriter.types.entrySet())
			types.put(d.getKey(), d.getValue());
//		System.out.println("types: " + types);
		for (Entry<String, CardGrouping> d : rewriter.cards.entrySet())
			cards.put(d.getKey(), d.getValue().struct);
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

	public void typecheck(Orchard<HSIEForm> functionsToCheck) {
//		System.out.println("---- Starting to typecheck");
		TypeEnvironment gamma = new TypeEnvironment(); // should be based on everything we already know
		PhiSolution phi = new PhiSolution(errors);
		// First, rewrite all the equations to have the "correct" set of variables
		// Equations at the same level of the tree should have "fresh" variables; while
		// nested equations should share their parent's variables
		Map<String, Object> localKnowledge = new HashMap<String, Object>();
		Map<String, HSIEForm> rewritten = new HashMap<String, HSIEForm>();
		int from = 201; // the actual number doesn't matter but might make debugging easier
		Map<Var, Var> rwvars = new HashMap<Var, Var>();
//		System.out.println("Rewriting function tree");
		for (Tree<HSIEForm> tree : functionsToCheck)
			rewriteFunctionTree(tree, localKnowledge, rwvars, rewritten, from, tree.getRoot());
//		System.out.println("Finished rewriting");
		Map<String, Object> actualTypes = new HashMap<String, Object>();
		List<TypeVar> vars = new ArrayList<TypeVar>();
		for (HSIEForm hsie : rewritten.values()) {
//			System.out.println("Allocating type vars for " + hsie.fnName);
			localKnowledge.put(hsie.fnName, factory.next());
//			System.out.println("Allocated tv " + localKnowledge.get(hsie.fnName) + " for result of " + hsie.fnName);
			gamma = allocateTVs(vars, gamma, hsie);
		}
//		System.out.println("Allocated new type vars; checking forms");
		for (HSIEForm hsie : rewritten.values()) {
			Object te = checkHSIE(localKnowledge, phi, gamma, hsie);
			if (te == null)
				return;
			actualTypes.put(hsie.fnName, te);
		}
//		System.out.println("Checked forms: actualTypes = " + actualTypes);
//		System.out.println("Attempting to unify types");
		for (HSIEForm f : rewritten.values()) {
			Object rwt = phi.unify(localKnowledge.get(f.fnName), actualTypes.get(f.fnName));
			actualTypes.put(f.fnName, phi.subst(rwt)); 
		}
		phi.validateUnionTypes(this);
//		System.out.println("Done final unification; building types");
		for (HSIEForm f : rewritten.values()) {
			Object tmp = phi.subst(actualTypes.get(f.fnName));
			if (!(tmp instanceof TypeExpr)) {
				System.out.println("I truly believe tmp should be a TypeExpr, not " + tmp.getClass());
				continue;
			}
			TypeExpr subst = (TypeExpr) tmp;
//			System.out.println("Storing type knowledge about " + f.fnName);
			knowledge.put(f.fnName, subst.asType(this));
//			System.out.println(f.fnName + " :: " + knowledge.get(f.fnName));
		}
//		System.out.println("---- Done with typecheck");
	}

	private void rewriteFunctionTree(Tree<HSIEForm> functionsToCheck, Map<String, Object> localKnowledge, Map<Var, Var> rwvars, Map<String, HSIEForm> rewritten, int from, Node<HSIEForm> nh) {
		HSIEForm hsie = nh.getEntry();
		Map<Var, Var> rwvars2 = new HashMap<Var, Var>(rwvars);
		rewritten.put(hsie.fnName, rewriteWithFreshVars(rwvars2, hsie, from));
		from += hsie.vars.size();
		for (Node<HSIEForm> c : functionsToCheck.getChildren(nh))
			rewriteFunctionTree(functionsToCheck, localKnowledge, rwvars2, rewritten, from, c);
	}

	private HSIEForm rewriteWithFreshVars(Map<Var, Var> mapping, HSIEForm hsie, int from) {
		List<Var> vars = new ArrayList<Var>();
		for (Var v : hsie.vars) {
			if (!mapping.containsKey(v)) {
				Var newVar = new Var(from++);
				mapping.put(v, newVar);
			}
			vars.add(mapping.get(v));
		}
		HSIEForm ret = new HSIEForm(hsie.mytype, hsie.fnName, hsie.alreadyUsed, hsie.nformal, vars, hsie.externals);
		mapBlock(ret, hsie, mapping);
		for (HSIEBlock b : hsie.closures()) {
			HSIEBlock closure = ret.closure(mapping.get(((ClosureCmd)b).var));
			mapBlock(closure, b, mapping);
		}
//		ret.dump();
		return ret;
	}

	private void mapBlock(HSIEBlock ret, HSIEBlock hsie, Map<Var, Var> mapping) {
		for (HSIEBlock b : hsie.nestedCommands()) {
			if (b instanceof Head) {
				ret.head(mapping.get(((Head)b).v));
			} else if (b instanceof Switch) {
				Switch sc = (Switch)b;
				HSIEBlock ib = ret.switchCmd(sc.location, mapping.get(sc.var), sc.ctor);
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
					ret.doReturn(rc.location, mapping.get(rc.var), deps);
				else if (rc.ival != null)
					ret.doReturn(rc.location, rc.ival, deps);
				else if (rc.sval != null)
					ret.doReturn(rc.location, rc.sval, deps);
				else if (rc.fn != null)
					ret.doReturn(rc.location, rc.fn, deps);
				else if (rc.tlv != null)
					ret.doReturn(rc.location, rc.tlv, deps);
				else
					throw new UtilException("Unhandled");
			} else if (b instanceof PushCmd) {
				PushCmd pc = (PushCmd) b;
				if (pc.var != null)
					ret.push(pc.location, mapping.get(pc.var));
				else if (pc.ival != null)
					ret.push(pc.location, pc.ival);
				else if (pc.fn != null)
					ret.push(pc.location, pc.fn);
				else if (pc.sval != null)
					ret.push(pc.location, pc.sval);
				else if (pc.tlv != null)
					ret.push(pc.location, pc.tlv);
				else if (pc.func != null)
					ret.push(pc.location, pc.func);
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

	TypeEnvironment allocateTVs(List<TypeVar> vars, TypeEnvironment gamma, HSIEForm hsie) {
//		for (int i=0;i<hsie.alreadyUsed;i++)
//			throw new UtilException("Need to make sure these are reused from existing parent, even after renaming");
		for (int i=0;i<hsie.nformal;i++) {
			TypeVar tv = factory.next();
//			System.out.println("Allocating " + tv + " for " + hsie.fnName + " arg " + i + " var " + (i+hsie.alreadyUsed));
			gamma = gamma.bind(hsie.vars.get(i+hsie.alreadyUsed), new TypeScheme(null, tv));
			vars.add(tv);
		}
//		System.out.println(gamma);
		return gamma;
	}

	Object checkHSIE(Map<String, Object> localKnowledge, PhiSolution phi, TypeEnvironment gamma, HSIEForm hsie) {
		logger.info("Checking " + hsie.fnName + " with " + hsie.nformal + " args");
		// what we need to do is to apply tcExpr to the right hand side with the new gamma
		Object rhs = checkBlock(new SFTypes(null), localKnowledge, phi, gamma, hsie, hsie);
		if (rhs == null)
			return null;
		// then we need to build an expr tv0 -> tv1 -> tv2 -> E with all the vars substituted
		for (int i=hsie.nformal-1;i>=0;i--) {
			Var myarg = hsie.vars.get(hsie.alreadyUsed+i);
			Object tv = gamma.valueOf(myarg).typeExpr;
			rhs = new TypeExpr("->", phi.subst(tv), rhs);
		}
		return rhs;
	}

	private Object checkBlock(SFTypes sft, Map<String, Object> localKnowledge, PhiSolution phi, TypeEnvironment gamma, HSIEForm form, HSIEBlock hsie) {
		List<Object> returns = new ArrayList<Object>();
		for (HSIEBlock o : hsie.nestedCommands()) {
			if (o instanceof ReturnCmd) {
//				System.out.println("Checking expr " + o);
				Object ret = checkExpr(localKnowledge, phi, gamma, form, o);
//				System.out.println("Checked expr " + o + " as " + ret);
//				logger.info(o.toString() + " checked as " + ret);
				return ret;
			}
			else if (o instanceof Head)
				;
			else if (o instanceof Switch) {
				Switch s = (Switch) o;
				TypeScheme valueOf = gamma.valueOf(s.var);
				if (s.ctor.equals("Number") || s.ctor.equals("Boolean") || s.ctor.equals("String")) {
					phi.unify(valueOf.typeExpr, new TypeExpr(s.ctor));
					returns.add(checkBlock(sft, localKnowledge, phi, gamma, form, s));
					logger.info(o.toString() + " links " + s.var + " to " + s.ctor);
				} else {
					StructDefn sd = structs.get(s.ctor);
					if (sd == null) {
						errors.message(s.location, "there is no definition for struct " + s.ctor);
						return null;
					}
	//				System.out.println(valueOf);
					Map<String, TypeVar> polys = new HashMap<String, TypeVar>();
					// we need a complex map of form var -> ctor -> field -> type
					// and type needs to be cunningly constructed from TypeReference
					List<Object> targs = new ArrayList<Object>();
					for (String x : sd.args) {
						TypeVar tv = factory.next();
						targs.add(tv);
						polys.put(x, tv);
					}
	//				System.out.println(polys);
					SFTypes inner = new SFTypes(sft);
					for (StructField x : sd.fields) {
//						System.out.println("field " + x.name + " has " + x.type);
						Object fr = TypeExpr.fromReference(x.type, polys);
						inner.put(s.var, x.name, fr);
//						System.out.println(fr);
					}
					phi.unify(valueOf.typeExpr, new TypeExpr(s.ctor, targs));
					returns.add(checkBlock(inner, localKnowledge, phi, gamma, form, s));
				}
			} else if (o instanceof IFCmd) {
				IFCmd ic = (IFCmd) o;
				if (ic.value == null) { // closure case
					logger.info(o.toString() + " forces " + ic.var + " to be boolean");
					checkClosure(localKnowledge, phi, gamma, form, form.getClosure(ic.var));
				}
				// Since we have to have done a SWITCH before we get here, this gives us no new information
				returns.add(checkBlock(sft, localKnowledge, phi, gamma, form, ic));
			} else if (o instanceof BindCmd) {
				BindCmd bc = (BindCmd) o;
				TypeVar tv = factory.next();
				phi.unify(tv, sft.get(bc.from, bc.field));
//				System.out.println("binding " + bc.bind + " to " + tv);
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
//			System.out.println("Attempting to unify return values " + t1 + " and " + returns.get(i));
			t1 = phi.unify(t1, returns.get(i));
		}
		if (t1 == null)
			return null;
		return phi.subst(t1);
	}

	Object checkExpr(Map<String, Object> localKnowledge, PhiSolution phi, TypeEnvironment gamma, HSIEForm form, HSIEBlock cmd) {
		if (cmd instanceof PushReturn) {
			PushReturn r = (PushReturn) cmd;
			if (r.ival != null) {
				logger.info(r.toString() + " is of type Number");
				return new TypeExpr("Number");
			} else if (r.sval != null) {
				logger.info(r.toString() + " is of type String");
				return new TypeExpr("String");
			} else if (r.tlv != null) {
				// I don't think it's quite as simple as this ... I think we need to introduce it in one place and return it in another or something
				return factory.next();
			} else if (r.var != null) {
				HSIEBlock c = form.getClosure(r.var);
				if (c == null) {
					// phi is not updated
					// assume it must be a bound var; we will fail to get the existing type scheme if not
					TypeScheme old = gamma.valueOf(r.var);
					PhiSolution temp = new PhiSolution(errors);
					for (TypeVar tv : old.schematicVars) {
						temp.bind(tv, factory.next());
//						System.out.println("Allocating tv " + temp.meaning(tv) + " for " + tv + " when instantiating typescheme");
					}
					return temp.subst(old.typeExpr);
				} else {
					// c is a closure, which must be a function application
					return checkClosure(localKnowledge, phi, gamma, form, c);
				}
			} else if (r.fn != null) {
				// phi is not updated
				// I am going to say that by getting here, we know that it must be an external
				// all lambdas should be variables by now
				
				if (r.fn.uniqueName().equals("FLEval.tuple")) {
					return "()";
				}
				if (r.fn instanceof CardMember) {
					CardMember cm = (CardMember) r.fn;
					// try and find the name of the card class
					String structName = cm.card; // form.fnName; //.substring(0, idx);
					if (r.fn.equals("_card"))
						return freshVarsIn(new TypeReference(cm.location, structName, null));
					StructDefn sd = cards.get(structName);
					if (sd == null)
						throw new UtilException("There was no struct definition called " + structName);
					for (StructField sf : sd.fields) {
						if (sf.name.equals(cm.var)) {
							return freshVarsIn(sf.type);
						}
					}
					errors.message(cm.location, "there is no field " + cm.var + " in card " + structName);
					return null;
				} else if (r.fn instanceof HandlerLambda) {
					HandlerLambda hl = (HandlerLambda) r.fn;
					// try and find the name of the handler class
					// this is likewise a hack and I know it ...
//					int idx = form.fnName.length();
//					for (int i=0;i<2;i++)
//						idx = form.fnName.lastIndexOf('.', idx-1);
					String structName = hl.hi; // form.fnName.substring(0, idx);
					if (r.fn.equals("_handler"))
						return freshVarsIn(new TypeReference(hl.location, structName, null));
					StructDefn sd = structs.get(structName);
					for (StructField sf : sd.fields) {
						if (sf.name.equals(hl.var)) {
							return freshVarsIn(sf.type);
						}
					}
					throw new UtilException("Could not find field " + hl.var + " in handler " + structName);
				}
				
				String name = r.fn.uniqueName();
				Object te = localKnowledge.get(name);
				if (te != null)
					return te;
				te = knowledge.get(name);
				if (te == null) {
					if (cards.containsKey(name))
						return new TypeExpr(name);
					if (structs.containsKey(name))
						return freshVarsIn(typeForStructCtor(null, structs.get(name)));
					// This is probably a failure on our part rather than user error
					// We should not be able to get here if r.fn is not already an external which has been resolved
					for (Entry<String, Type> x : knowledge.entrySet())
						System.out.println(x.getKey() + " => " + x.getValue());
					errors.message(r.location, "There is no type for identifier: " + r.fn + " when checking " + form.fnName);
					return null;
				} else {
//					System.out.print("Replacing vars in " + r.fn +": ");
					return freshVarsIn(te);
				}
			} else if (r.func != null) {
				return new TypeExpr("FunctionLiteral");
			} else
				throw new UtilException("What are you returning?");
		} else
			throw new UtilException("Missing cases");
	}

	private Object checkClosure(Map<String, Object> localKnowledge, PhiSolution phi, TypeEnvironment gamma, HSIEForm form, HSIEBlock c) {
		List<Object> args = new ArrayList<Object>();
		for (HSIEBlock b : c.nestedCommands()) {
			Object te = checkExpr(localKnowledge, phi, gamma, form, b);
			if (te == null)
				return null;
			args.add(te);
		}
		Object Tf = args.get(0);
		if ("()".equals(Tf)) { // tuples need special handling
			List<Object> newVars = new ArrayList<Object>();
			for (int i=1;i<args.size();i++)
				newVars.add(factory.next());
			Tf = new TypeExpr("()", newVars);
			logger.info("Closure " + c + " has type " + Tf);
		} else {
			logger.info("Closure " + c + " requires " + flatten(new StringBuilder(), Tf, false) + " to apply to " + arrowify(args));
			for (int i=1;i<args.size();i++)
				Tf = checkSingleApplication(phi, Tf, args.get(i));
		}
		return Tf;
	}

	private String flatten(StringBuilder sb, Object tf, boolean needParens) {
//		StringBuilder sb = new StringBuilder();
		if (tf instanceof TypeExpr) {
			TypeExpr ae = (TypeExpr) tf;
			if (ae.type.equals("->")) {
				if (needParens)
					sb.append("(");
				flatten(sb, ae.args.get(0), true);
				sb.append("->");
				flatten(sb, ae.args.get(1), false);
				if (needParens)
					sb.append(")");
				return sb.toString();
			}
		}
		sb.append(tf);
		return sb.toString();
	}

	private String arrowify(List<Object> args) {
		StringBuilder sb = new StringBuilder("(");
		for (int i=1;i<args.size();i++) {
			if (i > 1)
				sb.append(",");
			sb.append(args.get(i));
		}
		sb.append(")");
		return sb.toString();
	}

	private Type typeForStructCtor(InputPosition location, StructDefn structDefn) {
		List<Type> args = new ArrayList<Type>();
		for (StructField x : structDefn.fields)
			args.add(fromTypeReference(x.type));
		args.add(fromTypeReference(new TypeReference(location, structDefn.typename, null)));
		return Type.function(args);
	}

	private Type typeForCardCtor(InputPosition location, StructDefn structDefn) {
		List<Type> args = new ArrayList<Type>();
		args.add(fromTypeReference(new TypeReference(location, "_Wrapper", null)));
		args.add(fromTypeReference(new TypeReference(location, structDefn.typename, null)));
		return Type.function(args);
	}

	private Type fromTypeReference(TypeReference type) {
		if (type.var != null)
			return Type.polyvar(type.var);
		List<Type> args = new ArrayList<Type>();
		for (TypeReference tr : type.args)
			args.add(fromTypeReference(tr));
		return Type.simple(type.name, args);
	}

	private Object checkSingleApplication(PhiSolution phi, Object Tf, Object Tx) {
		TypeVar Tr = factory.next();
//		System.out.println("Allocating " + Tr + " for new application of " + Tf + " to " + Tx);
		TypeExpr Tf2 = new TypeExpr("->", Tx, Tr);
		phi.unify(Tf, Tf2);
		if (errors.hasErrors())
			return null;
		return phi.meaning(Tr);
	}

	private Object freshVarsIn(Object te) {
		if (te instanceof TypeReference)
			te = fromTypeReference((TypeReference) te);
		if (te instanceof Type) {
			Object ret = ((Type)te).asExpr(factory);
			return ret;
		}
		Set<TypeVar> vs = new HashSet<TypeVar>();
		findVarsIn(vs, te);
		Map<TypeVar, TypeVar> map = new HashMap<TypeVar, TypeVar>();
		for (TypeVar tv : vs)
			map.put(tv, factory.next());
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

	public Type getTypeDefn(String fn) {
		if (knowledge.containsKey(fn))
			return knowledge.get(fn);
		if (structs.containsKey(fn))
			return typeForStructCtor(null, structs.get(fn));
		if (cards.containsKey(fn))
			return typeForCardCtor(null, cards.get(fn));
//		System.out.println(knowledge);
		throw new UtilException("There is no type: " + fn);
	}

	public void writeLearnedKnowledge(OutputStream wex) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(wex);
		List<StructDefn> str = new ArrayList<StructDefn>();
		for (StructDefn sd : structs.values()) {
			if (sd.generate) {
				str.add(sd);
			}
		}
		oos.writeObject(str);
		List<TypeDefn> ts = new ArrayList<TypeDefn>();
		for (TypeDefn td : types.values()) {
			if (td.generate) {
				ts.add(td);
			}
		}
		oos.writeObject(ts);
		
		// TODO: should write contracts
		oos.flush();
	}
}
