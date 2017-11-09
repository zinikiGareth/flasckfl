package org.flasck.flas.droidgen;

import org.flasck.flas.generators.CodeGenerator;
import org.flasck.flas.generators.GenerationContext;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.IExpr;

public class DroidHSIEFormGenerator {
	private final ByteCodeStorage bce;

	public DroidHSIEFormGenerator(ByteCodeStorage bce) {
		this.bce = bce;
	}

	public void generate(HSIEForm form) {
		GenerationContext cxt = new MethodGenerationContext(bce, form);
		CodeGenerator cg = form.mytype.generator();
		cg.begin(cxt);
		
		IExpr blk = new DroidHSIGenerator(new DroidClosureGenerator(form, cxt.getMethod(), cxt.getVarHolder()), form, cxt.getMethod(), cxt.getVarHolder()).generateHSI(form, null);
		if (blk != null)
			blk.flush();
	}
}
