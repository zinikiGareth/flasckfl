package org.flasck.flas.droidgen;

import org.flasck.flas.generators.ObjectReferenceGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.zinutils.bytecode.IExpr;

public class DroidObjectReferenceGenerator implements ObjectReferenceGenerator<IExpr> {
	private final IMethodGenerationContext cxt;

	public DroidObjectReferenceGenerator(IMethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(ObjectReference defn, ObjectNeeded myOn, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		// This case covers at least handling the construction of Object Handlers to pass to service methods
		cxt.doEval(myOn, cxt.getMethod().classConst(defn.myName().javaClassName()), closure, handler);
	}

	public void push(ObjectReference or, OutputHandler<IExpr> handler) {
		handler.result(cxt.getMethod().classConst(or.myName().javaClassName()));
	}

}
