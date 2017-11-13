package org.flasck.flas.droidgen;

import org.flasck.flas.generators.HandlerLambdaGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.zinutils.bytecode.IExpr;

public class DroidHandlerLambdaGenerator implements HandlerLambdaGenerator<IExpr> {
	private final MethodGenerationContext cxt;

	public DroidHandlerLambdaGenerator(MethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(HandlerLambda hl, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		IExpr var = cxt.getMethod().getField(hl.var);
		cxt.doEval(ObjectNeeded.NONE, var, closure, handler);
	}
}
