package org.flasck.flas.hsie;

import org.flasck.flas.generators.GenerationContext;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;

public class HSIGenerator<T> {
	private final ClosureTraverser<T> closGen;
	private final HSIEForm form;
	private GenerationContext<T> cxt;
	
	public HSIGenerator(ClosureTraverser<T> closGen, HSIEForm form, GenerationContext<T> cxt) {
		this.closGen = closGen;
		this.form = form;
		this.cxt = cxt;
	}

	public T generateHSI(HSIEBlock blk) {
		return blk.visit(cxt.hsi(this, form, cxt, closGen));
	}
}
