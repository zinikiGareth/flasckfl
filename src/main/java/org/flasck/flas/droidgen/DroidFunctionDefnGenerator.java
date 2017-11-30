package org.flasck.flas.droidgen;

import java.util.ArrayList;

import org.flasck.flas.generators.FunctionDefnGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class DroidFunctionDefnGenerator implements FunctionDefnGenerator<IExpr> {
	private final IMethodGenerationContext cxt;

	public DroidFunctionDefnGenerator(IMethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(RWFunctionDefinition rwfn, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		// a regular function
		MethodDefiner meth = cxt.getMethod();
		String clz = rwfn.getName().javaClassName();
		if (rwfn.nargs == 0) { // invoke it as a function using eval
			cxt.doEval(ObjectNeeded.NONE, meth.callStatic(clz, J.OBJECT, "eval", cxt.getCxtArg(), meth.arrayOf(J.OBJECT, new ArrayList<>())), closure, handler);
		} else {
			cxt.doEval(ObjectNeeded.NONE, meth.classConst(clz), closure, handler);
		}
	}

	public void push(RWFunctionDefinition fn, OutputHandler<IExpr> handler) {
		if (fn.nargs == 0) { // invoke it as a function using eval
			handler.result(cxt.getMethod().callStatic(fn.getName().javaClassName(), J.OBJECT, "eval", cxt.getCxtArg(), cxt.getMethod().arrayOf(J.OBJECT, new ArrayList<>())));
		} else {
			handler.result(cxt.getMethod().classConst(fn.getName().javaClassName()));
		}
	}

}
