package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypedPattern;
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
	private static class SwitchLevel {
		private String currentVar;
		private JSBlockCreator matchDefault;
		private JSBlockCreator elseBlock;
	}
	
	private final JSStorage jse;
	private JSMethodCreator meth;
	private JSBlockCreator block;
	private JSExpr runner;
	private List<JSExpr> stack = new ArrayList<>();
	private SwitchLevel currentLevel;
	private final List<SwitchLevel> switchStack = new ArrayList<>();
	private final Map<Slot, String> switchVars = new HashMap<>();

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
		System.out.println("JS " + fn.name().jsName() + " = " + fn.intros().size());
		switchVars.clear();
		if (fn.intros().isEmpty()) {
			this.meth = null;
			return;
		}
		String pkg = fn.name().packageName().jsName();
		this.meth = jse.newFunction(pkg, fn.name().jsName().substring(pkg.length()+1));
		this.meth.argument("_cxt");
		for (int i=0;i<fn.argCount();i++)
			this.meth.argument("_" + i);
		this.block = meth;
	}
	
	@Override
	public void hsiArgs(List<Slot> slots) {
		for (Slot s : slots) {
			switchVars.put(s, "_" + switchVars.size());
		}
	}

	@Override
	public void switchOn(Slot slot) {
		currentLevel = new SwitchLevel();
		currentLevel.currentVar = switchVars.get(slot);
		this.block.head(currentLevel.currentVar);
		switchStack.add(0, currentLevel);
	}

	@Override
	public void withConstructor(String ctor) {
		if (currentLevel.elseBlock != null) {
			if (!stack.isEmpty())
				this.block.returnObject(stack.remove(0));
			this.block = currentLevel.elseBlock;
		}
		JSIfExpr ifCtor = this.block.ifCtor(currentLevel.currentVar, ctor);
		this.block = ifCtor.trueCase();
		this.currentLevel.elseBlock = ifCtor.falseCase();
	}

	@Override
	public void constructorField(Slot parent, String field, Slot slot) {
		String var = "_" + switchVars.size();
		this.block.field(var, switchVars.get(parent), field);
		switchVars.put(slot, var);
	}

	// TODO: would this be better as a switch?
	@Override
	public void matchNumber(int val) {
		JSIfExpr ifCtor = this.block.ifConst(currentLevel.currentVar, val);
		this.block = ifCtor.trueCase();
		this.currentLevel.matchDefault = ifCtor.falseCase();
	}

	@Override
	public void matchString(String val) {
		JSIfExpr ifCtor = this.block.ifConst(currentLevel.currentVar, val);
		this.block = ifCtor.trueCase();
		this.currentLevel.matchDefault = ifCtor.falseCase();
	}

	@Override
	public void matchDefault() {
		if (this.currentLevel.matchDefault != null) {
			if (!stack.isEmpty())
				this.block.returnObject(this.stack.remove(0));
			this.block = this.currentLevel.matchDefault;
		}
	}

	@Override
	public void defaultCase() {
		if (!stack.isEmpty())
			this.block.returnObject(this.stack.remove(0));
		this.block = this.currentLevel.elseBlock;
	}

	@Override
	public void errorNoCase() {
		this.block.errorNoCase();
	}

	@Override
	public void bind(Slot slot, String var) {
		this.block.bindVar("_" + slot.id(), var);
	}

	@Override
	public void startInline(FunctionIntro fi) {
	}

	@Override
	public void endInline(FunctionIntro fi) {
	}

	@Override
	public void endSwitch() {
		if (!stack.isEmpty())
			this.block.returnObject(this.stack.remove(0));
		switchStack.remove(0);
		if (!switchStack.isEmpty())
			currentLevel = switchStack.get(0);
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
		generateFnOrCtor(defn, handleBuiltinName(defn), nargs);
	}

	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator, int nargs) {
		RepositoryEntry defn = operator.defn();
		if (defn == null)
			throw new RuntimeException("operator " + operator + " was still not resolved");
		generateFnOrCtor(defn, resolveOpName(operator.op, nargs), nargs);
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
			// TODO: I think we should do something here ...
		} else if (defn instanceof VarPattern) {
			stack.add(block.boundVar(((VarPattern)defn).var));
		} else if (defn instanceof TypedPattern) {
			stack.add(block.boundVar(((TypedPattern)defn).var.var));
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
		if (defn.name().uniqueName().equals("Nil")) {
			JSExpr[] args = new JSExpr[depth];
			int k = stack.size()-depth;
			for (int i=0;i<depth;i++)
				args[i] = stack.remove(k);
			if (depth == 0) // This is a hack because we don't actually push MakeArray above for some reason ...
				stack.remove(k-1); // the "Nil" or "MakeArray" that was pushed
			stack.add(block.makeArray(args));
		} else if (defn instanceof StructDefn && depth > 0) {
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
	
	private String handleBuiltinName(RepositoryEntry defn) {
		NameOfThing name = defn.name();
		if (name instanceof FunctionName && ((FunctionName)name).inContext == null) {
			String un = name.uniqueName();
			if (un.equals("length"))
				un = "arr_length";
			return "FLBuiltin." + un;
		} else
			return name.jsName();
	}

	private String resolveOpName(String op, int nargs) {
		switch (op) {
		case "+":
			return "FLBuiltin.plus";
		case "*":
			return "FLBuiltin.mul";
		case "[]":
		{
			if (nargs == 0)
				return "Nil";
			else
				return "MakeArray";
		}
		default:
			throw new RuntimeException("There is no operator " + op);
		}
	}

	public static JSGenerator forTests(JSMethodCreator meth, JSExpr runner) {
		return new JSGenerator(meth, runner);
	}

	public boolean stackIsSize(int size) {
		return stack.size() == size;
	}
}
