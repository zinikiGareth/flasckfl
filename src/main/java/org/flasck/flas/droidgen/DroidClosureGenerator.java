package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.flim.BuiltinOperation;
import org.flasck.flas.generators.GenerationContext;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.ExprHandler;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
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
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.UtilException;

public class DroidClosureGenerator implements ExprHandler {
	private final HSIEForm form;
	private final NewMethodDefiner meth;
	private final VarHolder vh;
	private final Var cxtVar;
	enum ObjectNeeded { NONE, THIS, CARD };
	private final ObjectNeeded myOn;
	private final DroidPushArgument dpa;
	private final GenerationContext cxt;
	
	public DroidClosureGenerator(HSIEForm form, GenerationContext cxt) {
		this.form = form;
		this.cxt = cxt;
		this.meth = cxt.getMethod();
		this.vh = cxt.getVarHolder();
		this.cxtVar = cxt.getCxtArg();
		dpa = new DroidPushArgument(form, meth, cxtVar, vh);
		if (form.needsCardMember())
			myOn = ObjectNeeded.CARD;
		else if (form.isCardMethod())
			myOn = ObjectNeeded.THIS;
		else
			myOn = ObjectNeeded.NONE;
	}

	public IExpr closure(ClosureGenerator closure) {
		return pushReturn((PushReturn) closure.nestedCommands().get(0), closure);
	}

	public IExpr pushReturn(PushReturn pr, ClosureGenerator closure) {
		if (pr instanceof PushExternal) {
			ExternalRef fn = ((PushExternal)pr).fn;
			Object defn = fn;
			while (defn instanceof PackageVar)
				defn = ((PackageVar)defn).defn;
			if (fn.uniqueName().equals("FLEval.field"))
				return handleField(closure);
			else if (fn.uniqueName().equals("FLEval.curry"))
				return handleCurry(defn, closure);
			String clz = fn.myName().javaClassName();
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
				else if (form.needsCardMember()) {
					card = meth.getField("_card");
				} else
					throw new UtilException("Can't handle card member with " + form.mytype);
				IExpr fld;
				if (cm.type instanceof RWContractImplements)
					fld = meth.getField(card, cm.var);
				else
					fld = meth.callVirtual(J.OBJECT, card, "getVar", cxtVar, meth.stringConst(cm.var));
				return doEval(myOn, fld, closure);
			} else if (defn instanceof RWFunctionDefinition) {
				RWFunctionDefinition rwfn = (RWFunctionDefinition) defn;
				// a regular function
				if (rwfn.nargs == 0) { // invoke it as a function using eval
					return doEval(ObjectNeeded.NONE, meth.callStatic(clz, J.OBJECT, "eval", cxtVar, meth.arrayOf(J.OBJECT, new ArrayList<>())), closure);
				} else {
					return doEval(ObjectNeeded.NONE, meth.classConst(clz), closure);
				}
			} else if (defn instanceof RWStructDefn) {
				// creating a struct is just like calling a static function
				RWStructDefn sd = (RWStructDefn) defn;
				if (sd.fields.size() == 0)
					return doEval(ObjectNeeded.NONE, meth.callStatic(clz, J.OBJECT, "eval", cxtVar, meth.arrayOf(J.OBJECT, new ArrayList<>())), closure);
				else
					return doEval(ObjectNeeded.NONE, meth.classConst(clz), closure);
			} else if (defn instanceof RWObjectDefn) {
				// creating an object is just like calling a static function
				RWObjectDefn od = (RWObjectDefn) defn;
				if (od.ctorArgs.isEmpty())
					return doEval(ObjectNeeded.NONE, meth.callStatic(clz, J.OBJECT, "eval", cxtVar, meth.arrayOf(J.OBJECT, new ArrayList<>())), closure);
				else
					return doEval(ObjectNeeded.NONE, meth.classConst(clz), closure);
			} else if (defn instanceof HandlerLambda) {
				HandlerLambda hl = (HandlerLambda) defn;
				IExpr var = meth.getField(hl.var);
				return doEval(ObjectNeeded.NONE, var, closure);
			} else if (defn instanceof ScopedVar) {
				ScopedVar sv = (ScopedVar) defn;
				ObjectNeeded ot = ObjectNeeded.NONE;
				if (sv.defn instanceof RWFunctionDefinition && ((RWFunctionDefinition)sv.defn).mytype == CodeType.HANDLERFUNCTION)
					ot = ObjectNeeded.THIS;
				if (closure != null && closure.justScoping())
					return doEval(ot, meth.classConst(clz), closure);
				else
					return doEval(ot, vh.getScoped(sv.uniqueName()), closure);
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

	protected IExpr doEval(ObjectNeeded on, IExpr fnToCall, ClosureGenerator closure) {
		if (closure == null)
			return meth.returnObject(fnToCall);
		else
			return makeClosure(on, fnToCall, closure.arguments(this, 1));
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

	private IExpr handleField(ClosureGenerator closure) {
		List<IExpr> al = new ArrayList<>();
		al.add(meth.box((Expr) ((PushReturn)closure.nestedCommands().get(1)).visit(dpa)));
		al.add(meth.box((Expr) ((PushReturn)closure.nestedCommands().get(2)).visit(dpa)));
		return meth.makeNew(J.FLCLOSURE, meth.classConst(J.FLFIELD), meth.arrayOf(J.OBJECT, al));
	}

	private IExpr handleCurry(Object defn, ClosureGenerator closure) {
		PushExternal curriedFn = (PushExternal)closure.nestedCommands().get(1);
		PushInt cnt = (PushInt) closure.nestedCommands().get(2);
		ExternalRef f2 = curriedFn.fn;
		String clz = f2.myName().javaClassName();
		if (f2 instanceof ObjectReference || f2 instanceof CardFunction) {
			IExpr needsObject = null;
			if (form.needsCardMember())
				needsObject = meth.getField("_card");
			else
				needsObject = meth.myThis();
			return meth.makeNew(J.FLCURRY, meth.as(needsObject, J.OBJECT), meth.classConst(clz), meth.intConst(cnt.ival), closure.arguments(this, 3));
		} else
			return meth.makeNew(J.FLCURRY, meth.classConst(clz), meth.intConst(cnt.ival), closure.arguments(this, 3));
	}

	@Override
	public void visit(PushReturn expr) {
		cxt.closureArg(expr.visit(dpa));
	}

	@Override
	public void beginClosure() {
		cxt.beginClosure();
	}

	@Override
	public IExpr endClosure() {
		return cxt.endClosure();
	}
}
