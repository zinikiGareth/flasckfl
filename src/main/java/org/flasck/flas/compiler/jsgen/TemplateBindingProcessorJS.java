package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSIfExpr;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateCustomization;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.ziniki.splitter.FieldType;
import org.zinutils.collections.CollectionUtils;

public class TemplateBindingProcessorJS extends LeafAdapter implements ResultAware {
	enum Mode {
		COND, EXPR
	}
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final TemplateBinding b;
	private final List<JSStyleIf> styles = new ArrayList<>();
	private final List<JSExpr> cexpr = new ArrayList<>();

	private Mode mode;
	private JSIfExpr ie;
	private JSBlockCreator bindingBlock;
	private TemplateBindingOption currentTBO;

	public TemplateBindingProcessorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator templateBlock, TemplateBinding b) {
		this.state = state;
		this.sv = sv;
		this.bindingBlock = templateBlock;
		this.b = b;
		sv.push(this);
	}

	@Override
	public void visitTemplateBindingOption(TemplateBindingOption option) {
		currentTBO = option;
	}
	
	@Override
	public void visitTemplateBindingCondition(Expr cond) {
		mode = Mode.COND;
		new ExprGeneratorJS(state, sv, bindingBlock, false);
	}
	
	@Override
	public void visitTemplateBindingExpr(Expr expr) {
		mode = Mode.EXPR;
		new ExprGeneratorJS(state, sv, bindingBlock, false);
	}

	@Override
	public void visitTemplateStyling(TemplateStylingOption tso) {
		new TemplateStylingJS(state, sv, bindingBlock, tso);
	}
	
	@Override
	public void result(Object r) {
		if (r instanceof JSStyleIf) {
			JSStyleIf si = (JSStyleIf)r;
			if (si.cond != null)
				styles.add(si);
			else
				cexpr.add(si.style);
		} else {
			if (mode == Mode.COND) {
				ie = bindingBlock.ifTrue((JSExpr) r);
				bindingBlock = ie.trueCase();
			} else {
				if (currentTBO.sendsTo != null) {
					ArrayList<JSExpr> wanted = new ArrayList<>();
					if (state.templateObj() != null) {
						for (int i : currentTBO.sendsTo.contextPosns()) {
							wanted.add(CollectionUtils.nth(state.templateObj().values(), i));
						}
					}
					boolean isOtherObject = (currentTBO.expr instanceof UnresolvedVar) &&
						((UnresolvedVar)currentTBO.expr).defn() instanceof StructField &&
						((StructField)((UnresolvedVar)currentTBO.expr).defn()).type.defn() instanceof ObjectDefn;
					bindingBlock.updateTemplate(b.assignsTo, currentTBO.sendsTo.template().position(),
						isOtherObject,
						currentTBO.sendsTo.defn().id(),
						(JSExpr) r,
						bindingBlock.makeArray(wanted));
				} else if (currentTBO.assignsTo.type() == FieldType.CONTAINER) {
					bindingBlock.updateContainer(b.assignsTo, (JSExpr) r);
				} else
					bindingBlock.updateContent(b.assignsTo, (JSExpr) r);
			}
		}
	}

	@Override
	public void leaveTemplateCustomization(TemplateCustomization tc) {
		if (!styles.isEmpty() || !cexpr.isEmpty()) {
			JSExpr ce;
			if (cexpr.isEmpty())
				ce = bindingBlock.literal("null");
			else if (cexpr.size() == 1)
				ce = cexpr.get(0);
			else
				ce = bindingBlock.callMethod(bindingBlock.literal("FLBuiltin"), "concatMany", cexpr.toArray(new JSExpr[cexpr.size()]));

			bindingBlock.updateStyle(b.assignsTo, ce, styles);
			styles.clear();
			cexpr.clear();
		}
		if (ie != null)
			bindingBlock = ie.falseCase();
	}
	
	@Override
	public void leaveTemplateBindingOption(TemplateBindingOption option) {
		currentTBO = null;
	}
	
	@Override
	public void leaveTemplateBinding(TemplateBinding tb) {
		sv.result(null);
	}
}
