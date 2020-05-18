package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class TemplateProcessorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator templateBlock;
	private final JSClassCreator templateCreator;
	private final AtomicInteger containerIdx;
	private final List<JSStyleIf> styles = new ArrayList<>();
	private final List<JSExpr> cexpr = new ArrayList<>();

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
	public void visitTemplateStyling(TemplateStylingOption tso) {
		new TemplateStylingJS(state, sv, templateBlock, tso);
	}
	
	@Override
	public void result(Object r) {
		if (r instanceof JSStyleIf) {
			JSStyleIf si = (JSStyleIf)r;
			if (si.cond != null)
				styles.add(si);
			else
				cexpr.add(si.style);
		}
	}

	@Override
	public void leaveTemplate(Template t) {
		TemplateBindingProcessorJS.applyStyles(templateBlock, null, styles, cexpr);
		sv.result(null);
	}
}
