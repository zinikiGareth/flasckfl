package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateCustomization;
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.collections.CollectionUtils;

public class TemplateBindingProcessor extends LeafAdapter implements ResultAware {
	enum Mode {
		COND, EXPR
	}
	public class JVMBinding {
		public IExpr cond;
		public List<IExpr> trueBlock = new ArrayList<IExpr>();
		public IExpr du;
	}
	private final FunctionState fs;
	private final StackVisitor sv;
	private final TemplateField assignsTo;
	private final List<JVMStyleIf> styles = new ArrayList<>();
	private final List<IExpr> cexpr = new ArrayList<>();

	private final List<JVMBinding> bindings = new ArrayList<>();
	private Mode mode;
	private JVMBinding curr;
	private List<IExpr> bindingBlock;
	private TemplateBindingOption currentTBO;

	public TemplateBindingProcessor(FunctionState fs, StackVisitor sv, TemplateBinding b) {
		this.fs = fs;
		this.sv = sv;
		this.bindingBlock = new ArrayList<IExpr>();
		assignsTo = b.assignsTo;
		sv.push(this);
	}

	@Override
	public void visitTemplateBindingOption(TemplateBindingOption option) {
		curr = new JVMBinding();
		bindings.add(0, curr);
		currentTBO = option;
	}
	
	@Override
	public void visitTemplateBindingCondition(Expr cond) {
		mode = Mode.COND;
		new ExprGenerator(fs, sv, bindingBlock, false);
	}
	
	@Override
	public void visitTemplateBindingExpr(Expr expr) {
		mode = Mode.EXPR;
		new ExprGenerator(fs, sv, bindingBlock, false);
	}
	
	@Override
	public void visitTemplateStyling(TemplateStylingOption tso) {
		new TemplateStyling(fs, sv, bindingBlock, tso);
	}

	@Override
	public void result(Object r) {
		if (r instanceof JVMStyleIf) {
			JVMStyleIf si = (JVMStyleIf)r;
			if (si.cond != null)
				styles.add(si);
			else
				cexpr.add(si.style);
		} else {
			if (mode == Mode.COND) {
				curr.cond = (IExpr) r;
				curr.trueBlock = new ArrayList<>();
			} else {
				IExpr expr = (IExpr) r;
				if (currentTBO.sendsTo != null) {
					IExpr tc;
					if (fs.templateObj == null)
						tc = fs.meth.arrayOf(J.OBJECT);
					else {
						ArrayList<IExpr> wanted = new ArrayList<>();
						for (int i : currentTBO.sendsTo.contextPosns()) {
							wanted.add(CollectionUtils.nth(fs.templateObj.values(), i));
						}
						tc = fs.meth.arrayOf(J.OBJECT, wanted);
					}
					boolean isOtherObject = (currentTBO.expr instanceof UnresolvedVar) &&
						((UnresolvedVar)currentTBO.expr).defn() instanceof StructField &&
						((StructField)((UnresolvedVar)currentTBO.expr).defn()).type.defn() instanceof ObjectDefn;

					IExpr invokeOn;
					IExpr send;
					if (isOtherObject) {
						invokeOn = fs.meth.as(expr, J.TEMPLATE_HOLDER);
						send = fs.meth.makeNew(J.OBJECT);
					} else {
						invokeOn = fs.meth.as(fs.container, J.TEMPLATE_HOLDER);
						send = fs.meth.as(expr, J.OBJECT);
					}
					
					curr.du = fs.meth.callInterface("void", invokeOn, "_updateTemplate",
						fs.fcx, fs.renderTree(), 
						fs.meth.stringConst(assignsTo.type().toString().toLowerCase()), fs.meth.stringConst(assignsTo.text),
						fs.meth.intConst(currentTBO.sendsTo.template().position()),
						fs.meth.stringConst(currentTBO.sendsTo.defn().id()),
						send,
						tc);
					if (isOtherObject) {
						curr.du = fs.meth.ifNotNull(invokeOn, curr.du, null);
					}
				} else
					curr.du = fs.meth.callVirtual("void", fs.container, "_updateContent", fs.fcx, fs.renderTree(), fs.meth.stringConst(assignsTo.text), fs.meth.as(expr, J.OBJECT));
			}
			this.bindingBlock = curr.trueBlock;
		}
	}
	
	@Override
	public void leaveTemplateCustomization(TemplateCustomization tc) {
		if (styles.isEmpty() && cexpr.isEmpty())
			return;
		IExpr ce;
		if (cexpr.isEmpty())
			ce = fs.meth.as(fs.meth.aNull(), J.STRING);
		else if (cexpr.size() == 1)
			ce = cexpr.get(0);
		else
			ce = fs.meth.callStatic(J.BUILTINPKG+".PACKAGEFUNCTIONS", J.STRING, "concatMany", fs.fcx, fs.meth.arrayOf(J.OBJECT, cexpr));
		
		List<IExpr> arr = new ArrayList<>();
		for (JVMStyleIf si : styles) {
			arr.add(si.cond);
			arr.add(si.style);
		}
		
		IExpr doUpdate = fs.meth.callVirtual("void", fs.container, "_updateStyles", fs.fcx, fs.renderTree(), fs.meth.stringConst(assignsTo.type().toString().toLowerCase()), fs.meth.stringConst(assignsTo.text), ce, fs.meth.arrayOf(J.OBJECT, arr));
		bindingBlock.add(doUpdate);
		styles.clear();
		cexpr.clear();
	}

	@Override
	public void leaveTemplateBindingOption(TemplateBindingOption option) {
		currentTBO = null;
	}
	
	@Override
	public void leaveTemplateBinding(TemplateBinding tb) {
		IExpr ret = null;
		if (bindings.isEmpty() && !bindingBlock.isEmpty())
			ret = JVMGenerator.makeBlock(fs.meth, bindingBlock);
		for (JVMBinding b : bindings) {
			b.trueBlock.add(b.du);
			IExpr truth = JVMGenerator.makeBlock(fs.meth, b.trueBlock);
			if (b.cond == null)
				ret = truth;
			else
				ret = fs.meth.ifBoolean(
					fs.meth.callInterface(JavaType.boolean_.toString(), fs.fcx, "isTruthy", fs.meth.as(b.cond, J.OBJECT)),
					truth,
					ret);
		}
		if (ret != null)
			ret.flush();
		sv.result(null);
	}
}
