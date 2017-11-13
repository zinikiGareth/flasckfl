package org.flasck.flas.generators;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.flim.BuiltinOperation;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.zinutils.bytecode.IExpr;

public interface BuiltinOpGenerator<T> {

	void generate(BuiltinOperation defn, NameOfThing name, OutputHandler<IExpr> handler, ClosureGenerator closure);

}
