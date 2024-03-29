package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;

public class UDDChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private final String fnCxt;
	private NamedType actualType;

	public UDDChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, String fnCxt) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.fnCxt = fnCxt;
		sv.push(this);
	}

	@Override
	public void visitTypeReference(TypeReference var, boolean expectPolys, int exprNargs) {
		actualType = var.namedDefn();
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		sv.push(new ExpressionChecker(errors, repository, new FunctionGroupTCState(repository, new DependencyGroup()), sv, fnCxt, false));
	}
	
	@Override
	public void result(Object r) {
		ExprResult exprType = (ExprResult) r;
		if (!actualType.incorporates(exprType.pos, exprType.type)) {
			if (!(exprType.type instanceof ErrorType))
				errors.message(exprType.pos, "cannot store " + exprType.type.signature() + " in data " + actualType.signature());
			sv.result(null);
			return;
		}
	}

	@Override
	public void leaveUnitDataDeclaration(UnitDataDeclaration udd) {
		sv.result(null);
	}
}
