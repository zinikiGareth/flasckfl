package org.flasck.flas.droidgen;

import org.flasck.flas.generators.CSRGenerator;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushCSR;
import org.zinutils.bytecode.IExpr;

public class DroidCSRGenerator implements CSRGenerator<IExpr> {
	private final IMethodGenerationContext cxt;

	public DroidCSRGenerator(IMethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(PushCSR csr, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public void push(PushCSR pc, OutputHandler<IExpr> handler) {
		if (pc.csr.fromHandler)
			handler.result(cxt.getMethod().getField("_card"));
		else
			handler.result(cxt.getMethod().myThis());
	}

}
