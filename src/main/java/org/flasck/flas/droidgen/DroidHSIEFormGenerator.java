package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.generators.CodeGenerator;
import org.flasck.flas.generators.GenerationContext;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;

public class DroidHSIEFormGenerator {
	private final ByteCodeStorage bce;

	public DroidHSIEFormGenerator(ByteCodeStorage bce) {
		this.bce = bce;
	}

	public void generate(HSIEForm form) {
		GenerationContext cxt = new MethodGenerationContext(bce, form);
		CodeGenerator cg = form.mytype.generator();
		cg.begin(cxt);
		
		// come back to me ...
		if (form.mytype == CodeType.EVENT) {
			// There may be duplication between what I need here and what I already have,
			// but everything is in such a mess at the moment I can't deal ...
			// Refactor later
			generateEventConnector(cg, cxt, form);
			return;
		}
		
		IExpr blk = new DroidHSIGenerator(new DroidClosureGenerator(form, cxt.getMethod(), cxt.getVarHolder()), form, cxt.getMethod(), cxt.getVarHolder()).generateHSI(form, null);
		if (blk != null)
			blk.flush();
	}

	private void generateEventConnector(CodeGenerator cg, GenerationContext cxt, HSIEForm form) {
		ByteCodeSink bcc = cxt.getSink();
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
