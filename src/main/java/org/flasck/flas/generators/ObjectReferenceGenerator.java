package org.flasck.flas.generators;

import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.zinutils.bytecode.IExpr;

public interface ObjectReferenceGenerator<T> {

	void generate(ObjectReference defn, ObjectNeeded myOn, OutputHandler<IExpr> handler, ClosureGenerator closure);

}
