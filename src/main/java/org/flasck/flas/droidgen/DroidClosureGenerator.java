package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushInt;
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
		} else if (c0 instanceof PushExternal && ((PushExternal)c0).fn.uniqueName().equals("FLEval.curry")) {
			return handleCurry(closure);
		} else {

			Expr needsObject = null;
			Expr fnToCall;
			if (c0 instanceof PushExternal) {
				ExternalRef fn = ((PushExternal)c0).fn;
				boolean fromHandler = form.needsCardMember();
				Object defn = fn;
				if (fn != null) {
					while (defn instanceof PackageVar)
						defn = ((PackageVar)defn).defn;
					if (defn == null)
						; // This appears to be mainly builtin things - eg. Tuple // throw new UtilException("Didn't find a definition for " + fn);
					else if (defn instanceof ObjectReference || defn instanceof CardFunction) {
						needsObject = meth.myThis();
						fromHandler |= fn.fromHandler();
					} else if (defn instanceof RWHandlerImplements) {
						RWHandlerImplements hi = (RWHandlerImplements) defn;
						if (hi.inCard)
							needsObject = meth.myThis();
						System.out.println("Creating handler " + fn + " in block " + closure);
					} else if (defn instanceof RWFunctionDefinition) {
						;
					} else if (defn instanceof RWStructDefn || defn instanceof RWObjectDefn) {
						;
					} else if (defn instanceof HandlerLambda) {
						;
					} else if (defn instanceof ScopedVar) {
						;
					} else
						throw new UtilException("Didn't do anything with " + defn + " " + (defn != null ? defn.getClass() : ""));
				}
				if (needsObject != null && fromHandler)
					needsObject = meth.getField("_card");
				fnToCall = appendValue(c0, true);
			} else if (c0 instanceof PushVar) {
				fnToCall = vh.get(((PushVar)c0).var.var);
			} else
				throw new UtilException("Can't handle " + c0);

			if (needsObject != null)
				return meth.makeNew(J.FLCLOSURE, meth.as(needsObject, J.OBJECT), fnToCall, arguments(closure, 1));
			else
				return meth.makeNew(J.FLCLOSURE, fnToCall, arguments(closure, 1));
		}
	}

	private IExpr handleField(HSIEBlock closure) {
		List<Expr> al = new ArrayList<>();
		al.add(meth.box(appendValue((PushReturn) closure.nestedCommands().get(1), false)));
		al.add(meth.box(appendValue((PushReturn) closure.nestedCommands().get(2), false)));
		return meth.makeNew(J.FLCLOSURE, meth.classConst(J.FLFIELD), meth.arrayOf(J.OBJECT, al));
	}

	private IExpr handleCurry(HSIEBlock closure) {
		List<Expr> al = new ArrayList<>();
		for (int i=3;i<closure.nestedCommands().size();i++)
			al.add(meth.box(appendValue((PushReturn) closure.nestedCommands().get(i), false)));
		Expr args = meth.arrayOf(J.OBJECT, al);
		PushExternal curriedFn = (PushExternal)closure.nestedCommands().get(1);
		PushInt cnt = (PushInt) closure.nestedCommands().get(2);
		ExternalRef f2 = curriedFn.fn;
		if (f2 instanceof ObjectReference || f2 instanceof CardFunction) {
			Expr needsObject = null;
			if (form.needsCardMember())
				needsObject = meth.getField("_card");
			else
				needsObject = meth.myThis();
			return meth.makeNew(J.FLCURRY, meth.as(needsObject, J.OBJECT), appendValue(curriedFn, true), meth.intConst(cnt.ival), arguments(closure, 3));
		} else
			return meth.makeNew(J.FLCURRY, appendValue(curriedFn, true), meth.intConst(cnt.ival), arguments(closure, 3));
	}

	protected Expr arguments(HSIEBlock closure, int from) {
		// Process all the arguments
		List<Expr> al = new ArrayList<Expr>();
		for (int i=from;i<closure.nestedCommands().size();i++) {
			PushReturn c = (PushReturn) closure.nestedCommands().get(i);
			al.add(meth.box(appendValue(c, false)));
		}
		return meth.arrayOf(J.OBJECT, al);
	}

	Expr appendValue(PushReturn c, boolean isFirst) {
		return (Expr) c.visit(new DroidAppendPush(form, meth, vh, isFirst));
	}
}
