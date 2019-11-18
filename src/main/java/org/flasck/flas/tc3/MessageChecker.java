package org.flasck.flas.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;

public class MessageChecker extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final ErrorReporter errors;
	private InputPosition pos;

	public MessageChecker(ErrorReporter errors, CurrentTCState state, NestedVisitor sv, InputPosition pos) {
		this.errors = errors;
		this.sv = sv;
		this.pos = pos;
		sv.push(this);
		sv.push(new ExpressionChecker(errors, state, sv));
	}

	@Override
	public void result(Object r) {
		Type t = ((ExprResult) r).type;
		
		// an empty list is fine
		if (t == LoadBuiltins.nil) {
			sv.result(r);
			return;
		}
		
		Type check = t;
		
		// a poly list is fine (cons or list) as long as the type is
		if (t instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) t;
			NamedType nt = pi.struct();
			if (nt == LoadBuiltins.cons || nt == LoadBuiltins.list)
				check = pi.getPolys().get(0);
			else {
				errors.message(pos, t.signature() + " cannot be a Message");
				sv.result(new ExprResult(pos, new ErrorType()));
				return;
			}
		}
		if (LoadBuiltins.message.incorporates(pos, check)) {
			sv.result(r);
			return;
		}
		errors.message(pos, t.signature() + " cannot be a Message");
		sv.result(new ExprResult(pos, new ErrorType()));
	}

}
