package org.flasck.flas.droidgen;

import org.flasck.flas.generators.TLVGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushTLV;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class DroidTLVGenerator implements TLVGenerator<IExpr> {
	private final MethodGenerationContext cxt;
	private final MethodDefiner meth;

	public DroidTLVGenerator(MethodGenerationContext cxt) {
		this.cxt = cxt;
		this.meth = cxt.getMethod();
	}

	@Override
	public void generate(PushTLV pt, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		cxt.doEval(ObjectNeeded.NONE, meth.getField(meth.getField("_src_" + pt.tlv.simpleName), pt.tlv.simpleName), closure, handler);
	}
}
