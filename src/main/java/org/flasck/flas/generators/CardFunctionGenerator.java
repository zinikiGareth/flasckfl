package org.flasck.flas.generators;

import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.zinutils.bytecode.IExpr;

public interface CardFunctionGenerator<T> {

	void generate(CardFunction defn, ObjectNeeded myOn, OutputHandler<IExpr> handler, ClosureGenerator closure);

}
