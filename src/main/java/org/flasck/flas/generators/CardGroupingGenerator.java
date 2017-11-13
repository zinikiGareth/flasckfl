package org.flasck.flas.generators;

import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;

public interface CardGroupingGenerator<T> {

	void generate(CardGrouping defn, OutputHandler<T> handler, ClosureGenerator closure);

}
