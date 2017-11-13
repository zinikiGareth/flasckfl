package org.flasck.flas.generators;

import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;

public interface ScopedVarGenerator<T> {

	void generate(ScopedVar sv, OutputHandler<T> handler, ClosureGenerator closure);

}
