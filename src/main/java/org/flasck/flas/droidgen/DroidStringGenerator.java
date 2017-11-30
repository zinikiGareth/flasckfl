package org.flasck.flas.droidgen;

import org.flasck.flas.generators.StringGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushString;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class DroidStringGenerator implements StringGenerator<IExpr> {
	private final IMethodGenerationContext cxt;
	private final MethodDefiner meth;

	public DroidStringGenerator(IMethodGenerationContext cxt) {
		this.cxt = cxt;
		this.meth = cxt.getMethod();
	}

	@Override
	public void generate(PushString pr, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		cxt.doEval(ObjectNeeded.NONE, meth.stringConst(((PushString)pr).sval.text), closure, handler);
	}

	@Override
	public void push(PushString ps, OutputHandler<IExpr> handler) {
		handler.result(meth.stringConst(ps.sval.text));
	}
}