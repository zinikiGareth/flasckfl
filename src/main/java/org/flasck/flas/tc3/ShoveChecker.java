package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestShove;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.zinutils.exceptions.NotImplementedException;

public class ShoveChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private Type curr;
	private List<Type> results = new ArrayList<>();

	public ShoveChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		sv.push(this);
	}
	
	@Override
	public void visitShoveSlot(UnresolvedVar v) {
		if (curr == null) {
			StackVisitor nv = new StackVisitor() {
				@Override
				public void result(Object r) {
					curr = ((ExprResult)r).type;
				}
			};
			ExpressionChecker ec = new ExpressionChecker(errors, repository, new FunctionGroupTCState(repository, new DependencyGroup()), nv, false);
			ec.visitUnresolvedVar(v, 0);
		} else if (curr instanceof ErrorType) {
			// ignore cascades ...
		} else if (curr instanceof StateHolder) {
			StateDefinition state = ((StateHolder)curr).state();
			curr = state.findField(v.var).type();
		} else if (curr instanceof StructDefn) {
			StructDefn sd = (StructDefn) curr;
			curr = sd.findField(v.var).type();
		} else
			throw new NotImplementedException("cannot shove member " + v.var + " into " + curr);
	}
	
	@Override
	public void visitShoveExpr(Expr e) {
		sv.push(new ExpressionChecker(errors, repository, new FunctionGroupTCState(repository, new DependencyGroup()), sv, false));
	}
	
	@Override
	public void result(Object r) {
		Type t = ((ExprResult)r).type;
		results.add(t);
	}
	
	@Override
	public void leaveUnitTestShove(UnitTestShove s) {
		if (results.size() != 1)
			throw new NotImplementedException();
		Type expr = results.get(0);
		if (expr instanceof ErrorType) {// there were errors in the expressions, so don't cascade things
			sv.result(null);
			return;
		}
		if (curr == expr)
			; // fine
		else if (expr instanceof UnifiableType)
			((UnifiableType)expr).incorporatedBy(s.value.location(), curr);
		else if (curr.incorporates(s.value.location(), expr))
			; // fine
		else if (expr == LoadBuiltins.error)
			; // errors are always possible
		else {
			errors.message(s.value.location(), "value of type " + expr.signature() + " cannot be shoved into a slot of type " + curr.signature());
		}
		// TODO: we probably need to try and resolve any UTs if there weren't errors
		sv.result(null);
	}
}
