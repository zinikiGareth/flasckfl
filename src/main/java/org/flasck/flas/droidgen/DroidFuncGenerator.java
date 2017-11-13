package org.flasck.flas.droidgen;

import org.flasck.flas.generators.FuncGenerator;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushFunc;
import org.zinutils.bytecode.IExpr;

public class DroidFuncGenerator implements FuncGenerator<IExpr> {
	private final IMethodGenerationContext cxt;

	public DroidFuncGenerator(IMethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(PushFunc pf, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public void push(PushFunc pf, OutputHandler<IExpr> handler) {
		// this is clearly wrong, but we need to return a "function" object and I don't have one of those right now, I don't think 
		handler.result(cxt.getMethod().myThis());
	}

}
