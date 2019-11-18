package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.zinutils.exceptions.NotImplementedException;

public class UTAChecker extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private ErrorReporter errors;
	private RepositoryReader repository;
	private List<Type> results = new ArrayList<>();

	public UTAChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, UnitTestAssert a) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		sv.push(this);
	}
	
	@Override
	public void visitAssertExpr(boolean isValue, Expr e) {
		sv.push(new ExpressionChecker(errors, new FunctionGroupTCState(repository, new DependencyGroup()), sv));
	}
	
	@Override
	public void result(Object r) {
		Type t = ((ExprResult)r).type;
		results.add(t);
	}
	
	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
		if (results.size() != 2)
			throw new NotImplementedException();
		Type value = results.get(0);
		Type expr = results.get(1);
		if (value instanceof ErrorType || expr instanceof ErrorType) {// there were errors in the expressions, so don't cascade things
			sv.result(null);
			return;
		}
		if (value == expr)
			; // fine
		else if (expr instanceof UnifiableType)
			((UnifiableType)expr).incorporatedBy(a.expr.location(), value);
		else if (expr.incorporates(value))
			; // fine
		else if (value == LoadBuiltins.error)
			; // errors are always possible
		else {
			errors.message(a.value.location(), "value is of type " + value.signature() + " that cannot be the result of an expression of type " + expr.signature());
		}
		// TODO: we probably need to try and resolve any UTs if there weren't errors
		sv.result(null);
	}
}
