package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;

public class GroupChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private CurrentTCState state;
	private TypeBinder currentFunction;
	private final Map<TypeBinder, Type> memberTypes = new HashMap<>(); 

	public GroupChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, CurrentTCState state) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.state = state;
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		sv.push(new FunctionChecker(errors, sv, state));
		this.currentFunction = fn;
	}

	@Override
	public void visitObjectMethod(ObjectMethod meth) {
		sv.push(new FunctionChecker(errors, sv, state));
		this.currentFunction = meth;
	}

	@Override
	public void result(Object r) {
		memberTypes.put(currentFunction, (Type)r);
		this.currentFunction = null;
	}

	@Override
	public void leaveFunctionGroup(FunctionGroup grp) {
		// if we picked up anything based on the invocation of the method in this group, add that into the mix
		for (Entry<TypeBinder, Type> m : memberTypes.entrySet()) {
			String name = m.getKey().name().uniqueName();
			UnifiableType ut = state.requireVarConstraints(m.getKey().location(), name);
			ut.determinedType(m.getValue());
		}
		
		// First go through and figure out what we can
		for (Type ty : memberTypes.values()) {
			consolidate(ty, false);
		}
		
		// Then we can resolve all the UTs
		state.resolveAll(false);
		
		// We may need to re-consolidate UTs associated with functions
		for (Entry<TypeBinder, Type> m : memberTypes.entrySet()) {
			String name = m.getKey().name().uniqueName();
			UnifiableType ut = state.requireVarConstraints(m.getKey().location(), name);
			ut.rebind(consolidate(ut.resolve(false), false));
		}
		state.resolveAll(true);
		
		// Then we can bind the types
		for (Entry<TypeBinder, Type> e : memberTypes.entrySet()) {
			e.getKey().bindType(consolidate(e.getValue(), true));
		}
		state.bindVarPatternTypes();
		sv.result(null);
	}

	public Type consolidate(Type value, boolean haveResolvedUTs) {
		if (value instanceof ConsolidateTypes) {
			ConsolidateTypes ct = (ConsolidateTypes) value;
			if (ct.isConsolidated())
				return ct.consolidatedAs();
			Set<Type> tys = new HashSet<>();
			for (Type t : ct.types)
				tys.add(consolidate(t, haveResolvedUTs));
			if (haveResolvedUTs) {
				for (UnifiableType ut : ct.uts)
					tys.add(ut.resolve());
			}
			if (tys.isEmpty()) {
				if (!haveResolvedUTs)
					return null;
				throw new RuntimeException("I can't figure out how this can come to pass");
			}
			Type ret = unifyApply(tys);
			if (ret == null)
				ret = repository.findUnionWith(tys);
			if (ret == null) {
				state.resolveAll(true);
				Set<String> sigs = new TreeSet<>();
				for (Type t : tys) {
					sigs.add(t.signature());
				}
				errors.message(ct.location(), "unable to unify " + String.join(", ", sigs));
				return null;
			}
			ct.consolidatesTo(ret);
			return ret;
		} else if (value instanceof UnifiableType && haveResolvedUTs) {
			return ((UnifiableType)value).resolve();
		} else if (value instanceof Apply) {
			Apply apply = (Apply)value;
			List<Type> consolidated = new ArrayList<Type>();
			for (Type t : apply.tys)
				consolidated.add(consolidate(t, haveResolvedUTs));
			return new Apply(consolidated);
		} else if (value instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) value;
			List<Type> polys = new ArrayList<>();
			for (Type t : pi.getPolys()) {
				polys.add(consolidate(t, haveResolvedUTs));
			}
			return new PolyInstance(pi.struct(), polys);
		} else
			return value;
	}
	
	private Type unifyApply(Set<Type> tys) {
		int nargs = -1;
		List<List<Type>> matrix = new ArrayList<>();
		for (Type t : tys) {
			if (t instanceof Apply) {
				Apply apply = (Apply)t;
				int ac = apply.argCount();
				if (nargs == -1) {
					for (int i=0;i<=ac;i++) {
						List<Type> mt = new ArrayList<>();
						mt.add(apply.get(i));
						matrix.add(mt);
					}
					nargs = ac;
				} else if (nargs == ac) {
					for (int i=0;i<=ac;i++) {
						matrix.get(i).add(apply.get(i));
					}
				} else
					throw new RuntimeException("I think this may be legit with currying and all, but it's certainly not handled");
			} else
				return null;
		}
		
		List<Type> res = new ArrayList<>();
		for (List<Type> ts : matrix) {
			res.add(consolidate(new ConsolidateTypes(null, ts), false));
		}
		return new Apply(res.subList(0, res.size()-1), res.get(res.size()-1));
	}

	public CurrentTCState testsWantToCheckState() {
		return state;
	}
}
