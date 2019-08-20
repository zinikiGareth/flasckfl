package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.WithTypeSignature;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.RepositoryEntry;

public class JSGenerator extends LeafAdapter {
	private final JSStorage jse;
	private JSMethodCreator meth;
	private JSExpr runner;
	private List<JSExpr> stack = new ArrayList<>();


	public JSGenerator(JSStorage jse) {
		this.jse = jse;
	}

	public JSGenerator(JSMethodCreator meth, JSExpr runner) {
		this.jse = null;
		this.meth = meth;
		this.runner = runner;
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		this.meth = jse.newFunction(fn.name().container().jsName(), fn.name().name);
	}
	
	// TODO: this should have been reduced to HSIE, which we should generate from
	// But I am hacking for now to get a walking skeleton up and running so we can E2E TDD
	// The actual traversal is done by the traverser ...

	@Override
	public void leaveFunction(FunctionDefinition fn) {
		if (stack.size() != 1) {
			throw new RuntimeException("I was expecting a stack depth of 1, not " + stack.size());
		}
		meth.returnObject(stack.remove(0));
		this.meth = null;
	}
	

	@Override
	public void visitNumericLiteral(NumericLiteral expr) {
		stack.add(meth.literal(expr.text));
	}

	@Override
	public void visitStringLiteral(StringLiteral expr) {
		stack.add(meth.string(expr.text));
	}
	
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		RepositoryEntry defn = var.defn();
		if (defn == null)
			throw new RuntimeException("var " + var + " was still not resolved");
		if (defn instanceof FunctionDefinition) {
			if (nargs == 0) {
				FunctionDefinition fn = (FunctionDefinition) defn;
				stack.add(meth.pushFunction(defn.name().jsName()));
				makeClosure(fn, 0, fn.argCount());
			} else
				stack.add(meth.pushFunction(defn.name().jsName()));
		} else if (defn instanceof StructDefn) {
			// if the constructor has no args, eval it here
			// otherwise leave it until "leaveExpr" or "leaveFunction"
			if (nargs == 0 && ((StructDefn)defn).argCount() == 0) {
				stack.add(meth.callFunction(defn.name().jsName()));
			}
		}
	}

	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator) {
		String opName = resolveOpName(operator.op);
		stack.add(meth.pushFunction(opName));
	}

	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		Object fn = expr.fn;
		WithTypeSignature defn = null;
		if (fn instanceof UnresolvedVar)
			defn = (WithTypeSignature) ((UnresolvedVar)fn).defn();
		else if (fn instanceof UnresolvedOperator)
			defn = (WithTypeSignature) ((UnresolvedOperator)fn).defn();
		if (expr.args.isEmpty()) // then it's a spurious apply
			return;
		makeClosure(defn, expr.args.size(), defn.argCount());
	}

	private void makeClosure(WithTypeSignature defn, int depth, int expArgs) {
		if (defn instanceof StructDefn && depth > 0) {
			// do the creation immediately
			// Note that we didn't push anything onto the stack earlier ...
			// TODO: I think we need to cover the currying case separately ...
			JSExpr[] args = new JSExpr[depth];
			int k = stack.size()-depth;
			for (int i=0;i<depth;i++)
				args[i] = stack.remove(k);
			stack.add(meth.callFunction(defn.name().jsName(), args));
		} else {
			JSExpr[] args = new JSExpr[depth+1];
			int k = stack.size()-depth-1;
			for (int i=0;i<=depth;i++)
				args[i] = stack.remove(k);
			JSExpr call;
			if (depth < expArgs)
				call = meth.curry(expArgs, args);
			else
				call = meth.closure(args);
			stack.add(call);
		}
	}

	@Override
	public void visitUnitTest(UnitTestCase e) {
		UnitTestName clzName = e.name;
		meth = jse.newFunction(clzName.container().jsName(), clzName.baseName());
		runner = meth.argument("runner");
	}

	@Override
	public void leaveUnitTest(UnitTestCase e) {
		meth = null;
	}

	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
		if (stack.size() != 2) {
			throw new RuntimeException("I was expecting a stack depth of 2, not " + stack.size());
		}
		JSExpr lhs = stack.get(0);
		JSExpr rhs = stack.get(1);
		meth.assertable(runner, "assertSameValue", lhs, rhs);
		stack.clear();
	}
	
	private String resolveOpName(String op) {
		switch (op) {
		case "+":
			return "FLEval.plus";
		case "*":
			return "FLEval.mul";
		default:
			throw new RuntimeException("There is no operator " + op);
		}
	}

	public static JSGenerator forTests(JSMethodCreator meth, JSExpr runner) {
		return new JSGenerator(meth, runner);
	}
}
