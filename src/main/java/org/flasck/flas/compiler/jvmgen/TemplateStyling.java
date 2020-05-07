package org.flasck.flas.compiler.jvmgen;

import java.util.List;

import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.zinutils.bytecode.IExpr;

public class TemplateStyling extends LeafAdapter implements ResultAware {
	private final StackVisitor sv;
	private IExpr cond;

	public TemplateStyling(FunctionState fs, StackVisitor sv, List<IExpr> currentBlock, TemplateStylingOption tso) {
		this.sv = sv;
		sv.push(this);
		if (tso.cond != null)
			new ExprGenerator(fs, sv, currentBlock, false);
	}
	@Override
	public void result(Object r) {
		cond = (IExpr) r;
	}

	@Override
	public void leaveTemplateStyling(TemplateStylingOption tso) {
		sv.result(new JVMStyleIf(cond, tso.styleString()));
	}

}
