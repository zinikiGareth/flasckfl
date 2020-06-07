package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSCurryArg;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSThis;
import org.flasck.flas.parsedForm.AnonymousVar;
import org.flasck.flas.parsedForm.CheckTypeExpr;
import org.flasck.flas.parsedForm.CurrentContainer;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.ObjectContract;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TemplateNestedField;
import org.flasck.flas.parsedForm.TupleMember;
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
	private final boolean isExpectation;

	public ExprGeneratorJS(JSFunctionState state, NestedVisitor nv, JSBlockCreator block, boolean isExpectation) {
		this.state = state;
		this.sv = nv;
		this.isExpectation = isExpectation;
		if (block == null)
			throw new NullPointerException("Cannot have a null block");
		this.block = block;
		nv.push(this);
	}
	
	@Override
	public void visitMakeSend(MakeSend expr) {
		new ApplyExprGeneratorJS(state, sv, block);
	}

	@Override
	public void visitMakeAcor(MakeAcor expr) {
		new ApplyExprGeneratorJS(state, sv, block);
	}

	@Override
	public void visitCurrentContainer(CurrentContainer expr, boolean isObjState, boolean wouldWantState) {
		sv.result(state.container());
	}
	
	@Override
	public void visitCheckTypeExpr(CheckTypeExpr expr) {
		new CheckTypeGeneratorJS(state, sv, block, isExpectation);
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
	public void visitAnonymousVar(AnonymousVar var) {
		if (isExpectation)
			sv.result(block.introduceVar(null));
		else
			sv.result(new JSCurryArg());
	}

	@Override
	public void visitIntroduceVar(IntroduceVar var) {
		if (isExpectation) {
			JSExpr jsv = block.introduceVar(var.var);
			state.addIntroduction(var, jsv);
			sv.result(jsv);
		}
		else
			throw new NotImplementedException("Cannot introduce vars here");
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
				makeFunctionClosure(fn.hasState(), myName, fn.argCount());
			} else if ("MakeTuple".equals(myName))
				sv.result(null);
			else
				sv.result(block.pushFunction(myName));
		} else if (defn instanceof StandaloneMethod) {
			if (nargs == 0) {
				StandaloneMethod fn = (StandaloneMethod) defn;
				makeFunctionClosure(false, myName, fn.argCount());
			} else
				sv.result(block.pushFunction(myName));
		} else if (defn instanceof ObjectMethod) {
			// TODO: does this need some kind of "object" closure?
			if (nargs == 0) {
				ObjectMethod fn = (ObjectMethod) defn;
				makeFunctionClosure(true, myName, fn.argCount());
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
				sv.result(block.curry(false, sd.argCount(), block.pushConstructor(myName)));
			}
		} else if (defn instanceof HandlerImplements) {
			// if the constructor has no args, eval it here
			// otherwise leave it until "leaveExpr" or "leaveFunction"
			HandlerImplements hi = (HandlerImplements)defn;
			if (nargs == 0 && hi.argCount() == 0) {
				List<JSExpr> args = new ArrayList<JSExpr>();
				args.add(state.container());
				sv.result(block.createObject(myName, args));
			} else if (nargs > 0) {
				sv.result(block.pushConstructor(myName));
			} else {
				sv.result(block.curry(false, hi.argCount(), block.pushConstructor(myName)));
			}
		} else if (defn instanceof VarPattern) {
			sv.result(block.boundVar(((VarPattern)defn).var));
		} else if (defn instanceof TypedPattern) {
			sv.result(block.boundVar(((TypedPattern)defn).var.var));
		} else if (defn instanceof HandlerLambda) {
			sv.result(block.lambda((HandlerLambda)defn));
		} else if (defn instanceof StructField) {
			sv.result(block.loadField(state.container(), ((StructField)defn).name));
		} else if (defn instanceof TemplateNestedField) {
			TemplateNestedField tnf = (TemplateNestedField)defn;
			StructField sf = tnf.getField();
			JSExpr from = state.templateObj().get(tnf.name().var);
			if (sf != null) {
				sv.result(block.loadField(from, sf.name));
			} else {
				sv.result(from);
			}
		} else if (defn instanceof RequiresContract) {
			sv.result(block.contractByVar(state.container(), ((RequiresContract)defn).referAsVar));
		} else if (defn instanceof ObjectContract) {
			sv.result(block.member(((ObjectContract)defn).varName().var));
		} else if (defn instanceof TupleMember) {
			makeFunctionClosure(false, myName, 0);
		} else if (defn instanceof UnitDataDeclaration) {
			handleUnitTestData((UnitDataDeclaration) defn);
		} else if (defn instanceof IntroduceVar) {
			sv.result(block.fromIntroduction(state.resolveIntroduction((IntroduceVar) defn)));
		} else if (defn instanceof ObjectCtor) {
			ObjectCtor oc = (ObjectCtor) defn;
			JSExpr fn = block.callStatic(oc.name().container().jsName(), oc.name().name);
			if (nargs == 0) {
				int expArgs = oc.argCountIncludingContracts();
				JSExpr[] args = new JSExpr[] { fn };
				JSExpr call;
				if (expArgs > 0)
					call = block.curry(false, expArgs, args);
				else
					call = block.closure(false, args);
				sv.result(call);
			} else
				sv.result(fn);
		} else
			throw new NotImplementedException("cannot generate fn for " + defn);
	}

	private void handleUnitTestData(UnitDataDeclaration udd) {
		sv.result(state.resolveMock(block, udd));
	}

	private void makeFunctionClosure(boolean hasState, String func, int expArgs) {
		JSExpr[] args;
		if (hasState)
			args = new JSExpr[] { block.pushFunction(func), new JSThis() };
		else
			args = new JSExpr[] { block.pushFunction(func) };
		if (expArgs > 0) {
			sv.result(block.curry(hasState, expArgs, args));
		} else
			sv.result(block.closure(hasState, args));
	}

	private String handleBuiltinName(RepositoryEntry defn) {
		NameOfThing name = defn.name();
		if (name instanceof FunctionName && ((FunctionName)name).inContext == null) {
			String un = name.uniqueName();
			if (un.equals("length"))
				un = "arr_length";
			return "FLBuiltin." + un;
		} else if (defn instanceof ObjectMethod) {
			return ((FunctionName)name).jsPName();
		} else if (defn instanceof FunctionDefinition && ((FunctionDefinition)defn).hasState())
			return ((FunctionName)name).jsPName();
		else
			return name.jsName();
	}

	private String resolveOpName(String op, int nargs) {
		switch (op) {
		case "&&":
			return "FLBuiltin.boolAnd";
		case "||":
			return "FLBuiltin.boolOr";
		case "!":
			return "FLBuiltin.not";
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
		case "()":
		{
			return "MakeTuple";
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
