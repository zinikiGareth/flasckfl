package org.flasck.flas.generators;

import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;

public interface FunctionDefnGenerator<T> {

	void generate(RWFunctionDefinition defn, OutputHandler<T> handler, ClosureGenerator closure);

	void push(RWFunctionDefinition defn, OutputHandler<T> handler);

}
