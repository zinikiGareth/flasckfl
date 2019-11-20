package org.flasck.flas.tc3;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ActionMessage;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;

public class MessageChecker extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final ErrorReporter errors;
	private final InputPosition pos;
	private ExprResult rhsType;

	public MessageChecker(ErrorReporter errors, CurrentTCState state, NestedVisitor sv, InputPosition pos) {
		this.errors = errors;
		this.sv = sv;
		this.pos = pos;
		sv.push(this);
		sv.push(new ExpressionChecker(errors, state, sv));
	}

	@Override
	public void visitAssignSlot(List<UnresolvedVar> slot) {
		if (!(rhsType.type instanceof ErrorType))
			rhsType = new ExprResult(rhsType.pos, LoadBuiltins.message);
	}
	
	@Override
	public void leaveMessage(ActionMessage msg) {
		check();
	}
	
	@Override
	public void result(Object r) {
		rhsType = (ExprResult) r;
	}

	private void check() {
		Type check = rhsType.type;

		// an empty list is fine
		if (check == LoadBuiltins.nil) {
			sv.result(rhsType);
			return;
		}
		
		// a poly list is fine (cons or list) as long as the type is
		if (check instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) check;
			NamedType nt = pi.struct();
			if (nt == LoadBuiltins.cons || nt == LoadBuiltins.list)
				check = pi.getPolys().get(0);
			else {
				errors.message(pos, check.signature() + " cannot be a Message");
				sv.result(new ExprResult(pos, new ErrorType()));
				return;
			}
		}
		if (LoadBuiltins.message.incorporates(pos, check)) {
			sv.result(rhsType);
			return;
		}
		errors.message(pos, rhsType.type.signature() + " cannot be a Message");
		sv.result(new ExprResult(pos, new ErrorType()));
	}

}
