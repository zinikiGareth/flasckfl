package org.flasck.flas.generators;

import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;

public interface PrimitiveTypeGenerator<T> {

	void generate(PrimitiveType defn, OutputHandler<T> handler, ClosureGenerator closure);

}
