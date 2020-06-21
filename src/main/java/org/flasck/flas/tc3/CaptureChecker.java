package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;

public class CaptureChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final CurrentTCState state;
	private final StackVisitor sv;
	private final String fnCxt;
	private final boolean inTemplate;
	private Object result;

	public CaptureChecker(ErrorReporter errors, RepositoryReader repository, CurrentTCState state, StackVisitor sv,	String fnCxt, boolean inTemplate) {
		this.errors = errors;
		this.repository = repository;
		this.state = state;
		this.sv = sv;
		this.fnCxt = fnCxt;
		this.inTemplate = inTemplate;
		sv.push(this);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		sv.push(new ExpressionChecker(errors, repository, state, sv, fnCxt, inTemplate));
	}

	@Override
	public void result(Object r) {
		this.result = r;
	}

	public Object get() {
		return result;
	}
}
