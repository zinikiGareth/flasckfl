package org.flasck.flas.jsform;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
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
import org.zinutils.exceptions.UtilException;

public final class JSPushArgument implements PushVisitor<String> {
	private final HSIEForm form;
	private final StringBuilder sb;

	public JSPushArgument(HSIEForm form, StringBuilder sb) {
		this.form = form;
		this.sb = sb;
	}

	@Override
	public void visit(PushVar pv, OutputHandler<String> handler) {
		sb.append("v"+ pv.var.var.idx);
	}

	@Override
	public void visit(PushInt pi, OutputHandler<String> handler) {
		sb.append(pi.ival);
	}

	@Override
	public void visit(PushDouble pd, OutputHandler<String> handler) {
		sb.append(pd.dval);
	}

	@Override
	public void visit(PushString ps, OutputHandler<String> handler) {
		sb.append("'" + ps.sval.text + "'");
	}

	@Override
	public void visit(PushBool ps, OutputHandler<String> handler) {
		sb.append(ps.bval.value());
	}

	@Override
	public void visitExternal(CardMember cm, OutputHandler<String> handler) {
		if (form.mytype == CodeType.CARD || form.mytype == CodeType.EVENTHANDLER || form.mytype == CodeType.OBJECT)
			sb.append("this." + cm.var);
		else if (form.mytype == CodeType.HANDLER || form.mytype == CodeType.CONTRACT || form.mytype == CodeType.AREA)
			sb.append("this._card." + cm.var);
		else
			throw new UtilException("Can't handle " + form.mytype + " for card member");
	}

	@Override
	public void visitExternal(CardFunction cf, OutputHandler<String> handler) {
		String jsname = cf.uniqueName();
		int idx = jsname.lastIndexOf(".");
		jsname = jsname.substring(0, idx+1) + "prototype" + jsname.substring(idx);
		sb.append(jsname);
	}
	
	@Override
	public void visitExternal(HandlerLambda hl, OutputHandler<String> handler) {
		if (form.mytype == CodeType.HANDLER)
			sb.append("this." + hl.var);
		else if (form.mytype == CodeType.HANDLERFUNCTION)
			// I'm guessing at this right now while working on JVM
			sb.append("this." + hl.var);
		else
			throw new UtilException("Can't handle " + form.mytype + " with handler lambda");
	}
	
	@Override
	public void visitExternal(ScopedVar sv, OutputHandler<String> handler) {
		int j = 0;
		if (sv.definedBy.equals(form.funcName)) {
			return;
		}
		for (ScopedVar s : form.scoped)
			if (s.uniqueName().equals(sv.uniqueName())) {
				sb.append("s" + j);
				return;
			} else
				j++;
		throw new UtilException("ScopedVar not in scope: " + sv + " in " + form.funcName.uniqueName() + ": we have " + form.scoped);
	}

	@Override
	public void visitExternal(ObjectReference or, OutputHandler<String> handler) {
		sb.append(JSForm.rename(or.uniqueName()));
	}

	@Override
	public void visitExternal(PackageVar pv, OutputHandler<String> handler) {
		sb.append(JSForm.rename(pv.uniqueName()));
	}

	@Override
	public void visit(PushBuiltin pb, OutputHandler<String> handler) {
		if (pb.isField())
			sb.append("FLEval.field");
		else if (pb.isTuple())
			sb.append("FLEval.tuple");
		else if (pb.isOctor())
			sb.append("FLEval.octor");
		else
			throw new RuntimeException("Not handled");
	}

	@Override
	public void visit(PushTLV pt, OutputHandler<String> handler) {
		sb.append("this._src_" + pt.tlv.simpleName + "." + pt.tlv.simpleName);
	}

	@Override
	public void visit(PushCSR pc, OutputHandler<String> handler) {
		if (pc.csr.fromHandler)
			sb.append("this._card");
		else
			sb.append("this");
	}

	@Override
	public void visit(PushFunc pf, OutputHandler<String> handler) {
		FunctionName name = pf.func.name;
		sb.append(name.inContext.jsName() + ".prototype." + name.name);
	}
}