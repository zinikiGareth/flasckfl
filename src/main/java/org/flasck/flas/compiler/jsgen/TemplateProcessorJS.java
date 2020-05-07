package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class TemplateProcessorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator currentBlock;
	private JSExpr expr;
	private final List<JSStyleIf> styles = new ArrayList<>();

	public TemplateProcessorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator currentBlock) {
		this.state = state;
		this.sv = sv;
		this.currentBlock = currentBlock;
		sv.push(this);
	}
	
	@Override
	public void result(Object r) {
		if (r instanceof JSStyleIf) {
			styles.add((JSStyleIf)r);
		} else {
			// TODO: need to consider "cond" as well ...
			expr = (JSExpr) r;
		}
	}

	@Override
	public void visitTemplateBindingOption(TemplateBindingOption tbo) {
		new ExprGeneratorJS(state, sv, currentBlock, false);
	}
	
	@Override
	public void visitTemplateStyling(TemplateStylingOption tso) {
		new TemplateStylingJS(state, sv, currentBlock, tso);
	}
	
	@Override
	public void leaveTemplateBindingOption(TemplateBindingOption tbo) {
		currentBlock.updateContent(tbo.assignsTo, expr);
		if (!styles.isEmpty()) {
			currentBlock.updateStyle(tbo.assignsTo, styles);
			styles.clear();
		}
	}
	
	@Override
	public void leaveTemplateBinding(TemplateBinding tb) {
		TemplateField assignsTo = new TemplateField(tb.slotLoc, tb.slot);
		assignsTo.fieldType(tb.fieldType());
		if (!styles.isEmpty()) {
			currentBlock.updateStyle(assignsTo, styles);
			styles.clear();
		}
	}
	
	@Override
	public void leaveTemplate(Template t) {
		sv.result(null);
	}
}
