package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.generators.GenerationContext;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public class MethodGenerationContext implements GenerationContext {
	private final ByteCodeStorage bce;
	private final HSIEForm form;
	private ByteCodeSink bcc;
	private MethodDefiner meth;
	private List<PendingVar> pendingVars = new ArrayList<PendingVar>();
	private VarHolder vh;
	private Var cxtArg;

	public MethodGenerationContext(ByteCodeStorage bce, HSIEForm form) {
		this.bce = bce;
		this.form = form;
	}

	@Override
	public NameOfThing nameContext() {
		return form.funcName.inContext;
	}

	@Override
	public FunctionName funcName() {
		return form.funcName;
	}

	@Override
	public boolean selectClass(String inClz) {
		if (bce.hasClass(inClz)) {
			bcc = bce.get(inClz);
			return false;
		} else {
			bcc = bce.newClass(inClz);
			bcc.generateAssociatedSourceFile();
			bcc.superclass(J.OBJECT);
			return true;
		}
	}

	@Override
	public void defaultCtor() {
		final ByteCodeSink clz = bcc;
		defaultCtor(clz);
	}

	private void defaultCtor(final ByteCodeSink clz) {
		GenericAnnotator ann = GenericAnnotator.newConstructor(clz, false);
		MethodDefiner ctor = ann.done();
		ctor.callSuper("void", J.OBJECT, "<init>").flush();
		ctor.returnVoid().flush();
	}

	@Override
	public void instanceMethod() {
		doMethod(false);
	}

	@Override
	public void staticMethod() {
		doMethod(true);
	}

	private void doMethod(boolean isStatic) {
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, isStatic, form.funcName.name);
		PendingVar cxt = gen.argument(J.OBJECT, "_context");
		gen.returns("java.lang.Object");
		int j = 0;
		for (@SuppressWarnings("unused") ScopedVar s : form.scoped)
			pendingVars.add(gen.argument("java.lang.Object", "_s"+(j++)));
		for (int i=0;i<form.nformal;i++)
			pendingVars.add(gen.argument("java.lang.Object", "_"+i));
		meth = gen.done();
		cxtArg = cxt.getVar();
		vh = new VarHolder(form, pendingVars);
	}

	@Override
	public void trampoline(String outerClz) {
		ByteCodeSink inner = bce.newClass(outerClz + "$" + form.funcName.name);
		inner.generateAssociatedSourceFile();
		inner.superclass(J.OBJECT);
		defaultCtor(inner);
		GenericAnnotator g2 = GenericAnnotator.newMethod(inner, true, "eval");
		PendingVar cx = g2.argument(J.OBJECT, "cxt");
		g2.returns(J.OBJECT);
		PendingVar args = g2.argument("[" + J.OBJECT, "args");
		MethodDefiner m2 = g2.done();
		IExpr[] fnArgs = new IExpr[pendingVars.size()+1];
		fnArgs[0] = cx.getVar();
		for (int i=0;i<pendingVars.size();i++) {
			fnArgs[i+1] = m2.arrayElt(args.getVar(), m2.intConst(i));
		}
		IExpr doCall = m2.callStatic(outerClz, J.OBJECT, form.funcName.name, fnArgs);
		m2.returnObject(doCall).flush();
	}
	
	@Override
	public void trampolineWithSelf(String outerClz) {
		ByteCodeSink inner = bce.newClass(outerClz + "$" + form.funcName.name);
		inner.generateAssociatedSourceFile();
		inner.superclass(J.OBJECT);
		defaultCtor(inner);
//		IFieldInfo fi = inner.defineField(true, Access.PRIVATE, bcc.getCreatedName(), "_card");
//		GenericAnnotator ctor = GenericAnnotator.newConstructor(inner, false);
//		PendingVar arg = ctor.argument(bcc.getCreatedName(), "card");
//		MethodDefiner c = ctor.done();
//		c.callSuper("void", "java.lang.Object", "<init>").flush();
//		c.assign(fi.asExpr(c), arg.getVar()).flush();
//		c.returnVoid().flush();
		GenericAnnotator g2 = GenericAnnotator.newMethod(inner, true, "eval");
		PendingVar cx = g2.argument(J.OBJECT, "cxt");
		g2.returns(J.OBJECT);
		PendingVar forThis = g2.argument(J.OBJECT,  "self");
		PendingVar args = g2.argument("[" + J.OBJECT, "args");
		MethodDefiner m2 = g2.done();
		IExpr[] fnArgs = new IExpr[pendingVars.size()+1];
		fnArgs[0] = cx.getVar();
		for (int i=0;i<pendingVars.size();i++) {
			fnArgs[i+1] = m2.arrayElt(args.getVar(), m2.intConst(i));
		}
		IExpr doCall = m2.callVirtual(J.OBJECT, m2.castTo(forThis.getVar(), outerClz), form.funcName.name, fnArgs);
		m2.returnObject(doCall).flush();
	}

	// TODO: not sure these are needed at the end of the day

	public Var getCxtArg() {
		return cxtArg;
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
	public VarHolder getVarHolder() {
		return vh;
	}
}
