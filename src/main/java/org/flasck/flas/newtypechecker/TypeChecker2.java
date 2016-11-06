package org.flasck.flas.newtypechecker;

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
	private final Map<String, RWStructDefn> structs = new HashMap<String, RWStructDefn>();
	private final Map<String, TypeInfo> structTypes = new HashMap<String, TypeInfo>();
	// is there a real need to keep these separate?  especially when we are promoting?
	private final Map<String, TypeInfo> globalKnowledge = new HashMap<String, TypeInfo>();
	private final Map<String, TypeInfo> localKnowledge = new HashMap<String, TypeInfo>();
	private final SetMap<Var, Constraint> constraints = new SetMap<Var, Constraint>(new SimpleVarComparator(), new ConstraintComparator());
	private final Map<String, Var> returns = new HashMap<String, Var>();
	private int nextVar;
	private final Map<Var, HSIEBlock> scoping = new HashMap<>();
	private final Map<String, RWUnionTypeDefn> unions = new HashMap<>();
	
	public TypeChecker2(ErrorResult errors) {
		this.errors = errors;
	}

	public void populateTypes(Rewriter rw) {
		for (Type bi : rw.primitives.values()) {
			globalKnowledge.put(bi.name(), new NamedType(bi.name()));
		}
		for (RWUnionTypeDefn ud : rw.types.values()) {
			List<TypeInfo> polys = null;
			if (ud.hasPolys()) {
				polys = new ArrayList<>();
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
			List<TypeInfo> polys = null;
			if (od.hasPolys()) {
				polys = new ArrayList<>();
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
			List<TypeInfo> polys = null;
			if (sd.hasPolys()) {
				polys = new ArrayList<>();
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
		
		// 3. Resolve constraints
		closeConstraints();
		
		for (Var k : constraints.keySet())
			System.out.println(k + " -> " + constraints.get(k));
		
		// something in here has to be recursive over type parameters.  Is it the whole block, unify, or what?
		Map<Var, TypeInfo> unions = unifyConstraints();

		for (Var k : constraints.keySet())
			System.out.println(k + " -> " + constraints.get(k));

		for (Entry<Var, TypeInfo> e : unions.entrySet())
			System.out.println(e.getKey() + " -> " + e.getValue());
		
		Map<Var, TypeInfo> resolved = new TreeMap<>(new SimpleVarComparator());
		for (Entry<Var, TypeInfo> e : unions.entrySet()) {
			resolved.put(e.getKey(), resolveUnions(e.getValue(), unions));
		}
		unions = resolved;

		for (Entry<Var, TypeInfo> e : unions.entrySet())
			System.out.println(e.getKey() + " -> " + e.getValue());
		
		
		
		// 4. Deduce types by looking at formal arguments & return types
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
			constraints.add(fv, new FnCallConstraint(argtypes, new TypeVar(c.var)));
			constraints.add(c.var, new AppliedFnConstraint(fv, cmds.size()-1));
		} else {
			// I think we need to consider FLEval.field as a special case here ...
			TypeInfo ti = freshPolys(getTypeOf(cmd), new HashMap<>());
			// TODO: if function is polymorphic, introduce fresh vars NOW
			System.out.println("In " + c.var + " fi = " + ti);
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
					constraints.add(((TypeVar)want).var, new TopOfConstraint(have));
				}
				if (have instanceof TypeVar) {
					constraints.add(((TypeVar)have).var, new BottomOfConstraint(want));
				}
			}
			TypeInfo ret = called.args.get(called.args.size()-1);
			if (called.args.size() == argtypes.size()+1) {
				constraints.add(c.var, new TypeConstraint(ret));
			} else {
				List<TypeInfo> args = new ArrayList<TypeInfo>();
				for (int i=argtypes.size();i+1<called.args.size();i++)
					args.add(argtypes.get(i));
				TypeFunc tf = new TypeFunc(argtypes, ret);
				constraints.add(c.var, new TypeConstraint(tf));
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
				constraints.add(sw.var, new SwitchConstraint((NamedType) freshPolys(structTypes.get(sd.name()), mapping)));
				for (HSIEBlock sc : sw.nestedCommands()) {
					if (sc instanceof BindCmd) {
						BindCmd b = (BindCmd)sc;
						TypeInfo ty = freshPolys(convertType(sd.findField(b.field).type), mapping);
						System.out.println("Processing BIND " + b.bind + " with " + ty);
						constraints.add(b.bind, new BindConstraint(ty));
					} else
						processOne(f, sc);
				}
			} else {
				constraints.add(sw.var, new TypeConstraint(new NamedType(sw.ctor)));
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
				constraints.add(rv, new TopOfConstraint(new TypeVar(val.var)));
			} else {
				TypeInfo ty = getTypeOf(pr);
				System.out.println("Can return " + rv + " as " + ty);
				constraints.add(rv, new TopOfConstraint(ty));
			}
		} else 
			System.out.println("Handle " + c);
	}

	private void closeConstraints() {
		boolean didSomething = true;
		while (didSomething) {
			didSomething = false;
			 
			for (Var v : constraints.keySet()) {
				Set<Constraint> values = constraints.get(v);
				int sz = values.size();
				Set<Constraint> toAdd = new HashSet<>();
				for (Constraint cs : values) {
					if (cs instanceof TopOfConstraint) {
						pullConstraintsFromTypeVar(toAdd, ((TopOfConstraint) cs).have, true);
					} else if (cs instanceof TypeConstraint) {
						pullConstraintsFromTypeVar(toAdd, ((TypeConstraint) cs).ty, false);
					} else if (cs instanceof BindConstraint) {
						pullConstraintsFromTypeVar(toAdd, ((BindConstraint) cs).ty, false);
					}
				}
				values.addAll(toAdd);
				didSomething |= values.size() > sz;
			}
		}
	}

	protected void pullConstraintsFromTypeVar(Set<Constraint> toAdd, TypeInfo hv, boolean makeMoreTop) {
		if (hv instanceof TypeVar) {
			Var rv = ((TypeVar)hv).var;
			for (Constraint rc : constraints.get(rv))
				if (rc instanceof BottomOfConstraint)
					;
				else if (rc instanceof TopOfConstraint || !makeMoreTop)
					toAdd.add(rc);
				else if (rc instanceof TypeConstraint)
					toAdd.add(new TopOfConstraint(((TypeConstraint)rc).ty));
				else if (rc instanceof BindConstraint)
					toAdd.add(new TopOfConstraint(((BindConstraint)rc).ty));
		}
	}

	private Map<Var, TypeInfo> unifyConstraints() {
		Map<Var, TypeInfo> unions = new TreeMap<>(new SimpleVarComparator());
		// TODO: I think we need to copy this before we start hacking it around
		for (Var v : constraints.keySet()) {
			Set<Constraint> values = constraints.get(v);
			Set<Constraint> toRemove = new HashSet<>();
			for (Constraint cs : values) {
				if (cs instanceof BottomOfConstraint)
					toRemove.add(cs);
				else if (cs instanceof TopOfConstraint)
					removeVars(toRemove, cs, ((TopOfConstraint) cs).have);
				else if (cs instanceof TypeConstraint)
					removeVars(toRemove, cs, ((TypeConstraint) cs).ty);
				else if (cs instanceof BindConstraint)
					removeVars(toRemove, cs, ((BindConstraint) cs).ty);
			}
			values.removeAll(toRemove);
		}
		for (Var v : constraints.keySet())
			unions.put(v, makeUnion(constraints.get(v)));
		for (Var v : constraints.keySet())
			applyFunctions(unions, v, constraints.get(v));
		return unions;
	}

	private void removeVars(Set<Constraint> toRemove, Constraint cs, TypeInfo hv) {
		if (hv instanceof TypeVar) {
			toRemove.add(cs);
		}
	}

	private TypeInfo makeUnion(Set<Constraint> values) {
		Set<TypeInfo> types = new HashSet<TypeInfo>();
		for (Constraint val : values) {
			if (val instanceof AppliedFnConstraint)
				continue;
			types.add(val.typeInfo());
		}
		if (types.isEmpty())
			return null;
		else if (types.size() == 1)
			return CollectionUtils.any(types);
		else
			return unifyTypes(types);
	}
	
	private void applyFunctions(Map<Var, TypeInfo> unions, Var v, Set<Constraint> values) {
		if (values.isEmpty())
			return;
		
		HashSet<TypeInfo> toAdd = new HashSet<TypeInfo>();
		for (Constraint cs : values) {
			if (cs instanceof AppliedFnConstraint) {
				toAdd.add(((AppliedFnConstraint)cs).typeInfo(unions));
			}
		}
		if (toAdd.isEmpty())
			return;
		TypeInfo already = unions.get(v);
		if (already != null)
			toAdd.add(already);
		TypeInfo addMe;
		if (toAdd.size() == 1)
			addMe = CollectionUtils.any(toAdd);
		else
			addMe = unifyTypes(toAdd);
		if (addMe instanceof TypeVar && ((TypeVar)addMe).var.equals(v))
			return; // OK, I'm me.  Big deal.
		unions.put(v, addMe);
	}
	
	// There are at least 3 separate things we need to unify here
	// First, we can unify multiple constructors into a union type, under certain circumstances
	// Secondly, we can unify functions by ensuring that all instances have the same arity, then unifying their arguments
	// Thirdly, we can unify polymorphic types by unifying the polymorphic arguments
	// TODO: I believe this will be recursive, but I have some doubts about the termination conditions.
	// Theoretically, it should end, because the recursion is structural; however, the need to reference other
	// variables throws a spanner in the works.
	// TODO: I think the solution to that is to unify the args later by creating a new UnifyTypeInfo right now.
	private TypeInfo unifyTypes(Set<TypeInfo> set) {
		Set<RWUnionTypeDefn> unions = new HashSet<RWUnionTypeDefn>();
		Set<String> structs = new HashSet<String>();
		// Set<TypeInfo> functions = new HashSet<TypeInfo>();
		
		TypeInfo tv = null;
		for (TypeInfo ti : set) {
			// TODO: I'm not sure this should be allowed to get here; move it somewhere else?
			while (ti instanceof TypeFunc && ((TypeFunc)ti).args.size() == 1) {
				ti = ((TypeFunc)ti).args.get(0);
			}
			if (ti instanceof NamedType) {
				NamedType nt = (NamedType) ti;
				String ctor = nt.name;
				structs.add(ctor);
				if (this.unions.containsKey(ctor))
					unions.add(this.unions.get(ctor));
			} else if (ti instanceof TypeVar) {
				tv  = ti;
			} else
				throw new NotImplementedException("There is at least a function case we need to handle: " + ti.getClass());
		}
		if (unions.isEmpty() && structs.isEmpty())
			return tv; // there is no constraint to speak of 
		// TODO: check that not both unions and functions have entries
		// else if unions
		if (structs.size() == 1) {
			// they all have the same type, so we just need to unify any (hypothetical) args
			List<TypeInfo> polyArgs = null;
			for (TypeInfo ti : set) {
				// TODO: I'm not sure this should be allowed to get here; move it somewhere else?
				while (ti instanceof TypeFunc && ((TypeFunc)ti).args.size() == 1) {
					ti = ((TypeFunc)ti).args.get(0);
				}
				// We can now be sure they are all NamedTypes, because otherwise we have already failed
				NamedType nt = (NamedType) ti;
				if (polyArgs == null) {
					polyArgs = new ArrayList<TypeInfo>();
					for (TypeInfo a : nt.polyArgs)
						polyArgs.add(new UnifyType(a));
				} else {
					if (nt.polyArgs.size() != polyArgs.size())
						throw new UtilException("this suggests something went wrong elsewhere");
					for (int i=0;i<nt.polyArgs.size();i++)
						((UnifyType)polyArgs.get(i)).add(nt.polyArgs.get(i));
				}
			}
			return new NamedType(CollectionUtils.any(structs), polyArgs);
		} else {
			RWUnionTypeDefn chosen = null;
			if (unions.size() == 1)
				chosen = CollectionUtils.any(unions);
			else {
				// we need to find exactly one union which matches all the constructors
				for (RWUnionTypeDefn ud : unions) {
					boolean haveAll = true;
					for (Type cs : ud.cases)
						haveAll &= structs.contains(cs.name());
					for (String s : structs) {
						haveAll &= ud.hasCtor(s) || ud.name().equals(s);
					}
					if (haveAll) {
						if (chosen != null)
							throw new UtilException("Cannot choose between " + chosen + " and " + ud);
						chosen = ud;
					}
				}
				if (chosen == null)
					throw new UtilException("There is no union that applies to " + structs);
			}
			List<TypeInfo> polys = new ArrayList<TypeInfo>();
			if (chosen.hasPolys()) {
				for (@SuppressWarnings("unused") Type x : chosen.polys())
					polys.add(new UnifyType());
				for (TypeInfo ti : set) { // go through the list again, looking for poly vars
					// TODO: I'm not sure this should be allowed to get here; move it somewhere else?
					while (ti instanceof TypeFunc && ((TypeFunc)ti).args.size() == 1) {
						ti = ((TypeFunc)ti).args.get(0);
					}
					NamedType nt = (NamedType) ti;
					List<Integer> pas;
					if (nt.name.equals(chosen.name())) {
						pas = new ArrayList<Integer>();
						for (@SuppressWarnings("unused") Type x : chosen.polys())
							pas.add(pas.size());
					} else
						pas = chosen.getCtorPolyArgPosns(nt.name);
					for (int i=0;i<pas.size();i++) {
						((UnifyType) polys.get(pas.get(i))).add(nt.polyArgs.get(i));
					}
				}
			}
			return new NamedType(chosen.name(), polys);
		}
		// else if functions
		// else // huh? something must have an entry
	}

	private TypeInfo resolveUnions(TypeInfo value, Map<Var, TypeInfo> unions) {
		if (value == null)
			return null;
		else if (value instanceof NamedType) {
			NamedType nt = (NamedType) value;
			List<TypeInfo> polyArgs = new ArrayList<TypeInfo>();
			if (nt.polyArgs != null) {
				for (TypeInfo ti : nt.polyArgs) {
					if (!(ti instanceof UnifyType))
						polyArgs.add(ti);
					else {
						UnifyType ut = (UnifyType) ti;
						polyArgs.add(unifyTypes(expandUnions(unions, ut.types)));
					}
				}
			}
			return new NamedType(nt.name, polyArgs);
		} else if (value instanceof TypeFunc) {
			// work needs to be done here, I think
			return value;
		} else
			throw new NotImplementedException("other cases: " + value.getClass());
	}

	private Set<TypeInfo> expandUnions(Map<Var, TypeInfo> unions, Set<TypeInfo> types) {
		Set<TypeInfo> ret = new HashSet<>();
		for (TypeInfo ti : types) {
			if (ti instanceof TypeVar) {
				TypeInfo add = unions.get(((TypeVar)ti).var);
				if (add != null)
					ret.add(add);
			}
			ret.add(ti);
		}
		return ret;
	}

	private TypeInfo convertType(Type type) {
		if (type.iam == WhatAmI.POLYVAR)
			return new PolyInfo(type.name());
		else if (type.iam == WhatAmI.BUILTIN ||
				type instanceof RWStructDefn || type instanceof RWUnionTypeDefn ||
				type instanceof RWContractDecl || type instanceof RWContractImplements || type instanceof RWObjectDefn)
			return getTypeOf(type.location(), type.name());
		else if (type.iam == WhatAmI.INSTANCE) {
			return new InstanceType(type);
		} else if (type.iam == WhatAmI.FUNCTION) {
			List<TypeInfo> args = new ArrayList<TypeInfo>();
			int arity = type.arity();
			for (int i=0;i<arity-1;i++)
				args.add(convertType(type.arg(i)));
			return new TypeFunc(args, convertType(type.arg(arity)));
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
}
