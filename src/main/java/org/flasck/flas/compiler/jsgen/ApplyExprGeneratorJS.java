package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSCurryArg;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.WithTypeSignature;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.zinutils.exceptions.NotImplementedException;

public class ApplyExprGeneratorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private final List<JSExpr> stack = new ArrayList<>();

	public ApplyExprGeneratorJS(JSFunctionState state, NestedVisitor nv, JSBlockCreator block) {
		this.state = state;
		this.sv = nv;
		if (block == null)
			throw new NullPointerException("Cannot have a null block");
		this.block = block;
		nv.push(this);
	}

	public void visitExpr(Expr expr, int nArgs) {
		new ExprGeneratorJS(state, sv, block);
	}
	
	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		if (expr.args.isEmpty()) { // if it's a single-arg apply, just return the value
			if (stack.size() != 1)
				throw new NotImplementedException("stack should now have size 1");
			sv.result(stack.remove(0));
		} else {
			Object fn = expr.fn;
			WithTypeSignature defn;
			if (fn instanceof UnresolvedVar)
				defn = (WithTypeSignature) ((UnresolvedVar)fn).defn();
			else if (fn instanceof UnresolvedOperator)
				defn = (WithTypeSignature) ((UnresolvedOperator)fn).defn();
			else if (fn instanceof MakeSend)
				defn = (MakeSend) fn;
			else
				throw new NotImplementedException("unknown operator type: " + fn.getClass());
			makeClosure(defn, defn.argCount());
		}
	}

	@Override
	public void leaveMessages(Messages msgs) {
		sv.result(block.makeArray(stack.toArray(new JSExpr[stack.size()])));
	}

	// TODO: I think in these first two cases we should also check for explicit currying
	// There certainly isn't implicit currying on the first one which is an Array of unspecified length
	private void makeClosure(WithTypeSignature defn, int expArgs) {
		if (defn instanceof StructDefn && defn.name().uniqueName().equals("Nil")) {
			stack.remove(0); // should be "null", I think
			sv.result(block.makeArray(stack.toArray(new JSExpr[stack.size()])));
		} else if (defn instanceof StructDefn && stack.size() > 1) {
			// do the creation immediately
			stack.remove(0); // should be "null", I think
			String fn = defn.name().jsName();
			if (fn.equals("Error"))
				fn = "FLError";
			sv.result(block.structArgs(fn, stack.toArray(new JSExpr[stack.size()])));
		} else {
			JSExpr[] args = new JSExpr[stack.size()];
			List<XCArg> xcs = new ArrayList<>();
			int k = 0;
			boolean explicit = false;
			for (JSExpr arg : stack) {
				if (arg instanceof JSCurryArg)
					explicit = true;
				else
					xcs.add(new XCArg(k, arg));
				args[k++] = arg;
			}
			JSExpr call;
			if (explicit)
				call = block.xcurry(expArgs, xcs);
			else if (stack.size() < expArgs+1)
				call = block.curry(expArgs, args);
			else
				call = block.closure(args);
			sv.result(call);
		}
	}

	@Override
	public void leaveMakeSend(MakeSend expr) {
		JSExpr obj = stack.remove(stack.size()-1);
		stack.add(block.makeSend(expr.sendMeth.jsName(), obj, expr.nargs));
	}

	@Override
	public void leaveMakeAcor(MakeAcor expr) {
		if (stack.size() != 1)
			throw new NotImplementedException(); // I don't think I understand this case, yet
		JSExpr obj = stack.remove(0);
		sv.result(block.makeAcor(expr.acorMeth.jsPName(), obj, expr.nargs));
	}
	
	@Override
	public void result(Object r) {
		stack.add((JSExpr)r);
	}
}
