package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.hsie.ObjectNeeded;

public interface ExprHandler {
	public void beginClosure();
	public void visit(PushReturn expr);
	public ExprHandler curry(NameOfThing clz, ObjectNeeded on, Integer arity);
	public Object endClosure();
}
