package org.flasck.flas.droidgen;

import org.flasck.flas.generators.DoubleGenerator;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushDouble;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class DroidDoubleGenerator implements DoubleGenerator<IExpr> {
	private final MethodGenerationContext cxt;

	public DroidDoubleGenerator(MethodGenerationContext cxt) {
		this.cxt = cxt;
	}
	
	@Override
	public void generate(PushDouble defn, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	public void push(PushDouble pd, OutputHandler<IExpr> handler) {
		MethodDefiner meth = cxt.getMethod();
		handler.result(meth.callStatic(J.NUMBER, J.NUMBER, "fromDouble", meth.doubleConst(pd.dval)));
	}

}
