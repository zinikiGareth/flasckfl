package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.exceptions.UtilException;

public class DroidClosureGenerator {
	private final HSIEForm form;
	private final NewMethodDefiner meth;
	private VarHolder vh;

	public DroidClosureGenerator(HSIEForm form, NewMethodDefiner meth, VarHolder vh) {
		this.form = form;
		this.meth = meth;
		this.vh = vh;
	}

	public IExpr closure(HSIEBlock closure) {
		PushReturn c0 = (PushReturn) closure.nestedCommands().get(0);
		if (c0 instanceof PushExternal && ((PushExternal)c0).fn.uniqueName().equals("FLEval.field")) {
			return handleField(closure);
//		} else if (c0 instanceof PushExternal && ((PushExternal)c0).fn.uniqueName().equals("FLEval.curry")) {
//			return handleCurry(closure);
		} else {
			// Process all the arguments
			List<Expr> al = new ArrayList<Expr>();
			for (int i=1;i<closure.nestedCommands().size();i++) {
				PushReturn c = (PushReturn) closure.nestedCommands().get(i);
				al.add(meth.box(upcast(appendValue(c, false))));
			}

			Expr needsObject = null;
			Expr fnToCall;
			if (c0 instanceof PushExternal) {
				ExternalRef fn = ((PushExternal)c0).fn;
				boolean fromHandler = form.needsCardMember();
				Object defn = fn;
				if (fn != null) {
					while (defn instanceof PackageVar)
						defn = ((PackageVar)defn).defn;
					if (defn instanceof ObjectReference || defn instanceof CardFunction) {
						needsObject = meth.myThis();
						fromHandler |= fn.fromHandler();
					} else if (defn instanceof RWHandlerImplements) {
						RWHandlerImplements hi = (RWHandlerImplements) defn;
						if (hi.inCard)
							needsObject = meth.myThis();
						System.out.println("Creating handler " + fn + " in block " + closure);
					} else if (fn.toString().equals("FLEval.curry")) {
						ExternalRef f2 = ((PushExternal)closure.nestedCommands().get(1)).fn;
						if (f2 instanceof ObjectReference || f2 instanceof CardFunction) {
							needsObject = meth.myThis();
							fromHandler |= f2.fromHandler();
						}
					}
				}
				if (needsObject != null && fromHandler)
					needsObject = meth.getField("_card");
				fnToCall = appendValue(c0, true);
			} else if (c0 instanceof PushVar) {
				fnToCall = vh.get(((PushVar)c0).var.var);
			} else
				throw new UtilException("Can't handle " + c0);
			if (needsObject != null)
				return meth.makeNew(J.FLCLOSURE, meth.as(needsObject, J.OBJECT), fnToCall, meth.arrayOf(J.OBJECT, al));
			else
				return meth.makeNew(J.FLCLOSURE, fnToCall, meth.arrayOf(J.OBJECT, al));
		}
	}

	private IExpr handleField(HSIEBlock closure) {
		List<Expr> al = new ArrayList<>();
		al.add(meth.box(appendValue((PushReturn) closure.nestedCommands().get(1), false)));
		al.add(meth.box(appendValue((PushReturn) closure.nestedCommands().get(2), false)));
		return meth.makeNew(J.FLCLOSURE, meth.classConst(J.FLFIELD), meth.arrayOf(J.OBJECT, al));
	}

	private IExpr handleCurry(HSIEBlock closure) {
//		List<Expr> al = new ArrayList<>();
//		al.add(meth.box(appendValue((PushReturn) closure.nestedCommands().get(1), false)));
//		al.add(meth.box(appendValue((PushReturn) closure.nestedCommands().get(2), false)));
//		return meth.makeNew(J.FLCLOSURE, meth.classConst(J.FLFIELD), meth.arrayOf(J.OBJECT, al));
		throw new UtilException("Handle curry");
	}

	Expr appendValue(PushReturn c, boolean isFirst) {
		return (Expr) c.visit(new DroidAppendPush(form, meth, vh, isFirst));
	}

	// TODO: I think this wants to go away and we just want to autobox stuff
	Expr upcast(Expr expr) {
//		return meth.box(expr);
		if (expr.getType().equals("int"))
			return meth.callStatic("java.lang.Integer", "java.lang.Integer", "valueOf", expr);
		return expr;
	}

}
