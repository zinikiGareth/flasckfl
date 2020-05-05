package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;

public class ObjectDefnChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private ExprResult result;

	public ObjectDefnChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, ObjectDefn obj) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		sv.push(this);
	}

	@Override
	public void visitStructField(StructField sf) {
		result = null;
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		sv.push(new ExpressionChecker(errors, repository, new FunctionGroupTCState(repository, new DependencyGroup()), sv));
	}
	
	@Override
	public void leaveStructField(StructField sf) {
		if (result == null)
			return;
		
		if (!sf.type().incorporates(result.pos, result.type))
			errors.message(result.pos, "cannot initialize " + sf.name +" from " + result.type.signature());
	}
	
	@Override
	public void leaveObjectDefn(ObjectDefn obj) {
		sv.result(null);
	}

	@Override
	public void result(Object r) {
		result = (ExprResult) r;
	}
}
