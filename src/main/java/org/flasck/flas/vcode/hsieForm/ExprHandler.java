package org.flasck.flas.vcode.hsieForm;

import org.zinutils.bytecode.IExpr;

public interface ExprHandler {
	public void beginClosure();
	public void visit(PushReturn expr);
	public IExpr endClosure();
}
