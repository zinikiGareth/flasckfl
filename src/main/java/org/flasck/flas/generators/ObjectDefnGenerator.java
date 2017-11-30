package org.flasck.flas.generators;

import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;

public interface ObjectDefnGenerator<T> {
	void generate(RWObjectDefn defn, OutputHandler<T> handler, ClosureGenerator closure);
	void push(RWObjectDefn od, OutputHandler<T> handler);
}
