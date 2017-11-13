package org.flasck.flas.generators;

import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushInt;

public interface IntGenerator<T> {

	void generate(PushInt pr, OutputHandler<T> handler, ClosureGenerator closure);

	void push(PushInt pi, OutputHandler<T> handler);

}
