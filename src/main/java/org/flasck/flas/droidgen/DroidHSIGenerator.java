package org.flasck.flas.droidgen;

import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;

public class DroidHSIGenerator {
	private final DroidClosureGenerator closGen;
	private final HSIEForm form;
	private final NewMethodDefiner meth;
	private final VarHolder vh;
	
	public DroidHSIGenerator(DroidClosureGenerator closGen, HSIEForm form, NewMethodDefiner meth, VarHolder vh) {
		this.closGen = closGen;
		this.form = form;
		this.meth = meth;
		this.vh = vh;
	}

	public IExpr generateHSI(HSIEBlock blk, Var assignReturnTo) {
		StmtCollector coll = new StmtCollector(meth);
		blk.visit(new DroidHSIProcessor(this, form, meth, coll, closGen, vh, assignReturnTo));
		return coll.asBlock();
	}
}
