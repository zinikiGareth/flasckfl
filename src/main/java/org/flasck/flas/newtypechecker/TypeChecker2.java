package org.flasck.flas.newtypechecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.UtilException;

public class TypeChecker2 {
	private final ErrorResult errors;
	private final Map<String, RWStructDefn> structs = new HashMap<String, RWStructDefn>();
	// is there a real need to keep these separate?  especially when we are promoting?
	private final Map<String, TypeInfo> globalKnowledge = new HashMap<String, TypeInfo>();
	private final Map<String, TypeInfo> localKnowledge = new HashMap<String, TypeInfo>();
	private final ListMap<Var, Constraint> constraints = new ListMap<Var, Constraint>();
	private final Map<String, Var> returns = new HashMap<String, Var>();
	private int nextVar;
	
	public TypeChecker2(ErrorResult errors) {
		this.errors = errors;
	}

	public void populateTypes(Rewriter rw) {
		for (Type bi : rw.primitives.values()) {
			globalKnowledge.put(bi.name(), new NamedType(bi.name()));
		}
		for (RWUnionTypeDefn ud : rw.types.values()) {
			globalKnowledge.put(ud.name(), new NamedType(ud.name()));
		}
		for (RWObjectDefn od : rw.objects.values()) {
			globalKnowledge.put(od.name(), new NamedType(od.name()));
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
			globalKnowledge.put(sd.uniqueName(), new TypeFunc(fs, new NamedType(sd.uniqueName(), polys)));
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
		}
		System.out.println("allocating FRESH vars from " + nextVar);

		// 1c. Now allocate FRESH vars for the return types
		for (HSIEForm f : forms) {
			System.out.println("Allocating function/return vars for " + f.fnName);
			Var rv = new Var(nextVar++);
			localKnowledge.put(f.fnName, new TypeFunc(f.vars, f.nformal, new TypeVar(rv)));
			System.out.println("Return type of " + f.fnName + " is " + rv);
			if (constraints.contains(rv))
				throw new UtilException("Duplicate var definition " + rv);
			constraints.ensure(rv);
			returns.put(f.fnName, rv);
		}
		
		// 1d. Now allocate FRESH vars for any scoped variables that still haven't been defined
		for (HSIEForm f : forms) {
			System.out.println("Checking type of " + f.fnName);
			if (globalKnowledge.containsKey(f.fnName))
				errors.message(f.location, "duplicate entry for " + f.fnName + " in type checking");
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
		// 2a. look at all the closures we have
		for (HSIEForm f : forms) {
			for (ClosureCmd c : f.closures()) {
				processClosure(f, c);
			}
		}
		
		// 2b. and at the switching blocks
		for (HSIEForm f : forms) {
			processHSI(f, f);
		}
		
		for (Var k : constraints.keySet())
			System.out.println(k + " -> " + constraints.get(k));
		
		// 3. Resolve constraints
		
		// 4. Deduce types by looking at formal arguments & return types
	}

	protected void processClosure(HSIEForm f, ClosureCmd c) {
		List<HSIEBlock> cmds = c.nestedCommands();
		if (c.justScoping) {
			TypeFunc ti = (TypeFunc) getTypeOf(cmds.get(0));
			System.out.println("Copying type of " + cmds.get(0) + " to " + c.var + ": " + ti);
			constraints.add(c.var, new FnCallConstraint(ti.args));
			return;
		}
		System.out.println("Need to check " + f.fnName + " " + c.var);
		List<TypeInfo> argtypes = new ArrayList<TypeInfo>();
		for (int i=1;i<cmds.size();i++) {
			TypeInfo ai = getTypeOf(cmds.get(i));
			argtypes.add(ai);
		}
		HSIEBlock cmd = cmds.get(0);
		if (cmd instanceof PushVar) {
			Var fv = ((PushVar)cmd).var.var;
			constraints.add(fv, new FnCallConstraint(argtypes, new TypeVar(c.var)));
		} else {
			// I think we need to consider FLEval.field as a special case here ...
			TypeInfo ti = freshPolys(getTypeOf(cmd), new HashMap<>());
			// TODO: if function is polymorphic, introduce fresh vars NOW
			System.out.println("In " + c.var + " fi = " + ti);
			if (c.var.idx == 9)
				System.out.println(ti);
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
				constraints.add(sw.var, new SwitchConstraint(sd));
				for (HSIEBlock sc : sw.nestedCommands()) {
					if (sc instanceof BindCmd) {
						BindCmd b = (BindCmd)sc;
						TypeInfo ty = convertType(sd.findField(b.field).type);
						System.out.println("Processing BIND " + b.bind + " with " + ty);
						if (ty != null) // null for poly vars, which (AFAIK) don't add constraints
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
			return new TypeVar(((PushVar)cmd).var.var);
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
			if (!curr.containsKey(pv.name))
				curr.put(pv.name, new TypeVar(new Var(nextVar++)));
			return curr.get(pv.name);
		} else if (ti instanceof NamedType) {
			NamedType nt = (NamedType) ti;
			if (nt.polyArgs == null)
				return nt;
			else {
				List<TypeInfo> polyArgs = new ArrayList<TypeInfo>();
				for (TypeInfo t : nt.polyArgs) {
//					if (t instanceof PolyInfo) {
//						PolyInfo pi = (PolyInfo) t;
//						if (curr.containsKey(pi.name)) // we assume that "equals" works for poly vars
//							polyArgs.add(curr.get(t));
//						else {
//							TypeVar tv = new TypeVar(new Var(nextVar++));
//							curr.put(pi.name, tv);
//							polyArgs.add(tv);
//						}
//					} else {
						polyArgs.add(freshPolys(t, curr));
//					}
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
