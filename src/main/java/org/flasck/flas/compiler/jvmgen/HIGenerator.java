package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.hsi.HLSlot;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.exceptions.NotImplementedException;

public class HIGenerator extends LeafAdapter {
	private final StackVisitor sv;
	private final ByteCodeSink definingClz;
	private final FunctionState fs;
	private final MethodDefiner meth;
	private final ArrayList<IExpr> currentBlock;
	private final AtomicInteger nextArg = new AtomicInteger();

	public HIGenerator(StackVisitor sv, ByteCodeStorage bce, HandlerImplements hi, StateHolder sh, IExpr runner) {
		this.sv = sv;
		sv.push(this);
		
		HandlerName name = (HandlerName) hi.name();
		String clzName = name.javaClassName();
		definingClz = bce.newClass(clzName);
		definingClz.superclass(J.LOGGINGIDEMPOTENTHANDLER);
		definingClz.implementsInterface(J.IDEMPOTENTHANDLER);
		definingClz.generateAssociatedSourceFile();
		definingClz.defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs").constValue(hi.argCount());
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(definingClz, false);
			/*PendingVar cx = */gen.argument(J.FLEVALCONTEXT, "cxt");
			MethodDefiner ctor = gen.done();
			ctor.callSuper("void", J.OBJECT, "<init>").flush();
			ctor.returnVoid().flush();
		}
		{ // eval(cx)
			GenericAnnotator gen = GenericAnnotator.newMethod(definingClz, true, "eval");
			PendingVar cx = gen.argument(J.FLEVALCONTEXT, "cxt");
			PendingVar pargs = gen.argument("[" + J.OBJECT, "args");
			gen.returns(J.OBJECT);
			MethodDefiner meth = gen.done();
			Var ret = meth.avar(clzName, "ret");
			meth.assign(ret, meth.makeNew(clzName, cx.getVar())).flush();
			this.fs = new FunctionState(meth, cx.getVar(), null, pargs.getVar(), runner);
			this.meth = meth;
			fs.evalRet = ret;
			this.currentBlock = new ArrayList<IExpr>();
		}
	}

	@Override
	public void visitHandlerLambda(HandlerLambda hl) {
		if (definingClz != null) {
			String name = ((TypedPattern)hl.patt).name().var;
			definingClz.defineField(true, Access.PRIVATE, J.OBJECT, name);
			meth.assign(meth.getField(fs.evalRet, name), meth.arrayElt(fs.fargs, meth.intConst(nextArg.getAndIncrement()))).flush();
		} else if (fs != null) {
			// method with lambdas
			if (hl.patt instanceof TypedPattern) {
				TypedPattern tp = (TypedPattern)hl.patt;
				fs.bindVar(currentBlock, tp.var.var, new HLSlot(tp.var.var), meth.aNull());
			} else
				throw new NotImplementedException("support varpattern " + hl);
		}
	}
	
	@Override
	public void leaveHandlerImplements(HandlerImplements hi) {
		if (this.currentBlock != null && !this.currentBlock.isEmpty())
			JVMGenerator.makeBlock(meth, currentBlock).flush();
		this.meth.returnObject(fs.evalRet).flush();
		sv.result(null);
	}
}
