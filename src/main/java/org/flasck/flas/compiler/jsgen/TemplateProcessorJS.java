package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateEvent;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.web.EventPlacement;

public class TemplateProcessorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator currentBlock;
	private final EventPlacement etz;
	private final String templateId;
	private JSExpr expr;
	private TemplateBinding currentBinding;

	public TemplateProcessorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator currentBlock, EventPlacement etz, String templateId) {
		this.state = state;
		this.sv = sv;
		this.currentBlock = currentBlock;
		this.etz = etz;
		this.templateId = templateId;
		sv.push(this);
	}

	@Override
	public void visitTemplateBindingOption(TemplateBindingOption tbo) {
		new ExprGeneratorJS(state, sv, currentBlock, false);
	}
	@Override
	public void visitTemplateBinding(TemplateBinding b) {
		currentBinding = b;
	}
	
	@Override
	public void visitTemplateEvent(TemplateEvent te) {
		etz.binding(templateId, currentBinding, te.handler);
	}
	
	@Override
	public void leaveTemplateBinding(TemplateBinding b) {
		currentBinding = null;
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
