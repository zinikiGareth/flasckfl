package org.flasck.flas.droidgen;

import org.flasck.flas.generators.BoolGenerator;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushBool;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.exceptions.NotImplementedException;

public class DroidBoolGenerator implements BoolGenerator<IExpr> {
	private final MethodDefiner meth;

	public DroidBoolGenerator(IMethodGenerationContext cxt) {
		this.meth = cxt.getMethod();
	}

	@Override
	public void generate(PushBool pb, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		throw new NotImplementedException();
	}

	@Override
	public void push(PushBool pb, OutputHandler<IExpr> handler) {
		handler.result(meth.boolConst(pb.bval.value()));
	}
}
