package org.flasck.flas.droidgen;

import org.flasck.flas.generators.ObjectDefnGenerator;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.exceptions.NotImplementedException;

public class DroidObjectDefnGenerator implements ObjectDefnGenerator<IExpr> {
	private final IMethodGenerationContext cxt;

	public DroidObjectDefnGenerator(IMethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(RWObjectDefn od, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		MethodDefiner meth = cxt.getMethod();
		String clz = od.getName().javaClassName();
		throw new NotImplementedException("cannot generate object ctor for " + clz + " " + meth);
		// creating an object is just like calling a static function
//		if (od.ctorArgs.isEmpty())
//			cxt.doEval(ObjectNeeded.NONE, meth.callStatic(clz, J.OBJECT, "eval", cxt.getCxtArg(), meth.arrayOf(J.OBJECT, new ArrayList<>())), closure, handler);
//		else
//			cxt.doEval(ObjectNeeded.NONE, meth.classConst(clz), closure, handler);
	}
}
