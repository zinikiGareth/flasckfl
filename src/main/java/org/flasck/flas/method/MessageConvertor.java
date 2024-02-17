package org.flasck.flas.method;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ActionMessage;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.parsedForm.CurrentContainer;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.ImplementsContract;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeExpr;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.Traverser;
import org.flasck.flas.tc3.NamedType;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class MessageConvertor extends LeafAdapter implements ResultAware {
	public enum Mode {
		RHS, SLOT, NESTEDSLOT, HAVESLOT, SUBSCRIBERNAME
	}

	private final ErrorReporter errors;
	private final NestedVisitor nv;
	private final ObjectActionHandler oah;
	private final AssignMessage assign;
	private final List<Object> stack = new ArrayList<>();
	private Mode mode = Mode.RHS;
	private Expr slotContainer;

	public MessageConvertor(ErrorReporter errors, NestedVisitor nv, ObjectActionHandler oah, AssignMessage assign) {
		this.errors = errors;
		this.nv = nv;
		this.oah = oah;
		this.assign = assign;
	}

	@Override
	public void leaveSendHandler(Expr handlerExpr) {
		if (stack.size() != 2) {
			throw new CantHappenException("when we have a handler, there should be two items on the stack");
		}
		Object sr = stack.get(0);
		MakeSend ms;
		if (sr instanceof MakeSend)
			ms = (MakeSend) sr;
		else if (sr instanceof ApplyExpr)
			ms = (MakeSend) ((ApplyExpr)sr).fn;
		else
			throw new NotImplementedException();
		Expr h = (Expr) stack.remove(1);
		ms.handler = h;
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		if (expr instanceof ApplyExpr)
			;
		else if (expr instanceof MemberExpr)
			;
		else if (mode == Mode.RHS) {
			if (expr instanceof CastExpr || expr instanceof TypeExpr)
				new SpecialConvertor(errors, nv, oah, assign);
			else
				stack.add(expr);
		} else if (mode == Mode.SLOT && assign.assignsToCons()) {
			slotContainer = expr;
			mode = Mode.HAVESLOT;
		}
	}

	@Override
	public void visitApplyExpr(ApplyExpr expr) {
		nv.push(new MessageConvertor(errors, nv, oah, null));
	}
	
	@Override
	public boolean visitMemberExpr(MemberExpr expr, int nargs) {
		if (expr.boundEarly()) {
			UnresolvedVar uv = new UnresolvedVar(expr.location, expr.asName());
			uv.bind(expr.defn());
			stack.add(uv);
			return true;
		}
		if (mode == Mode.SLOT) {
			mode = Mode.NESTEDSLOT;
			if (!(expr.from instanceof MemberExpr))
				slotContainer = expr.from;
		} else if (mode == Mode.NESTEDSLOT || mode == Mode.RHS) { 
			new MemberExprConvertor(errors, nv, oah, (MemberExpr) expr);
		} else
			throw new CantHappenException("shouldn't see ME in mode " + mode);
		return false;
	}
	
	// Never forget that we assign the slot AT THE END
	@Override
	public void visitAssignSlot(Expr slot) {
		mode = Mode.SLOT;
	}
	
	@Override
	public void result(Object r) {
		if (mode == Mode.RHS || mode == Mode.SUBSCRIBERNAME)
			stack.add((Expr) r);
		else {
			if (slotContainer != null)
				throw new NotImplementedException();
			slotContainer = (Expr) r;
		}
	}
	
	@Override
	public void leaveAssignMessage(AssignMessage msg) {
		if (stack.size() != 1)
			throw new NotImplementedException("stack size should be 1 but was " + stack.size());
		Expr expr = (Expr) stack.remove(0);
		UnresolvedVar inner;
		List<Object> args = new ArrayList<>();
		if (slotContainer == null) {
			inner = (UnresolvedVar) msg.slot;
			NamedType nt = (NamedType) this.oah.state();
			if (nt instanceof ImplementsContract) {
				nt = ((ImplementsContract)nt).getParent();
			} else if (nt instanceof HandlerImplements) {
				nt = ((HandlerImplements)nt).state();
			}
			args.add(new CurrentContainer(msg.kw, nt));
		} else if (slotContainer instanceof UnresolvedVar && msg.assignsToCons()) {
			inner = null;
			args.add(slotContainer);
		} else {
			inner = (UnresolvedVar) ((MemberExpr)msg.slot).fld;
			args.add(slotContainer);
		}
		
		UnresolvedVar op;
		if (msg.assignsToCons()) {
			op = new UnresolvedVar(msg.kw, "AssignCons");
			op.bind(LoadBuiltins.assignCons);
		} else {
			op = new UnresolvedVar(msg.kw, "Assign");
			op.bind(LoadBuiltins.assign);
			args.add(new StringLiteral(inner.location, inner.var));
		}
		args.add(expr);
		stack.add(new ApplyExpr(msg.kw, op, args));
	}
	
	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		Object op = stack.remove(0);
		nv.result(new ApplyExpr(expr.location(), op, stack));
	}
	
	@Override
	public void leaveCastExpr(CastExpr expr) {
		nv.result(new CastExpr(expr.location(), expr.tyLoc, expr.valLoc, expr.type, (Expr) stack.remove(0)));
	}

	@Override
	public void leaveMemberExpr(MemberExpr expr, boolean done) {
		if (expr.boundEarly())
			return;
		if (mode == Mode.NESTEDSLOT) {
//			if (stack.size() != 1)
//				throw new CantHappenException("stack should have one element");
//			nv.result(stack.remove(0));
			mode = Mode.HAVESLOT;
		} else
			throw new CantHappenException("shouldn't see leaveMemberExpr in mode " + mode);
	}
	
	@Override
	public void leaveStructField(StructField sf) {
		if (stack.isEmpty())
			return;
		if (stack.size() != 1)
			throw new NotImplementedException("when sending messages, should only have 1 arg");
		new Traverser(new CheckNoMessages(errors)).visitExpr(sf.init, 0);
		nv.result(null);
	}
	
	@Override
	public void visitSubscriberName(Expr expr) {
		mode = Mode.SUBSCRIBERNAME;
		nv.push(new MessageConvertor(errors, nv, oah, null));
	}
	
	@Override
	public void leaveSubscriberName(Expr handlerName) {
		nv.result(stack.remove(0));
	}

	@Override
	public void leaveSendMessage(SendMessage msg) {
		if (msg.subscriberName() != null) {
			// the handler name expr has been added to the end of results, and needs to be moved onto the MakeSend just before that
			Expr hn = (Expr) stack.remove(1);
			Expr ms = (Expr) stack.get(0);
			if (ms instanceof ApplyExpr) { // it is a MakeSend with a handler
				ms = (MakeSend)((ApplyExpr)ms).fn;
			}
			((MakeSend)ms).handlerName = hn;
		}
	}
	
	@Override
	public void leaveMessage(ActionMessage msg) {
		if (stack.size() != 1)
			throw new NotImplementedException("when sending messages, should only have 1 arg");
		nv.result(stack.remove(0));
	}
	
	@Override
	public void leaveUnitTestInvoke(UnitTestInvoke uti) {
		if (stack.size() != 1)
			throw new NotImplementedException("should be 1");
		uti.conversion((Expr) stack.remove(0));
		nv.result(null);
	}
}
