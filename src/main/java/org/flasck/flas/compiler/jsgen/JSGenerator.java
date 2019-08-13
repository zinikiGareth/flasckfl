package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.LeafAdapter;

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
		meth.returnObject(stack.get(0));
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
	public void visitUnresolvedVar(UnresolvedVar var) {
		FunctionDefinition defn = (FunctionDefinition)var.defn();
		if (defn == null)
			throw new RuntimeException("var " + var + " was still not resolved");
		stack.add(meth.callFunction(defn.name().jsName()));
	}
	
	@Override
	public void visitUnitTest(UnitTestCase e) {
		UnitTestName clzName = e.name;
		JSClassCreator jcc = jse.newClass(clzName.container().jsName(), clzName.baseName());
		meth = jcc.createMethod("dotest");
		meth.argument("runner");
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

	public static JSGenerator forTests(JSMethodCreator meth, JSExpr runner) {
		return new JSGenerator(meth, runner);
	}
}
