package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.generators.GenerationContext;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;

public class MethodGenerationContext implements GenerationContext {
	private final ByteCodeStorage bce;
	private final HSIEForm form;
	private ByteCodeSink bcc;
	private MethodDefiner meth;
	private List<PendingVar> pendingVars = new ArrayList<PendingVar>();

	public MethodGenerationContext(ByteCodeStorage bce, HSIEForm form) {
		this.bce = bce;
		this.form = form;
	}

	@Override
	public NameOfThing nameContext() {
		return form.funcName.inContext;
	}

	@Override
	public void selectClass(String inClz) {
		if (bce.hasClass(inClz))
			bcc = bce.get(inClz);
		else {
			bcc = bce.newClass(inClz);
			bcc.generateAssociatedSourceFile();
			bcc.superclass("java.lang.Object");
		}
	}

	@Override
	public void instanceMethod(boolean withContext) {
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, form.funcName.name);
		gen.returns("java.lang.Object");
		if (withContext)
			gen.argument(J.OBJECT, "_context");
		int j = 0;
		for (@SuppressWarnings("unused") ScopedVar s : form.scoped)
			pendingVars.add(gen.argument("java.lang.Object", "_s"+(j++)));
		for (int i=0;i<form.nformal;i++)
			pendingVars.add(gen.argument("java.lang.Object", "_"+i));
		meth = gen.done();
	}

	// TODO: not sure these are needed at the end of the day
	@Override
	public boolean hasMethod() {
		return meth != null;
	}

	@Override
	public ByteCodeSink getSink() {
		return bcc;
	}

	@Override
	public MethodDefiner getMethod() {
		return meth;
	}

	@Override
	public List<PendingVar> getVars() {
		return pendingVars;
	}

}
