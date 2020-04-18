package org.flasck.flas.tc3;

import java.util.HashMap;
import java.util.Map;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class GroupChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final NestedVisitor sv;
	private CurrentTCState state;
	private TypeBinder currentFunction;
	private final Map<TypeBinder, PosType> memberTypes = new HashMap<>(); 

	public GroupChecker(ErrorReporter errors, NestedVisitor sv, CurrentTCState state) {
		this.errors = errors;
		this.sv = sv;
		this.state = state;
		sv.push(this);
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		new FunctionChecker(errors, sv, state, null);
		this.currentFunction = fn;
	}

	@Override
	public void visitObjectMethod(ObjectMethod meth) {
		new FunctionChecker(errors, sv, state, meth);
		this.currentFunction = meth;
	}

	@Override
	public void visitTuple(TupleAssignment ta) {
		new FunctionChecker(errors, sv, state, null);
		this.currentFunction = ta;
		sv.push(new ExpressionChecker(errors, state, sv));
	}

	@Override
	public void visitTupleMember(TupleMember tm) {
		new FunctionChecker(errors, sv, state, null);
		this.currentFunction = tm;
		sv.push(new ExpressionChecker(errors, state, sv));
	}
	
	@Override
	public void result(Object r) {
		memberTypes.put(currentFunction, (PosType)r);
		this.currentFunction = null;
	}

	@Override
	public void leaveFunctionGroup(FunctionGroup grp) {
		state.groupDone(errors, memberTypes);
		sv.result(null);
	}

	public CurrentTCState testsWantToCheckState() {
		return state;
	}
}
