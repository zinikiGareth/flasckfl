package org.flasck.flas.droidgen;

import org.flasck.flas.generators.GenerationContext;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.zinutils.bytecode.IExpr;

public interface IMethodGenerationContext extends GenerationContext<IExpr> {
	void doEval(ObjectNeeded on, IExpr fnToCall, ClosureGenerator closure, OutputHandler<IExpr> handler);
}
