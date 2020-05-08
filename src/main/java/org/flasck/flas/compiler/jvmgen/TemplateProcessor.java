package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;

public class TemplateProcessor extends LeafAdapter implements ResultAware {
	public class JVMBinding {
		public IExpr cond;
		public IExpr expr;
		public List<IExpr> trueBlock;
		public List<IExpr> falseBlock;
	}

	private final FunctionState fs;
	private final StackVisitor sv;
	private final List<IExpr> currentBlock;
	private List<IExpr> bindingBlock;
	private final List<JVMStyleIf> styles = new ArrayList<>();
	private final List<JVMBinding> bindings = new ArrayList<>();
	private TemplateField assignsTo;
	private boolean collectingCond;
	private JVMBinding curr;

	public TemplateProcessor(FunctionState functionState, StackVisitor sv) {
		this.fs = functionState;
		this.sv = sv;
		this.currentBlock = new ArrayList<IExpr>();
		this.bindingBlock = currentBlock;
		sv.push(this);
	}
	
	@Override
	public void visitTemplateBinding(TemplateBinding b) {
		assignsTo = b.assignsTo;
	}
	
	@Override
	public void visitTemplateBindingOption(TemplateBindingOption option) {
		curr = new JVMBinding();
		bindings.add(0, curr);
	}
	
	@Override
	public void visitTemplateBindingCondition(Expr cond) {
		collectingCond = true;
		new ExprGenerator(fs, sv, bindingBlock, false);
	}
	
	@Override
	public void visitTemplateBindingExpr(Expr expr) {
		collectingCond = false;
		if (curr.trueBlock == null)
			curr.trueBlock = bindingBlock;
		new ExprGenerator(fs, sv, bindingBlock, false);
	}
	
	@Override
	public void visitTemplateStyling(TemplateStylingOption tso) {
		new TemplateStyling(fs, sv, currentBlock, tso);
	}

	@Override
	public void result(Object r) {
		if (r instanceof JVMStyleIf) {
			styles.add((JVMStyleIf)r);
		} else {
			if (collectingCond) {
				curr.cond = (IExpr) r;
				curr.trueBlock = new ArrayList<>();
				curr.falseBlock = new ArrayList<>();
				bindingBlock = curr.falseBlock;
			} else
				curr.expr = (IExpr) r;
		}
	}

	@Override
	public void leaveTemplateBindingOption(TemplateBindingOption tbo) {
//		updateStyling(tbo.assignsTo);
	}

	@Override
	public void leaveTemplateBinding(TemplateBinding tb) {
		IExpr ret = null;
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
		currentBlock.clear();
		updateStyling(tb.assignsTo);
		assignsTo = null;
	}
	
	public void updateStyling(TemplateField assignsTo) {
		if (styles.isEmpty())
			return;
		StringBuilder sb = new StringBuilder();
		List<IExpr> arr = new ArrayList<>();
		for (JVMStyleIf si : styles) {
			if (si.cond == null) {
				if (sb.length() > 0)
					sb.append(" ");
				sb.append(si.styles);
			} else {
				arr.add(si.cond);
				arr.add(fs.meth.stringConst(si.styles));
			}
		}
		
		IExpr doUpdate = fs.meth.callVirtual("void", fs.container, "_updateStyles", fs.fcx, fs.meth.stringConst(assignsTo.type().toString().toLowerCase()), fs.meth.stringConst(assignsTo.text), fs.meth.stringConst(sb.toString()), fs.meth.arrayOf(J.OBJECT, arr));
//		if (tso.cond != null) {
//			IExpr isTruthy = fs.meth.callInterface("boolean", fs.fcx, "isTruthy", expr);
//			currentBlock.add(fs.meth.ifBoolean(isTruthy, doUpdate, null));
//		} else {
			currentBlock.add(doUpdate);
//		}
		JVMGenerator.makeBlock(fs.meth, currentBlock).flush();
		currentBlock.clear();
		styles.clear();
	}

	@Override
	public void leaveTemplate(Template t) {
		fs.meth.returnVoid().flush();
		sv.result(null);
	}
}
