package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.IExpr;

public class TemplateProcessor extends LeafAdapter implements ResultAware {
	private final FunctionState fs;
	private final StackVisitor sv;
	private final ByteCodeSink templateClass;
	private final AtomicInteger containerIdx;
	private final IExpr source;
	private final Template t;
	private final List<IExpr> block;
	private final List<JVMStyleIf> styles = new ArrayList<>();
	private final List<IExpr> cexpr = new ArrayList<>();

	public TemplateProcessor(FunctionState functionState, StackVisitor sv, ByteCodeSink templateClass, AtomicInteger containerIdx, IExpr source, Template t) {
		this.fs = functionState;
		this.sv = sv;
		this.templateClass = templateClass;
		this.containerIdx = containerIdx;
		this.source = source;
		this.t = t;
		this.block = new ArrayList<IExpr>();
		sv.push(this);
	}
	
	@Override
	public void visitTemplateBinding(TemplateBinding b) {
		new TemplateBindingProcessor(fs, sv, templateClass, containerIdx, t, source, b);
	}
	
	@Override
	public void visitTemplateStyling(TemplateStylingOption tso) {
		new TemplateStyling(fs, sv, block, tso);
	}
	
	@Override
	public void result(Object r) {
		if (r instanceof JVMStyleIf) {
			JVMStyleIf si = (JVMStyleIf)r;
			if (si.cond != null)
				styles.add(si);
			else
				cexpr.add(si.style);
		}
	}

	@Override
	public void leaveTemplate(Template t) {
		TemplateBindingProcessor.applyStyles(fs, block, null, styles, cexpr);
		if (!block.isEmpty())
			JVMGenerator.makeBlock(fs.meth, block).flush();
		fs.meth.returnVoid().flush();
		sv.result(null);
	}
}
