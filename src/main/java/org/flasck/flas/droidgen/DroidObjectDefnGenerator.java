package org.flasck.flas.droidgen;

import java.util.ArrayList;

import org.flasck.flas.generators.ObjectDefnGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class DroidObjectDefnGenerator implements ObjectDefnGenerator<IExpr> {
	private final MethodGenerationContext cxt;

	public DroidObjectDefnGenerator(MethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(RWObjectDefn od, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		MethodDefiner meth = cxt.getMethod();
		String clz = od.getName().javaClassName();
		// creating an object is just like calling a static function
		if (od.ctorArgs.isEmpty())
			cxt.doEval(ObjectNeeded.NONE, meth.callStatic(clz, J.OBJECT, "eval", cxt.getCxtArg(), meth.arrayOf(J.OBJECT, new ArrayList<>())), closure, handler);
		else
			cxt.doEval(ObjectNeeded.NONE, meth.classConst(clz), closure, handler);
	}
}
