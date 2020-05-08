package org.flasck.flas.compiler.jvmgen;

import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.StackVisitor;

public class TemplateProcessor extends LeafAdapter {
	private final FunctionState fs;
	private final StackVisitor sv;

	public TemplateProcessor(FunctionState functionState, StackVisitor sv) {
		this.fs = functionState;
		this.sv = sv;
		sv.push(this);
	}
	
	@Override
	public void visitTemplateBinding(TemplateBinding b) {
		new TemplateBindingProcessor(fs, sv, b);
	}
	
	@Override
	public void leaveTemplate(Template t) {
		fs.meth.returnVoid().flush();
		sv.result(null);
	}
}
