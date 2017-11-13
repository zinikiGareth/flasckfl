package org.flasck.flas.generators;

import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.zinutils.bytecode.IExpr;

public interface CardGroupingGenerator<T> {

	void generate(CardGrouping defn, OutputHandler<IExpr> handler, ClosureGenerator closure);

}
