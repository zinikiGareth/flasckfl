package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.templates.EventPlacement;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateEvent;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class TemplateProcessorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator currentBlock;
	private JSExpr expr;

	public TemplateProcessorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator currentBlock) {
		this.state = state;
		this.sv = sv;
		this.currentBlock = currentBlock;
		sv.push(this);
	}

	@Override
	public void visitTemplateBindingOption(TemplateBindingOption tbo) {
		new ExprGeneratorJS(state, sv, currentBlock, false);
	}
	@Override
	public void result(Object r) {
		// TODO: need to consider "cond" as well ...
		expr = (JSExpr) r;
	}
	
	@Override
	public void leaveTemplateBindingOption(TemplateBindingOption tbo) {
		currentBlock.updateContent(tbo.assignsTo, expr);
	}
	
	@Override
	public void leaveTemplate(Template t) {
		sv.result(null);
	}
}
