package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSIfExpr;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class TemplateProcessorJS extends LeafAdapter implements ResultAware {
	private JSExpr cond;
	private JSExpr expr;
	private JSIfExpr ie;
	private JSBlockCreator condBlock;
	private JSBlockCreator exprBlock;

	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator templateBlock;
	private final List<JSStyleIf> styles = new ArrayList<>();
	private JSBlockCreator bindingBlock;
	private TemplateField assignsTo;

	public TemplateProcessorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator currentBlock) {
		this.state = state;
		this.sv = sv;
		this.templateBlock = currentBlock;
		this.bindingBlock = currentBlock;
		sv.push(this);
	}
	
	@Override
	public void visitTemplateBinding(TemplateBinding b) {
		assignsTo = b.assignsTo;
	}
	
	@Override
	public void visitTemplateBindingCondition(Expr cond) {
		condBlock = bindingBlock;  
		new ExprGeneratorJS(state, sv, condBlock, false);
	}
	
	
	@Override
	public void visitTemplateBindingExpr(Expr expr) {
		exprBlock = bindingBlock;
		new ExprGeneratorJS(state, sv, exprBlock, false);
	}

	@Override
	public void visitTemplateStyling(TemplateStylingOption tso) {
		new TemplateStylingJS(state, sv, templateBlock, tso);
	}
	
	@Override
	public void result(Object r) {
		if (r instanceof JSStyleIf) {
			styles.add((JSStyleIf)r);
		} else {
			if (exprBlock != null) {
				expr = (JSExpr) r;
				exprBlock.updateContent(assignsTo, expr);
				if  (ie != null)
					bindingBlock = ie.falseCase();
			} else {
				cond = (JSExpr) r;
				ie = bindingBlock.ifTrue(cond);
				bindingBlock = ie.trueCase();
			}
		}
	}

	@Override
	public void leaveTemplateBinding(TemplateBinding tb) {
		assignsTo = null;
		if (!styles.isEmpty()) {
			templateBlock.updateStyle(tb.assignsTo, styles);
			styles.clear();
		}
	}
	
	@Override
	public void leaveTemplate(Template t) {
		sv.result(null);
	}
}
