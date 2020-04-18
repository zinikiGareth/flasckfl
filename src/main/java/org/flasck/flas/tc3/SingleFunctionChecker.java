package org.flasck.flas.tc3;

import java.util.HashMap;
import java.util.Map;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;

public class SingleFunctionChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final FunctionGroupTCState state;
	private final NestedVisitor sv;
	private final TypeBinder meth; 
	private final Map<TypeBinder, PosType> memberTypes = new HashMap<>();

	public SingleFunctionChecker(ErrorReporter errors, NestedVisitor sv, RepositoryReader repository, ObjectActionHandler meth) {
		this.errors = errors;
		this.sv = sv;
		this.meth = meth;
		state = new FunctionGroupTCState(repository, new DependencyGroup());
		state.bindVarToUT(meth.name().uniqueName(), state.createUT(meth.location(), "method " + meth.name().uniqueName()));

		sv.push(this); // it's very important to push this before allowing the FunctionChecker to push itself ...
		new FunctionChecker(errors, sv, state, meth);
	}

	@Override
	public void result(Object r) {
		memberTypes.put(meth, (PosType)r);
		state.groupDone(errors, memberTypes);
		sv.result(null);
	}
}
