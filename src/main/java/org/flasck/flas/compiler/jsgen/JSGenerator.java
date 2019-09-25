package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.WithTypeSignature;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.RepositoryEntry;
import org.zinutils.exceptions.NotImplementedException;

public class JSGenerator extends LeafAdapter implements HSIVisitor {
	private final JSStorage jse;
	private JSMethodCreator meth;
	private JSBlockCreator block;
	private JSExpr runner;
	private List<JSExpr> stack = new ArrayList<>();
	private JSBlockCreator elseBlock;

	public JSGenerator(JSStorage jse) {
		this.jse = jse;
	}

	public JSGenerator(JSMethodCreator meth, JSExpr runner) {
		this.jse = null;
		this.meth = meth;
		this.block = meth;
		this.runner = runner;
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		if (fn.intros().isEmpty()) {
			this.meth = null;
			return;
		}
		this.meth = jse.newFunction(fn.name().container().jsName(), fn.name().name);
		this.meth.argument("_cxt");
		for (int i=0;i<fn.argCount();i++)
			this.meth.argument("_" + i);
		this.block = meth;
		this.elseBlock = null;
	}
	
	@Override
	public void hsiArgs(List<Slot> slots) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void switchOn(Slot slot) {
		this.meth.head("_0");
	}

	@Override
	public void withConstructor(String ctor) {
		if (elseBlock != null) {
			this.block.returnObject(stack.remove(0));
			this.block = elseBlock;
		}
		JSIfExpr ifCtor = this.block.ifCtor("_0", ctor);
		this.block = ifCtor.trueCase();
		// TODO: ultimately this will be a stack ...
		this.elseBlock = ifCtor.falseCase();
	}

	@Override
	public void errorNoCase() {
		if (!stack.isEmpty())
			this.block.returnObject(this.stack.remove(0));
		this.elseBlock.errorNoCase();
//		this.block = null;
	}

	@Override
	public void bind(Slot slot, String var) {
		this.meth.bindVar("_0", var);
	}

	@Override
	public void startInline(FunctionIntro fi) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endInline(FunctionIntro fi) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endSwitch() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void leaveFunction(FunctionDefinition fn) {
		if (meth == null) {
			// we elected not to generate, so just forget it ...
			return;
		}
		if (stack.size() == 1) {
			block.returnObject(stack.remove(0));
		} else if (!stack.isEmpty()) {
			throw new RuntimeException("I was expecting a stack depth of 1, not " + stack.size());
		}
		this.meth = null;
	}
	

	@Override
	public void visitNumericLiteral(NumericLiteral expr) {
		stack.add(block.literal(expr.text));
	}

	@Override
	public void visitStringLiteral(StringLiteral expr) {
		stack.add(block.string(expr.text));
	}
	
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		RepositoryEntry defn = var.defn();
		if (defn == null)
			throw new RuntimeException("var " + var + " was still not resolved");
		generateFnOrCtor(defn, defn.name().jsName(), nargs);
	}

	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator, int nargs) {
		RepositoryEntry defn = operator.defn();
		if (defn == null)
			throw new RuntimeException("operator " + operator + " was still not resolved");
		generateFnOrCtor(defn, resolveOpName(operator.op), nargs);
	}

	private void generateFnOrCtor(RepositoryEntry defn, String myName, int nargs) {
		if (defn instanceof FunctionDefinition) {
			if (nargs == 0) {
				FunctionDefinition fn = (FunctionDefinition) defn;
				stack.add(block.pushFunction(myName));
				makeClosure(fn, 0, fn.argCount());
			} else
				stack.add(block.pushFunction(myName));
		} else if (defn instanceof StructDefn) {
			// if the constructor has no args, eval it here
			// otherwise leave it until "leaveExpr" or "leaveFunction"
			if (nargs == 0 && ((StructDefn)defn).argCount() == 0) {
				stack.add(block.structConst(myName));
			}
		} else if (defn instanceof VarPattern) {
			stack.add(block.boundVar(((VarPattern)defn).var));
		} else
			throw new NotImplementedException();
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
			String fn = defn.name().jsName();
			if (fn.equals("Error"))
				fn = "FLError";
			stack.add(block.callFunction(fn, args));
		} else {
			JSExpr[] args = new JSExpr[depth+1];
			int k = stack.size()-depth-1;
			for (int i=0;i<=depth;i++)
				args[i] = stack.remove(k);
			JSExpr call;
			if (depth < expArgs)
				call = block.curry(expArgs, args);
			else
				call = block.closure(args);
			stack.add(call);
		}
	}

	@Override
	public void visitUnitTest(UnitTestCase e) {
		UnitTestName clzName = e.name;
		meth = jse.newFunction(clzName.container().jsName(), clzName.baseName());
		this.block = meth;
		/*JSExpr cxt = */meth.argument("_cxt");
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
			return "FLBuiltin.plus";
		case "*":
			return "FLBuiltin.mul";
		case "[]":
			return "Nil";
		default:
			throw new RuntimeException("There is no operator " + op);
		}
	}

	public static JSGenerator forTests(JSMethodCreator meth, JSExpr runner) {
		return new JSGenerator(meth, runner);
	}
}
