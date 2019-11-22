package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSCurryArg;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSThis;
import org.flasck.flas.parsedForm.CurrentContainer;
import org.flasck.flas.parsedForm.CurryArgument;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.ResultAware;
import org.zinutils.exceptions.NotImplementedException;

public class ExprGeneratorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator block;

	public ExprGeneratorJS(JSFunctionState state, NestedVisitor nv, JSBlockCreator block) {
		this.state = state;
		this.sv = nv;
		if (block == null)
			throw new NullPointerException("Cannot have a null block");
		this.block = block;
		nv.push(this);
	}
	
	@Override
	public void visitMakeAcor(MakeAcor expr) {
		new ApplyExprGeneratorJS(state, sv, block);
	}

	@Override
	public void visitCurrentContainer(CurrentContainer expr) {
		sv.result(new JSThis());
	}
	
	@Override
	public void visitApplyExpr(ApplyExpr expr) {
		new ApplyExprGeneratorJS(state, sv, block);
	}
	
	@Override
	public void visitMessages(Messages msgs) {
		new ApplyExprGeneratorJS(state, sv, block);
	}
	
	@Override
	public void visitNumericLiteral(NumericLiteral expr) {
		sv.result(block.literal(expr.text));
	}

	@Override
	public void visitStringLiteral(StringLiteral expr) {
		sv.result(block.string(expr.text));
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
				makeFunctionClosure(myName, fn.argCount());
			} else
				sv.result(block.pushFunction(myName));
		} else if (defn instanceof StandaloneMethod) {
			if (nargs == 0) {
				StandaloneMethod fn = (StandaloneMethod) defn;
				makeFunctionClosure(myName, fn.argCount());
			} else
				sv.result(block.pushFunction(myName));
		} else if (defn instanceof StructDefn) {
			// if the constructor has no args, eval it here
			// otherwise leave it until "leaveExpr" or "leaveFunction"
			StructDefn sd = (StructDefn)defn;
			if (nargs == 0 && sd.argCount() == 0) {
				sv.result(block.structConst(myName));
			} else if (myName.equals("MakeArray") || sd.argCount() == nargs) {
				sv.result(null); // MakeArray does not exist
			} else if (nargs > 0) {
				sv.result(block.pushConstructor(myName));
			} else {
				sv.result(block.curry(sd.argCount(), block.pushConstructor(myName)));
			}
		} else if (defn instanceof VarPattern) {
			sv.result(block.boundVar(((VarPattern)defn).var));
		} else if (defn instanceof TypedPattern) {
			sv.result(block.boundVar(((TypedPattern)defn).var.var));
		} else if (defn instanceof StructField) {
			sv.result(block.loadField(((StructField)defn).name));
		} else if (defn instanceof CurryArgument) {
			sv.result(new JSCurryArg());
		} else if (defn instanceof UnitDataDeclaration) {
			handleUnitTestData((UnitDataDeclaration) defn);
		} else
			throw new NotImplementedException("cannot generate fn for " + defn);
	}

	private void handleUnitTestData(UnitDataDeclaration udd) {
		sv.result(state.resolveMock(udd));
	}

	private void makeFunctionClosure(String func, int expArgs) {
		JSExpr[] args = new JSExpr[] { block.pushFunction(func) };
		if (expArgs > 0)
			sv.result(block.curry(expArgs, args));
		else
			sv.result(block.closure(args));
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
		case "++":
			return "FLBuiltin.concat";
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
		sv.result((JSExpr)r);
	}
}
