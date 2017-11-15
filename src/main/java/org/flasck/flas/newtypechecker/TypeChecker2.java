package org.flasck.flas.newtypechecker;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.flim.KnowledgeWriter;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.FunctionLiteral;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWContractService;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.types.FunctionType;
import org.flasck.flas.types.InstanceType;
import org.flasck.flas.types.PolyVar;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.types.TupleType;
import org.flasck.flas.types.Type;
import org.flasck.flas.types.TypeOfSomethingElse;
import org.flasck.flas.types.TypeWithMethods;
import org.flasck.flas.types.TypeWithName;
import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.ClosureCmd;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.ErrorCmd;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Head;
import org.flasck.flas.vcode.hsieForm.IFCmd;
import org.flasck.flas.vcode.hsieForm.PushBool;
import org.flasck.flas.vcode.hsieForm.PushBuiltin;
import org.flasck.flas.vcode.hsieForm.PushCSR;
import org.flasck.flas.vcode.hsieForm.PushDouble;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushFunc;
import org.flasck.flas.vcode.hsieForm.PushInt;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.PushString;
import org.flasck.flas.vcode.hsieForm.PushTLV;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.flasck.flas.vcode.hsieForm.Switch;
import org.flasck.flas.vcode.hsieForm.Var;
import org.flasck.flas.vcode.hsieForm.VarInSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.collections.SetMap;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.UtilException;
import org.zinutils.xml.XML;

public class TypeChecker2 {
	public final static Logger logger = LoggerFactory.getLogger("TypeChecker");
	private final ErrorResult errors;
	private final Rewriter rw;
	final Map<String, RWStructDefn> structs = new HashMap<String, RWStructDefn>();
	final Map<String, RWObjectDefn> objects = new HashMap<String, RWObjectDefn>();
	final Map<String, TypeInfo> structTypes = new HashMap<String, TypeInfo>();
	final Map<String, Type> export = new TreeMap<>();
	final Map<String, Type> ctors = new TreeMap<>();
	private final Set<RWUnionTypeDefn> unions = new TreeSet<>();
	private PrintWriter trackTo;

	private int nextVar;
	// is there a real need to keep these separate?  especially when we are promoting?
	private final Map<String, TypeInfo> globalKnowledge = new TreeMap<String, TypeInfo>();
	private final Map<String, TypeInfo> localKnowledge = new TreeMap<String, TypeInfo>();
	private final SetMap<Var, TypeInfo> constraints = new SetMap<Var, TypeInfo>(new SimpleVarComparator(), new TypeInfoComparator());
	private final Map<String, Var> returns = new TreeMap<String, Var>();
	private final Map<Var, HSIEBlock> scoping = new HashMap<>();
	
	public TypeChecker2(ErrorResult errors, Rewriter rw) {
		this.errors = errors;
		this.rw = rw;
	}

	// Mainly for golden tests
	public void trackTo(PrintWriter pw) {
		this.trackTo = pw;
	}

	public void populateTypes() {
		try {
			pass1();
			pass2();
		} catch (NeedIndirectionException ex) {
			throw new UtilException("this should have been dealt with", ex);
		}
	}

	private void pass1() throws NeedIndirectionException {
		rw.visit(new Pass1Visitor(this), true);
		for (PrimitiveType bi : rw.primitives.values()) {
			export.put(bi.name(), bi);
			gk(bi.name(), new NamedType(bi.location(), bi.getTypeName()));
		}
		for (RWUnionTypeDefn ud : rw.types.values()) {
			List<TypeInfo> polys = new ArrayList<>();
			if (ud.hasPolys()) {
				for (Type t : ud.polys())
					polys.add(convertType(t));
			}
			NamedType uty = new NamedType(ud.location(), ud.getTypeName(), polys);
			gk(ud.name(), uty);
			
			unions.add(ud);
		}
		for (RWObjectDefn od : rw.objects.values()) {
			objects.put(od.uniqueName(), od);
			List<TypeInfo> polys = new ArrayList<>();
			if (od.hasPolys()) {
				for (Type t : od.polys())
					polys.add(convertType(t));
			}
			gk(od.name(), new NamedType(od.location(), od.getTypeName(), polys));
			if (od.ctorArgs != null) {
				List<Type> args = new ArrayList<>();
				for (RWStructField sf : od.ctorArgs)
					args.add(sf.type);
				args.add(od);
				ctors.put(od.name(), new FunctionType(od.location(), args));
			}
		}
	}
	
	private void pass2() throws NeedIndirectionException {
		rw.visit(new Pass2Visitor(this), true);
		for (RWFunctionDefinition fn : rw.functions.values()) {
			if (fn.getType() != null) { // a function has already been typechecked
				TypeInfo ct = convertType(fn.getType());
				if (ct instanceof TypeVar)
					throw new UtilException("That's not really a type now, is it ... " + ct);
				gk(fn.uniqueName(), ct);
				export.put(fn.uniqueName(), fn.getType());
			}
		}
		for (Entry<String, Type> e : rw.fnArgs.entrySet()) {
			if (e.getValue() instanceof RWStructDefn) {
				RWStructDefn val = (RWStructDefn)e.getValue();
				gk(e.getKey(), new NamedType(val.location(), val.getTypeName()));
			} else {
				TypeInfo ty = convertType(e.getValue());
				gk(e.getKey(), ty);
			}
		}
	}

	void gk(String name, TypeInfo ty) {
		if (ty instanceof TypeVar)
			throw new UtilException("That's not really a type now, is it ... binding " + name + " to " + ty);
		globalKnowledge.put(name, ty);
	}

	// Typecheck a set of HSIE forms in parallel ...
	public void typecheck(Set<HSIEForm> forms) {
		
		// 1. initialize the state for doing the checking ...
		// 1a. clean up from previous attempts (should this go in a separate currentState object?)
		localKnowledge.clear();
		constraints.clear();
		returns.clear();
		scoping.clear();
		nextVar=0;
		
		// 1b. define all the vars that are already in the HSIE, trapping the max value for future reference
		Map<String, VarInSource> knownScoped = new HashMap<>();
		for (HSIEForm f : forms) {
			logger.info("Checking type of " + f.funcName.uniqueName());
			f.dump(logger);
			if (globalKnowledge.containsKey(f.funcName.uniqueName()))
				errors.message(f.location, "duplicate entry for " + f.funcName.uniqueName() + " in type checking");
			for (Var v : f.vars) {
				if (constraints.contains(v))
					throw new UtilException("Duplicate var definition " + v);
				constraints.ensure(v);
				if (v.idx >= nextVar)
					nextVar = v.idx+1;
			}
			collectVarNames(knownScoped, f, f);
			for (ClosureCmd c : f.closuresX())
				collectVarNames(knownScoped, f, c);
		}
		for (Entry<String, VarInSource> e : knownScoped.entrySet()) {
			if (!globalKnowledge.containsKey(e.getKey()))
				localKnowledge.put(e.getKey(), new TypeVar(e.getValue().loc, e.getValue().var));
		}
		logger.info("allocating FRESH vars from " + nextVar);

		// 1c. Now allocate FRESH vars for the return types
		for (HSIEForm f : forms) {
			logger.info("Allocating function/return vars for " + f.funcName.uniqueName());
			Var rv = new Var(nextVar++);
			localKnowledge.put(f.funcName.uniqueName(), new TypeFunc(f.location, f.vars, f.nformal, new TypeVar(f.location, rv)));
			logger.info("Allocating " + rv + " as return type of " + f.funcName.uniqueName());
			if (constraints.contains(rv))
				throw new UtilException("Duplicate var definition " + rv);
			constraints.ensure(rv);
			returns.put(f.funcName.uniqueName(), rv);
		}
		
		// 1d. Now allocate FRESH vars for any scoped variables that still haven't been defined
		for (HSIEForm f : forms) {
			for (ScopedVar vn : f.scoped) {
				String name = vn.id.uniqueName();
				if (globalKnowledge.containsKey(name) || localKnowledge.containsKey(name)) {
					logger.debug("Have definition for " + name);
					continue;
				}
				Var sv = new Var(nextVar++);
				logger.info("Introducing scoped var " + sv + " for " + name);
				localKnowledge.put(name, new TypeVar(vn.location(), sv));
				if (constraints.contains(sv))
					throw new UtilException("Duplicate var definition " + sv);
				constraints.ensure(sv);
			}
		}

		// 2. collect constraints
		
		// 2a. from the switching blocks
		for (HSIEForm f : forms) {
			processHSI(f, f);
		}

		// 2b. define "scoping" closures as what they really are
		for (HSIEForm f : forms) {
			for (ClosureCmd c : f.closuresX()) {
				if (c.justScoping()) {
					scoping.put(c.var, c.nestedCommands().get(0));
					constraints.removeAll(c.var);
					for (int i=1;i<c.nestedCommands().size();i++) {
						HSIEBlock a = c.nestedCommands().get(i);
						if (a instanceof PushVar) {
							String name = ((PushVar)a).var.called;
							TypeInfo ti = globalKnowledge.get(name);
							if (ti != null)
								constraints.add(((PushVar)a).var.var, ti);
						}
					}
				}
			}
		}

		// 2c. look at all the actual closures
		for (HSIEForm f : forms) {
			for (ClosureCmd c : f.closuresX()) {
				try {
					processClosure(f, c);
				} catch (Exception ex) {
					ex.printStackTrace();
					errors.message(c.location, ex.toString());
				}
			}

			// If we generated additional constraints (such as StructWithField) check those constraints now
			for (ClosureCmd c : f.closuresX()) {
				if (c.justScoping())
					continue;
				checkAdditionalConstraints(f, c);
			}
		}

		// TODO: I suspect 3 & 4 may need to be merged into a single, until-we're-done loop
		// 3. Unify type arguments
		SetMap<Var, TypeInfo> addTo = new SetMap<Var, TypeInfo>();
		for (Var k : constraints.keySet()) {
			Set<TypeInfo> tis = constraints.get(k);
			if (tis.size() < 2)
				continue;
			Set<TypeFunc> tfs = new HashSet<TypeFunc>();
			Set<NamedType> nts = new HashSet<NamedType>();
			for (TypeInfo ti : tis) {
				if (ti instanceof TypeVar)
					continue;
				else if (ti instanceof TypeFunc)
					tfs.add((TypeFunc) ti);
				else if (ti instanceof NamedType)
					nts.add((NamedType) ti);
				else if (ti instanceof TupleInfo)
					; // TODO: this is also a case that needs handling
				else
					throw new UtilException("Cannot handle " + ti + " " + ti.getClass());
			}
			if (!tfs.isEmpty() && !nts.isEmpty())
				throw new UtilException("There should be a clear error message for this case: cannot merge " + nts + " with " + tfs);
			if (tfs.size() > 1) {
				int arity = -1;
				for (TypeFunc tf : tfs) {
					if (arity == -1)
						arity = tf.args.size();
					else if (arity != tf.args.size())
						throw new UtilException("Cannot merge multiple functions with different arity unless the final thing is a var, which is a complicated case");
				}
				List<TypeInfo> unified = new ArrayList<TypeInfo>();
				for (int i=0;i<arity;i++) {
					// TODO: in the real world, I think this needs to be recursive, handling nested function and polymorphic types
					Set<TypeInfo> mas = new HashSet<TypeInfo>();
					for (TypeFunc tf : tfs) {
						mas.add(tf.args.get(i));
					}
					TypeInfo hard = null;
					TypeInfo soft = null;
					for (TypeInfo ti : mas) {
						if (!(ti instanceof TypeVar)) {
							if (hard != null)
								throw new UtilException("Need to handle recursive case or something");
							else
								hard = ti;
							continue;
						}
						soft = ti;
						Var v = ((TypeVar)ti).var;
						for (TypeInfo tj : mas) {
							if (ti.equals(tj))
								continue;
							addTo.add(v, tj);
						}
					}
					if (hard != null)
						unified.add(hard);
					else
						unified.add(soft);
				}
				tis.removeAll(tfs);
				tis.add(new TypeFunc(CollectionUtils.any(tfs).location(), unified));
			}
			if (nts.size() > 1) {
				// Note: there are two cases here
				// It is possible we have something like Nil & Cons[v0], in which case we want to defer until merging
				// But we should look at the case where we have Cons[v0] and Cons[String]
			}
		}
		for (Var v : addTo.keySet()) {
			constraints.addAll(v, addTo.get(v));
		}
		
		// 4. Eliminate vars that are duplicates
		Map<Var, Var> renames = new TreeMap<Var, Var>(new SimpleVarComparator());
		boolean changed = true;
		elimLoop:
		while (changed) {
			changed = false;
			for (Var k : constraints.keySet()) {
				Set<TypeInfo> toRemove = new HashSet<TypeInfo>();
				for (TypeInfo ti : constraints.get(k)) {
					if (ti instanceof TypeVar) {
						Var v = ((TypeVar) ti).var;
						if (v.idx == k.idx) {
							changed = true;
							constraints.remove(k, ti);
							continue elimLoop;
						}
						else if (v.idx < k.idx) { // eliminate k, replacing with v
							changed = true;
							new Eliminator(constraints, renames).subst(k, v);
							continue elimLoop;
						} else { // v.idx > k.idx
							changed = true;
							new Eliminator(constraints, renames).subst(v, k);
							continue elimLoop;
						}
					}
				}
				constraints.get(k).removeAll(toRemove);
			}
		}

		// Now check closures with "Send" tags
		for (HSIEForm f : forms) {
			for (ClosureCmd c : f.closuresX()) {
				if (c.checkSend) {
					try {
						checkSendCall(f, c);
					} catch (NeedIndirectionException ex) {
						throw new UtilException("this should have been handled", ex);
					}
				}
			}
		}
		
		// 5. Merge union types
		Map<Var, TypeInfo> merged = new TreeMap<Var, TypeInfo>(new SimpleVarComparator());
		for (Var k : constraints.keySet())
			merged.put(k, mergeDown(k, constraints.get(k)));

		// 6. Deduce actual function types
		for (HSIEForm f : forms) {
			TypeInfo nt = deduceType(renames, merged, f);
			logger.info("Concluded that " + f.funcName.uniqueName() + " has type " + nt);
			gk(f.funcName.uniqueName(), nt);
			FunctionType ty = (FunctionType) asType(nt);
			export.put(f.funcName.uniqueName(), ty);
			if (trackTo != null) {
				Type t1 = ty;
				if (ty.arity() == 0)
					t1 = ty.arg(0);
				trackTo.println(f.funcName.uniqueName() + " :: " + t1);
			}
		}
	}

	private void collectVarNames(Map<String, VarInSource> knownScoped, HSIEForm f, HSIEBlock blk) {
		for (HSIEBlock c : blk.nestedCommands()) {
			if (c instanceof Head || c instanceof BindCmd || c instanceof ErrorCmd)
				continue;
			if (c instanceof Switch || c instanceof IFCmd)
				collectVarNames(knownScoped, f, c);
			else if (c instanceof PushVar) {
				VarInSource v = ((PushVar)c).var;
				if (knownScoped.containsKey(v.called) && knownScoped.get(v.called).var.idx != v.var.idx) {
					throw new UtilException("Inconsistent var names " + v.called + " has " + v.var + " and " + knownScoped.get(v.called));
				}
				knownScoped.put(v.called, v);
			} else if (c instanceof PushReturn)
				;
			else
				throw new UtilException("What is " + c + "?");
		}
	}

	protected void processClosure(HSIEForm f, ClosureCmd c) throws NeedIndirectionException {
		List<HSIEBlock> cmds = c.nestedCommands();
		if (c.justScoping()) {
			return;
		}
		if (c.downcastType != null)
			constraints.add(c.var, convertType(c.downcastType));
		logger.info("Need to check " + f.funcName.uniqueName() + " " + c.var);
		List<TypeInfo> argtypes = new ArrayList<TypeInfo>();
		List<VarName> isScoped = new ArrayList<>();
		for (int i=1;i<cmds.size();i++) {
			HSIEBlock cmd = cmds.get(i);
			argtypes.add(getTypeOf(f, cmd));
			isScoped.add(isPushScope(cmd));
		}
		HSIEBlock cmd = cmds.get(0);
		if (cmd instanceof PushVar && scoping.containsKey(((PushVar)cmd).var.var))
			cmd = scoping.get(((PushVar)cmd).var.var);
		if (cmd instanceof PushVar) {
			PushVar pc = (PushVar)cmd;
			Var fv = pc.var.var;
			if (argtypes.isEmpty())
				constraints.add(fv, new TypeVar(pc.var.loc, c.var));
			else
				constraints.add(fv, new TypeFunc(cmd.location, argtypes, new TypeVar(pc.var.loc, c.var)));
		} else if (cmd instanceof PushBuiltin) {
			PushBuiltin pb = (PushBuiltin) cmd;
			if (pb.isField()) {
				TypeInfo ty;
				String fname = ((PushString)cmds.get(2)).sval.text;
				if (argtypes.get(0) instanceof TypeVar) {
					Set<TypeInfo> set = new HashSet<>();
					final Var structVar = ((TypeVar)argtypes.get(0)).var;
					for (TypeInfo ti : constraints.get(structVar))
						if (ti instanceof NamedType)
							set.add(ti);
					if (set.isEmpty()) {
						constraints.get(structVar).add(new StructWithFieldConstraint(cmd.location, fname));
						return;
					}
					if (set.size() > 1)
						throw new UtilException("This is a dubious case, I think, and one I cannot handle: "  + set);
					ty = CollectionUtils.any(set);
				} else if (argtypes.get(0) instanceof NamedType) {
					ty = argtypes.get(0);
				} else {
					c.dumpOne(new PrintWriter(System.err), 0);
					throw new NotImplementedException("field(unhandled): " + argtypes.get(0) + " " + argtypes.get(0).getClass());
				}
				if (ty instanceof NamedType) {
					NamedType nt = (NamedType) ty;
					String sn = nt.name;
					RWStructDefn sd = structs.get(sn);
					RWObjectDefn od = objects.get(sn);
					if (sd != null) {
						RWStructField sf = sd.findField(fname);
						if (sf == null)
							throw new UtilException(sn + " does not have a field '" + fname + "'");
						constraints.add(c.var, freshPolys(convertType(sf.type), new HashMap<>()));
					} else if (od != null) {
						Type ot;
						if (od.state != null && od.state.findField(fname) != null) {
							ot = od.state.findField(fname).type;
						} else if (od.hasMethod(fname)) {
							ot = od.getMethodType(fname);
						} else
							throw new UtilException("There is no field " + fname + " in object " + sn);
						constraints.add(c.var, instantiate(convertType(ot), nt, od));
					} else {
						c.dumpOne(new PrintWriter(System.err), 0);
						throw new UtilException(sn + " is not a struct; cannot do .");
					}
				} else
					throw new NotImplementedException("field(non-named): " + ty);
				return;
			} else if (pb.isTuple()) {
				constraints.add(c.var, new TupleInfo(cmd.location, argtypes));
				return;
			} else
				throw new RuntimeException("Cannot handle builtin " + pb);
		} else {
			TypeInfo ti = freshPolys(getTypeOf(f, cmd), new HashMap<>());
			// TODO: if function is polymorphic, introduce fresh vars NOW
			logger.debug("In " + c.var + ", cmd = " + cmd + " fi = " + ti);
			if (ti == null) {
				logger.debug(c.var + " has a null first arg");
				return;
			}
			if (!(ti instanceof TypeFunc)) {
				// There is a caveat here, in which we could have ti be polymorphic (or "Any") 
				errors.message(cmd.location, "can only call functions, not " + ti + " for " + cmd);
				return;
			}
			TypeFunc called = (TypeFunc) ti;
			for (int i=0;i<argtypes.size();i++) {
				if (called.args.size() < i)
					throw new UtilException("Error about applying a non-function to arg " + i + " in " + c.var);
				TypeInfo tai = called.args.get(i);
				checkArgType(tai, argtypes.get(i));
				VarName si = isScoped.get(i);
				if (si != null) {
					TypeInfo foo = globalKnowledge.get(si.uniqueName());
					if (foo == null || ((foo instanceof NamedType) && ((NamedType)foo).name.equals("Any"))) {
						if (!(tai instanceof TypeVar))
							gk(si.uniqueName(), tai);
					} else if ((tai instanceof NamedType) && ((NamedType)tai).name.equals("Any"))
						gk(si.uniqueName(), foo);
					else if (foo instanceof TypeVar)
						constraints.add(((TypeVar)foo).var, tai);
					else if (tai instanceof TypeVar)
						constraints.add(((TypeVar)tai).var, foo);
					else if (foo.equals(tai))
						;
					else if (foo instanceof TypeFunc && ((TypeFunc)foo).args.size() == 1 && ((TypeFunc)foo).args.get(0).equals(tai))
						;
					else
						throw new UtilException("Scoped var = " + si + " with already " + foo + " and now " + tai);
				}
			}
			TypeInfo ret = called.args.get(called.args.size()-1);
			if (called.args.size() == argtypes.size()+1) {
				constraints.add(c.var, ret);
			} else {
				List<TypeInfo> args = new ArrayList<TypeInfo>();
				for (int i=argtypes.size();i+1<called.args.size();i++)
					args.add(called.args.get(i));
				TypeFunc tf = new TypeFunc(cmd.location, args, ret);
				constraints.add(c.var, tf);
			}
		}
	}

	protected void checkAdditionalConstraints(HSIEForm f, ClosureCmd c) {
		Set<StructWithFieldConstraint> check = new HashSet<>();
		Set<NamedType> nts = new HashSet<>();
		final Set<TypeInfo> all = constraints.get(c.var);
		for (TypeInfo ctr : all) {
			if (ctr instanceof StructWithFieldConstraint) {
				check.add((StructWithFieldConstraint) ctr);
			} else if (ctr instanceof NamedType) {
				nts.add((NamedType) ctr);
			}
		}

		for (StructWithFieldConstraint ctr : check) {
			if (nts.isEmpty())
				errors.message(ctr.posn, "there is no valid type to identify the field operator for " + ctr.fname);
			else {
				for (NamedType nt : nts) {
					RWStructDefn ti = structs.get(nt.name);
					if (ti.findField(ctr.fname) == null)
						errors.message(ctr.posn, "the type " + ti.name() + " does not have a field " + ctr.fname);
				}
			}
		}
		all.removeAll(check);
	}
	
	private VarName isPushScope(HSIEBlock cmd) {
		if (cmd instanceof PushExternal && ((PushExternal)cmd).fn instanceof ScopedVar)
			return ((ScopedVar)((PushExternal)cmd).fn).id;
		return null;
	}

	protected void checkArgType(TypeInfo want, TypeInfo have) throws NeedIndirectionException {
		logger.info("Compare " + want + " to " + have);
		if (want instanceof NamedType && ((NamedType)want).name.equals("Any"))
			return; // this is not much of a constraint, but can confuse things
		if (want instanceof TypeVar) {
			constraints.add(((TypeVar)want).var, have);
		}
		if (have instanceof TypeVar) {
			constraints.add(((TypeVar)have).var, want);
		}
		if (want instanceof TypeFunc && !(have instanceof TypeVar)) {
			TypeFunc wf = (TypeFunc) want;
			if (!(have instanceof TypeFunc))
				throw new UtilException("Cannot pass " + have + " to " + want + ": not function");
			TypeFunc hf = (TypeFunc) freshPolys(have, new HashMap<>());
			if (wf.args.size() != hf.args.size())
				throw new UtilException("Wrong arity: " + have + " not " + want);
			for (int i=0;i<wf.args.size();i++)
				checkArgType(wf.args.get(i), hf.args.get(i));
		}
	}

	private void processHSI(HSIEForm f, HSIEBlock blk) {
		for (HSIEBlock c : blk.nestedCommands()) {
			try {
				processOne(f, c);
			} catch (NeedIndirectionException ex) {
				throw new UtilException("this should have been handled", ex);
			}
		}
	}

	protected void processOne(HSIEForm f, HSIEBlock c) throws NeedIndirectionException {
		if (c instanceof Head || c instanceof ErrorCmd)
			return;
		if (c instanceof Switch) {
			Switch sw = (Switch) c;
			RWStructDefn sd = structs.get(sw.ctor);
			if (sd != null) {
				Map<String, TypeVar> mapping = new HashMap<>();
				constraints.add(sw.var, freshPolys(structTypes.get(sd.name()), mapping));
				for (HSIEBlock sc : sw.nestedCommands()) {
					if (sc instanceof BindCmd) {
						BindCmd b = (BindCmd)sc;
						TypeInfo ty = freshPolys(convertType(sd.findField(b.field).type), mapping);
						logger.info("Processing BIND " + b.bind + " with " + ty);
						constraints.add(b.bind, ty);
					} else
						processOne(f, sc);
				}
			} else {
				constraints.add(sw.var, getTypeOf(sw.location, sw.ctor));
				processHSI(f, sw);
			}
		} else if (c instanceof BindCmd) {
			throw new UtilException("Cannot have BIND except as child of SWITCH");
		} else if (c instanceof IFCmd) {
			IFCmd ic = (IFCmd) c;
			// TODO: state with certainty that Boolean is an option for sw.var (in this context, is a requirement - but what is the nature of a context?)
			processHSI(f, ic);
		} else if (c instanceof PushReturn) {
			PushReturn pr = (PushReturn) c;
			Var rv = returns.get(f.funcName.uniqueName());
			if (pr instanceof PushVar) {
				VarInSource val = ((PushVar)pr).var;
				logger.info("Need to add a constraint to " + rv + " of " + val);
				constraints.add(rv, new TypeVar(val.loc, val.var));
			} else {
				TypeInfo ty = freshPolys(getTypeOf(f, pr), new HashMap<>());
				logger.info("Can return " + rv + " as " + ty);
				constraints.add(rv, ty);
			}
		} else 
			logger.info("Handle " + c);
	}

	private void checkSendCall(HSIEForm f, ClosureCmd c) throws NeedIndirectionException {
		// By definition, the closure must have four fields: Send; the contract var; the method (string literal); a closure pointing to the list of args (or else Nil)
		List<HSIEBlock> ncs = c.nestedCommands();
		TypeInfo ot = getTypeOf(f, ncs.get(1));
		PushString ps = (PushString) ncs.get(2);
		if (ot instanceof NamedType) {
			NamedType tot = (NamedType) ot;
			checkMethodCall(tot.location(), f, ncs, ps, tot.myName);
		} else if (ot instanceof TypeVar) {
			TypeVar tv = (TypeVar) ot;
			Var v = tv.var;
			Set<TypeInfo> cs = constraints.get(v);
			Set<NameOfThing> nts = new HashSet<NameOfThing>();
			for (TypeInfo t : cs) {
				if (t instanceof NamedType)
					nts.add(((NamedType)t).myName);
			}
			if (nts.size() != 1)
				throw new UtilException("Cannot handle " + v + " with constraints " + cs + " leading to " + nts);
			checkMethodCall(tv.location(), f, ncs, ps, CollectionUtils.any(nts));
		} else if (ot instanceof TypeFunc) {
			TypeFunc tf = (TypeFunc) ot;
			if (tf.args.size() != 1)
				throw new UtilException("Should have just 1 arg");
			checkMethodCall(tf.location(), f, ncs, ps, ((NamedType)tf.args.get(0)).myName);
		} else
			throw new UtilException("Cannot handle ot = " + ot + " " + ot.getClass());
	}

	private void checkMethodCall(InputPosition loc, HSIEForm f, List<HSIEBlock> ncs, PushString ps, NameOfThing tn) throws NeedIndirectionException {
		Type t = (Type) rw.getMe(loc, tn).defn;
		if (t instanceof TypeWithMethods) {
			TypeWithMethods cd = (TypeWithMethods) t;
			FunctionType mt = cd.getMethodType(ps.sval.text);
			checkCallArgs(f, mt, 0, ncs.get(3));
		} else
			throw new UtilException("Cannot handle t = " + t +  " " + t.getClass());
	}

	private void checkCallArgs(HSIEForm f, FunctionType mt, int pos, HSIEBlock cmd) throws NeedIndirectionException {
		boolean isNil = cmd instanceof PushExternal && ((PushExternal)cmd).fn.uniqueName().equals("Nil");
		if (pos >= mt.arity()) {
			// we have run out of args to call
			if (!isNil)
				errors.message(cmd.location, "too many arguments to method");
		} else if (cmd instanceof PushVar) {
			Var cv = ((PushVar)cmd).var.var;
			ClosureGenerator c = f.getClosure(cv);
			List<HSIEBlock> nc = c.nestedCommands();
			HSIEBlock shouldBeCons = nc.get(0);
			if (!(shouldBeCons instanceof PushExternal) || !((PushExternal)shouldBeCons).fn.uniqueName().equals("Cons"))
				throw new UtilException("No, should be cons at " + cv);
			checkArgType(convertType(mt.arg(pos)), getTypeOf(f, nc.get(1)));
			checkCallArgs(f, mt, pos+1, nc.get(2));
		} else if (isNil)
			errors.message(cmd.location, "too few arguments to method");
		else
			throw new UtilException("Cannot handle " + cmd + " " + cmd.getClass());
	}

	private TypeInfo mergeDown(Var v, Set<TypeInfo> tis) {
		if (tis.isEmpty())
			return new TypeVar(new InputPosition("mergeDown", 1, 1, null), v);
		else if (tis.size() == 1)
			return CollectionUtils.any(tis);
		
		List<String> ctors = new ArrayList<String>();
		List<TypeFunc> funcs = new ArrayList<TypeFunc>();
		for (TypeInfo ti : tis) {
			while (ti instanceof TypeFunc && ((TypeFunc) ti).args.size() == 1) {
				ti = ((TypeFunc) ti).args.get(0);
			}
			if (ti instanceof NamedType)
				ctors.add(((NamedType)ti).name);
			else if (ti instanceof TypeFunc)
				funcs.add((TypeFunc)ti);
			else
				throw new NotImplementedException(ti.getClass().getName());
		}
		
		if (!ctors.isEmpty() && !funcs.isEmpty())
			throw new NotImplementedException("Cannot merge functions and structs: " + funcs + " " + ctors);
		else if (funcs.size() > 1)
			throw new NotImplementedException("Merging multiple functions: " + funcs);
		else if (ctors.size() > 1) {
			// try and find a union type that covers exactly and all these cases
			HashSet<RWUnionTypeDefn> possibles = new HashSet<>();
			nextUnion:
			for (RWUnionTypeDefn ud : unions) {
				if (!ctors.contains(ud.name())) {
					// Make sure all the cases are actually used
					for (TypeWithName cs : ud.cases)
						if (!ctors.contains(cs.name()))
							continue nextUnion;
				}
				if (!ud.name().equals("Any")) {
					// make sure all the ctors are in the union
					for (String s : ctors)
						if (!ud.hasCtor(s) && !ud.name().equals(s))
							continue nextUnion;
				}
				
				// OK, this is viable
				possibles.add(ud);
			}
			if (possibles.isEmpty())
				throw new UtilException("There is no good union for " + ctors);
			if (possibles.size() > 1) {
				for (RWUnionTypeDefn p : possibles) {
					if (p.name().equals("Any")) {
						possibles.remove(p);
						break;
					}
				}
				if (possibles.size() > 1)
					throw new UtilException("Two many possible matching unions: " + possibles + " for " + ctors);
			}
			List<TypeInfo> polyArgs = new ArrayList<>();
			RWUnionTypeDefn chosen = CollectionUtils.any(possibles);
			if (chosen.hasPolys()) {
				for (@SuppressWarnings("unused") Type x : chosen.polys())
					polyArgs.add(null);
				for (TypeInfo ti : tis) { // go through the list again, looking for poly vars
					// TODO: I'm not sure this should be allowed to get here;
					// move it somewhere else?
					while (ti instanceof TypeFunc && ((TypeFunc) ti).args.size() == 1) {
						ti = ((TypeFunc) ti).args.get(0);
					}
					NamedType nt = (NamedType) ti;
					if (nt.name.equals(chosen.name())) {
						for (int i=0;i<chosen.polys().size();i++)
							polyArgs.set(i, nt.polyArgs.get(i));
					} else {
						List<Integer> pas = chosen.getCtorPolyArgPosns(nt.name);
						for (int i=0;i<pas.size();i++)
							polyArgs.set(pas.get(i), nt.polyArgs.get(i));
					}
				}
			}
			return new NamedType(chosen.location(), chosen.getTypeName(), polyArgs);
		} else
			throw new NotImplementedException("Other cases");
	}

	private TypeInfo deduceType(Map<Var, Var> renames, Map<Var, TypeInfo> merged, HSIEForm f) {
		Map<Var, PolyInfo> install = new HashMap<Var, PolyInfo>();
		List<TypeInfo> args = new ArrayList<TypeInfo>();
		for (int i=0;i<f.nformal;i++) {
			args.add(merged.get(rename(renames, f.vars.get(i))));
		}
		args.add(merged.get(rename(renames, this.returns.get(f.funcName.uniqueName()))));
		for (int i=0;i<args.size();i++)
			args.set(i, poly(renames, merged, install, args.get(i)));
		return new TypeFunc(f.location, args);
	}

	public void writeLearnedKnowledge(File exportTo, String inPkg, boolean dumpTypes) {
		if (dumpTypes)
			System.out.println("Exporting inferred types at top scope:");
		XML knowledge = buildXML(inPkg, dumpTypes);
		knowledge.write(exportTo);
	}

	public XML buildXML(String inPkg, boolean dumpTypes) {
		KnowledgeWriter kw = new KnowledgeWriter(inPkg, dumpTypes);

		rw.visit(kw, false);

		for (RWUnionTypeDefn td : rw.types.values()) {
			if (td.generate) {
				kw.add(td);
			}
		}

		for (Entry<String, Type> x : this.export.entrySet()) {
			// Only save things in our package
			if (!x.getKey().startsWith(inPkg + "."))
				continue;

			// Don't save any nested definitions, because they will not be accessible from outside the defining object
			if (x.getKey().substring(inPkg.length()+1).indexOf(".") != -1)
				continue;

			kw.add(x.getKey(), (FunctionType) x.getValue());
		}
		return kw.commit();
	}

	private Var rename(Map<Var, Var> renames, Var var) {
		if (renames.containsKey(var))
			return renames.get(var);
		return var;
	}

	// The objective of this is to turn poly vars back into real things
	private TypeInfo poly(Map<Var, Var> renames, Map<Var, TypeInfo> merged, Map<Var, PolyInfo> install, TypeInfo ti) {
		if (ti instanceof TypeVar) {
			Var v = rename(renames, ((TypeVar)ti).var);
			TypeInfo ret = merged.get(v);
			if (ret instanceof TypeVar) {
				Var rv = ((TypeVar)ret).var;
				if (install.containsKey(rv))
					return install.get(rv);
				PolyInfo pv = new PolyInfo(((TypeVar)ret).location(), new String(new char[] { (char)(65+install.size()) }));
				install.put(rv, pv);
				return pv;
			} else
				return poly(renames, merged, install, ret);
		} else if (ti instanceof TypeFunc) {
			TypeFunc tf = (TypeFunc) ti;
			List<TypeInfo> args = new ArrayList<TypeInfo>();
			for (TypeInfo i : tf.args)
				args.add(poly(renames, merged, install, i));
			return new TypeFunc(tf.location(), args);
		} else if (ti instanceof TupleInfo) {
			TupleInfo tf = (TupleInfo) ti;
			List<TypeInfo> args = new ArrayList<TypeInfo>();
			for (TypeInfo i : tf.args)
				args.add(poly(renames, merged, install, i));
			return new TupleInfo(tf.location(), args);
		} else if (ti instanceof NamedType) {
			NamedType nt = (NamedType) ti;
			List<TypeInfo> args = new ArrayList<TypeInfo>();
			if (nt.polyArgs != null) {
				for (TypeInfo i : nt.polyArgs)
					args.add(poly(renames, merged, install, i));
			}
			return new NamedType(nt.location(), nt.myName, args);
		} else if (ti instanceof PolyInfo) {
			return ti;
		} else
			throw new NotImplementedException(ti + " " +(ti == null ? "<null>": ti.getClass().getName()));
	}

	TypeInfo convertType(Type type) {
		if (type instanceof PolyVar)
			return new PolyInfo(type.location(), ((PolyVar) type).name());
		else if (type instanceof RWStructDefn)
			return structTypes.get(((RWStructDefn) type).name());
		else if (type instanceof PrimitiveType ||
				type instanceof RWUnionTypeDefn || type instanceof RWObjectDefn ||
				type instanceof RWContractDecl || type instanceof RWContractImplements || type instanceof RWContractService ||
				type instanceof RWHandlerImplements)
			try {
				return getTypeOf(type.location(), ((TypeWithName) type).name());
			} catch (NeedIndirectionException ex) {
				throw new UtilException("this should have been handled", ex);
			}
		else if (type instanceof InstanceType) {
			List<TypeInfo> args = new ArrayList<>();
			for (Type t : ((InstanceType)type).polys())
				args.add(convertType(t));
			return new NamedType(type.location(), ((InstanceType) type).getTypeName(), args);
		} else if (type instanceof FunctionType) {
			FunctionType ft = (FunctionType) type;
			List<TypeInfo> args = new ArrayList<TypeInfo>();
			for (int i=0;i<ft.arity()+1;i++)
				args.add(convertType(ft.arg(i)));
			return new TypeFunc(ft.location(), args);
		} else if (type instanceof TypeOfSomethingElse) {
			String other = ((TypeOfSomethingElse)type).other().uniqueName();
			try {
				return getTypeOf(type.location(), other);
			} catch (NeedIndirectionException ex) {
				return new TypeIndirect(type.location(), other);
			}
		} else
			throw new UtilException("Cannot convert " + type.getClass());
	}

	private TypeInfo getTypeOf(HSIEForm form, HSIEBlock cmd) throws NeedIndirectionException {
		if (cmd instanceof PushExternal) {
			PushExternal pe = (PushExternal) cmd;
			String name = pe.fn.uniqueName();
			if (pe.fn instanceof CardMember) {
				CardMember cm = (CardMember) pe.fn;
				return freshPolys(convertType(cm.type), new HashMap<>());
			} else if (pe.fn instanceof HandlerLambda) {
				HandlerLambda hl = (HandlerLambda) pe.fn;
				SolidName sdname = new SolidName(hl.clzName.name, hl.clzName.baseName+"$struct");
				RWStructDefn sd = structs.get(sdname.uniqueName());
				for (RWStructField sf : sd.fields) {
					if (sf.name.equals(hl.var)) {
						return freshPolys(convertType(sf.type), new HashMap<>());
					}
				}
				throw new UtilException("Could not find field " + hl.var + " in handler " + sdname.uniqueName());
			} else {
				if (ctors.containsKey(name))
					return freshPolys(convertType(ctors.get(name)), new HashMap<>());
				return getTypeOf(cmd.location, name);
			}
		} else if (cmd instanceof PushTLV) {
			PushTLV pt = (PushTLV) cmd;
			return freshPolys(deList(getTypeOf(cmd.location, pt.tlv.dataFunc.uniqueName())), new HashMap<>());
		} else if (cmd instanceof PushVar) {
			VarInSource cov = ((PushVar)cmd).var;
			Var var = cov.var;
			if (scoping.containsKey(var))
				return getTypeOf(form, scoping.get(var));
			return new TypeVar(cov.loc, var);
		} else if (cmd instanceof PushInt || cmd instanceof PushDouble) {
			return getTypeOf(cmd.location, "Number");
		} else if (cmd instanceof PushString) {
			return getTypeOf(cmd.location, "String");
		} else if (cmd instanceof PushBool) {
			return getTypeOf(cmd.location, "Boolean");
		} else if (cmd instanceof PushCSR) {
			if (form.inCard == null) {
				form.dump(new PrintWriter(System.err));
				throw new UtilException("Cannot get type of CSR if no card");
			}
			return getTypeOf(cmd.location, form.inCard.uniqueName());
		} else if (cmd instanceof PushFunc) {
			FunctionLiteral func = ((PushFunc)cmd).func;
			return getTypeOf(func.location, func.name.uniqueName());
		} else
			throw new UtilException("Need to determine type of " + cmd.getClass());
	}

	private TypeInfo getTypeOf(InputPosition pos, String name) throws NeedIndirectionException {
		TypeInfo ret = localKnowledge.get(name);
		if (ret != null)
			return ret;
		ret = globalKnowledge.get(name);
		if (ret == null)
			throw new NeedIndirectionException(name);
		return ret;
	}
	
	private TypeInfo freshPolys(TypeInfo ti, Map<String, TypeVar> curr) throws NeedIndirectionException {
		if (ti instanceof TypeVar) {
			return ti;
		} else if (ti instanceof PolyInfo) {
			PolyInfo pv = (PolyInfo) ti;
			if (!curr.containsKey(pv.name)) {
				Var rv = new Var(nextVar++);
				curr.put(pv.name, new TypeVar(pv.location(), rv));
				logger.info("Allocating " + rv + " as fresh var for poly type " + pv.name);
				constraints.ensure(rv);
			}
			return curr.get(pv.name);
		} else if (ti instanceof NamedType) {
			NamedType nt = (NamedType) ti;
			if (nt.polyArgs == null)
				return nt;
			else {
				List<TypeInfo> polyArgs = new ArrayList<TypeInfo>();
				for (TypeInfo t : nt.polyArgs) {
					polyArgs.add(freshPolys(t, curr));
				}
				return new NamedType(nt.location(), nt.myName, polyArgs);
			}
		} else if (ti instanceof TypeFunc) {
			TypeFunc tf = (TypeFunc) ti;
			List<TypeInfo> args = new ArrayList<TypeInfo>();
			for (TypeInfo x : tf.args)
				args.add(freshPolys(x, curr));
			return new TypeFunc(tf.location(), args);
		} else if (ti instanceof TypeIndirect) {
			return getTypeOf(((TypeIndirect)ti).location, ((TypeIndirect) ti).other);
		} else
			throw new UtilException("Do what now? " + ti);
	}
	
	private TypeInfo instantiate(TypeInfo subst, NamedType nt, RWObjectDefn od) {
		if (subst instanceof TypeFunc) {
			TypeFunc work = (TypeFunc) subst;
			List<TypeInfo> args = new ArrayList<TypeInfo>();
			for (TypeInfo ti : work.args) {
				if (ti instanceof PolyInfo) {
					String s = ((PolyInfo)ti).name;
					TypeInfo ut = null;
					for (int i=0;i<od.polys().size();i++) {
						if (od.poly(i).name().equals(s))
							ut = nt.polyArgs.get(i);
					}
					if (ut == null)
						throw new UtilException("Could not find poly " + s + " in " + od);
					args.add(ut);
				} else
					args.add(ti);
			}
			return new TypeFunc(work.location(), args);
		} else
			throw new UtilException("Cannot handle subst being " + subst + " " + subst.getClass());
	}

	Type asType(TypeInfo ti) {
		if (ti instanceof NamedType) {
			NamedType nt = (NamedType) ti;
			Object obj = rw.getMe(nt.location(), nt.myName).defn;
			if (obj instanceof CardGrouping)
				obj = ((CardGrouping)obj).struct;
			Type ret = (Type) obj;
			if (ret == null)
				throw new UtilException("Could not find type " + nt.name);
			if (nt.polyArgs.isEmpty())
				return ret;
			
			// if we have poly vars, we need to create an instance ...
			List<Type> polys = new ArrayList<Type>();
			for (TypeInfo t : nt.polyArgs)
				polys.add(nonFunction(asType(t)));
			return ((TypeWithName)ret).instance(nt.location(), polys);
		} else if (ti instanceof TypeFunc) {
			TypeFunc tf = (TypeFunc) ti; 
			List<Type> args = new ArrayList<Type>();
			for (TypeInfo t : tf.args)
				args.add(asType(t));
			return new FunctionType(tf.location(), args);
		} else if (ti instanceof TupleInfo) {
			TupleInfo tf = (TupleInfo) ti; 
			List<Type> args = new ArrayList<Type>();
			for (TypeInfo t : tf.args)
				args.add(asType(t));
			return new TupleType(tf.location(), args);
		} else if (ti instanceof PolyInfo) {
			PolyInfo pi = (PolyInfo) ti;
			return new PolyVar(pi.location(), pi.name);
		} else if (ti instanceof TypeIndirect) {
			// This shouldn't happen in types we care about, but in HandlerLambdas
			// I don't think we actually test this ever
			return new PrimitiveType(((TypeIndirect) ti).location(), new SolidName(null, "Any"));
		} else
			throw new UtilException("Have computed type " + ti.getClass() + " but can't convert back to real Type");
	}

	private Type nonFunction(Type asType) {
		if (asType instanceof FunctionType) {
			FunctionType ft = (FunctionType) asType;
			if (ft.arity() == 0)
				return ft.arg(0);
		}
		return asType;
	}

	public Type getTypeAsCtor(InputPosition location, String uniqueName) {
		if (ctors.containsKey(uniqueName))
			return ctors.get(uniqueName);
		else if (export.containsKey(uniqueName))
			return export.get(uniqueName);
		else
			throw new UtilException("There is no name " + uniqueName);
	}

	private TypeInfo deList(TypeInfo typeOf) {
		// There may well be multiple cases here; add them as we see them
		if (typeOf instanceof TypeFunc) {
			TypeFunc tf = (TypeFunc)typeOf;
			if (tf.args.size() == 1)
				return deList(tf.args.get(0));
			throw new UtilException("deList(" + typeOf + ")");
		} else if (typeOf instanceof NamedType) {
			NamedType nt = (NamedType) typeOf;
			if (nt.name.equals("Croset"))
				return nt.polyArgs.get(0);
			else
				throw new UtilException("deList(" + typeOf + ")");
		} else
			throw new UtilException("deList(" + typeOf + ")");
	}

	public Type getExportedType(String name) {
		return export.get(name);
	}

	public void define(String name, Type ty) {
		export.put(name, ty);
	}
}
