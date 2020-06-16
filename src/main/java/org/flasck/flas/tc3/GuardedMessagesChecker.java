package org.flasck.flas.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.ut.GuardedMessages;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.zinutils.exceptions.NotImplementedException;

public class GuardedMessagesChecker extends LeafAdapter implements ResultAware {

	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final CurrentTCState state;
	private final NestedVisitor sv;
	private final ObjectActionHandler inMeth;
	private InputPosition pos;

	public GuardedMessagesChecker(ErrorReporter errors, RepositoryReader repository, CurrentTCState state, NestedVisitor sv, ObjectActionHandler inMeth) {
		this.errors = errors;
		this.repository = repository;
		this.state = state;
		this.sv = sv;
		this.inMeth = inMeth;
		sv.push(this);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		if (expr != null)
			pos = expr.location();
		sv.push(new ExpressionChecker(errors, repository, state, sv, false));
	}
	
	@Override
	public void visitSendMessage(SendMessage sm) {
		new MessageChecker(errors,repository, state, sv, inMeth, null);
	}
	
	@Override
	public void visitAssignMessage(AssignMessage assign) {
		new MessageChecker(errors,repository, state, sv, inMeth, null);
	}
	
	@Override
	public void result(Object r) {
		if (!(r instanceof ExprResult))
			throw new NotImplementedException("should be an expr result");
		ExprResult exprResult = (ExprResult)r;
		if (pos == null)
			pos = exprResult.pos;
		Type ret = exprResult.type;
		if (ret instanceof UnifiableType)
			((UnifiableType)ret).isReturned(exprResult.pos);
	}

	@Override
	public void leaveGuardedMessage(GuardedMessages gm) {
		sv.result(new ExprResult(pos, LoadBuiltins.listMessages));
	}
}
