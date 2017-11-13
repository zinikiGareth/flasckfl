package org.flasck.flas.generators;

import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushVar;

public interface VarGenerator<T> {

	void generate(PushVar pr, OutputHandler<T> handler, @Deprecated ClosureGenerator closure);

}
