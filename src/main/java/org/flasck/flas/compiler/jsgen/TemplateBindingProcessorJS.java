package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSIfExpr;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateCustomization;
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.NamedType;
import org.ziniki.splitter.FieldType;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.NotImplementedException;

public class TemplateBindingProcessorJS extends LeafAdapter implements ResultAware {
	enum Mode {
		COND, EXPR
	}
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final String templateName;
	private final JSClassCreator templateCreator;
	private final AtomicInteger containerIdx;
	private final TemplateBinding b;
	private final JSExpr source;
	private final List<JSStyleIf> styles = new ArrayList<>();
	private final List<JSExpr> cexpr = new ArrayList<>();

	private Mode mode;
	private JSIfExpr ie;
	private JSBlockCreator bindingBlock;
	private TemplateBindingOption currentTBO;
	private int option = 0;

	public TemplateBindingProcessorJS(JSFunctionState state, NestedVisitor sv, JSClassCreator templateCreator, AtomicInteger containerIdx, JSBlockCreator templateBlock, Template t, JSExpr source, TemplateBinding b) {
		this.state = state;
		this.sv = sv;
		this.source = source;
		this.templateName = t.webinfo().id();
		this.templateCreator = templateCreator;
		this.containerIdx = containerIdx;
		this.bindingBlock = templateBlock;
		this.b = b;
		sv.push(this);
	}

	@Override
	public void visitTemplateBindingOption(TemplateBindingOption option) {
		currentTBO = option;
		this.option++;
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
					Map<NamedType, Template> mapping = currentTBO.mapping();
					if (mapping == null)
						throw new NotImplementedException("No mapping for " + currentTBO.assignsTo.text);
					int ucidx = containerIdx.getAndIncrement();
					JSMethodCreator uc = templateCreator.createMethod("_updateContainer" + ucidx, true);
					uc.argument("_cxt");
					uc.argument("_renderTree");
					uc.argument("parent");
					uc.argument("currNode");
					uc.argument("e");
					JSVar expr = uc.arg(4);
					if (mapping.size() == 1) {
						templateMember(uc, mapping.values().iterator().next(), expr);
					} else {
						JSBlockCreator block = uc;
						for (Entry<NamedType, Template> e : mapping.entrySet()) {
							JSIfExpr ifExpr = block.ifCtor(expr, e.getKey().name());
							templateMember(ifExpr.trueCase(), e.getValue(), expr);
							block = ifExpr.falseCase();
						}
						block.error(expr);
					}
					bindingBlock.updateContainer(b.assignsTo, (JSExpr) r, ucidx);
				} else
					bindingBlock.updateContent(templateName, b.assignsTo, option, source, (JSExpr) r);
			}
		}
	}

	private void templateMember(JSBlockCreator block, Template e, JSExpr expr) {
		ArrayList<JSExpr> wanted = new ArrayList<>();
		if (state.templateObj() != null) {
			// TODO: this cannot use "sendsTo" since it is null
			// I think it needs to read from the template passed in
			if (currentTBO.sendsTo != null) {
				for (int i : currentTBO.sendsTo.contextPosns()) {
					wanted.add(CollectionUtils.nth(state.templateObj().values(), i));
				}
			}
		}
		block.addItem(e.position(),
			e.webinfo().id(),
			expr,
			block.makeArray(wanted));
	}

	@Override
	public void leaveTemplateCustomization(TemplateCustomization tc) {
		applyStyles(bindingBlock, templateName, b.assignsTo, option, source, styles, cexpr, !tc.events.isEmpty());
		if (ie != null)
			bindingBlock = ie.falseCase();
	}

	static void applyStyles(JSBlockCreator bindingBlock, String templateName, TemplateField update, int option, JSExpr source, List<JSStyleIf> styles, List<JSExpr> cexpr, boolean hasStylingEvents) {
		if (!styles.isEmpty() || !cexpr.isEmpty()) {
			JSExpr ce;
			if (cexpr.isEmpty())
				ce = bindingBlock.literal("null");
			else if (cexpr.size() == 1)
				ce = cexpr.get(0);
			else
				ce = bindingBlock.callMethod(bindingBlock.literal("FLBuiltin"), "concatMany", cexpr.toArray(new JSExpr[cexpr.size()]));

			bindingBlock.updateStyle(templateName, update, option, source, ce, styles);
			styles.clear();
			cexpr.clear();
		} else if (hasStylingEvents)
			bindingBlock.updateStyle(templateName, update, option, source, bindingBlock.literal("null"), styles);
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
