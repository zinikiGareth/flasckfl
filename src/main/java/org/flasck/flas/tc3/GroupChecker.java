package org.flasck.flas.tc3;

import java.util.HashMap;
import java.util.Map;

import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
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
	private final Map<TypeBinder, PosType> memberTypes = new HashMap<>();
	private final ErrorMark mark;

	public GroupChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, CurrentTCState state, ErrorMark mark) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.state = state;
		this.mark = mark;
		sv.push(this);
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		new FunctionChecker(errors, repository, sv, fn.name(), state, null);
		this.currentFunction = fn;
	}

	@Override
	public void visitObjectMethod(ObjectMethod meth) {
		new FunctionChecker(errors, repository, sv, meth.name(), state, meth);
		this.currentFunction = meth;
	}

	@Override
	public void visitTuple(TupleAssignment ta) {
		new FunctionChecker(errors, repository, sv, ta.name(), state, null);
		this.currentFunction = ta;
		sv.push(new ExpressionChecker(errors, repository, state, sv, false));
	}

	@Override
	public void leaveTupleMember(TupleMember tm) {
//		new FunctionChecker(errors, repository, sv, tm.name(), state, null);
//		this.currentFunction = tm;
//		sv.push(new ExpressionChecker(errors, repository, state, sv, false));
		// This should have been bound previously in tuple assignment
		memberTypes.put(tm, new PosType(tm.location(), tm.type()));
	}
	
	@Override
	public void result(Object r) {
		memberTypes.put(currentFunction, (PosType)r);
		this.currentFunction = null;
	}

	@Override
	public void leaveFunctionGroup(FunctionGroup grp) {
		// I would like this to be here, but it needs to be more refined
		// Specifically, I think it should check dependencies and if they had errors
//		if (mark != null && !mark.hasMoreNow())
		state.groupDone(errors, memberTypes);
		sv.result(null);
	}

	public CurrentTCState testsWantToCheckState() {
		return state;
	}
}
