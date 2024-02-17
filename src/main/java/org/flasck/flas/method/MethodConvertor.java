package org.flasck.flas.method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ActionMessage;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.ut.GuardedMessages;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class MethodConvertor extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final NestedVisitor sv;
	private final List<Expr> results = new ArrayList<>();
	private ObjectActionHandler oah;
	private final FunctionIntro fi;
	private boolean msgHasGuard = false;
	private boolean haveGuards = false;

	public MethodConvertor(ErrorReporter errors, NestedVisitor sv, ObjectActionHandler oah) {
		fi = new FunctionIntro(oah.name(), new ArrayList<>());
		this.errors = errors;
		this.sv = sv;
		this.oah = oah;
	}
	
	@Override
	public void visitGuardedMessage(GuardedMessages gm) {
		haveGuards = true;
		msgHasGuard = false;
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		if (!haveGuards)
			return;
		
		msgHasGuard = true;
		if (expr instanceof ApplyExpr)
			;
		else if (expr instanceof MemberExpr)
			;
		else
			results.add(expr);
	}


	@Override
	public void visitApplyExpr(ApplyExpr expr) {
		if (haveGuards)
			sv.push(new MessageConvertor(errors, sv, oah, null));
	}
	
	@Override
	public boolean visitMemberExpr(MemberExpr expr, int nargs) {
		if (haveGuards)
			new MemberExprConvertor(errors, sv, oah, (MemberExpr) expr);
		return false;
	}

	@Override
	public void visitMessage(ActionMessage msg) {
		sv.push(new MessageConvertor(errors, sv, oah, (msg instanceof AssignMessage)?(AssignMessage)msg:null));
	}
	
	@Override
	public void result(Object r) {
		results.add((Expr)r);
	}

	@Override
	public void leaveGuardedMessage(GuardedMessages gm) {
		Expr guard = null;
		if (msgHasGuard)
			guard = results.remove(0);
		fi.functionCase(new FunctionCaseDefn(oah.location(), fi, guard, new Messages(oah.location(), new ArrayList<>(results))));
		results.clear();
	}

	@Override
	public void leaveObjectMethod(ObjectMethod meth) {
		if (!haveGuards)
			fi.functionCase(new FunctionCaseDefn(oah.location(), fi, null, new Messages(meth.location(), results)));
		meth.conversion(Arrays.asList(fi));
		sv.result(null);
	}

	@Override
	public void leaveObjectCtor(ObjectCtor meth) {
		if (!haveGuards)
			fi.functionCase(new FunctionCaseDefn(oah.location(), fi, null, new Messages(meth.location(), results)));
		meth.conversion(Arrays.asList(fi));
		sv.result(null);
	}
}
