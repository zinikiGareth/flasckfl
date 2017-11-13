package org.flasck.flas.droidgen;

import org.flasck.flas.generators.CardMemberGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.exceptions.UtilException;

public class DroidCardMemberGenerator implements CardMemberGenerator<IExpr> {
	private final IMethodGenerationContext cxt;

	public DroidCardMemberGenerator(IMethodGenerationContext cxt) {
		this.cxt = cxt;
	}

	@Override
	public void generate(CardMember cm, HSIEForm form, ObjectNeeded myOn, OutputHandler<IExpr> handler, ClosureGenerator closure) {
		MethodDefiner meth = cxt.getMethod();
		IExpr card;
		if (form.isCardMethod())
			card = meth.myThis();
		else if (form.needsCardMember()) {
			card = meth.getField("_card");
		} else
			throw new UtilException("Can't handle card member with " + form.mytype);
		IExpr fld;
		if (cm.type instanceof RWContractImplements)
			fld = meth.getField(card, cm.var);
		else
			fld = meth.callVirtual(J.OBJECT, card, "getVar", cxt.getCxtArg(), meth.stringConst(cm.var));
		cxt.doEval(myOn, fld, closure, handler);
	}

	@Override
	public void push(CardMember cm, HSIEForm form, OutputHandler<IExpr> handler) {
		MethodDefiner meth = cxt.getMethod();
		IExpr card;
		if (form.isCardMethod())
			card = meth.myThis();
		else if (form.needsCardMember())
			card = meth.getField("_card");
		else
			throw new UtilException("Can't handle card member with " + form.mytype);
		if (cm.type instanceof RWContractImplements)
			handler.result(meth.getField(card, cm.var));
		else
			handler.result(meth.callVirtual(J.OBJECT, card, "getVar", cxt.getCxtArg(), meth.stringConst(cm.var)));
	}
}
