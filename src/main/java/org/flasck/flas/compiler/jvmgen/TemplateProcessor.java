package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.zinutils.bytecode.IExpr;

public class TemplateProcessor extends LeafAdapter implements ResultAware {
	private final FunctionState fs;
	private final StackVisitor sv;
	private final List<IExpr> currentBlock;
	private IExpr expr;

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
		// Handle cond
		expr = (IExpr) r;
	}

	@Override
	public void leaveTemplateBindingOption(TemplateBindingOption tbo) {
		currentBlock.add(fs.meth.callVirtual("void", fs.container, "_updateContent", fs.fcx, fs.meth.stringConst(tbo.assignsTo.text), expr));
		JVMGenerator.makeBlock(fs.meth, currentBlock).flush();
	}

	@Override
	public void visitTemplateStyling(TemplateStylingOption tso) {
		if (tso.cond != null)
			new ExprGenerator(fs, sv, currentBlock, false);
	}

	@Override
	public void leaveTemplateStyling(TemplateStylingOption tso) {
		currentBlock.add(fs.meth.callVirtual("void", fs.container, "_updateStyles", fs.fcx, fs.meth.stringConst(tso.styleField.text), fs.meth.stringConst(tso.styleString())));
		JVMGenerator.makeBlock(fs.meth, currentBlock).flush();
	}

	@Override
	public void leaveTemplate(Template t) {
		fs.meth.returnVoid().flush();
		sv.result(null);
	}
}
