package org.flasck.flas.droidgen;

import java.util.ArrayList;

import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushBool;
import org.flasck.flas.vcode.hsieForm.PushBuiltin;
import org.flasck.flas.vcode.hsieForm.PushCSR;
import org.flasck.flas.vcode.hsieForm.PushDouble;
import org.flasck.flas.vcode.hsieForm.PushFunc;
import org.flasck.flas.vcode.hsieForm.PushInt;
import org.flasck.flas.vcode.hsieForm.PushString;
import org.flasck.flas.vcode.hsieForm.PushTLV;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.flasck.flas.vcode.hsieForm.PushVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.StringUtil;

public final class DroidPushArgument implements PushVisitor<IExpr> {
	private final HSIEForm form;
	private final NewMethodDefiner meth;
	private final Var cx;
	private final VarHolder vh;

	public DroidPushArgument(HSIEForm form, NewMethodDefiner meth, Var cx, VarHolder vh) {
		this.form = form;
		this.meth = meth;
		this.cx = cx;
		this.vh = vh;
	}

	@Override
	public void visitExternal(CardMember cm, OutputHandler<IExpr> handler) {
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
			handler.result(meth.callVirtual(J.OBJECT, card, "getVar", cx, meth.stringConst(cm.var)));
	}

	@Override
	public void visitExternal(CardFunction cf, OutputHandler<IExpr> handler) {
		handler.result(meth.classConst(cf.myName().javaClassName()));
	}

	@Override
	public void visitExternal(HandlerLambda hl, OutputHandler<IExpr> handler) {
		if (form.mytype == CodeType.HANDLER)
			handler.result(meth.getField(hl.var));
		else if (form.mytype == CodeType.HANDLERFUNCTION)
			handler.result(meth.getField(hl.var));
		else
			throw new UtilException("Can't handle handler lambda with " + form.mytype);
	}

	@Override
	public void visitExternal(ScopedVar sv, OutputHandler<IExpr> handler) {
		if (sv.definedBy.equals(form.funcName)) {
			// TODO: I'm not quite sure what should happen here, or even what this case represents, but I know it should be something to do with the *actual* function definition
			handler.result(meth.stringConst(sv.uniqueName()));
		} else
			handler.result(vh.getScoped(sv.uniqueName()));
	}

	@Override
	public void visitExternal(ObjectReference or, OutputHandler<IExpr> handler) {
		handleNamedThing(or, handler);
	}

	@Override
	public void visitExternal(PackageVar pv, OutputHandler<IExpr> handler) {
		handleNamedThing(pv, handler);
	}

	@Override
	public void visit(PushBuiltin pb, OutputHandler<IExpr> handler) {
		handler.result(meth.classConst(J.FLEVAL+"$" + StringUtil.capitalize(pb.bval.opName)));
	}

	private void handleNamedThing(ExternalRef name, OutputHandler<IExpr> handler) {
		boolean needToCallEvalMethod = false;
		Object defn = null;
		if (name instanceof PackageVar) {
			defn = name;
			while (defn instanceof PackageVar)
				defn = ((PackageVar)defn).defn;
			if (defn instanceof RWStructDefn && ((RWStructDefn)defn).fields.isEmpty())
				needToCallEvalMethod = true;
		}
		String clz = name.myName().javaClassName();
		if (!needToCallEvalMethod) { // handle the simple class case ...
			handler.result(meth.classConst(clz));
		} else {
			handler.result(meth.callStatic(clz, J.OBJECT, "eval", meth.arrayOf(J.OBJECT, new ArrayList<>())));
		}
	}

	@Override
	public void visit(PushVar pv, OutputHandler<IExpr> handler) {
		handler.result(vh.get(pv.var.var));
	}

	@Override
	public void visit(PushInt pi, OutputHandler<IExpr> handler) {
		handler.result(meth.callStatic("java.lang.Integer", "java.lang.Integer", "valueOf", meth.intConst(pi.ival)));
	}

	@Override
	public void visit(PushDouble pd, OutputHandler<IExpr> handler) {
		handler.result(meth.callStatic("java.lang.Double", "java.lang.Double", "valueOf", meth.doubleConst(pd.dval)));
	}

	@Override
	public void visit(PushString ps, OutputHandler<IExpr> handler) {
		handler.result(meth.stringConst(ps.sval.text));
	}

	@Override
	public void visit(PushBool pb, OutputHandler<IExpr> handler) {
		handler.result(meth.boolConst(pb.bval.value()));
	}

	@Override
	public void visit(PushTLV pt, OutputHandler<IExpr> handler) {
		handler.result(meth.getField(meth.getField("_src_" + pt.tlv.simpleName), pt.tlv.simpleName));
	}

	@Override
	public void visit(PushCSR pc, OutputHandler<IExpr> handler) {
		if (pc.csr.fromHandler)
			handler.result(meth.getField("_card"));
		else
			handler.result(meth.myThis());
	}

	@Override
	public void visit(PushFunc pf, OutputHandler<IExpr> handler) {
		// this is clearly wrong, but we need to return a "function" object and I don't have one of those right now, I don't think 
		handler.result(meth.myThis());
	}
}