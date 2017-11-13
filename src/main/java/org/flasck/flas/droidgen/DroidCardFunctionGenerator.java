package org.flasck.flas.droidgen;

import org.flasck.flas.generators.CardFunctionGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.zinutils.bytecode.IExpr;

public class DroidCardFunctionGenerator implements CardFunctionGenerator<IExpr> {
	private final MethodGenerationContext cxt;

	public DroidCardFunctionGenerator(MethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(CardFunction defn, ObjectNeeded myOn, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		// This case covers at least event handlers
		cxt.doEval(myOn, cxt.getMethod().classConst(defn.myName().javaClassName()), closure, handler);
	}

	
}
