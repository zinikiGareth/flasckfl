package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.flasck.flas.parsedForm.CurryArgument;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.WithTypeSignature;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.ResultAware;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class ExprGeneratorJS extends LeafAdapter implements ResultAware {
	public static class JSCurryArg implements JSExpr {
		@Override
		public String asVar() {
			throw new NotImplementedException();
		}

		@Override
		public void write(IndentWriter w) {
			throw new NotImplementedException();
		}
	}

	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private final List<JSExpr> stack = new ArrayList<>();

	public ExprGeneratorJS(NestedVisitor nv, JSBlockCreator block) {
		this.sv = nv;
		this.block = block;
		System.out.println("Create ExprJS");
	}

	@Override
	public void leaveGuard(FunctionCaseDefn c) {
		if (stack.size() != 1)
			throw new RuntimeException("I think this is impossible, but obviously not: " + stack.size());
		sv.result(stack.remove(0));
	}

	@Override
	public void leaveCase(FunctionCaseDefn c) {
		if (stack.size() != 1)
			throw new RuntimeException("I think this is impossible, but obviously not: " + stack.size());
		sv.result(stack.remove(0));
	}

	@Override
	public void endInline(FunctionIntro fi) {
		if (stack.size() != 1)
			throw new RuntimeException("I think this is impossible, but obviously not: " + stack.size());
		sv.result(stack.remove(0));
	}

	@Override
	public void leaveAssertExpr(boolean isValue, Expr e) {
		if (stack.size() != 1)
			throw new RuntimeException("I think this is impossible, but obviously not");
		sv.result(stack.remove(0));
	}

	@Override
	public void visitApplyExpr(ApplyExpr expr) {
		sv.push(new ExprGeneratorJS(sv, block));
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
		} else if (defn instanceof CurryArgument) {
			stack.add(new JSCurryArg());
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
		if (!expr.args.isEmpty()) // only if it's a real apply
			makeClosure(defn, expr.args.size(), defn.argCount());
		if (stack.size() != 1)
			throw new NotImplementedException();
		sv.result(stack.remove(0));
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
		case "==":
			return "FLBuiltin.isEqual";
		case "+":
			return "FLBuiltin.plus";
		case "-":
			return "FLBuiltin.minus";
		case "*":
			return "FLBuiltin.mul";
		case "/":
			return "FLBuiltin.div";
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

	@Override
	public void result(Object r) {
		stack.add((JSExpr)r);
	}
}
