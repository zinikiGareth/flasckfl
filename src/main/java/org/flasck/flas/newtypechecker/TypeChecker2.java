package org.flasck.flas.newtypechecker;

import java.io.PrintWriter;
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
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.rewrittenForm.VarNestedFromOuterFunctionScope;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.Type.WhatAmI;
import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.ClosureCmd;
import org.flasck.flas.vcode.hsieForm.ErrorCmd;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Head;
import org.flasck.flas.vcode.hsieForm.IFCmd;
import org.flasck.flas.vcode.hsieForm.PushCSR;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushInt;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.PushString;
import org.flasck.flas.vcode.hsieForm.PushTLV;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.flasck.flas.vcode.hsieForm.Switch;
import org.flasck.flas.vcode.hsieForm.Var;
import org.flasck.flas.vcode.hsieForm.VarInSource;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.collections.SetMap;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.UtilException;

public class TypeChecker2 {
	private final ErrorResult errors;
	private final Rewriter rw;
	private final Map<String, RWStructDefn> structs = new HashMap<String, RWStructDefn>();
	private final Map<String, TypeInfo> structTypes = new HashMap<String, TypeInfo>();
	// is there a real need to keep these separate?  especially when we are promoting?
	private final Map<String, TypeInfo> globalKnowledge = new HashMap<String, TypeInfo>();
	private final Map<String, TypeInfo> localKnowledge = new HashMap<String, TypeInfo>();
	private final SetMap<Var, TypeInfo> constraints = new SetMap<Var, TypeInfo>(new SimpleVarComparator(), new TypeInfoComparator());
	private final Map<String, Var> returns = new HashMap<String, Var>();
	private int nextVar;
	private final Map<Var, HSIEBlock> scoping = new HashMap<>();
	private final Map<String, RWUnionTypeDefn> unions = new HashMap<>();
	private PrintWriter trackTo;
	
	public TypeChecker2(ErrorResult errors, Rewriter rw) {
		this.errors = errors;
		this.rw = rw;
	}

	// Mainly for golden tests
	public void trackTo(PrintWriter pw) {
		this.trackTo = pw;
	}

	public void populateTypes() {
		for (Type bi : rw.primitives.values()) {
			globalKnowledge.put(bi.name(), new NamedType(bi.name()));
		}
		for (RWUnionTypeDefn ud : rw.types.values()) {
			List<TypeInfo> polys = new ArrayList<>();
			if (ud.hasPolys()) {
				for (Type t : ud.polys())
					polys.add(convertType(t));
			}
			NamedType uty = new NamedType(ud.name(), polys);
			globalKnowledge.put(ud.name(), uty);
			
			for (Type x : ud.cases) {
				unions.put(x.name(), ud);
			}
		}
		for (RWObjectDefn od : rw.objects.values()) {
			List<TypeInfo> polys = new ArrayList<>();
			if (od.hasPolys()) {
				for (Type t : od.polys())
					polys.add(convertType(t));
			}
			globalKnowledge.put(od.name(), new NamedType(od.name(), polys));
		}
		for (RWContractDecl cd : rw.contracts.values())
			globalKnowledge.put(cd.name(), new NamedType(cd.name()));
		for (RWStructDefn sd : rw.structs.values()) {
			structs.put(sd.uniqueName(), sd);
			List<TypeInfo> fs = new ArrayList<>();
			for (RWStructField f : sd.fields)
				fs.add(convertType(f.type));
			List<TypeInfo> polys = new ArrayList<>();
			if (sd.hasPolys()) {
				for (Type t : sd.polys())
					polys.add(convertType(t));
			}
			NamedType sty = new NamedType(sd.uniqueName(), polys);
			structTypes.put(sd.uniqueName(), sty);
			globalKnowledge.put(sd.uniqueName(), new TypeFunc(fs, sty));
		}
		for (Entry<String, CardGrouping> d : rw.cards.entrySet()) {
			globalKnowledge.put(d.getKey(), new NamedType(d.getKey()));
			// The elements of the card struct can appear directly as CardMembers
			// push their types into the knowledge
			for (RWStructField f : d.getValue().struct.fields) {
				// TODO: right now, I feel that renaming this is really a rewriter responsibility, but I'm not clear on the consequences
				globalKnowledge.put(d.getKey()+"."+f.name, convertType(f.type));
			}
		}
		for (RWFunctionDefinition fn : rw.functions.values()) {
			if (fn.getType() != null) // a function has already been typechecked
				globalKnowledge.put(fn.name(), convertType(fn.getType()));
		}
	}

	// Typecheck a set of HSIE forms in parallel ...
	public void typecheck(Set<HSIEForm> forms) {
		
		// 1. initialize the state for doing the checking ...
		// 1a. clean up from previous attempts (should this go in a separate currentState object?)
		localKnowledge.clear();
		constraints.clear();
		returns.clear();
		nextVar=0;
		
		// 1b. define all the vars that are already in the HSIE, trapping the max value for future reference
		Map<String, Var> knownScoped = new HashMap<String, Var>();
		for (HSIEForm f : forms) {
			System.out.println("Checking type of " + f.fnName);
			f.dump(new PrintWriter(System.out));
			if (globalKnowledge.containsKey(f.fnName))
				errors.message(f.location, "duplicate entry for " + f.fnName + " in type checking");
			for (Var v : f.vars) {
				if (constraints.contains(v))
					throw new UtilException("Duplicate var definition " + v);
				constraints.ensure(v);
				if (v.idx >= nextVar)
					nextVar = v.idx+1;
			}
			collectVarNames(knownScoped, f);
			for (ClosureCmd c : f.closures())
				collectVarNames(knownScoped, c);
		}
		System.out.println("collected " + knownScoped);
		for (Entry<String, Var> e : knownScoped.entrySet())
			localKnowledge.put(e.getKey(), new TypeVar(e.getValue()));
		System.out.println("allocating FRESH vars from " + nextVar);

		// 1c. Now allocate FRESH vars for the return types
		for (HSIEForm f : forms) {
			System.out.println("Allocating function/return vars for " + f.fnName);
			Var rv = new Var(nextVar++);
			localKnowledge.put(f.fnName, new TypeFunc(f.vars, f.nformal, new TypeVar(rv)));
			System.out.println("Allocating " + rv + " as return type of " + f.fnName);
			if (constraints.contains(rv))
				throw new UtilException("Duplicate var definition " + rv);
			constraints.ensure(rv);
			returns.put(f.fnName, rv);
		}
		
		// 1d. Now allocate FRESH vars for any scoped variables that still haven't been defined
		for (HSIEForm f : forms) {
			for (VarNestedFromOuterFunctionScope vn : f.scoped) {
				String name = vn.id;
				if (globalKnowledge.containsKey(name) || localKnowledge.containsKey(name)) {
					System.out.println("Have definition for " + name);
					continue;
				}
				Var sv = new Var(nextVar++);
				System.out.println("Introducing scoped var " + sv + " for " + name);
				localKnowledge.put(name, new TypeVar(sv));
				if (constraints.contains(sv))
					throw new UtilException("Duplicate var definition " + sv);
				constraints.ensure(sv);
			}
		}
		
		// 2. collect constraints
		// 2a. define "scoping" closures as what they really are
		for (HSIEForm f : forms) {
			for (ClosureCmd c : f.closures()) {
				if (c.justScoping) {
					scoping.put(c.var, c.nestedCommands().get(0));
					constraints.removeAll(c.var);
				}
			}
		}
		
		// 2b. look at all the closures we have
		for (HSIEForm f : forms) {
			for (ClosureCmd c : f.closures()) {
				processClosure(f, c);
			}
		}
		
		// 2c. and at the switching blocks
		for (HSIEForm f : forms) {
			processHSI(f, f);
		}
		
		for (Var k : constraints.keySet())
			System.out.println(k + " -> " + constraints.get(k));

		// 3. Eliminate vars that are duplicates
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

		System.out.println(renames);
		for (Var k : constraints.keySet())
			System.out.println(k + " -> " + constraints.get(k));

		// 4. Unify type arguments
		
		// TODO: as we don't have this case "right now", but basically v0 -> Cons[Number], Cons[v2]: ah, v2 must be Number
		
		// 5. Merge union types
		Map<Var, TypeInfo> merged = new TreeMap<Var, TypeInfo>(new SimpleVarComparator());
		for (Var k : constraints.keySet())
			merged.put(k, mergeDown(k, constraints.get(k)));

		for (Var k : merged.keySet())
			System.out.println(k + " -> " + merged.get(k));

		// 6. Deduce actual function types
		for (HSIEForm f : forms) {
			TypeInfo nt = deduceType(renames, merged, f);
			System.out.println("Concluded that " + f.fnName + " has type " + nt);
			globalKnowledge.put(f.fnName, nt);
			if (trackTo != null)
				trackTo.println(f.fnName + " :: " + asType(nt));
		}
	}

	private void collectVarNames(Map<String, Var> knownScoped, HSIEBlock f) {
		for (HSIEBlock c : f.nestedCommands()) {
			if (c instanceof Head || c instanceof BindCmd || c instanceof ErrorCmd)
				continue;
			if (c instanceof Switch || c instanceof IFCmd)
				collectVarNames(knownScoped, c);
			else if (c instanceof PushVar) {
				VarInSource v = ((PushVar)c).var;
				if (knownScoped.containsKey(v.called) && knownScoped.get(v.called).idx != v.var.idx)
					throw new UtilException("Inconsistent var names " + v.called + " has " + v.var + " and " + knownScoped.get(v.called));
				knownScoped.put(v.called, v.var);
			} else if (c instanceof PushReturn)
				;
			else
				throw new UtilException("What is " + c + "?");
		}
	}

	protected void processClosure(HSIEForm f, ClosureCmd c) {
		List<HSIEBlock> cmds = c.nestedCommands();
		if (c.justScoping) {
			return;
		}
		System.out.println("Need to check " + f.fnName + " " + c.var);
		List<TypeInfo> argtypes = new ArrayList<TypeInfo>();
		for (int i=1;i<cmds.size();i++) {
			TypeInfo ai = getTypeOf(cmds.get(i));
			argtypes.add(ai);
		}
		HSIEBlock cmd = cmds.get(0);
		if (cmd instanceof PushVar && scoping.containsKey(((PushVar)cmd).var.var))
			cmd = scoping.get(((PushVar)cmd).var.var);
		if (cmd instanceof PushVar) {
			Var fv = ((PushVar)cmd).var.var;
			constraints.add(fv, new TypeFunc(argtypes, new TypeVar(c.var)));
		} else {
			// I think we need to consider FLEval.field as a special case here ...
			TypeInfo ti = freshPolys(getTypeOf(cmd), new HashMap<>());
			// TODO: if function is polymorphic, introduce fresh vars NOW
			System.out.println("In " + c.var + ", cmd = " + cmd + " fi = " + ti);
			if (ti == null) {
				System.out.println(c.var + " has a null first arg");
				return;
			}
			if (!(ti instanceof TypeFunc))
				throw new UtilException("I guess it's possible we could have a constant by itself or something"); // TODO: is this an error?
			TypeFunc called = (TypeFunc) ti;
			for (int i=0;i<argtypes.size();i++) {
				if (called.args.size() < i)
					throw new UtilException("Error about applying a non-function to arg " + i + " in " + c.var);
				TypeInfo want = called.args.get(i);
				TypeInfo have = argtypes.get(i);
				System.out.println("Compare " + want + " to " + have);
				if (want instanceof TypeVar) {
					constraints.add(((TypeVar)want).var, have);
				}
				if (have instanceof TypeVar) {
					constraints.add(((TypeVar)have).var, want);
				}
			}
			TypeInfo ret = called.args.get(called.args.size()-1);
			if (called.args.size() == argtypes.size()+1) {
				constraints.add(c.var, ret);
			} else {
				List<TypeInfo> args = new ArrayList<TypeInfo>();
				for (int i=argtypes.size();i+1<called.args.size();i++)
					args.add(argtypes.get(i));
				TypeFunc tf = new TypeFunc(argtypes, ret);
				constraints.add(c.var, tf);
			}
		}
	}

	private void processHSI(HSIEForm f, HSIEBlock blk) {
		for (HSIEBlock c : blk.nestedCommands()) {
			processOne(f, c);
		}
	}

	protected void processOne(HSIEForm f, HSIEBlock c) {
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
						System.out.println("Processing BIND " + b.bind + " with " + ty);
						constraints.add(b.bind, ty);
					} else
						processOne(f, sc);
				}
			} else {
				constraints.add(sw.var, new NamedType(sw.ctor));
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
			Var rv = returns.get(f.fnName);
			if (pr instanceof PushVar) {
				VarInSource val = ((PushVar)pr).var;
				System.out.println("Need to add a constraint to " + rv + " of " + val);
				constraints.add(rv, new TypeVar(val.var));
			} else {
				TypeInfo ty = getTypeOf(pr);
				System.out.println("Can return " + rv + " as " + ty);
				constraints.add(rv, ty);
			}
		} else 
			System.out.println("Handle " + c);
	}

	private TypeInfo mergeDown(Var v, Set<TypeInfo> tis) {
		if (tis.isEmpty())
			return new TypeVar(v);
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
			for (RWUnionTypeDefn ud : unions.values()) {
				// Make sure all the cases are actually used
				for (Type cs : ud.cases)
					if (!ctors.contains(cs.name()))
						continue nextUnion;
				// make sure all the ctors are in the union
				for (String s : ctors)
					if (!ud.hasCtor(s) && !ud.name().equals(s))
						continue nextUnion;
				
				// OK, this is viable
				possibles.add(ud);
			}
			if (possibles.isEmpty())
				throw new UtilException("There is no good union for " + ctors);
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
			return new NamedType(chosen.name(), polyArgs);
		} else
			throw new NotImplementedException("Other cases");
	}

	private TypeInfo deduceType(Map<Var, Var> renames, Map<Var, TypeInfo> merged, HSIEForm f) {
		Map<Var, PolyInfo> install = new HashMap<Var, PolyInfo>();
		List<TypeInfo> args = new ArrayList<TypeInfo>();
		for (int i=0;i<f.nformal;i++) {
			args.add(merged.get(rename(renames, f.vars.get(i))));
		}
		args.add(merged.get(rename(renames, this.returns.get(f.fnName))));
		System.out.println("have " + args);
		for (int i=0;i<args.size();i++)
			args.set(i, poly(renames, merged, install, args.get(i)));
		System.out.println("install = " + install);
		System.out.println("made " + args);
		if (args.size() == 1)
			return args.get(0);
		else
			return new TypeFunc(args);
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
				PolyInfo pv = new PolyInfo(new String(new char[] { (char)(65+install.size()) }));
				install.put(rv, pv);
				return pv;
			} else
				return ret;
		} else if (ti instanceof TypeFunc) {
			TypeFunc tf = (TypeFunc) ti;
			List<TypeInfo> args = new ArrayList<TypeInfo>();
			for (TypeInfo i : tf.args)
				args.add(poly(renames, merged, install, i));
			return new TypeFunc(args);
		} else if (ti instanceof NamedType) {
			NamedType nt = (NamedType) ti;
			List<TypeInfo> args = new ArrayList<TypeInfo>();
			if (nt.polyArgs != null) {
				for (TypeInfo i : nt.polyArgs)
					args.add(poly(renames, merged, install, i));
			}
			return new NamedType(nt.name, args);
		} else
			throw new NotImplementedException(ti + " " +(ti == null ? "<null>": ti.getClass().getName()));
	}

	private TypeInfo convertType(Type type) {
		if (type.iam == WhatAmI.POLYVAR)
			return new PolyInfo(type.name());
		else if (type.iam == WhatAmI.BUILTIN ||
				type instanceof RWStructDefn || type instanceof RWUnionTypeDefn ||
				type instanceof RWContractDecl || type instanceof RWContractImplements || type instanceof RWObjectDefn)
			return getTypeOf(type.location(), type.name());
		else if (type.iam == WhatAmI.INSTANCE) {
			List<TypeInfo> args = new ArrayList<>();
			for (Type t : type.polys())
				args.add(convertType(t));
			return new NamedType(type.name(), args);
		} else if (type.iam == WhatAmI.FUNCTION) {
			List<TypeInfo> args = new ArrayList<TypeInfo>();
			for (int i=0;i<type.arity()+1;i++)
				args.add(convertType(type.arg(i)));
			return new TypeFunc(args);
		} else
			throw new UtilException("Cannot convert " + type.getClass() + " " + type.iam + ": " + type.name());
	}

	private TypeInfo getTypeOf(HSIEBlock cmd) {
		if (cmd instanceof PushExternal) {
			PushExternal pe = (PushExternal) cmd;
			String name = pe.fn.uniqueName();
			if (name.equals("FLEval.field")) { // . needs special handling
				return null;
			}
			return getTypeOf(cmd.location, name);
		} else if (cmd instanceof PushTLV) {
			PushTLV pt = (PushTLV) cmd;
			String name = pt.tlv.name;
			return getTypeOf(cmd.location, name);
		} else if (cmd instanceof PushVar) {
			Var var = ((PushVar)cmd).var.var;
			if (scoping.containsKey(var))
				return getTypeOf(scoping.get(var));
			return new TypeVar(var);
		} else if (cmd instanceof PushInt) {
			return getTypeOf(cmd.location, "Number");
		} else if (cmd instanceof PushString) {
			return getTypeOf(cmd.location, "String");
		} else if (cmd instanceof PushCSR) {
			return getTypeOf(cmd.location, "Card");
		} else
			throw new UtilException("Need to determine type of " + cmd.getClass());
	}

	private TypeInfo getTypeOf(InputPosition pos, String name) {
		TypeInfo ret = localKnowledge.get(name);
		if (ret != null)
			return ret;
		ret = globalKnowledge.get(name);
		if (ret == null)
			throw new UtilException("the name '" + name + "' cannot be resolved for typechecking");
		return ret;
	}
	
	private TypeInfo freshPolys(TypeInfo ti, Map<String, TypeVar> curr) {
		if (ti instanceof TypeVar) {
			return ti;
		} else if (ti instanceof PolyInfo) {
			PolyInfo pv = (PolyInfo) ti;
			if (!curr.containsKey(pv.name)) {
				Var rv = new Var(nextVar++);
				curr.put(pv.name, new TypeVar(rv));
				System.out.println("Allocating " + rv + " as fresh var for poly type " + pv.name);
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
				return new NamedType(nt.name, polyArgs);
			}
		} else if (ti instanceof TypeFunc) {
			TypeFunc tf = (TypeFunc) ti;
			List<TypeInfo> args = new ArrayList<TypeInfo>();
			for (TypeInfo x : tf.args)
				args.add(freshPolys(x, curr));
			return new TypeFunc(args);
		} else
			throw new UtilException("Do what now? " + ti);
	}
	
	private Type asType(TypeInfo ti) {
		InputPosition posn = new InputPosition("should_have_this", 1, 1, null);
		if (ti instanceof NamedType) {
			NamedType nt = (NamedType) ti;
			Type ret;
			if (rw.primitives.containsKey(nt.name))
				ret = rw.primitives.get(nt.name);
			else if (rw.types.containsKey(nt.name))
				ret = rw.types.get(nt.name);
			else
				throw new UtilException("Could not find type " + nt.name);
			if (nt.polyArgs.isEmpty())
				return ret;
			
			// if we have poly vars, we need to create an instance ...
			List<Type> polys = new ArrayList<Type>();
			for (TypeInfo t : nt.polyArgs)
				polys.add(asType(t));
			return ret.instance(posn, polys);
		} else if (ti instanceof TypeFunc) {
			TypeFunc tf = (TypeFunc) ti; 
			List<Type> args = new ArrayList<Type>();
			for (TypeInfo t : tf.args)
				args.add(asType(t));
			return Type.function(posn, args);
		} else if (ti instanceof PolyInfo) {
			PolyInfo pi = (PolyInfo) ti;
			return Type.polyvar(posn, pi.name);
		} else
			throw new UtilException("Not handled: " + ti.getClass());
	}
}
