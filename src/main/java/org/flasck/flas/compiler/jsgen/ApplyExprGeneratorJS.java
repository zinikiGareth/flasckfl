package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSCurryArg;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
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
		Object fn = expr.fn;
		if (!expr.args.isEmpty()) { // only if it's a real apply
			WithTypeSignature defn;
			if (fn instanceof UnresolvedVar)
				defn = (WithTypeSignature) ((UnresolvedVar)fn).defn();
			else if (fn instanceof UnresolvedOperator)
				defn = (WithTypeSignature) ((UnresolvedOperator)fn).defn();
			else if (fn instanceof MakeSend)
				defn = (MakeSend) fn;
			else
				throw new NotImplementedException("unknown operator type: " + fn.getClass());
			makeClosure(defn, expr.args.size(), defn.argCount());
		}
		if (stack.size() != 1)
			throw new NotImplementedException("stack should now have size 1");
		sv.result(stack.remove(0));
	}

	@Override
	public void leaveMessages(Messages msgs) {
		JSExpr[] args = new JSExpr[stack.size()];
		int k = stack.size();
		for (int i=0;i<k;i++)
			args[i] = stack.remove(0);
		sv.result(block.makeArray(args));
	}

	private void makeClosure(WithTypeSignature defn, int depth, int expArgs) {
		if (defn instanceof StructDefn && defn.name().uniqueName().equals("Nil")) {
			stack.remove(0); // should be "null", I think
			JSExpr[] args = new JSExpr[depth];
			int k = stack.size()-depth;
			for (int i=0;i<depth;i++)
				args[i] = stack.remove(k);
			stack.add(block.makeArray(args));
		} else if (defn instanceof StructDefn && depth > 0) {
			// do the creation immediately
			// Note that we didn't push anything onto the stack earlier ...
			// TODO: I think we need to cover the currying case separately ...
			stack.remove(0); // should be "null", I think
			JSExpr[] args = new JSExpr[depth];
			int k = stack.size()-depth;
			for (int i=0;i<depth;i++)
				args[i] = stack.remove(k);
			String fn = defn.name().jsName();
			if (fn.equals("Error"))
				fn = "FLError";
			stack.add(block.structArgs(fn, args));
		} else {
			JSExpr[] args = new JSExpr[depth+1];
			List<XCArg> xcs = new ArrayList<>();
			int k = stack.size()-depth-1;
			for (int i=0;i<=depth;i++) {
				JSExpr arg = stack.remove(k);
				if (!(arg instanceof JSCurryArg))
					xcs.add(new XCArg(i, arg));
				args[i] = arg;
			}
			JSExpr call;
			if (xcs.size() < depth+1)
				call = block.xcurry(expArgs, xcs);
			else if (depth < expArgs)
				call = block.curry(expArgs, args);
			else
				call = block.closure(args);
			stack.add(call);
		}
	}

	@Override
	public void visitMakeSend(MakeSend expr) {
		JSExpr obj = stack.remove(stack.size()-1);
		stack.add(block.makeSend(expr.sendMeth.jsName(), obj, expr.nargs));
	}

	@Override
	public void result(Object r) {
		stack.add((JSExpr)r);
	}
}
