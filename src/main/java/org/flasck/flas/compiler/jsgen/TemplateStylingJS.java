package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class TemplateStylingJS extends LeafAdapter implements ResultAware {
	public enum Mode {
		COND, EXPR, ELSE, NESTED
	}

	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator currentBlock;
	private final List<JSExpr> exprs = new ArrayList<>();
	private List<JSExpr> elseExprs = null;
	private JSExpr cond;
	private Mode mode;
	
	// Because we process nested styles before our own, we need to keep them on delay until we are ready to send them ...
	private List<JSStyleIf> styles = new ArrayList<>();

	public TemplateStylingJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator currentBlock, TemplateStylingOption tso) {
		this.state = state;
		this.sv = sv;
		this.currentBlock = currentBlock;
		sv.push(this);
	}

	@Override
	public void visitTemplateStyling(TemplateStylingOption tso) {
		mode = Mode.NESTED;
		new TemplateStylingJS(state, sv, currentBlock, tso);
	}
	
	@Override
	public void visitTemplateStyleCond(Expr cond) {
		mode = Mode.COND;
		new ExprGeneratorJS(state, sv, currentBlock, false);
	}

	@Override
	public void visitTemplateStyleExpr(Expr expr) {
		if (mode != Mode.ELSE) // don't overwrite ELSE
			mode = Mode.EXPR;
		new ExprGeneratorJS(state, sv, currentBlock, false);
	}
	
	@Override
	public void visitTemplateStylesElse(TemplateStylingOption tso) {
		mode = Mode.ELSE;
		elseExprs = new ArrayList<JSExpr>();
	}

	@Override
	public void result(Object r) {
		if (mode == Mode.COND)
			cond = (JSExpr) r;
		else if (mode == Mode.EXPR)
			exprs.add((JSExpr)r);
		else if (mode == Mode.ELSE)
			elseExprs.add((JSExpr)r);
		else {
			@SuppressWarnings("unchecked")
			List<JSStyleIf> lsi = (List<JSStyleIf>) r;
			for (JSStyleIf si : lsi) {
				if (cond == null)
					styles.add(si);
				else {
					JSExpr doAnd = cond;
					if (si.cond != null)
						doAnd = currentBlock.closure(false, currentBlock.pushFunction("FLBuiltin.boolAnd", FunctionName.function(null, null, "And"), -1), cond, si.cond);
					styles.add(new JSStyleIf(doAnd, si.style));
				}
			}
		}
	}

	@Override
	public void leaveTemplateStyling(TemplateStylingOption tso) {
		// put ours before the nested ones because that is where it 'logically' comes
		// more importantly, that is where the JS event-handling code will be looking for it

		styles.add(0, new JSStyleIf(cond, styleIf(tso.strings(), exprs)));
		if (elseExprs != null)
			styles.add(0, new JSStyleIf(notOf(cond), styleIf(tso.elseStrings(), elseExprs)));
		sv.result(styles);
	}

	private JSExpr notOf(JSExpr c) {
		return currentBlock.closure(false, currentBlock.pushFunction("FLBuiltin.not", FunctionName.function(null, null, "Not"), -1), cond);
	}

	private JSExpr styleIf(String c, List<JSExpr> es) {
		JSExpr ret;
		if (es.isEmpty())
			ret = currentBlock.string(c==null?"":c);
		else {
			if (c != null)
				es.add(0, currentBlock.string(c));
			ret = currentBlock.closure(false, currentBlock.callStatic(FunctionName.function(null, new PackageName("FLBuiltin"), "concatMany"), 1), currentBlock.makeArray(es));
		}
		return ret;
	}
}
