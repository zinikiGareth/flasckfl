package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.zinutils.exceptions.NotImplementedException;

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
//		System.out.println(grp);
//		state.debugInfo();
		
		for (Entry<TypeBinder, Type> m : memberTypes.entrySet()) {
			String name = m.getKey().name().uniqueName();
			UnifiableType ut = state.requireVarConstraints(m.getKey().location(), name);
			ut.determinedType(m.getValue());
		}
//		state.debugInfo();
		
		// Then we can resolve all the UTs
		state.resolveAll(errors, false);
//		state.debugInfo();
		state.enhanceAllMutualUTs();
//		state.debugInfo();
		state.resolveAll(errors, true);
//		state.debugInfo();
		
		// Then we can bind the types
		for (Entry<TypeBinder, Type> e : memberTypes.entrySet()) {
			e.getKey().bindType(cleanUTs(e.getValue()));
		}
		state.bindVarPatternTypes(errors);
		sv.result(null);
	}

	private Type cleanUTs(Type ty) {
		if (ty instanceof UnifiableType)
			return cleanUTs(((UnifiableType)ty).resolve(errors, true));
		else if (ty instanceof Apply) {
			Apply a = (Apply) ty;
			List<Type> tys = new ArrayList<>();
			for (Type t : a.tys)
				tys.add(cleanUTs(t));
			return new Apply(tys);
		} else
			return ty;
	}

	public CurrentTCState testsWantToCheckState() {
		return state;
	}
}
