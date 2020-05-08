package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class TemplateStylingJS extends LeafAdapter implements ResultAware {
	public enum Mode {
		COND, EXPR
	}

	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator currentBlock;
	private final List<JSExpr> exprs = new ArrayList<>();
	private JSExpr cond;
	private Mode mode;

	public TemplateStylingJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator currentBlock, TemplateStylingOption tso) {
		this.state = state;
		this.sv = sv;
		this.currentBlock = currentBlock;
		sv.push(this);
	}

	@Override
	public void visitTemplateStyleCond(Expr cond) {
		mode = Mode.COND;
		new ExprGeneratorJS(state, sv, currentBlock, false);
	}

	@Override
	public void visitTemplateStyleExpr(Expr expr) {
		mode = Mode.EXPR;
		new ExprGeneratorJS(state, sv, currentBlock, false);
	}
	
	@Override
	public void result(Object r) {
		if (mode == Mode.COND)
			cond = (JSExpr) r;
		else
			exprs.add((JSExpr)r);
	}

	@Override
	public void leaveTemplateStyling(TemplateStylingOption tso) {
//		System.out.println("leaving " + tso.styleField.type() + " " + tso.styleField.text + " if " + tso.cond + " with " + expr + " will " + tso.styles);
//		currentBlock.updateStyle(tso.styleField, expr, tso.styleString());
		String c = tso.constant();
		JSExpr ret;
		if (exprs.isEmpty())
			ret = currentBlock.string(c);
		else {
			if (c != null)
				exprs.add(0, currentBlock.string(c));
			ret = currentBlock.callMethod(currentBlock.literal("FLBuiltin"), "concatMany", exprs.toArray(new JSExpr[exprs.size()]));
		}
				
		sv.result(new JSStyleIf(cond, ret));
	}
	
}
