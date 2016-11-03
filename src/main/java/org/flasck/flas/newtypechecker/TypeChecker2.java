package org.flasck.flas.newtypechecker;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.Type.WhatAmI;
import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.ClosureCmd;
import org.flasck.flas.vcode.hsieForm.ErrorCmd;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Head;
import org.flasck.flas.vcode.hsieForm.IFCmd;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.flasck.flas.vcode.hsieForm.Switch;
import org.flasck.flas.vcode.hsieForm.Var;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.UtilException;

public class TypeChecker2 {
	private final ErrorResult errors;
	private final Map<String, RWStructDefn> structs = new HashMap<String, RWStructDefn>();
	// is there a real need to keep these separate?  especially when we are promoting?
	private final Map<String, TypeInfo> globalKnowledge = new HashMap<String, TypeInfo>();
	private final Map<String, TypeInfo> localKnowledge = new HashMap<String, TypeInfo>();
	private int nextTv;
	private ListMap<Var, Constraint> constraints = new ListMap<Var, Constraint>();
	
	public TypeChecker2(ErrorResult errors) {
		this.errors = errors;
	}

	public void populateTypes(Rewriter rw) {
		for (Type bi : rw.builtins.values()) {
			globalKnowledge.put(bi.name(), new NamedType(bi.name()));
		}
		for (RWUnionTypeDefn sd : rw.types.values()) {
			globalKnowledge.put(sd.name(), new NamedType(sd.name()));
		}
		for (RWStructDefn sd : rw.structs.values()) {
			structs.put(sd.uniqueName(), sd);
			globalKnowledge.put(sd.uniqueName(), new TypeFunc(sd.fields, sd.uniqueName()));
		}
	}

	// Typecheck a set of HSIE forms in parallel ...
	public void typecheck(Set<HSIEForm> forms) {
		
		// 1. initialize the state for doing the checking ...
		localKnowledge.clear();
		constraints.clear();
		nextTv = 1;
		for (HSIEForm f : forms) {
			System.out.println("Checking type of " + f.fnName);
			if (globalKnowledge.containsKey(f.fnName))
				errors.message(f.location, "duplicate entry for " + f.fnName + " in type checking");
			localKnowledge.put(f.fnName, nextVar());
			for (Var v : f.vars) {
				if (constraints.contains(v))
					throw new UtilException("Duplicate var definition " + v);
				constraints.ensure(v);
			}
		}
		
		// 2. collect constraints
		// 2a. look at all the closures we have
		for (HSIEForm f : forms) {
			for (ClosureCmd c : f.closures()) {
				if (c.justScoping) {
					System.out.println("Not checking scoping closure for " + c.var);
					continue;
				}
				System.out.println("Need to check " + f.fnName + " " + c.var);
				HSIEBlock cmd = c.nestedCommands().get(0);
				if (cmd instanceof PushVar) {
					// this is a tricky case
				} else {
					TypeInfo ti = getTypeOf(cmd);
					if (ti == null)
						continue;
				}
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
				constraints.add(sw.var, new TypeConstraint(sw.ctor));
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
			if (pr instanceof PushVar)
				System.out.println("Need to add a constraint to " + ((PushVar)pr).var);
			else
				System.out.println("Can return " + getTypeOf(pr));
		} else 
			System.out.println("Handle " + c);
	}

	private TypeInfo convertType(Type type) {
		if (type.iam == WhatAmI.POLYVAR)
			return null; // OK, what to do here; introducing a type var seems obvious, but what is it's reach?
		else if (type instanceof RWUnionTypeDefn)
			return getTypeOf(type.location(), type.name());
		else
			throw new UtilException("Cannot convert " + type.getClass() + " " + type.iam);
	}

	private TypeInfo getTypeOf(HSIEBlock cmd) {
		if (cmd instanceof PushExternal) {
			String name = ((PushExternal)cmd).fn.uniqueName();
			return getTypeOf(cmd.location, name);
		} else
			throw new UtilException("Need to determine type of " + cmd.getClass());
	}

	private TypeInfo getTypeOf(InputPosition pos, String name) {
		TypeInfo ret = localKnowledge.get(name);
		if (ret != null)
			return ret;
		ret = globalKnowledge.get(name);
		if (ret == null) {
			errors.message(pos, "the name '" + name + "' cannot be resolved for typechecking");
		}
		return ret;
	}
	private TypeVar nextVar() {
		return new TypeVar(nextTv++);
	}

}
