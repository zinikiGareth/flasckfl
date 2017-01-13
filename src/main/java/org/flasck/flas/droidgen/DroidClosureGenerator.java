package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.flim.BuiltinOperation;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.vcode.hsieForm.ClosureCmd;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushInt;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.PushString;
import org.flasck.flas.vcode.hsieForm.PushTLV;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.flasck.jvm.J;
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
	private final DroidPushArgument dpa;
	
	public DroidClosureGenerator(HSIEForm form, NewMethodDefiner meth, VarHolder vh) {
		this.form = form;
		this.meth = meth;
		this.vh = vh;
		dpa = new DroidPushArgument(form, meth, vh);
		if (form.needsCardMember())
			myOn = ObjectNeeded.CARD;
		else if (form.isCardMethod())
			myOn = ObjectNeeded.THIS;
		else
			myOn = ObjectNeeded.NONE;
	}

	public IExpr closure(ClosureCmd closure) {
		return pushReturn((PushReturn) closure.nestedCommands().get(0), closure);
	}

	public IExpr pushReturn(PushReturn pr, ClosureCmd closure) {
		if (pr instanceof PushExternal) {
			ExternalRef fn = ((PushExternal)pr).fn;
			Object defn = fn;
			while (defn instanceof PackageVar)
				defn = ((PackageVar)defn).defn;
			if (fn.uniqueName().equals("FLEval.field"))
				return handleField(closure);
			else if (fn.uniqueName().equals("FLEval.curry"))
				return handleCurry(defn, closure);
			String clz = DroidUtils.getJavaClassForDefn(meth, fn, defn);
			if (defn instanceof BuiltinOperation) {
				// This covers both Field & Tuple, but Field was handled above
				return doEval(ObjectNeeded.NONE, meth.classConst(clz), closure);
			} else if (defn instanceof PrimitiveType) {
				// This is for "typeof Number" or "typeof String" and returns the corresponding class object
				// See typeop.fl for an example
				return doEval(ObjectNeeded.NONE, meth.classConst(clz), closure);
			} else if (defn instanceof CardGrouping) {
				// This is for "typeof <cardname>" and returns the "class" corresponding to the type
				// See typeop.fl for an example
				// TODO: figure out if this should really be "ObjectReference" and if that should be renamed
				return doEval(ObjectNeeded.NONE, meth.classConst(clz), closure);
			} else if (defn instanceof ObjectReference) {
				// This case covers at least handling the construction of Object Handlers to pass to service methods
				return doEval(myOn, meth.classConst(clz), closure);
			} else if (defn instanceof CardFunction) {
				// This case covers at least event handlers
				return doEval(myOn, meth.classConst(clz), closure);
			} else if (defn instanceof CardMember) {
				CardMember cm = (CardMember)defn;
				IExpr card;
				if (form.isCardMethod())
					card = meth.myThis();
				else if (form.needsCardMember())
					card = meth.getField("_card");
				else
					throw new UtilException("Can't handle card member with " + form.mytype);
				return doEval(myOn, meth.getField(card, cm.var), closure);
			} else if (defn instanceof RWFunctionDefinition) {
				RWFunctionDefinition rwfn = (RWFunctionDefinition) defn;
				// a regular function
				if (rwfn.nargs == 0) { // invoke it as a function using eval
					return doEval(ObjectNeeded.NONE, meth.callStatic(clz, J.OBJECT, "eval", meth.arrayOf(J.OBJECT, new ArrayList<>())), closure);
				} else {
					return doEval(ObjectNeeded.NONE, meth.classConst(clz), closure);
				}
			} else if (defn instanceof RWStructDefn) {
				// creating a struct is just like calling a static function
				RWStructDefn sd = (RWStructDefn) defn;
				if (sd.fields.size() == 0)
					return doEval(ObjectNeeded.NONE, meth.callStatic(clz, J.OBJECT, "eval", meth.arrayOf(J.OBJECT, new ArrayList<>())), closure);
				else
					return doEval(ObjectNeeded.NONE, meth.classConst(clz), closure);
			} else if (defn instanceof RWObjectDefn) {
				// creating an object is just like calling a static function
				RWObjectDefn od = (RWObjectDefn) defn;
				if (od.ctorArgs.isEmpty())
					return doEval(ObjectNeeded.NONE, meth.callStatic(clz, J.OBJECT, "eval", meth.arrayOf(J.OBJECT, new ArrayList<>())), closure);
				else
					return doEval(ObjectNeeded.NONE, meth.classConst(clz), closure);
			} else if (defn instanceof HandlerLambda) {
				HandlerLambda hl = (HandlerLambda) defn;
				IExpr var = meth.getField(hl.var);
				return doEval(ObjectNeeded.NONE, var, closure);
			} else if (defn instanceof ScopedVar) {
				ScopedVar sv = (ScopedVar) defn;
				if (closure != null && closure.justScoping)
					return doEval(ObjectNeeded.NONE, meth.classConst(clz), closure);
				else
					return doEval(ObjectNeeded.NONE, vh.getScoped(sv.uniqueName()), closure);
			} else
				throw new UtilException("Didn't do anything with " + defn + " " + (defn != null ? defn.getClass() : ""));
		} else if (pr instanceof PushVar) {
			return doEval(ObjectNeeded.NONE, vh.get(((PushVar)pr).var.var), closure);
		} else if (pr instanceof PushInt) {
			return doEval(ObjectNeeded.NONE, meth.callStatic("java.lang.Integer", "java.lang.Integer", "valueOf", meth.intConst(((PushInt)pr).ival)), closure);
		} else if (pr instanceof PushString) {
			return doEval(ObjectNeeded.NONE, meth.stringConst(((PushString)pr).sval.text), closure);
		} else if (pr instanceof PushTLV) {
			PushTLV pt = (PushTLV) pr;
			return doEval(ObjectNeeded.NONE, meth.getField(meth.getField("_src_" + pt.tlv.simpleName), pt.tlv.simpleName), closure);
		} else
			throw new UtilException("Can't handle " + pr);
	}

	protected IExpr doEval(ObjectNeeded on, IExpr fnToCall, HSIEBlock closure) {
		if (closure == null)
			return meth.returnObject(fnToCall);
		else
			return makeClosure(on, fnToCall, arguments(closure, 1));
	}
	
	protected IExpr makeClosure(ObjectNeeded on, IExpr fnToCall, IExpr args) {
		switch (on) {
		case NONE:
			return meth.makeNew(J.FLCLOSURE, fnToCall, args);
		case THIS:
			return meth.makeNew(J.FLCLOSURE, meth.as(meth.myThis(), J.OBJECT), fnToCall, args);
		case CARD:
			return meth.makeNew(J.FLCLOSURE, meth.as(meth.getField("_card"), J.OBJECT), fnToCall, args);
		default:
			throw new UtilException("What is " + on);
		}
	}

	private IExpr handleField(HSIEBlock closure) {
		List<IExpr> al = new ArrayList<>();
		al.add(meth.box((Expr) ((PushReturn)closure.nestedCommands().get(1)).visit(dpa)));
		al.add(meth.box((Expr) ((PushReturn)closure.nestedCommands().get(2)).visit(dpa)));
		return meth.makeNew(J.FLCLOSURE, meth.classConst(J.FLFIELD), meth.arrayOf(J.OBJECT, al));
	}

	private IExpr handleCurry(Object defn, HSIEBlock closure) {
		PushExternal curriedFn = (PushExternal)closure.nestedCommands().get(1);
		PushInt cnt = (PushInt) closure.nestedCommands().get(2);
		ExternalRef f2 = curriedFn.fn;
		String clz = DroidUtils.getJavaClassForDefn(meth, f2, defn);
		if (f2 instanceof ObjectReference || f2 instanceof CardFunction) {
			IExpr needsObject = null;
			if (form.needsCardMember())
				needsObject = meth.getField("_card");
			else
				needsObject = meth.myThis();
			return meth.makeNew(J.FLCURRY, meth.as(needsObject, J.OBJECT), meth.classConst(clz), meth.intConst(cnt.ival), arguments(closure, 3));
		} else
			return meth.makeNew(J.FLCURRY, meth.classConst(clz), meth.intConst(cnt.ival), arguments(closure, 3));
	}

	protected IExpr arguments(HSIEBlock closure, int from) {
		// Process all the arguments
		List<IExpr> al = new ArrayList<>();
		for (int i=from;i<closure.nestedCommands().size();i++) {
			PushReturn c = (PushReturn) closure.nestedCommands().get(i);
			al.add(meth.box((IExpr) c.visit(dpa)));
		}
		return meth.arrayOf(J.OBJECT, al);
	}
}
