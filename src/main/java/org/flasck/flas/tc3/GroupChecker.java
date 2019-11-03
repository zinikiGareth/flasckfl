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
		sv.push(new FunctionChecker(errors, repository, sv, state));
		this.currentFunction = fn;
	}

	@Override
	public void visitObjectMethod(ObjectMethod meth) {
		sv.push(new FunctionChecker(errors, repository, sv, state));
		this.currentFunction = meth;
	}

	@Override
	public void result(Object r) {
		memberTypes.put(currentFunction, (Type)r);
		this.currentFunction = null;
	}

	@Override
	public void leaveFunctionGroup(FunctionGroup grp) {
		// First go through and figure out what we can
		for (Type ty : memberTypes.values()) {
			consolidate(ty, false);
		}
		
		// Then we can resolve all the UTs
		state.resolveAll();
		
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
			Type ret = repository.findUnionWith(tys);
			if (ret == null) {
				Set<String> sigs = new TreeSet<>();
				for (Type t : tys)
					sigs.add(t.signature());
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
		} else
			return value;
	}
}
