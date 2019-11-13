package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jvmgen.JVMGenerator.XCArg;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.WithTypeSignature;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.NotImplementedException;

public class ApplyExprGenerator extends LeafAdapter implements ResultAware {
	private final FunctionState state;
	private final NestedVisitor sv;
	private final MethodDefiner meth;
	private final IExpr fcx;
	private final List<IExpr> stack = new ArrayList<IExpr>();
	private final List<IExpr> currentBlock;

	public ApplyExprGenerator(FunctionState state, NestedVisitor sv, List<IExpr> currentBlock) {
		this.state = state;
		this.sv = sv;
		this.currentBlock = currentBlock;
		this.meth = state.meth;
		this.fcx = state.fcx;
		sv.push(this);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGenerator(state, sv, currentBlock);
	}

	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		if (!expr.args.isEmpty()) {
			Object fn = expr.fn;
			int expArgs = 0;
			WithTypeSignature defn = null;
			if (fn instanceof UnresolvedVar) {
				defn = (WithTypeSignature) ((UnresolvedVar)fn).defn();
				expArgs = ((WithTypeSignature)defn).argCount();
			} else if (fn instanceof UnresolvedOperator) {
				UnresolvedOperator op = (UnresolvedOperator) fn;
				defn = (WithTypeSignature) op.defn();
				expArgs = ((WithTypeSignature)defn).argCount();
			} else if (fn instanceof MakeSend) {
				defn = (MakeSend) fn;
				expArgs = defn.argCount();
			} else
				throw new NotImplementedException("Cannot handle " + fn.getClass());
			makeClosure(defn, expr.args.size(), expArgs);
		}
		if (stack.size() != 1)
			throw new NotImplementedException("stack size is " + stack.size() + "; expected 1");
		sv.result(stack.remove(0));
	}

	@Override
	public void leaveMessages(Messages msgs) {
		List<IExpr> provided = new ArrayList<IExpr>();
		int k = stack.size();
		for (int i=0;i<k;i++)
			provided.add(stack.remove(0));
		IExpr args = meth.arrayOf(J.OBJECT, provided);
		IExpr call = meth.callStatic(J.FLEVAL, J.OBJECT, "makeArray", fcx, args);
		Var v = meth.avar(J.FLCLOSURE, state.nextVar("v"));
		currentBlock.add(meth.assign(v, call));
		sv.result(v);
	}
	
	private void makeClosure(WithTypeSignature defn, int depth, int expArgs) {
		List<IExpr> provided = new ArrayList<IExpr>();
		int k = stack.size()-depth;
		for (int i=0;i<depth;i++)
			provided.add(stack.remove(k));
		IExpr args = meth.arrayOf(J.OBJECT, provided);
		if (defn instanceof StructDefn && defn.name().uniqueName().equals("Nil")) {
			stack.remove(0); // should be "null", I think
			IExpr call = meth.callStatic(J.FLEVAL, J.OBJECT, "makeArray", fcx, args);
			Var v = meth.avar(J.FLCLOSURE, state.nextVar("v"));
			currentBlock.add(meth.assign(v, call));
			stack.add(v);
		} else if (defn instanceof StructDefn && !provided.isEmpty()) {
			stack.remove(0); // should be "null", I think
			// do the creation immediately
			// Note that we didn't push anything onto the stack earlier ...
			// TODO: I think we need to cover the currying case separately ...
			IExpr ctor = meth.callStatic(defn.name().javaClassName(), J.OBJECT, "eval", fcx, args);
			stack.add(ctor);
		} else {
			IExpr fn = stack.remove(stack.size()-1);
			List<XCArg> xcs = checkExtendedCurry(provided);
			IExpr call;
			if (xcs != null) {
				call = meth.callStatic(J.FLCLOSURE, J.FLCURRY, "xcurry", meth.as(fn, "java.lang.Object"), meth.intConst(expArgs), meth.arrayOf(J.OBJECT, asjvm(xcs)));
			} else if (depth < expArgs)
				call = meth.callStatic(J.FLCLOSURE, J.FLCURRY, "curry", meth.as(fn, "java.lang.Object"), meth.intConst(expArgs), args);
			else
				call = meth.callStatic(J.FLCLOSURE, J.FLCLOSURE, "simple", meth.as(fn, "java.lang.Object"), args);
			Var v = meth.avar(J.FLCLOSURE, state.nextVar("v"));
			currentBlock.add(meth.assign(v, call));
			stack.add(v);
		}
	}

	@Override
	public void visitMakeSend(MakeSend expr) {
		IExpr obj = stack.remove(stack.size()-1);
		// TODO: this should be on MakeSend as a MethodDir
		String dir = "$Up";
		IExpr mksend = meth.callInterface(J.OBJECT, fcx, "mksend", meth.classConst(expr.sendMeth.inContext.javaClassName() + dir), meth.stringConst(expr.sendMeth.name), obj, meth.intConst(expr.nargs));
		stack.add(mksend);
	}
	
	private List<XCArg> checkExtendedCurry(List<IExpr> provided) {
		List<XCArg> ret = new ArrayList<>();
		boolean needed = false;
		int i=0;
		for (IExpr e : provided) {
			if ((e instanceof JVMCurryArg))
				needed = true;
			else
				ret.add(new XCArg(i, e));
			i++;
		}
		if (!needed)
			return null;
		else
			return ret;
	}

	private List<IExpr> asjvm(List<XCArg> xcs) {
		List<IExpr> ret = new ArrayList<>();
		for (XCArg a : xcs) {
			ret.add(meth.box(meth.intConst(a.arg)));
			ret.add(a.expr);
		}
		return ret;
	}

	@Override
	public void leaveAssertExpr(boolean isValue, Expr e) {
		if (stack.size() != 1)
			throw new RuntimeException("I think this should be impossible, but obviously not");
		sv.result(stack.get(0));
	}

	@Override
	public void result(Object r) {
		stack.add((IExpr) r);
	}
}
