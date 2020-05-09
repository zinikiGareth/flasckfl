package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateCustomization;
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;

public class TemplateBindingProcessor extends LeafAdapter implements ResultAware {
	enum Mode {
		COND, EXPR
	}
	public class JVMBinding {
		public IExpr cond;
		public IExpr expr;
		public List<IExpr> trueBlock = new ArrayList<IExpr>();
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
			} else
				curr.expr = (IExpr) r;
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
		
		System.out.println("Styling " + assignsTo.text + (curr == null ? " styling": " " + (bindingBlock == curr.trueBlock)));
		IExpr doUpdate = fs.meth.callVirtual("void", fs.container, "_updateStyles", fs.fcx, fs.meth.stringConst(assignsTo.type().toString().toLowerCase()), fs.meth.stringConst(assignsTo.text), ce, fs.meth.arrayOf(J.OBJECT, arr));
		bindingBlock.add(doUpdate);
		styles.clear();
		cexpr.clear();
	}

	@Override
	public void leaveTemplateBinding(TemplateBinding tb) {
		IExpr ret = null;
		if (bindings.isEmpty() && !bindingBlock.isEmpty())
			ret = JVMGenerator.makeBlock(fs.meth, bindingBlock);
		for (JVMBinding b : bindings) {
			b.trueBlock.add(fs.meth.callVirtual("void", fs.container, "_updateContent", fs.fcx, fs.meth.stringConst(assignsTo.text), fs.meth.as(b.expr, J.OBJECT)));
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
