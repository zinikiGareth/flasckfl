package org.flasck.flas.tc3;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;

public class MessageChecker extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;

	public MessageChecker(ErrorReporter errors, CurrentTCState state, NestedVisitor sv) {
		this.sv = sv;
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
		
		// a poly list is fine (cons or list) as long as the type is
		if (t instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) t;
			NamedType nt = pi.struct();
			if (nt == LoadBuiltins.cons || nt == LoadBuiltins.list)
				t = pi.getPolys().get(0);
			else {
				// TODO: message
				sv.result(new ExprResult(new ErrorType()));
				return;
			}
		}
		if (LoadBuiltins.message.incorporates(t)) {
			sv.result(r);
			return;
		}
		// TODO: message
		sv.result(new ExprResult(new ErrorType()));
	}

}
