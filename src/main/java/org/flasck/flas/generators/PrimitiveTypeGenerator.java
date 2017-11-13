package org.flasck.flas.generators;

import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.zinutils.bytecode.IExpr;

public interface PrimitiveTypeGenerator<T> {

	void generate(PrimitiveType defn, OutputHandler<IExpr> handler, ClosureGenerator closure);

}
