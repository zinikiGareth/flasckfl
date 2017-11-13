package org.flasck.flas.generators;

import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;

public interface StructDefnGenerator<T> {

	void generate(RWStructDefn defn, OutputHandler<T> handler, ClosureGenerator closure);

	void push(RWStructDefn defn, OutputHandler<T> handler);

}
