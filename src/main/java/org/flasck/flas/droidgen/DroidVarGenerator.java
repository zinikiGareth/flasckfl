package org.flasck.flas.droidgen;

import org.flasck.flas.generators.VarGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.zinutils.bytecode.IExpr;

public class DroidVarGenerator implements VarGenerator<IExpr> {

	private final IMethodGenerationContext cxt;

	public DroidVarGenerator(IMethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(PushVar pr, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		cxt.doEval(ObjectNeeded.NONE, cxt.getVarHolder().get(((PushVar)pr).var.var), closure, handler);
	}

	public void push(PushVar pv, OutputHandler<IExpr> handler) {
		handler.result(cxt.getVarHolder().get(pv.var.var));
	}
}
