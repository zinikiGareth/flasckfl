package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.generators.BoolGenerator;
import org.flasck.flas.generators.BuiltinOpGenerator;
import org.flasck.flas.generators.CSRGenerator;
import org.flasck.flas.generators.CardFunctionGenerator;
import org.flasck.flas.generators.CardGroupingGenerator;
import org.flasck.flas.generators.CardMemberGenerator;
import org.flasck.flas.generators.DoubleGenerator;
import org.flasck.flas.generators.FuncGenerator;
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
import org.flasck.flas.hsie.ClosureTraverser;
import org.flasck.flas.hsie.HSIGenerator;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.ClosureHandler;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEVisitor;
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

public class MethodGenerationContext implements IMethodGenerationContext {
	private final ByteCodeStorage bce;
	private final HSIEForm form;
	private ByteCodeSink bcc;
	private MethodDefiner meth;
	private List<PendingVar> pendingVars = new ArrayList<PendingVar>();
	private VarHolder vh;
	private Var cxtArg;
	private List<IExpr> closureArgs;

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
		PendingVar cxt = gen.argument(J.FLEVALCONTEXT, "_context");
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
		PendingVar cx = g2.argument(J.FLEVALCONTEXT, "cxt");
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
		PendingVar cx = g2.argument(J.FLEVALCONTEXT, "cxt");
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
	
	@Override
	public ClosureHandler<IExpr> getClosureHandler() {
		return new DroidClosureHandler(this);
	}

	@Override
	public HSIEVisitor<IExpr> hsi(HSIGenerator<IExpr> droidHSIGenerator, HSIEForm form, GenerationContext<IExpr> cxt, ClosureTraverser<IExpr> closGen) {
		return new DroidHSIProcessor(droidHSIGenerator, form, cxt, closGen);
	}

	@Override
	public void doEval(ObjectNeeded on, IExpr fnToCall, ClosureGenerator closure, OutputHandler<IExpr> handler) {
		IExpr closExpr = meth.as(fnToCall, J.OBJECT);
		if (closure == null)
			handler.result(meth.returnObject(closExpr));
		else {
			closure.arguments(form, new DroidClosureHandler(this), 1, new OutputHandler<IExpr>() {
				@Override
				public void result(IExpr expr) {
					switch (on) {
					case NONE:
						handler.result(meth.callStatic(J.FLCLOSURE, J.FLCLOSURE, "simple", closExpr, expr));
						break;
					case THIS:
						handler.result(meth.callStatic(J.FLCLOSURE, J.FLCLOSURE, "obj", meth.as(meth.myThis(), J.OBJECT), closExpr, expr));
						break;
					case CARD:
						handler.result(meth.callStatic(J.FLCLOSURE, J.FLCLOSURE, "obj", meth.as(meth.getField("_card"), J.OBJECT), closExpr, expr));
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
	public BoolGenerator<IExpr> generateBool() {
		return new DroidBoolGenerator(this);
	}

	@Override
	public DoubleGenerator<IExpr> generateDouble() {
		return new DroidDoubleGenerator(this);
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
	public CSRGenerator<IExpr> generateCSR() {
		return new DroidCSRGenerator(this);
	}

	@Override
	public FuncGenerator<IExpr> generateFunc() {
		return new DroidFuncGenerator(this);
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
}
