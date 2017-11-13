package org.flasck.flas.generators;

import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushFunc;

public interface FuncGenerator<T> {
	void generate(PushFunc pf, OutputHandler<T> handler, ClosureGenerator closure);
	void push(PushFunc pf, OutputHandler<T> handler);
}
