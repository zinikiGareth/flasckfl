package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;

public class TemplateProcessorJS extends LeafAdapter {

	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator templateBlock;

	public TemplateProcessorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator currentBlock) {
		this.state = state;
		this.sv = sv;
		this.templateBlock = currentBlock;
		sv.push(this);
	}
	
	@Override
	public void visitTemplateBinding(TemplateBinding b) {
		new TemplateBindingProcessorJS(state, sv, templateBlock, b);
	}
	
	@Override
	public void leaveTemplate(Template t) {
		sv.result(null);
	}
}
