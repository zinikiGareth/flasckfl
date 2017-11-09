package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.generators.CodeGenerator;
import org.flasck.flas.generators.GenerationContext;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.exceptions.UtilException;

public class DroidHSIEFormGenerator {
	private final ByteCodeStorage bce;

	public DroidHSIEFormGenerator(ByteCodeStorage bce) {
		this.bce = bce;
	}

	public void generate(HSIEForm form) {
		String inClz;
		final String fn = form.funcName.name;
		boolean needTrampolineClass;
		boolean wantThis = false;
		boolean needContext = false;
		System.out.println("function " + fn);
		GenerationContext cxt = new MethodGenerationContext(bce, form);
		CodeGenerator cg = form.mytype.generator();
		if (form.mytype == CodeType.HANDLER || form.mytype == CodeType.CONTRACT || form.mytype == CodeType.SERVICE) {
			inClz = form.funcName.inContext.javaClassName();
			needContext = true;
			needTrampolineClass = false;
		} else if (form.mytype == CodeType.HANDLERFUNCTION) {
			inClz = form.funcName.inContext.javaClassName();
			needTrampolineClass = true;
			wantThis = true;
		} else if (form.mytype == CodeType.AREA) {
			cg.begin(cxt);
			inClz = form.funcName.inContext.javaClassName();
			needTrampolineClass = false;
		} else if (form.mytype == CodeType.CARD || form.mytype == CodeType.EVENTHANDLER) {
			inClz = form.funcName.inContext.uniqueName();
			needTrampolineClass = true;
			wantThis = true;
		} else if (form.mytype == CodeType.FUNCTION || form.mytype == CodeType.STANDALONE) {
			inClz = form.funcName.inContext.uniqueName() + ".PACKAGEFUNCTIONS";
			if (!bce.hasClass(inClz)) {
				ByteCodeSink bcc = bce.newClass(inClz);
				bcc.generateAssociatedSourceFile();
				bcc.superclass("java.lang.Object");
			}
			needTrampolineClass = true;
		} else if (form.mytype == CodeType.EVENT) {
			// There may be duplication between what I need here and what I already have,
			// but everything is in such a mess at the moment I can't deal ...
			// Refactor later
			generateEventConnector(form);
			return;
		} else
			throw new UtilException("Can't handle " + form.funcName + " of code type " + form.mytype);
		
		ByteCodeSink bcc;
		MethodDefiner meth;
		List<PendingVar> pendingVars;
		if (cxt.hasMethod()) {
			bcc = cxt.getSink();
			meth = cxt.getMethod();
			pendingVars = cxt.getVars();
		} else {
			bcc = bce.get(inClz);
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, needTrampolineClass && !wantThis, fn);
			gen.returns("java.lang.Object");
			pendingVars = new ArrayList<PendingVar>();
			if (needContext)
				gen.argument(J.OBJECT, "_context");
			int j = 0;
			for (@SuppressWarnings("unused") ScopedVar s : form.scoped)
				pendingVars.add(gen.argument("java.lang.Object", "_s"+(j++)));
			for (int i=0;i<form.nformal;i++)
				pendingVars.add(gen.argument("java.lang.Object", "_"+i));
			meth = gen.done();
	//		meth.lenientMode(true);
		}
		VarHolder vh = new VarHolder(form, pendingVars);
		IExpr blk = new DroidHSIGenerator(new DroidClosureGenerator(form, meth, vh), form, meth, vh).generateHSI(form, null);
		if (blk != null)
			blk.flush();
		
		// for package-level methods (i.e. regular floating functions in a functional language), generate a nested class
		if (needTrampolineClass) {
			ByteCodeSink inner = bce.newClass(inClz + "$" + fn);
			inner.generateAssociatedSourceFile();
			inner.superclass("java.lang.Object");
			if (wantThis) {
				IFieldInfo fi = inner.defineField(true, Access.PRIVATE, bcc.getCreatedName(), "_card");
				GenericAnnotator ctor = GenericAnnotator.newConstructor(inner, false);
				PendingVar arg = ctor.argument(bcc.getCreatedName(), "card");
				MethodDefiner c = ctor.done();
				c.callSuper("void", "java.lang.Object", "<init>").flush();
				c.assign(fi.asExpr(c), arg.getVar()).flush();
				c.returnVoid().flush();
			}
			GenericAnnotator g2 = GenericAnnotator.newMethod(inner, true, "eval");
			g2.returns(J.OBJECT);
			PendingVar forThis = null;
			if (wantThis)
				forThis = g2.argument(J.OBJECT,  "self");
			PendingVar args = g2.argument("[" + J.OBJECT, "args");
			MethodDefiner m2 = g2.done();
			IExpr[] fnArgs = new IExpr[pendingVars.size()];
			for (int i=0;i<pendingVars.size();i++) {
				fnArgs[i] = m2.arrayElt(args.getVar(), m2.intConst(i));
			}
			IExpr doCall;
			if (wantThis)
				doCall = m2.callVirtual(J.OBJECT, m2.castTo(forThis.getVar(), inClz), fn, fnArgs);
			else
				doCall = m2.callStatic(inClz, J.OBJECT, fn, fnArgs);
			
			m2.returnObject(doCall).flush();
		}
	}

	private void generateEventConnector(HSIEForm form) {
		String clzName = form.funcName.javaNameAsNestedClass();
		ByteCodeSink bcc = bce.newClass(clzName);
		bcc.generateAssociatedSourceFile();
		bcc.superclass(J.OBJECT);
		bcc.implementsInterface(J.HANDLER);
		String cardClz = form.funcName.containingCard().javaName();
		bcc.defineField(true, Access.PROTECTED, cardClz, "_card");
		{
			GenericAnnotator ann = GenericAnnotator.newConstructor(bcc, false);
			PendingVar card = ann.argument(J.OBJECT, "card");
			MethodDefiner ctor = ann.done();
			ctor.callSuper("void", J.OBJECT, "<init>").flush();
			ctor.assign(ctor.getField("_card"), ctor.castTo(card.getVar(), cardClz)).flush();
			ctor.returnVoid().flush();
		}
		{
			GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "handle");
			PendingVar evP = ann.argument(new JavaType(J.OBJECT), "ev");
			ann.returns(JavaType.object_);
			NewMethodDefiner meth = ann.done();
			meth.returnObject(meth.makeNew(J.FLCLOSURE, meth.as(meth.getField("_card"), J.OBJECT), meth.callVirtual(J.CLASS, meth.myThis(), "getHandler"), meth.arrayOf(J.OBJECT, Arrays.asList(evP.getVar())))).flush();
		}
		{
			GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "getHandler");
			ann.returns(J.CLASS);
			NewMethodDefiner meth = ann.done();
			List<PendingVar> pendingVars = new ArrayList<PendingVar>();
			VarHolder vh = new VarHolder(form, pendingVars);
			new DroidHSIGenerator(new DroidClosureGenerator(form, meth, vh), form, meth, vh).generateHSI(form, null).flush();
		}
	}
}
