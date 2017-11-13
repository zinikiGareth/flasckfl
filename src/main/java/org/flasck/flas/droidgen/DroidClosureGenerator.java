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
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.ClosureHandler;
import org.flasck.flas.vcode.hsieForm.CurryClosure;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
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
	private final GenerationContext<IExpr> cxt;
	
	public DroidClosureGenerator(HSIEForm form, GenerationContext<IExpr> cxt) {
		this.form = form;
		this.cxt = cxt;
		((MethodGenerationContext)cxt).setDCG(this);
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
			if (defn instanceof BuiltinOperation) {
				cxt.generateBuiltinOp().generate((BuiltinOperation) defn, fn.myName(), handler, closure);
			} else if (defn instanceof PrimitiveType) {
				cxt.generatePrimitiveType().generate((PrimitiveType) defn, handler, closure);
			} else if (defn instanceof CardGrouping) {
				cxt.generateCardGrouping().generate((CardGrouping) defn, handler, closure);
			} else if (defn instanceof ObjectReference) {
				cxt.generateObjectReference().generate((ObjectReference) defn, myOn, handler, closure);
			} else if (defn instanceof CardFunction) {
				cxt.generateCardFunction().generate((CardFunction) defn, myOn, handler, closure);
			} else if (defn instanceof CardMember) {
				cxt.generateCardMember().generate((CardMember)defn, form, myOn, handler, closure);
			} else if (defn instanceof RWFunctionDefinition) {
				cxt.generateFunctionDefn().generate((RWFunctionDefinition) defn, handler, closure);
			} else if (defn instanceof RWStructDefn) {
				cxt.generateStructDefn().generate((RWStructDefn) defn, handler, closure);
			} else if (defn instanceof RWObjectDefn) {
				cxt.generateObjectDefn().generate((RWObjectDefn) defn, handler, closure);
			} else if (defn instanceof HandlerLambda) {
				cxt.generateHandlerLambda().generate((HandlerLambda) defn, handler, closure);
			} else if (defn instanceof ScopedVar) {
				cxt.generateScopedVar().generate((ScopedVar) defn, handler, closure);
			} else
				throw new UtilException("Didn't do anything with " + defn + " " + (defn != null ? defn.getClass() : ""));
		} else if (pr instanceof PushVar) {
			cxt.generateVar().generate((PushVar)pr, handler, closure);
		} else if (pr instanceof PushInt) {
			cxt.generateInt().generate((PushInt)pr, handler, closure);
		} else if (pr instanceof PushString) {
			cxt.generateString().generate((PushString)pr, handler, closure);
		} else if (pr instanceof PushTLV) {
			cxt.generateTLV().generate((PushTLV) pr, handler, closure);
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
