package org.flasck.flas.typechecker;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.AsString;
import org.flasck.flas.parsedForm.CardGrouping;
import org.flasck.flas.parsedForm.CardGrouping.ContractGrouping;
import org.flasck.flas.parsedForm.CardGrouping.HandlerGrouping;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.LocalVar;
import org.flasck.flas.parsedForm.MethodInContext;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.ScopedVar;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.ClosureCmd;
import org.flasck.flas.vcode.hsieForm.CreationOfVar;
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
import org.zinutils.utils.Justification;
import org.zinutils.utils.StringComparator;

public class TypeChecker {
	public final static Logger logger = LoggerFactory.getLogger("TypeChecker");
	public final ErrorResult errors;
	private final VariableFactory factory = new VariableFactory();
	final Map<String, Type> knowledge = new TreeMap<String, Type>();
	final Map<String, StructDefn> structs = new TreeMap<String, StructDefn>();
	final Map<String, ObjectDefn> objects = new TreeMap<String, ObjectDefn>();
	final Map<String, UnionTypeDefn> types = new TreeMap<String, UnionTypeDefn>();
	final Map<String, ContractDecl> contracts = new TreeMap<String, ContractDecl>(new StringComparator());
	final Map<String, HandlerImplements> handlers = new TreeMap<String, HandlerImplements>();
	final Map<String, CardTypeInfo> cards = new TreeMap<String, CardTypeInfo>();
	final Map<String, TypeHolder> prefixes = new TreeMap<String, TypeHolder>(new StringComparator());
	
	public TypeChecker(ErrorResult errors) {
		this.errors = errors;
	}

	public void populateTypes(Rewriter rewriter) {
		for (Entry<String, StructDefn> d : rewriter.structs.entrySet())
			structs.put(d.getKey(), d.getValue());
//		System.out.println("structs: " + structs);
		for (Entry<String, UnionTypeDefn> d : rewriter.types.entrySet())
			types.put(d.getKey(), d.getValue());
//		System.out.println("types: " + types);
		for (Entry<String, ContractDecl> d : rewriter.contracts.entrySet())
			contracts.put(d.getKey(), d.getValue());
		for (Entry<String, CardGrouping> d : rewriter.cards.entrySet()) {
			CardTypeInfo cti = new CardTypeInfo(d.getValue());
			cards.put(d.getKey(), cti);
			prefixes.put(d.getKey(), cti);
			for (ContractGrouping x : d.getValue().contracts) {
				TypeHolder ctr = new TypeHolder(x.type);
				cti.contracts.add(ctr);
				prefixes.put(x.implName, ctr); // new ContractTypeInfo(cti); cti.addContract(that);
			}
			for (HandlerGrouping x : d.getValue().handlers) {
				TypeHolder ctr = new TypeHolder(x.type);
				cti.handlers.add(ctr);
//				handlers.put(ctr.name, x.impl);
				prefixes.put(x.type, ctr); // new ContractTypeInfo(cti); cti.addContract(that);
			}
		}
		for (Entry<String, HandlerImplements> x : rewriter.callbackHandlers.entrySet())
			handlers.put(x.getKey(), x.getValue());
		for (MethodInContext m : rewriter.standalone.values()) {
			List<Type> args = new ArrayList<Type>();
			// find the arg types, as claimed
			for (Object x : m.method.intro.args) {
				if (x instanceof TypedPattern)
					args.add(((TypedPattern)x).type);
				else if (x instanceof VarPattern)
					args.add(types.get("Any"));
				else
					throw new UtilException("Cannot handle " + x.getClass());
			}
			// by definition, these must return a "Message"
			args.add(types.get("Message"));
			knowledge.put(m.name, Type.function(m.contractLocation, args));
		}
	}

	public void addStructDefn(StructDefn structDefn) {
		structs.put(structDefn.name(), structDefn);
	}

	public void addObjectDefn(ObjectDefn objDefn) {
		objects.put(objDefn.name(), objDefn);
	}

	public void addTypeDefn(UnionTypeDefn typeDefn) {
		types.put(typeDefn.name(), typeDefn);
	}

	public void addExternalCard(CardTypeInfo val) {
		cards.put(val.name, val);
	}

	public void addExternal(String name, Type type) {
		if (type == null)
			throw new UtilException("Don't give me null types");
		if (type instanceof StructDefn || type instanceof UnionTypeDefn || type instanceof ObjectDefn)
			throw new UtilException("Not just a type ... call special thing");
		knowledge.put(name, type);
	}
	
	public Type checkExpr(HSIEForm expr, List<Type> args, List<InputPosition> locs) {
		TypeState s = new TypeState(errors, this);
		expr.dump(logger);
		Map<String, HSIEForm> forms = new TreeMap<String, HSIEForm>();
		forms.put(expr.fnName, expr);
		
		s.localKnowledge.put(expr.fnName, factory.next());
		for (int i=0;i<expr.nformal;i++) {
//			System.out.println("Allocating " + tv + " for " + hsie.fnName + " arg " + i + " var " + (i+hsie.alreadyUsed));
			s.gamma = s.gamma.bind(expr.vars.get(i+expr.alreadyUsed), new TypeScheme(null, args.get(i).asExpr(new GarneredFrom(locs.get(i)), this, factory)));
		}
				
		int inErrors = errors.count();
		Map<String, Object> typeinfo = checkAndUnify(s, forms);
		if (errors.count() > inErrors)
			return null;
		if (!typeinfo.containsKey(expr.fnName))
			throw new UtilException("Did not record a type for " + expr.fnName);
		Object tmp = s.phi.subst(typeinfo.get(expr.fnName));
		if (tmp == null)
			return null;
		if (!(tmp instanceof TypeExpr)) {
			System.out.println("I truly believe tmp should be a TypeExpr, not " + tmp.getClass());
			return null;
		}
		return ((TypeExpr)tmp).asType(this);
	}

	public void typecheck(Orchard<HSIEForm> functionsToCheck) {
		int mark = errors.count();
//		System.out.println("---- Starting to typecheck");
		TypeState s = new TypeState(errors, this);
		Map<String, HSIEForm> rewritten = rewriteForms(s, functionsToCheck);
//		System.out.println("Allocated new type vars; checking forms");
		Map<String, Object> actualTypes = checkAndUnify(s, rewritten);
//		System.out.println("Done final unification; building types");
		if (errors.moreErrors(mark))
			return;
		for (HSIEForm f : rewritten.values()) {
			Object tmp = s.phi.subst(actualTypes.get(f.fnName));
			if (tmp == null) {
				System.out.println("Encountered a null during typechecking; this is probably bad");
				continue;
			}
			if (!(tmp instanceof TypeExpr)) {
				System.out.println("I truly believe tmp should be a TypeExpr, not " + tmp.getClass());
				continue;
			}
			TypeExpr subst = (TypeExpr) tmp;
//			System.out.println("Storing type knowledge about " + f.fnName);
			Type mytype = subst.asType(this);
			knowledge.put(f.fnName, mytype);
			int idx = f.fnName.lastIndexOf(".");
			if (idx == -1) {
				System.out.println("This may or may not be possible for function name '" + f.fnName + "'");
				continue;
			}
			String pfx = f.fnName.substring(0, idx);
			TypeHolder found = prefixes.get(pfx);
			if (found == null)
				System.out.println("Didn't find anything that could hold " + f.fnName + " (this is currently true of package-level functions, etc)");
			else
				found.add(f.fnName.substring(idx+1), mytype);
//			System.out.println(f.fnName + " :: " + mytype);
		}
//		System.out.println("---- Done with typecheck");
	}

	protected Map<String, HSIEForm> rewriteForms(TypeState s, Orchard<HSIEForm> functionsToCheck) {
		// First, rewrite all the equations to have the "correct" set of variables
		// Equations at the same level of the tree should have "fresh" variables; while
		// nested equations should share their parent's variables
		Map<String, HSIEForm> rewritten = new HashMap<String, HSIEForm>();
		int from = 201; // the actual number doesn't matter but something big makes it different from the pre-rewritten numbers
		Map<Var, Var> rwvars = new HashMap<Var, Var>();
//		System.out.println("Rewriting function tree");
		for (Tree<HSIEForm> tree : functionsToCheck)
			from = rewriteFunctionTree(tree, s.localKnowledge, rwvars, rewritten, from, tree.getRoot());
//		System.out.println("Finished rewriting");
		allocateVars(s, rewritten);
		for (Entry<String, HSIEForm> x : rewritten.entrySet()) {
			logger.debug(x.getKey() + ":");
			x.getValue().dump(logger);
		}
		return rewritten;
	}

	protected void allocateVars(TypeState s, Map<String, HSIEForm> forms) {
		List<TypeVar> vars = new ArrayList<TypeVar>();
		for (HSIEForm hsie : forms.values()) {
//			System.out.println("Allocating type vars for " + hsie.fnName);
			s.localKnowledge.put(hsie.fnName, factory.next());
//			System.out.println("Allocated tv " + localKnowledge.get(hsie.fnName) + " for result of " + hsie.fnName);
			allocateTVs(vars, s, hsie);
		}
	}

	protected Map<String, Object> checkAndUnify(TypeState s, Map<String, HSIEForm> forms) {
		int mark = errors.count();
		Map<String, Object> actualTypes = new HashMap<String, Object>();
		for (HSIEForm hsie : forms.values()) {
			Object te = checkHSIE(s, hsie);
			if (te == null)
				continue;
			actualTypes.put(hsie.fnName, te);
		}
//		System.out.println("Checked forms: actualTypes = " + actualTypes);
//		System.out.println("Attempting to unify types");
		if (errors.moreErrors(mark))
			return actualTypes;
		for (HSIEForm f : forms.values()) {
			Object rwt = s.phi.unify(s.localKnowledge.get(f.fnName), actualTypes.get(f.fnName));
			actualTypes.put(f.fnName, s.phi.subst(rwt)); 
		}
		s.phi.validateUnionTypes(this);
		return actualTypes;
	}

	private int rewriteFunctionTree(Tree<HSIEForm> functionsToCheck, Map<String, Object> localKnowledge, Map<Var, Var> rwvars, Map<String, HSIEForm> rewritten, int from, Node<HSIEForm> nh) {
		HSIEForm hsie = nh.getEntry();
		Map<Var, Var> rwvars2 = new HashMap<Var, Var>(rwvars);
		rewritten.put(hsie.fnName, rewriteWithFreshVars(rwvars2, hsie, from));
		from += hsie.vars.size();
		for (Node<HSIEForm> c : functionsToCheck.getChildren(nh))
			from = rewriteFunctionTree(functionsToCheck, localKnowledge, rwvars2, rewritten, from, c);
		return from;
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
			ClosureCmd cc = (ClosureCmd)b;
			ClosureCmd closure = ret.closure(mapping.get(cc.var));
			closure.justScoping = cc.justScoping;
			closure.downcastType = cc.downcastType;
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
				HSIEBlock ib = ret.ifCmd(new CreationOfVar(mapping.get(ic.var), ic.var.loc, ic.var.called), ic.value);
				mapBlock(ib, ic, mapping);
			} else if (b instanceof BindCmd) {
				BindCmd bc = (BindCmd) b;
				ret.bindCmd(mapping.get(bc.bind), mapping.get(bc.from), bc.field);
			} else if (b instanceof ReturnCmd) {
				ReturnCmd rc = (ReturnCmd)b;
				List<CreationOfVar> deps = rewriteList(mapping, rc.deps);
				if (rc.var != null)
					ret.doReturn(rc.location, new CreationOfVar(mapping.get(rc.var), rc.var.loc, rc.var.called), deps);
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
					ret.push(pc.location, new CreationOfVar(mapping.get(pc.var), pc.var.loc, pc.var.called));
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

	private List<CreationOfVar> rewriteList(Map<Var, Var> mapping, List<CreationOfVar> deps) {
		if (deps == null)
			return null;
		List<CreationOfVar> ret = new ArrayList<CreationOfVar>();
		for (CreationOfVar var : deps)
			ret.add(new CreationOfVar(mapping.get(var), var.loc, var.called));
		return ret;
	}

	void allocateTVs(List<TypeVar> vars, TypeState s, HSIEForm hsie) {
//		for (int i=0;i<hsie.alreadyUsed;i++)
//			throw new UtilException("Need to make sure these are reused from existing parent, even after renaming");
		for (int i=0;i<hsie.nformal;i++) {
			TypeVar tv = factory.next();
//			System.out.println("Allocating " + tv + " for " + hsie.fnName + " arg " + i + " var " + hsie.vars.get(i+hsie.alreadyUsed));
			s.gamma = s.gamma.bind(hsie.vars.get(i+hsie.alreadyUsed), new TypeScheme(null, tv));
			vars.add(tv);
		}
//		System.out.println(s.gamma);
	}

	Object checkHSIE(TypeState s, HSIEForm hsie) {
		logger.info("Checking " + hsie.fnName + " with " + hsie.nformal + " args");
//		hsie.dump();
		// what we need to do is to apply tcExpr to the right hand side with the new gamma
		Object rhs = checkBlock(new SFTypes(null), s, hsie, hsie);
		if (rhs == null)
			return null;
		// then we need to build an expr tv0 -> tv1 -> tv2 -> E with all the vars substituted
		for (int i=hsie.nformal-1;i>=0;i--) {
			CreationOfVar myarg = new CreationOfVar(hsie.vars.get(hsie.alreadyUsed+i), null, "??");
			Object tv = s.gamma.valueOf(myarg).typeExpr;
			rhs = new TypeExpr(new GarneredFrom(hsie.fnName, i), Type.builtin(new InputPosition("->", 0,0,null), "->"), s.phi.subst(tv), rhs);
		}
		return rhs;
	}

	private Object checkBlock(SFTypes sft, TypeState s, HSIEForm form, HSIEBlock hsie) {
		List<Object> returns = new ArrayList<Object>();
		logger.debug("Checking block " + hsie);
		for (HSIEBlock o : hsie.nestedCommands()) {
			logger.debug("Checking command " + o);
			if (o instanceof ReturnCmd) {
//				System.out.println("Checking expr " + o);
				Object ret = checkExpr(s, form, o);
//				System.out.println("Checked expr " + o + " as " + ret);
//				logger.debug(o.toString() + " checked as " + ret);
				return ret;
			}
			else if (o instanceof Head)
				;
			else if (o instanceof Switch) {
				Switch sw = (Switch) o;
				String scname = sw.ctor;
				TypeScheme valueOf = s.gamma.valueOf(new CreationOfVar(sw.var, null, "??"));
				if (scname.equals("Number") || scname.equals("Boolean") || scname.equals("String")) {
					s.phi.unify(valueOf.typeExpr, new TypeExpr(new GarneredFrom(sw.location), this.knowledge.get(scname)));
					returns.add(checkBlock(sft, s, form, sw));
					logger.debug(o.toString() + " links " + sw.var + " to " + sw.ctor);
				} else {
					StructDefn sd = structs.get(sw.ctor);
					Type pt = sd;
					UnionTypeDefn ud = types.get(sw.ctor);
					if (pt == null) pt = ud;
					ContractDecl cd = contracts.get(sw.ctor);
					if (pt == null) pt = cd;
					if (pt == null) {
						errors.message(sw.location, "there is no definition for type " + sw.ctor);
						return null;
					}
	//				System.out.println(valueOf);
					Map<String, TypeVar> polys = new HashMap<String, TypeVar>();
					// we need a complex map of form var -> ctor -> field -> type
					// and type needs to be cunningly constructed from TypeReference
					List<Object> targs = new ArrayList<Object>();
					if (pt.hasPolys()) {
						for (Type x : pt.polys()) {
							TypeVar tv = factory.next();
							targs.add(tv);
							polys.put(x.name(), tv);
						}
					}
	//				System.out.println(polys);
					if (sd != null) {
						logger.debug(o + " asserts that " + sw.var + " is of type " + sw.ctor + " with type scheme " + valueOf);
						SFTypes inner = new SFTypes(sft);
						for (StructField x : sd.fields) {
	//						System.out.println("field " + x.name + " has " + x.type);
							Object fr = TypeExpr.from(x.type, polys);
							inner.put(sw.var, x.name, fr);
	//						System.out.println(fr);
						}
						s.phi.unify(valueOf.typeExpr, new TypeExpr(new GarneredFrom(sw.location), sd, targs));
						returns.add(checkBlock(inner, s, form, sw));
					} else if (ud != null) {
//						if (ud.name().equals("Any")) { // this is a special case
//							logger.debug(sw + " says " + sw.var + " is of Any type; of course it is ...");
//							s.phi.unify(valueOf.typeExpr, new TypeExpr(new GarneredFrom(sw.location), ud, targs));
//							returns.add(checkBlock(sft, s, form, sw));
//						} else {
							s.phi.unify(valueOf.typeExpr, new TypeExpr(new GarneredFrom(sw.location), ud, targs));
							returns.add(checkBlock(sft, s, form, sw));
//						}
					} else if (cd != null) {
						s.phi.unify(valueOf.typeExpr, new TypeExpr(new GarneredFrom(sw.location), cd));
						returns.add(checkBlock(sft, s, form, sw));
					} else
						throw new UtilException("Added case");
				}
			} else if (o instanceof IFCmd) {
				IFCmd ic = (IFCmd) o;
				if (ic.value == null) { // closure case
					logger.debug(o.toString() + " forces " + ic.var + " to be boolean");
					checkClosure(s, form, form.getClosure(ic.var.var));
				}
				// Since we have to have done a SWITCH before we get here, this gives us no new information
				returns.add(checkBlock(sft, s, form, ic));
			} else if (o instanceof BindCmd) {
				BindCmd bc = (BindCmd) o;
				TypeVar tv = factory.next();
				Object bt = sft.get(bc.from, bc.field);
				logger.debug(o + " introduces var " + tv + " with type " + bt);
				s.phi.unify(tv, bt);
//				System.out.println("binding " + bc.bind + " to " + tv);
				s.gamma = s.gamma.bind(bc.bind, new TypeScheme(null, tv));
			} else if (o instanceof ErrorCmd) {
				// nothing really to do here ...
			} else
				throw new UtilException("Missing cases " + o.getClass());
		}
		if (returns.isEmpty())
			return null;
		Object t1 = returns.get(0);
		for (int i=1;i<returns.size();i++) {
			if (t1 == null || returns.get(i) == null)
				return null;
//			System.out.println("Attempting to unify return values " + t1 + " and " + returns.get(i));
			t1 = s.phi.unify(t1, returns.get(i));
		}
		if (t1 == null)
			return null;
		return s.phi.subst(t1);
	}

	Object checkExpr(TypeState s, HSIEForm form, HSIEBlock cmd) {
		logger.debug("Checking command " + cmd);
		if (cmd instanceof PushReturn) {
			PushReturn r = (PushReturn) cmd;
			GarneredFrom myloc = new GarneredFrom(r.location);
			if (r.ival != null) {
				logger.debug(r.toString() + " is a constant of type Number");
				return new TypeExpr(myloc, Type.builtin(new InputPosition("builtin", 0, 0, null), "Number")); // TODO: it would be good to look this up; we should be able to do that
			} else if (r.sval != null) {
				logger.debug(r.toString() + " is a constant of type String");
				return new TypeExpr(myloc, Type.builtin(new InputPosition("builtin", 0, 0, null), "String"));
			} else if (r.tlv != null) {
				// I don't think it's quite as simple as this ... I think we need to introduce it in one place and return it in another or something
				TypeVar ret = factory.next();
				logger.debug(r.tlv.name + " is a template variable, assigning " + ret);
				return ret;
			} else if (r.var != null) {
				ClosureCmd c = form.getClosure(r.var.var);
				if (c == null) {
					// phi is not updated
					// assume it must be a bound var; we will fail to get the existing type scheme if not
					TypeScheme old = s.gamma.valueOf(r.var);
					TypeVariableMappings temp = new TypeVariableMappings(errors, this);
					for (TypeVar tv : old.schematicVars) {
						temp.bind(tv, factory.next());
//						System.out.println("Allocating tv " + temp.meaning(tv) + " for " + tv + " when instantiating typescheme");
					}
					Object ret = temp.subst(old.typeExpr);
					logger.debug(r.var + " is a pre-defined var of type " + old.typeExpr + " becoming " + ret);
					return ret;
				} else {
					// c is a closure, which must be a function application
					return checkClosure(s, form, c);
				}
			} else if (r.fn != null) {
				// phi is not updated
				// I am going to say that by getting here, we know that it must be an external
				// all lambdas should be variables by now
				
				if (r.fn.uniqueName().equals("FLEval.tuple")) {
					logger.debug(r.fn + " needs tuple handling");
					return new TypeExpr(myloc, Type.builtin(null, "()"));
				}
				if (r.fn instanceof CardMember) {
					logger.debug(r.fn + " is a card member");
					CardMember cm = (CardMember) r.fn;
					// try and find the name of the card class
					if (r.fn.equals("_card"))
						throw new UtilException("Died in housefire");
						// return freshVarsIn(new TypeReference(cm.location, cm.card, null));
					CardTypeInfo cti = cards.get(cm.card);
					if (cti == null)
						throw new UtilException("There was no card definition called " + cm.card);
					for (StructField sf : cti.struct.fields) {
						if (sf.name.equals(cm.var)) {
							return sf.type.asExpr(new GarneredFrom(cm.location), this, factory);
						}
					}
					errors.message(cm.location, "there is no field " + cm.var + " in card " + cm.card);
					return null;
				} else if (r.fn instanceof HandlerLambda) {
					logger.debug(r.fn + " is a lambda");
					HandlerLambda hl = (HandlerLambda) r.fn;
					// try and find the name of the handler class
					// this is likewise a hack and I know it ...
//					int idx = form.fnName.length();
//					for (int i=0;i<2;i++)
//						idx = form.fnName.lastIndexOf('.', idx-1);
					String structName = hl.clzName; // form.fnName.substring(0, idx);
//						return freshVarsIn(new TypeReference(hl.location, structName, null));
					StructDefn sd = structs.get(structName);
					for (StructField sf : sd.fields) {
						if (sf.name.equals(hl.var)) {
							return sf.type.asExpr(null, this, factory);
						}
					}
					throw new UtilException("Could not find field " + hl.var + " in handler " + structName);
				}
				if (r.fn instanceof ScopedVar) {
					ScopedVar sv = (ScopedVar)r.fn;
					if (sv.defn instanceof LocalVar) {
						LocalVar lv = (LocalVar) sv.defn;
						Type t = lv.type;
						if (t == null)
							t = types.get("Any");
						return new TypeExpr(new GarneredFrom(r.fn.location()), t);
					}
				}
				
				String name = r.fn.uniqueName();
				if (name.equals("FLEval.field")) {
					logger.debug(r.fn + " implies field handling");
					return new TypeExpr(myloc, Type.builtin(new InputPosition("builtin", 0, 0, null), "."));
				}
				Object te = s.localKnowledge.get(name);
				if (te != null) {
					logger.debug(r.fn + " is locally inferred " + te);
					return te;
				}
				te = knowledge.get(name);
				if (te == null) {
					if (cards.containsKey(name)) {
						logger.debug(r.fn + " is card " + name);
						return new TypeExpr(myloc, cards.get(name).struct);
					}
					if (handlers.containsKey(name)) {
						logger.debug(r.fn + " is handler " + name);
						return typeForHandlerCtor(r.fn.location(), handlers.get(name)).asExpr(myloc, this, factory);
					}
					if (structs.containsKey(name)) {
						logger.debug(r.fn + " is struct ctor " + name);
						return ((TypeExpr)typeForStructCtor(r.fn.location(), structs.get(name)).asExpr(myloc, this, factory)).butFrom(myloc);
					}
					if (objects.containsKey(name)) {
						logger.debug(r.fn + " is object ctor " + name);
						return ((TypeExpr)typeForObjectCtor(r.fn.location(), objects.get(name)).asExpr(myloc, this, factory)).butFrom(myloc);
					}
					// This is probably a failure on our part rather than user error
					// We should not be able to get here if r.fn is not already an external which has been resolved
					/*
					for (Entry<String, Type> x : knowledge.entrySet())
						System.out.println(x.getKey() + " => " + x.getValue());
					*/
					errors.message(r.location, "there is no type for identifier: " + r.fn + " when checking " + form.fnName);
					return null;
				} else {
					logger.debug(r.fn + " is globally implanted " + te);
//					System.out.print("Replacing vars in " + r.fn +": ");
					return ((Type)te).asExpr(new GarneredFrom(r.fn, te), this, factory);
				}
			} else if (r.func != null) {
				logger.debug(r.fn + " is a function literal");
				return new TypeExpr(null, Type.builtin(null, "FunctionLiteral")); // do we not want the type signature of r.func?
			} else
				throw new UtilException("What are you returning?");
		} else
			throw new UtilException("Missing cases");
	}

	private Object checkClosure(TypeState s, HSIEForm form, ClosureCmd c) {
		if (c == null)
			throw new UtilException("Error on recovering block to check");
		logger.debug("Checking closure " + c.var);
		c.dumpOne(logger, 0);
		if (c.justScoping) {
			logger.debug("Just checking first expr to handle scoping case");
			return checkExpr(s, form, c.nestedCommands().get(0));
		}
		List<Object> args = new ArrayList<Object>();
		List<InputPosition> locs = new ArrayList<InputPosition>();
		Object fnCall = null;
//		int argN = 0;
		for (HSIEBlock b : c.nestedCommands()) {
			Object te = checkExpr(s, form, b);
			if (te == null)
				return null;
			if (fnCall == null)
				fnCall = te;
//			else if (te instanceof TypeExpr && fnCall instanceof TypeExpr && ((TypeExpr)fnCall).type.equals("->"))
//				te = ((TypeExpr)te).butFrom(new GarneredFrom(((TypeExpr)fnCall).asType(this), argN));
			args.add(te);
			InputPosition ip = null;
			if (b instanceof PushReturn)
				ip = ((PushReturn)b).location;
			locs.add(ip);
//			argN++;
		}
		Object Tf = args.get(0);
		if (Tf instanceof TypeExpr && "()".equals(((TypeExpr)Tf).type.name())) { // tuples need special handling
			List<Object> newVars = new ArrayList<Object>();
			for (int i=1;i<args.size();i++)
				newVars.add(factory.next());
			TypeExpr TfE = (TypeExpr)Tf;
			Tf = new TypeExpr(TfE.from, TfE.type, newVars);
			logger.debug("Closure " + c + " has type " + Tf);
		} else if (Tf instanceof TypeExpr && ".".equals(((TypeExpr)Tf).type.name())) { // so does "." notation
			InputPosition posn = ((TypeExpr)Tf).from.posn;
			Object T1 = s.phi.subst(args.get(1));
			if (T1 instanceof TypeExpr) {
				TypeExpr te = (TypeExpr) T1;
				String tn = te.type.name();
				String fn = ((PushCmd)c.nestedCommands().get(2)).sval.text;
				StructDefn sd = this.structs.get(tn);
				if (sd != null) {
					for (StructField f : sd.fields) {
						if (f.name.equals(fn)) {
							Object r = f.type.asExpr(new GarneredFrom(f), this, factory);
							logger.debug("field " + f.name + " of " + sd.name() + " has type " + f.type + " with fresh vars as " + r);
							return r;
						}
					}
					errors.message(posn, "there is no field '" + fn + "' in '" + tn +"'");
					return null;
				}
				ObjectDefn od = this.objects.get(tn);
				if (od != null) {
					for (ObjectMethod m : od.methods) {
						if (m.name.equals(fn)) {
							Object r = m.type.asExpr(null, this, factory);
							logger.debug("field " + m.name + " of " + od.name() + " has type " + m.type + " with fresh vars as " + r);
							return r;
						}
					}
					// TODO: handle . from object method
					errors.message(posn, "there is no method '" + fn + "' in '" + tn +"'");
					return null;
				}
				if (this.contracts.containsKey(tn)) {
					errors.message(posn, "contract methods must be called at the top level (check parens)");
					return null;
				}
				errors.message(posn, "cannot use '.' with " + tn + " as it is not a struct or object definition");
				return null;
			} else if (T1 instanceof TypeVar) {
				errors.message(posn, "cannot use '.' notation without defined type for expression");
				return null;
			} else
				throw new UtilException("What is " + T1);
		} else {
			logger.debug(c + " requires " + flatten(new StringBuilder(), Tf, false) + " to apply to " + arrowify(args));
			for (int i=1;i<args.size();i++)
				Tf = checkSingleApplication(s, Tf, locs.get(i), args.get(i));
		}
		logger.debug("Closure " + c + " has type " + Tf);
		if (c.downcastType != null)
			return TypeExpr.from(c.downcastType, new HashMap<String, TypeVar>());
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
			args.add(x.type);
		args.add(structDefn);
		return Type.function(location, args);
	}

	private Type typeForObjectCtor(InputPosition location, ObjectDefn objectDefn) {
		List<Type> args = new ArrayList<Type>();
		for (StructField x : objectDefn.ctorArgs)
			args.add(x.type);
		args.add(objectDefn);
		return Type.function(location, args);
	}

	private Type typeForCardCtor(InputPosition location, StructDefn structDefn) {
		List<Type> args = new ArrayList<Type>();
		// I think this is OK being builtin ...
		args.add(Type.builtin(location, "_Wrapper"));
		args.add(structDefn);
		return Type.function(location, args);
	}

	private Type typeForHandlerCtor(InputPosition location, HandlerImplements impl) {
		List<Type> args = new ArrayList<Type>();
		for (Object x : impl.boundVars) {
			HandlerLambda hl = (HandlerLambda)x;
			if (hl.scopedFrom == null)
				args.add(hl.type);
		}
		args.add(impl);
		return Type.function(location, args);
	}

	private Object checkSingleApplication(TypeState s, Object fnType, InputPosition pos, Object argType) {
		TypeVar resultType = factory.next();
		TypeExpr hypoFunctionType = new TypeExpr(new GarneredFrom(pos), Type.builtin(new InputPosition("builtin", 0, 0, null), "->"), argType, resultType);
		int mark = errors.count();
		s.phi.unify(fnType, hypoFunctionType);
		if (errors.moreErrors(mark))
			return null;
		return s.phi.meaning(resultType);
	}

	public Type getTypeAsCtor(InputPosition loc, String fn) {
		if (knowledge.containsKey(fn))
			return knowledge.get(fn);
		if (structs.containsKey(fn))
			return typeForStructCtor(loc, structs.get(fn));
		if (objects.containsKey(fn))
			return typeForObjectCtor(loc, objects.get(fn));
		if (cards.containsKey(fn))
			return typeForCardCtor(loc, cards.get(fn).struct);
		throw new UtilException("There is no type: " + fn);
	}

	public Type getType(InputPosition loc, String name) {
		if (name == null)
			throw new UtilException("Cannot get a type with null name");
		if (structs.containsKey(name))
			return structs.get(name);
		if (types.containsKey(name))
			return types.get(name);
		if (knowledge.containsKey(name))
			return knowledge.get(name);
		if (cards.containsKey(name))
			return cards.get(name).struct;
		if (contracts.containsKey(name))
			return contracts.get(name);
		throw new UtilException("There is no type: " + name);
	}

	public void writeLearnedKnowledge(OutputStream wex, boolean copyToScreen) throws IOException {
		if (copyToScreen)
			System.out.println("Inferred types:");
		ObjectOutputStream oos = new ObjectOutputStream(wex);
		List<StructDefn> str = new ArrayList<StructDefn>();
		for (StructDefn sd : structs.values()) {
			if (sd.generate) {
				str.add(sd);
				if (copyToScreen)
					System.out.println("  struct " + sd.asString());
			}
		}
		oos.writeObject(str);
		List<UnionTypeDefn> ts = new ArrayList<UnionTypeDefn>();
		for (UnionTypeDefn td : types.values()) {
			if (td.generate) {
				ts.add(td);
				if (copyToScreen)
					System.out.println("  type " + td.name());
			}
		}
		oos.writeObject(ts);
		List<ContractDecl> cds = new ArrayList<ContractDecl>();
		for (ContractDecl cd : contracts.values()) {
			if (cd.generate) {
				cds.add(cd);
				if (copyToScreen) {
					System.out.println("  contract " + cd.name());
					for (ContractMethodDecl m : cd.methods) {
						System.out.print(Justification.LEFT.format("", 4));
						System.out.print(Justification.PADRIGHT.format(m.dir, 5));
						System.out.print(Justification.PADRIGHT.format(m.name, 12));
						System.out.print(" ::");
						String sep = " ";
						for (Object o : m.args) {
							System.out.print(sep + ((AsString)o).asString());
							sep = " -> ";
						}
						System.out.println();
					}
				}
			}
		}
		oos.writeObject(cds);
		
		List<CardTypeInfo> ctis = new ArrayList<CardTypeInfo>();
		for (CardTypeInfo cti : this.cards.values()) {
			ctis.add(cti);
			if (copyToScreen) {
				System.out.println("  card " + cti.name);
				for (TypeHolder x : cti.contracts) {
					System.out.println("    contract " + x.name);
					x.dump(6);
				}
				for (TypeHolder x : cti.handlers) {
					System.out.println("    handler " + x.name);
					x.dump(6);
				}
				cti.dump(4);
			}
		}
		oos.writeObject(ctis);
		oos.flush();
		
		// TODO: functions?
	}
}
