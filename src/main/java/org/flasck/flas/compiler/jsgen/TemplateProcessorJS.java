package org.flasck.flas.compiler.jsgen;

import java.util.concurrent.atomic.AtomicInteger;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;

public class TemplateProcessorJS extends LeafAdapter {

	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator templateBlock;
	private final JSClassCreator templateCreator;
	private final AtomicInteger containerIdx;

	public TemplateProcessorJS(JSFunctionState state, NestedVisitor sv, JSClassCreator templateCreator, AtomicInteger containerIdx, JSBlockCreator currentBlock) {
		this.state = state;
		this.sv = sv;
		this.templateCreator = templateCreator;
		this.containerIdx = containerIdx;
		this.templateBlock = currentBlock;
		sv.push(this);
	}
	
	@Override
	public void visitTemplateBinding(TemplateBinding b) {
		new TemplateBindingProcessorJS(state, sv, templateCreator, containerIdx, templateBlock, b);
	}
	
	@Override
	public void leaveTemplate(Template t) {
		sv.result(null);
	}
}
