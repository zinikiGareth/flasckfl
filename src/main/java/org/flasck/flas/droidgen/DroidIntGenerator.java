package org.flasck.flas.droidgen;

import org.flasck.flas.generators.IntGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushInt;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class DroidIntGenerator implements IntGenerator<IExpr> {
	private final IMethodGenerationContext cxt;
	private final MethodDefiner meth;

	public DroidIntGenerator(IMethodGenerationContext cxt) {
		this.cxt = cxt;
		this.meth = cxt.getMethod();
	}

	@Override
	public void generate(PushInt pr, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		cxt.doEval(ObjectNeeded.NONE, meth.callStatic("java.lang.Integer", "java.lang.Integer", "valueOf", meth.intConst(((PushInt)pr).ival)), closure, handler);
	}

	@Override
	public void push(PushInt pi, OutputHandler<IExpr> handler) {
		handler.result(meth.callStatic("java.lang.Integer", "java.lang.Integer", "valueOf", meth.intConst(pi.ival)));
	}
}
