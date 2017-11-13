package org.flasck.flas.generators;

import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushBuiltin;
import org.flasck.flas.vcode.hsieForm.PushVisitor;

public interface BuiltinOpGenerator<T> {

	void generate(PushBuiltin defn, PushVisitor<T> dpa, OutputHandler<T> handler, ClosureGenerator closure);

}
