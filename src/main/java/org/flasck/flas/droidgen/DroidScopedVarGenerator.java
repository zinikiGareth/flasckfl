package org.flasck.flas.droidgen;

import org.flasck.flas.generators.ScopedVarGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.zinutils.bytecode.IExpr;

public class DroidScopedVarGenerator implements ScopedVarGenerator<IExpr> {
	private final IMethodGenerationContext cxt;

	public DroidScopedVarGenerator(IMethodGenerationContext cxt) {
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

	public void push(ScopedVar sv, HSIEForm form, OutputHandler<IExpr> handler) {
		if (sv.definedBy.equals(form.funcName)) {
			// TODO: I'm not quite sure what should happen here, or even what this case represents, but I know it should be something to do with the *actual* function definition
			handler.result(cxt.getMethod().stringConst(sv.uniqueName()));
		} else
			handler.result(cxt.getVarHolder().getScoped(sv.uniqueName()));
	}
}
