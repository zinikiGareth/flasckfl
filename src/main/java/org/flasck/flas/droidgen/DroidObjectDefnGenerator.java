package org.flasck.flas.droidgen;

import org.flasck.flas.generators.ObjectDefnGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class DroidObjectDefnGenerator implements ObjectDefnGenerator<IExpr> {
	private final IMethodGenerationContext cxt;

	public DroidObjectDefnGenerator(IMethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(RWObjectDefn od, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		// This is the step in which we call the (private) constructor on an object given an
		// expression which is the state
		MethodDefiner meth = cxt.getMethod();
		String clz = od.getName().javaClassName();
		cxt.doEval(ObjectNeeded.NONE, meth.classConst(clz), closure, handler);
	}

	@Override
	public void push(RWObjectDefn od, OutputHandler<IExpr> handler) {
		MethodDefiner meth = cxt.getMethod();
		String clz = od.getName().javaClassName();
		handler.result(meth.classConst(clz));
	}
}
