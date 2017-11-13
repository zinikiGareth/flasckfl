package org.flasck.flas.generators;

import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;

public interface HandlerLambdaGenerator<T> {

	void generate(HandlerLambda defn, OutputHandler<T> handler, ClosureGenerator closure);

}
