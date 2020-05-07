package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

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

public class TemplateProcessor extends LeafAdapter implements ResultAware {
	private final FunctionState fs;
	private final StackVisitor sv;
	private final List<IExpr> currentBlock;
	private IExpr expr;
	private final List<JVMStyleIf> styles = new ArrayList<>();

	public TemplateProcessor(FunctionState functionState, StackVisitor sv) {
		this.fs = functionState;
		this.sv = sv;
		this.currentBlock = new ArrayList<IExpr>();
		sv.push(this);
	}
	
	@Override
	public void visitTemplateBindingOption(TemplateBindingOption option) {
		new ExprGenerator(fs, sv, currentBlock, false);
	}
	
	@Override
	public void result(Object r) {
		if (r instanceof JVMStyleIf) {
			styles.add((JVMStyleIf)r);
		} else {
			// Handle cond
			expr = (IExpr) r;
		}
	}

	@Override
	public void leaveTemplateBindingOption(TemplateBindingOption tbo) {
		currentBlock.add(fs.meth.callVirtual("void", fs.container, "_updateContent", fs.fcx, fs.meth.stringConst(tbo.assignsTo.text), expr));
		JVMGenerator.makeBlock(fs.meth, currentBlock).flush();
		currentBlock.clear();
		updateStyling(tbo.assignsTo);
	}

	@Override
	public void leaveTemplateBinding(TemplateBinding tb) {
		TemplateField assignsTo = new TemplateField(tb.slotLoc, tb.slot);
		assignsTo.fieldType(tb.fieldType());
		updateStyling(assignsTo);
	}
	
	@Override
	public void visitTemplateStyling(TemplateStylingOption tso) {
		new TemplateStyling(fs, sv, currentBlock, tso);
	}

	public void updateStyling(TemplateField assignsTo) {
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
	}

	@Override
	public void leaveTemplate(Template t) {
		fs.meth.returnVoid().flush();
		sv.result(null);
	}
}
