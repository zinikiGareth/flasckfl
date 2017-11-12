package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.flim.BuiltinOperation;
import org.flasck.flas.generators.GenerationContext;
import org.flasck.flas.hsie.ObjectNeeded;
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
import org.flasck.flas.vcode.hsieForm.ClosureHandler;
import org.flasck.flas.vcode.hsieForm.CurryClosure;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushInt;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.PushString;
import org.flasck.flas.vcode.hsieForm.PushTLV;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.UtilException;

public class DroidClosureGenerator implements ClosureHandler<IExpr> {
	private final HSIEForm form;
	private final NewMethodDefiner meth;
	private final VarHolder vh;
	private final Var cxtVar;
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

	public void closure(ClosureGenerator closure, OutputHandler<IExpr> handler) {
		if (closure instanceof CurryClosure)
			((CurryClosure)closure).handleCurry(form.needsCardMember(), this, handler);
		else
			pushReturn((PushReturn) closure.nestedCommands().get(0), closure, handler);
	}

	public void pushReturn(PushReturn pr, ClosureGenerator closure, OutputHandler<IExpr> handler) {
		if (pr instanceof PushExternal) {
			ExternalRef fn = ((PushExternal)pr).fn;
			Object defn = fn;
			while (defn instanceof PackageVar)
				defn = ((PackageVar)defn).defn;
			if (fn.uniqueName().equals("FLEval.field")) {
				handleField(closure, handler);
				return;
			}
			String clz = fn.myName().javaClassName();
			if (defn instanceof BuiltinOperation) {
				// This covers both Field & Tuple, but Field was handled above
				doEval(ObjectNeeded.NONE, meth.classConst(clz), closure, handler);
			} else if (defn instanceof PrimitiveType) {
				// This is for "typeof Number" or "typeof String" and returns the corresponding class object
				// See typeop.fl for an example
				doEval(ObjectNeeded.NONE, meth.classConst(clz), closure, handler);
			} else if (defn instanceof CardGrouping) {
				// This is for "typeof <cardname>" and returns the "class" corresponding to the type
				// See typeop.fl for an example
				// TODO: figure out if this should really be "ObjectReference" and if that should be renamed
				doEval(ObjectNeeded.NONE, meth.classConst(clz), closure, handler);
			} else if (defn instanceof ObjectReference) {
				// This case covers at least handling the construction of Object Handlers to pass to service methods
				doEval(myOn, meth.classConst(clz), closure, handler);
			} else if (defn instanceof CardFunction) {
				// This case covers at least event handlers
				doEval(myOn, meth.classConst(clz), closure, handler);
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
				doEval(myOn, fld, closure, handler);
			} else if (defn instanceof RWFunctionDefinition) {
				RWFunctionDefinition rwfn = (RWFunctionDefinition) defn;
				// a regular function
				if (rwfn.nargs == 0) { // invoke it as a function using eval
					doEval(ObjectNeeded.NONE, meth.callStatic(clz, J.OBJECT, "eval", cxtVar, meth.arrayOf(J.OBJECT, new ArrayList<>())), closure, handler);
				} else {
					doEval(ObjectNeeded.NONE, meth.classConst(clz), closure, handler);
				}
			} else if (defn instanceof RWStructDefn) {
				// creating a struct is just like calling a static function
				RWStructDefn sd = (RWStructDefn) defn;
				if (sd.fields.size() == 0)
					doEval(ObjectNeeded.NONE, meth.callStatic(clz, J.OBJECT, "eval", cxtVar, meth.arrayOf(J.OBJECT, new ArrayList<>())), closure, handler);
				else
					doEval(ObjectNeeded.NONE, meth.classConst(clz), closure, handler);
			} else if (defn instanceof RWObjectDefn) {
				// creating an object is just like calling a static function
				RWObjectDefn od = (RWObjectDefn) defn;
				if (od.ctorArgs.isEmpty())
					doEval(ObjectNeeded.NONE, meth.callStatic(clz, J.OBJECT, "eval", cxtVar, meth.arrayOf(J.OBJECT, new ArrayList<>())), closure, handler);
				else
					doEval(ObjectNeeded.NONE, meth.classConst(clz), closure, handler);
			} else if (defn instanceof HandlerLambda) {
				HandlerLambda hl = (HandlerLambda) defn;
				IExpr var = meth.getField(hl.var);
				doEval(ObjectNeeded.NONE, var, closure, handler);
			} else if (defn instanceof ScopedVar) {
				ScopedVar sv = (ScopedVar) defn;
				ObjectNeeded ot = ObjectNeeded.NONE;
				if (sv.defn instanceof RWFunctionDefinition && ((RWFunctionDefinition)sv.defn).mytype == CodeType.HANDLERFUNCTION)
					ot = ObjectNeeded.THIS;
				if (closure != null && closure.justScoping())
					doEval(ot, meth.classConst(clz), closure, handler);
				else
					doEval(ot, vh.getScoped(sv.uniqueName()), closure, handler);
			} else
				throw new UtilException("Didn't do anything with " + defn + " " + (defn != null ? defn.getClass() : ""));
		} else if (pr instanceof PushVar) {
			doEval(ObjectNeeded.NONE, vh.get(((PushVar)pr).var.var), closure, handler);
		} else if (pr instanceof PushInt) {
			doEval(ObjectNeeded.NONE, meth.callStatic("java.lang.Integer", "java.lang.Integer", "valueOf", meth.intConst(((PushInt)pr).ival)), closure, handler);
		} else if (pr instanceof PushString) {
			doEval(ObjectNeeded.NONE, meth.stringConst(((PushString)pr).sval.text), closure, handler);
		} else if (pr instanceof PushTLV) {
			PushTLV pt = (PushTLV) pr;
			doEval(ObjectNeeded.NONE, meth.getField(meth.getField("_src_" + pt.tlv.simpleName), pt.tlv.simpleName), closure, handler);
		} else
			throw new UtilException("Can't handle " + pr);
	}

	protected void doEval(ObjectNeeded on, IExpr fnToCall, ClosureGenerator closure, OutputHandler<IExpr> handler) {
		if (closure == null)
			handler.result(meth.returnObject(fnToCall));
		else {
			closure.arguments(this, 1, new OutputHandler<IExpr>() {
				@Override
				public void result(IExpr expr) {
					switch (on) {
					case NONE:
						handler.result(meth.makeNew(J.FLCLOSURE, fnToCall, expr));
						break;
					case THIS:
						handler.result(meth.makeNew(J.FLCLOSURE, meth.as(meth.myThis(), J.OBJECT), fnToCall, expr));
						break;
					case CARD:
						handler.result(meth.makeNew(J.FLCLOSURE, meth.as(meth.getField("_card"), J.OBJECT), fnToCall, expr));
						break;
					default:
						throw new UtilException("What is " + on);
					}
				}
			});
		}
	}

	private void handleField(ClosureGenerator closure, OutputHandler<IExpr> handler) {
		List<IExpr> al = new ArrayList<>();
		OutputHandler<IExpr> oh = new OutputHandler<IExpr>() {
			@Override
			public void result(IExpr expr) {
				al.add(meth.box(expr));
			}
		};
		((PushReturn)closure.nestedCommands().get(1)).visit(dpa, oh);
		((PushReturn)closure.nestedCommands().get(2)).visit(dpa, oh);
		handler.result(meth.makeNew(J.FLCLOSURE, meth.classConst(J.FLFIELD), meth.arrayOf(J.OBJECT, al)));
	}

	@Override
	public void visit(PushReturn expr) {
		expr.visit(dpa, new OutputHandler<IExpr>() {
			@Override
			public void result(IExpr expr) {
				cxt.closureArg(expr);
			}
		});
	}

	@Override
	public void beginClosure() {
		cxt.beginClosure();
	}

	@Override
	public ClosureHandler<IExpr> curry(NameOfThing clz, ObjectNeeded on, Integer arity) {
		return new ClosureHandler<IExpr>() {
			private List<IExpr> al = new ArrayList<>();
			private List<IExpr> vas = new ArrayList<>();
			
			@Override
			public void beginClosure() {
				if (on == ObjectNeeded.THIS)
					al.add(meth.myThis());
				else if (on == ObjectNeeded.CARD)
					al.add(meth.getField("_card"));
				al.add(meth.classConst(clz.javaClassName()));
				al.add(meth.intConst(arity));
			}
			
			@Override
			public void visit(PushReturn expr) {
				expr.visit(dpa, new OutputHandler<IExpr>() {

					@Override
					public void result(IExpr expr) {
						vas.add(meth.box(expr));
					}
				});
			}
			
			@Override
			public ClosureHandler<IExpr> curry(NameOfThing clz, ObjectNeeded on, Integer arity) {
				throw new org.zinutils.exceptions.NotImplementedException();
			}
			
			@Override
			public void endClosure(OutputHandler<IExpr> handler) {
				al.add(meth.arrayOf(J.OBJECT, vas));
				IExpr[] args = new IExpr[al.size()];
				al.toArray(args);
				handler.result(meth.makeNew(J.FLCURRY, args));
			}
		};
	}

	@Override
	public void endClosure(OutputHandler<IExpr> handler) {
		handler.result(cxt.endClosure());
	}
}
