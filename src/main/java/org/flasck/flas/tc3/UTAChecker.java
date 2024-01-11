package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestIdentical;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.zinutils.exceptions.NotImplementedException;

public class UTAChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private final String fnCxt;
	private final List<Type> results = new ArrayList<>();

	public UTAChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, String fnCxt) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.fnCxt = fnCxt;
		sv.push(this);
	}
	
	@Override
	public void visitAssertExpr(boolean isValue, Expr e) {
		sv.push(new ExpressionChecker(errors, repository, new FunctionGroupTCState(repository, new DependencyGroup()), sv, fnCxt, false));
	}
	
	@Override
	public void result(Object r) {
		Type t = ((ExprResult)r).type;
		results.add(t);
	}
	
	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
		assertOrIdentical(a.expr, a.value);
	}

	@Override
	public void postUnitTestIdentical(UnitTestIdentical a) {
		assertOrIdentical(a.expr, a.value);
	}

	private void assertOrIdentical(Expr pexpr, Expr pvalue) {
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
			((UnifiableType)expr).incorporatedBy(pexpr.location(), value);
		else if (expr.incorporates(pvalue.location(), value))
			; // fine
		else if (isError(value))
			; // errors are always possible
		else {
			errors.message(pvalue.location(), "value is of type " + value.signature() + " that cannot be the result of an expression of type " + expr.signature());
		}
		// TODO: we probably need to try and resolve any UTs if there weren't errors
		sv.result(null);
	}
	
	private boolean isError(Type value) {
		if (value == LoadBuiltins.error || value instanceof ErrorType)
			return true;
		if (value instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) value;
			for (Type t : pi.polys()) {
				if (isError(t))
					return true;
			}
		}
		return false;
	}
}
