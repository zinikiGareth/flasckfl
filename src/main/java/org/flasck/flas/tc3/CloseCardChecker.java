package org.flasck.flas.tc3;

import java.util.HashMap;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ut.UnitTestClose;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;

public class CloseCardChecker extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final CurrentTCState state;
	private final String fnCxt;
	private Type card;

	public CloseCardChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, String fnCxt, UnitTestClose utc) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.fnCxt = fnCxt;
		sv.push(this);
		state = new FunctionGroupTCState(repository, new DependencyGroup());
		sv.push(new ExpressionChecker(errors, repository, state, sv, fnCxt, false));
	}
	
	@Override
	public void visitExpr(Expr e, int nargs) {
		sv.push(new ExpressionChecker(errors, repository, state, sv, fnCxt, false));
	}
	
	@Override
	public void result(Object r) {
		card = ((ExprResult)r).type;
	}
	
	@Override
	public void leaveUnitTestClose(UnitTestClose utc) {
		// Check for cascades
		if (card instanceof ErrorType) {
			sv.result(card);
			return;
		}
		if (!(card instanceof CardDefinition)) {
			errors.message(utc.card.location(), "close requires a card variable");
			sv.result(null);
			return;
		}
		
		state.groupDone(errors, new HashMap<>(), new HashMap<>());
		state.bindIntroducedVarTypes(errors);
		sv.result(null);
	}
}
