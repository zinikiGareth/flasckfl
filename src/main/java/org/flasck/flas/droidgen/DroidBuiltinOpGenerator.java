package org.flasck.flas.droidgen;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.flim.BuiltinOperation;
import org.flasck.flas.generators.BuiltinOpGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.zinutils.bytecode.IExpr;

public class DroidBuiltinOpGenerator implements BuiltinOpGenerator<IExpr> {
	private final MethodGenerationContext cxt;

	public DroidBuiltinOpGenerator(MethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(BuiltinOperation defn, NameOfThing name, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		// This covers both Field & Tuple, but Field was handled above
		cxt.doEval(ObjectNeeded.NONE, cxt.getMethod().classConst(name.javaClassName()), closure, handler);
	}
}
