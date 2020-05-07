package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class TemplateStylingJS extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private JSExpr cond;

	public TemplateStylingJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator currentBlock, TemplateStylingOption tso) {
		this.sv = sv;
		sv.push(this);
		if (tso.cond != null)
			new ExprGeneratorJS(state, sv, currentBlock, false);
	}

	@Override
	public void result(Object r) {
		cond = (JSExpr) r;
	}

	@Override
	public void leaveTemplateStyling(TemplateStylingOption tso) {
//		System.out.println("leaving " + tso.styleField.type() + " " + tso.styleField.text + " if " + tso.cond + " with " + expr + " will " + tso.styles);
//		currentBlock.updateStyle(tso.styleField, expr, tso.styleString());
		sv.result(new JSStyleIf(cond, tso.styleString()));
	}
	
}
