package org.flasck.flas.compiler.jvmgen;

import java.util.concurrent.atomic.AtomicInteger;

import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.StackVisitor;
import org.zinutils.bytecode.ByteCodeSink;

public class TemplateProcessor extends LeafAdapter {
	private final FunctionState fs;
	private final StackVisitor sv;
	private final ByteCodeSink templateClass;
	private final AtomicInteger containerIdx;

	public TemplateProcessor(FunctionState functionState, StackVisitor sv, ByteCodeSink templateClass, AtomicInteger containerIdx) {
		this.fs = functionState;
		this.sv = sv;
		this.templateClass = templateClass;
		this.containerIdx = containerIdx;
		sv.push(this);
	}
	
	@Override
	public void visitTemplateBinding(TemplateBinding b) {
		new TemplateBindingProcessor(fs, sv, templateClass, containerIdx, b);
	}
	
	@Override
	public void leaveTemplate(Template t) {
		fs.meth.returnVoid().flush();
		sv.result(null);
	}
}
