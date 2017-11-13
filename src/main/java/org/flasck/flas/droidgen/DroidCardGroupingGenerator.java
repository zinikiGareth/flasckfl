package org.flasck.flas.droidgen;

import org.flasck.flas.generators.CardGroupingGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.zinutils.bytecode.IExpr;

public class DroidCardGroupingGenerator implements CardGroupingGenerator<IExpr> {
	private final MethodGenerationContext cxt;

	public DroidCardGroupingGenerator(MethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(CardGrouping defn, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		// This is for "typeof <cardname>" and returns the "class" corresponding to the type
		// See typeop.fl for an example
		// TODO: figure out if this should really be "ObjectReference" and if that should be renamed
		cxt.doEval(ObjectNeeded.NONE, cxt.getMethod().classConst(defn.getName().javaClassName()), closure, handler);
	}

}
