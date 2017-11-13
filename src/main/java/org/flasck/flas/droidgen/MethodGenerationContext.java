package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.generators.BuiltinOpGenerator;
import org.flasck.flas.generators.CardFunctionGenerator;
import org.flasck.flas.generators.CardGroupingGenerator;
import org.flasck.flas.generators.CardMemberGenerator;
import org.flasck.flas.generators.FunctionDefnGenerator;
import org.flasck.flas.generators.GenerationContext;
import org.flasck.flas.generators.HandlerLambdaGenerator;
import org.flasck.flas.generators.IntGenerator;
import org.flasck.flas.generators.ObjectDefnGenerator;
import org.flasck.flas.generators.ObjectReferenceGenerator;
import org.flasck.flas.generators.PrimitiveTypeGenerator;
import org.flasck.flas.generators.ScopedVarGenerator;
import org.flasck.flas.generators.StringGenerator;
import org.flasck.flas.generators.StructDefnGenerator;
import org.flasck.flas.generators.TLVGenerator;
import org.flasck.flas.generators.VarGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.ClosureHandler;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.UtilException;

public class MethodGenerationContext implements GenerationContext<IExpr> {
	private final ByteCodeStorage bce;
	private final HSIEForm form;
	private ByteCodeSink bcc;
	private MethodDefiner meth;
	private List<PendingVar> pendingVars = new ArrayList<PendingVar>();
	private VarHolder vh;
	private Var cxtArg;
	private List<IExpr> closureArgs;
	private DroidClosureGenerator dcg;

	public MethodGenerationContext(ByteCodeStorage bce, HSIEForm form) {
		this.bce = bce;
		this.form = form;
	}

	@Override
	public NameOfThing nameContext() {
		return form.funcName.inContext;
	}

	@Override
	public FunctionName funcName() {
		return form.funcName;
	}

	@Override
	public boolean selectClass(String inClz) {
		if (bce.hasClass(inClz)) {
			bcc = bce.get(inClz);
			return false;
		} else {
			bcc = bce.newClass(inClz);
			bcc.generateAssociatedSourceFile();
			bcc.superclass(J.OBJECT);
			return true;
		}
	}

	@Override
	public void defaultCtor() {
		final ByteCodeSink clz = bcc;
		defaultCtor(clz);
	}

	private void defaultCtor(final ByteCodeSink clz) {
		GenericAnnotator ann = GenericAnnotator.newConstructor(clz, false);
		MethodDefiner ctor = ann.done();
		ctor.callSuper("void", J.OBJECT, "<init>").flush();
		ctor.returnVoid().flush();
	}

	@Override
	public void instanceMethod() {
		doMethod(false);
	}

	@Override
	public void staticMethod() {
		doMethod(true);
	}

	private void doMethod(boolean isStatic) {
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, isStatic, form.funcName.name);
		PendingVar cxt = gen.argument(J.OBJECT, "_context");
		gen.returns("java.lang.Object");
		int j = 0;
		for (@SuppressWarnings("unused") ScopedVar s : form.scoped)
			pendingVars.add(gen.argument("java.lang.Object", "_s"+(j++)));
		for (int i=0;i<form.nformal;i++)
			pendingVars.add(gen.argument("java.lang.Object", "_"+i));
		meth = gen.done();
		cxtArg = cxt.getVar();
		vh = new VarHolder(form, pendingVars);
	}

	@Override
	public void trampoline(String outerClz) {
		ByteCodeSink inner = bce.newClass(outerClz + "$" + form.funcName.name);
		inner.generateAssociatedSourceFile();
		inner.superclass(J.OBJECT);
		defaultCtor(inner);
		GenericAnnotator g2 = GenericAnnotator.newMethod(inner, true, "eval");
		PendingVar cx = g2.argument(J.OBJECT, "cxt");
		g2.returns(J.OBJECT);
		PendingVar args = g2.argument("[" + J.OBJECT, "args");
		MethodDefiner m2 = g2.done();
		IExpr[] fnArgs = new IExpr[pendingVars.size()+1];
		fnArgs[0] = cx.getVar();
		for (int i=0;i<pendingVars.size();i++) {
			fnArgs[i+1] = m2.arrayElt(args.getVar(), m2.intConst(i));
		}
		IExpr doCall = m2.callStatic(outerClz, J.OBJECT, form.funcName.name, fnArgs);
		m2.returnObject(doCall).flush();
	}
	
	@Override
	public void trampolineWithSelf(String outerClz) {
		ByteCodeSink inner = bce.newClass(outerClz + "$" + form.funcName.name);
		inner.generateAssociatedSourceFile();
		inner.superclass(J.OBJECT);
		defaultCtor(inner);
		GenericAnnotator g2 = GenericAnnotator.newMethod(inner, true, "eval");
		PendingVar cx = g2.argument(J.OBJECT, "cxt");
		g2.returns(J.OBJECT);
		PendingVar forThis = g2.argument(J.OBJECT,  "self");
		PendingVar args = g2.argument("[" + J.OBJECT, "args");
		MethodDefiner m2 = g2.done();
		IExpr[] fnArgs = new IExpr[pendingVars.size()+1];
		fnArgs[0] = cx.getVar();
		for (int i=0;i<pendingVars.size();i++) {
			fnArgs[i+1] = m2.arrayElt(args.getVar(), m2.intConst(i));
		}
		IExpr doCall = m2.callVirtual(J.OBJECT, m2.castTo(forThis.getVar(), outerClz), form.funcName.name, fnArgs);
		m2.returnObject(doCall).flush();
	}
	
	public void doEval(ObjectNeeded on, IExpr fnToCall, ClosureGenerator closure, OutputHandler<IExpr> handler) {
		if (closure == null)
			handler.result(meth.returnObject(fnToCall));
		else {
			closure.arguments(getDCG(), 1, new OutputHandler<IExpr>() {
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

	@Override
	public VarGenerator<IExpr> generateVar() {
		return new DroidVarGenerator(this);
	}

	@Override
	public IntGenerator<IExpr> generateInt() {
		return new DroidIntGenerator(this);
	}

	@Override
	public StringGenerator<IExpr> generateString() {
		return new DroidStringGenerator(this);
	}

	@Override
	public TLVGenerator<IExpr> generateTLV() {
		return new DroidTLVGenerator(this);
	}

	@Override
	public ScopedVarGenerator<IExpr> generateScopedVar() {
		return new DroidScopedVarGenerator(this);
	}

	@Override
	public HandlerLambdaGenerator<IExpr> generateHandlerLambda() {
		return new DroidHandlerLambdaGenerator(this);
	}

	@Override
	public FunctionDefnGenerator<IExpr> generateFunctionDefn() {
		return new DroidFunctionDefnGenerator(this);
	}

	@Override
	public StructDefnGenerator<IExpr> generateStructDefn() {
		return new DroidStructDefnGenerator(this);
	}

	@Override
	public ObjectDefnGenerator<IExpr> generateObjectDefn() {
		return new DroidObjectDefnGenerator(this);
	}

	@Override
	public CardMemberGenerator<IExpr> generateCardMember() {
		return new DroidCardMemberGenerator(this);
	}

	@Override
	public CardGroupingGenerator<IExpr> generateCardGrouping() {
		return new DroidCardGroupingGenerator(this);
	}

	@Override
	public ObjectReferenceGenerator<IExpr> generateObjectReference() {
		return new DroidObjectReferenceGenerator(this);
	}

	@Override
	public CardFunctionGenerator<IExpr> generateCardFunction() {
		return new DroidCardFunctionGenerator(this);
	}

	@Override
	public BuiltinOpGenerator<IExpr> generateBuiltinOp() {
		return new DroidBuiltinOpGenerator(this);
	}

	@Override
	public PrimitiveTypeGenerator<IExpr> generatePrimitiveType() {
		return new DroidPrimitiveTypeGenerator(this);
	}

	@Override
	public void beginClosure() {
		if (closureArgs != null)
			throw new RuntimeException("Do we need a stack here?");
		closureArgs = new ArrayList<>();
	}

	@Override
	public void closureArg(Object arg) {
		closureArgs.add(meth.box((IExpr) arg));
	}

	@Override
	public IExpr endClosure() {
		final IExpr ret = meth.arrayOf(J.OBJECT, closureArgs);
		closureArgs = null;
		return ret;
	}

	// TODO: not sure these are needed at the end of the day
	public Var getCxtArg() {
		return cxtArg;
	}

	@Override
	public ByteCodeSink getSink() {
		return bcc;
	}

	@Override
	public MethodDefiner getMethod() {
		return meth;
	}

	@Override
	public VarHolder getVarHolder() {
		return vh;
	}

	public void setDCG(DroidClosureGenerator dcg) {
		this.dcg = dcg;
	}

	public ClosureHandler<IExpr> getDCG() {
		return dcg;
	}
}
