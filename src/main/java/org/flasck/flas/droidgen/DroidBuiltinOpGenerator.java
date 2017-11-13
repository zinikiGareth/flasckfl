package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.generators.BuiltinOpGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushBuiltin;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.PushVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class DroidBuiltinOpGenerator implements BuiltinOpGenerator<IExpr> {
	private final MethodGenerationContext cxt;

	public DroidBuiltinOpGenerator(MethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(PushBuiltin defn, PushVisitor<IExpr> dpa, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		final MethodDefiner meth = cxt.getMethod();
		if (defn.isTuple()) {
			cxt.doEval(ObjectNeeded.NONE, meth.classConst(J.FLTUPLE), closure, handler);
		} else if (defn.isField()) {
			List<IExpr> al = new ArrayList<>();
			OutputHandler<IExpr> oh = new OutputHandler<IExpr>() {
				@Override
				public void result(IExpr expr) {
					al.add(meth.box(expr));
				}
			};
			((PushReturn)closure.nestedCommands().get(1)).visit(dpa, oh);
			((PushReturn)closure.nestedCommands().get(2)).visit(dpa, oh);
			handler.result(meth.makeNew(J.FLCLOSURE, meth.classConst(J.FLFIELD), meth.arrayOf(J.OBJECT, al)));
		} else 
			throw new RuntimeException("not handled " + defn);
	}
}
