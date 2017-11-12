package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.hsie.ObjectNeeded;

public interface ClosureHandler<T> {
	public void beginClosure();
	public void visit(PushReturn expr);
	public ClosureHandler<T> curry(NameOfThing clz, ObjectNeeded on, Integer arity);
	public void endClosure(OutputHandler<T> handler);
}
