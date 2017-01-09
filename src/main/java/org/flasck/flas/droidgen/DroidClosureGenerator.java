package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
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
	private final VarHolder vh;
	enum ObjectNeeded { NONE, THIS, CARD };
	private final ObjectNeeded myOn;
	
	public DroidClosureGenerator(HSIEForm form, NewMethodDefiner meth, VarHolder vh) {
		this.form = form;
		this.meth = meth;
		this.vh = vh;
		if (form.needsCardMember())
			myOn = ObjectNeeded.CARD;
		else if (form.isCardMethod())
			myOn = ObjectNeeded.THIS;
		else
			myOn = ObjectNeeded.NONE;
	}

	public IExpr closure(HSIEBlock closure) {
		PushReturn c0 = (PushReturn) closure.nestedCommands().get(0);

		
		if (c0 instanceof PushExternal) {
			ExternalRef fn = ((PushExternal)c0).fn;
			if (fn.uniqueName().equals("FLEval.field"))
				return handleField(closure);
			else if (fn.uniqueName().equals("FLEval.curry"))
				return handleCurry(closure);
			ObjectNeeded on = ObjectNeeded.NONE;
			Object defn = fn;
			while (defn instanceof PackageVar)
				defn = ((PackageVar)defn).defn;
			if (defn == null)
				; // This appears to be mainly builtin things - eg. Tuple // throw new UtilException("Didn't find a definition for " + fn);
			else if (defn instanceof ObjectReference || defn instanceof CardFunction) {
				on = myOn;
			} else if (defn instanceof RWHandlerImplements) {
				RWHandlerImplements hi = (RWHandlerImplements) defn;
				if (hi.inCard)
					on = myOn;
				System.out.println("Creating handler " + fn + " in block " + closure);
			} else if (defn instanceof RWFunctionDefinition) {
				; // a regular function
			} else if (defn instanceof RWStructDefn || defn instanceof RWObjectDefn) {
				; // creating a struct or object is just like calling a static function
			} else if (defn instanceof HandlerLambda) {
				; // I think these are var cases really
			} else if (defn instanceof ScopedVar) {
				; // I think these are var cases really
			} else
				throw new UtilException("Didn't do anything with " + defn + " " + (defn != null ? defn.getClass() : ""));
			return makeClosure(closure, on, appendValue(c0, true));
		} else if (c0 instanceof PushVar) {
			return makeClosure(closure, ObjectNeeded.NONE, vh.get(((PushVar)c0).var.var));
		} else
			throw new UtilException("Can't handle " + c0);
	}

	protected IExpr makeClosure(HSIEBlock closure, ObjectNeeded on, Expr fnToCall) {
		switch (on) {
		case NONE:
			return meth.makeNew(J.FLCLOSURE, fnToCall, arguments(closure, 1));
		case THIS:
			return meth.makeNew(J.FLCLOSURE, meth.as(meth.myThis(), J.OBJECT), fnToCall, arguments(closure, 1));
		case CARD:
			return meth.makeNew(J.FLCLOSURE, meth.as(meth.getField("_card"), J.OBJECT), fnToCall, arguments(closure, 1));
		default:
			throw new UtilException("What is " + on);
		}
	}

	private IExpr handleField(HSIEBlock closure) {
		List<Expr> al = new ArrayList<>();
		al.add(meth.box(appendValue((PushReturn) closure.nestedCommands().get(1), false)));
		al.add(meth.box(appendValue((PushReturn) closure.nestedCommands().get(2), false)));
		return meth.makeNew(J.FLCLOSURE, meth.classConst(J.FLFIELD), meth.arrayOf(J.OBJECT, al));
	}

	private IExpr handleCurry(HSIEBlock closure) {
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
