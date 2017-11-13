package org.flasck.flas.droidgen;

import org.flasck.flas.generators.PrimitiveTypeGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.zinutils.bytecode.IExpr;

public class DroidPrimitiveTypeGenerator implements PrimitiveTypeGenerator<IExpr> {
	private final IMethodGenerationContext cxt;

	public DroidPrimitiveTypeGenerator(IMethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(PrimitiveType defn, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		// This is for "typeof Number" or "typeof String" and returns the corresponding class object
		// See typeop.fl for an example
		cxt.doEval(ObjectNeeded.NONE, cxt.getMethod().classConst(defn.getName().javaClassName()), closure, handler);
	}

}
