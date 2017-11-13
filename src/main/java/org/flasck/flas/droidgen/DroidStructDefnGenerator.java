package org.flasck.flas.droidgen;

import java.util.ArrayList;

import org.flasck.flas.generators.StructDefnGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class DroidStructDefnGenerator implements StructDefnGenerator<IExpr> {
	private final IMethodGenerationContext cxt;

	public DroidStructDefnGenerator(IMethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(RWStructDefn sd, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		MethodDefiner meth = cxt.getMethod();
		String clz = sd.getName().javaClassName();
		// creating a struct is just like calling a static function
		if (sd.fields.size() == 0)
			cxt.doEval(ObjectNeeded.NONE, meth.callStatic(clz, J.OBJECT, "eval", cxt.getCxtArg(), meth.arrayOf(J.OBJECT, new ArrayList<>())), closure, handler);
		else
			cxt.doEval(ObjectNeeded.NONE, meth.classConst(clz), closure, handler);
	}

	public void push(RWStructDefn defn, OutputHandler<IExpr> handler) {
		String clz = defn.myName().javaClassName();
		if (defn.fields.isEmpty()) { // invoke a no-args constructor
			handler.result(cxt.getMethod().callStatic(clz, J.OBJECT, "eval", cxt.getMethod().arrayOf(J.OBJECT, new ArrayList<>())));
		} else {
			handler.result(cxt.getMethod().classConst(clz));
		}
	}
}
