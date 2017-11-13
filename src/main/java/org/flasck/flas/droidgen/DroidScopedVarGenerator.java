package org.flasck.flas.droidgen;

import org.flasck.flas.generators.ScopedVarGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.zinutils.bytecode.IExpr;

public class DroidScopedVarGenerator implements ScopedVarGenerator<IExpr> {
	private final MethodGenerationContext cxt;

	public DroidScopedVarGenerator(MethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(ScopedVar sv, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		ObjectNeeded ot = ObjectNeeded.NONE;
		if (sv.defn instanceof RWFunctionDefinition && ((RWFunctionDefinition)sv.defn).mytype == CodeType.HANDLERFUNCTION)
			ot = ObjectNeeded.THIS;
		if (closure != null && closure.justScoping())
			cxt.doEval(ot, cxt.getMethod().classConst(sv.myName().javaClassName()), closure, handler);
		else
			cxt.doEval(ot, cxt.getVarHolder().getScoped(sv.uniqueName()), closure, handler);
	}
}
