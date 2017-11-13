package org.flasck.flas.droidgen;

import org.flasck.flas.generators.HandlerLambdaGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.bytecode.IExpr;
import org.zinutils.exceptions.UtilException;

public class DroidHandlerLambdaGenerator implements HandlerLambdaGenerator<IExpr> {
	private final IMethodGenerationContext cxt;

	public DroidHandlerLambdaGenerator(IMethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(HandlerLambda hl, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		IExpr var = cxt.getMethod().getField(hl.var);
		cxt.doEval(ObjectNeeded.NONE, var, closure, handler);
	}

	public void push(HandlerLambda hl, HSIEForm form, OutputHandler<IExpr> handler) {
		if (form.mytype == CodeType.HANDLER)
			handler.result(cxt.getMethod().getField(hl.var));
		else if (form.mytype == CodeType.HANDLERFUNCTION)
			handler.result(cxt.getMethod().getField(hl.var));
		else
			throw new UtilException("Can't handle handler lambda with " + form.mytype);
	}
}
