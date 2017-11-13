package org.flasck.flas.generators;

import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushDouble;

public interface DoubleGenerator<T> {
	void generate(PushDouble defn, OutputHandler<T> handler, ClosureGenerator closure);
	void push(PushDouble pd, OutputHandler<T> handler);
}
