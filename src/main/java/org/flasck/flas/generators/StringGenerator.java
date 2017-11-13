package org.flasck.flas.generators;

import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushString;

public interface StringGenerator<T> {

	void generate(PushString pr, OutputHandler<T> handler, ClosureGenerator closure);

}
