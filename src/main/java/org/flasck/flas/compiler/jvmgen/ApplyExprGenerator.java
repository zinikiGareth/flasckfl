package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.compiler.jvmgen.JVMGenerator.XCArg;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.ObjectCtor;
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
		new ExprGenerator(state, sv, currentBlock, false);
	}

	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		if (expr.args.isEmpty()) { // if it's a single-arg apply, just return the value
			if (stack.size() != 1)
				throw new NotImplementedException("stack size is " + stack.size() + "; expected 1");
			sv.result(stack.remove(0));
		} else {
			Object fn = expr.fn;
			WithTypeSignature defn;
			if (fn instanceof UnresolvedVar) {
				defn = (WithTypeSignature) ((UnresolvedVar)fn).defn();
			} else if (fn instanceof UnresolvedOperator) {
				UnresolvedOperator op = (UnresolvedOperator) fn;
				defn = (WithTypeSignature) op.defn();
			} else if (fn instanceof MakeSend) {
				defn = (MakeSend) fn;
			} else if (fn instanceof MemberExpr) {
				fn = ((MemberExpr)fn).converted();
				if (fn instanceof MakeSend)
					defn = (MakeSend) fn;
				else if (fn instanceof MakeAcor)
					defn = (MakeAcor) fn;
				else if (fn instanceof UnresolvedVar)
					defn = (WithTypeSignature) ((UnresolvedVar)fn).defn();
				else if (fn instanceof ApplyExpr) {
					defn = (WithTypeSignature) ((UnresolvedVar) ((ApplyExpr)fn).fn).defn();
					makeClosure(null, defn.argCount() - ((ApplyExpr)fn).args.size());
					return;
				} else
					throw new NotImplementedException("unknown operator type: " + fn.getClass());
			} else
				throw new NotImplementedException("Cannot handle " + fn.getClass());
			makeClosure(defn, defn.argCount());
		}
	}

	@Override
	public void leaveMessages(Messages msgs) {
		IExpr args = meth.arrayOf(J.OBJECT, stack);
		IExpr call = meth.callInterface("java.util.List", fcx, "array", args);
		Var v = meth.avar(J.FLCLOSURE, state.nextVar("v"));
		currentBlock.add(meth.assign(v, call));
		sv.result(v);
	}
	
	private void makeClosure(WithTypeSignature defn, int expArgs) {
		IExpr fn = stack.remove(0);
		
		// First adjust "expected Args" for "hidden" arguments
		if (defn instanceof HandlerImplements) {
			HandlerImplements hi = (HandlerImplements) defn;
			if (hi.getParent() != null) {
				expArgs++;
				stack.add(0, state.container);
			}
		} else if (defn instanceof ObjectCtor) {
			expArgs += ((ObjectCtor)defn).getObject().contracts.size();
		}
		
		// Then divide and conquer for special cases ...
		IExpr args = meth.arrayOf(J.OBJECT, stack);
		if (defn instanceof FunctionDefinition && defn.name().uniqueName().equals("()")) {
			// Tuple is junk
			IExpr call = meth.callInterface(J.OBJECT, fcx, "makeTuple", args);
			Var v = meth.avar(J.FLCLOSURE, state.nextVar("v"));
			currentBlock.add(meth.assign(v, call));
			sv.result(v);
		} else if (defn instanceof StructDefn && defn.name().uniqueName().equals("Nil")) {
			IExpr call = meth.callInterface("java.util.List", fcx, "array", args);
			Var v = meth.avar(J.FLCLOSURE, state.nextVar("v"));
			currentBlock.add(meth.assign(v, call));
			sv.result(v);
		} else if (defn instanceof StructDefn && !stack.isEmpty()) {
			// do the creation immediately
			if (stack.size() == expArgs) {
				IExpr ctor = meth.callStatic(defn.name().javaClassName(), J.OBJECT, "eval", fcx, args);
				sv.result(ctor);
			} else {
				IExpr call = meth.callInterface(J.FLCURRY, fcx, "curry", meth.intConst(expArgs), meth.as(meth.makeNew(J.CALLEVAL, fn), J.APPLICABLE), args);
				Var v = meth.avar(J.FLCURRY, state.nextVar("v"));
				currentBlock.add(meth.assign(v, call));
				sv.result(v);
			}
		} else {
			boolean wantObject = false;
			if (defn instanceof FunctionDefinition && ((FunctionDefinition)defn).hasState()) {
				wantObject = true;
				expArgs++;
			}
			List<XCArg> xcs = checkExtendedCurry(stack);
			IExpr call;
			if (xcs != null) {
				if (wantObject)
					call = meth.callInterface(J.FLCURRY, fcx, "oxcurry", meth.intConst(expArgs-1), meth.as(fn, J.APPLICABLE), meth.arrayOf(J.OBJECT, asjvm(xcs)));
				else
					call = meth.callInterface(J.FLCURRY, fcx, "xcurry", meth.intConst(expArgs), meth.as(fn, J.APPLICABLE), meth.arrayOf(J.OBJECT, asjvm(xcs)));
			} else if (stack.size() < expArgs) {
				if (wantObject)
					call = meth.callInterface(J.FLCURRY, fcx, "ocurry", meth.intConst(expArgs-1), meth.as(fn, J.APPLICABLE), args);
				else
					call = meth.callInterface(J.FLCURRY, fcx, "curry", meth.intConst(expArgs), meth.as(fn, J.APPLICABLE), args);
			} else {
				if (wantObject)
					call = meth.callInterface(J.FLCLOSURE, fcx, "oclosure", meth.as(fn, J.APPLICABLE), args);
				else
					call = meth.callInterface(J.FLCLOSURE, fcx, "closure", meth.as(fn, J.APPLICABLE), args);
			}
			Var v = meth.avar(J.FLCLOSURE, state.nextVar("v"));
			currentBlock.add(meth.assign(v, call));
			sv.result(v);
		}
	}

	@Override
	public void leaveMakeSend(MakeSend expr) {
		if (stack.size() != 1 && stack.size() != 2)
			throw new NotImplementedException("badly formed stack in MakeSend");
		IExpr obj = stack.remove(0);
		IExpr handler = stack.isEmpty()? meth.aNull() : stack.remove(0);
		String pkg;
		if (expr.sendMeth.inContext == null)
			pkg = J.BUILTINPKG;
		else
			pkg = expr.sendMeth.inContext.javaClassName();
		IExpr mksend = meth.callInterface(J.OBJECT, fcx, "mksend", meth.classConst(pkg), meth.stringConst(expr.sendMeth.name), meth.as(obj, J.OBJECT), meth.intConst(expr.nargs), meth.as(handler, J.OBJECT));
		sv.result(mksend);
	}
	
	@Override
	public void leaveMakeAcor(MakeAcor expr) {
		if (stack.size() != 1)
			throw new NotImplementedException(); // I don't understand this case
		IExpr obj = stack.remove(0);
		IExpr mkacor = meth.callInterface(J.OBJECT, fcx, "mkacor", meth.classConst(expr.acorMeth.inContext.javaClassName()), meth.stringConst(expr.acorMeth.name), meth.as(obj, J.OBJECT), meth.intConst(expr.nargs));
		sv.result(mkacor);
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
