package org.flasck.flas.generators;

import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushBool;

public interface BoolGenerator<T> {

	void generate(PushBool pb, OutputHandler<T> handler, ClosureGenerator closure);
	void push(PushBool pb, OutputHandler<T> handler);

}
