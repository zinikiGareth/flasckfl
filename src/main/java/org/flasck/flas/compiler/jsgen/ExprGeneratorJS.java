package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
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
import org.flasck.flas.parsedForm.LogicHolder;
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
import org.flasck.flas.parsedForm.TypeReference;
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
		sv.result(state.container(expr.type.name()));
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
	public void visitTypeReference(TypeReference tr, boolean expectPolys, int exprNargs) {
		if (exprNargs == -1) {
			return; // this is not an expression case
		}
		RepositoryEntry defn = (RepositoryEntry) tr.defn();
		if (defn == null)
			throw new RuntimeException("var " + tr + " was still not resolved");
		generateFnOrCtor(defn, handleBuiltinName(defn), exprNargs);
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
			FunctionDefinition fn = (FunctionDefinition) defn;
			if (nargs == 0) {
				makeFunctionClosure(fn.hasState(), fn.name(), myName, fn.argCount());
			} else if ("MakeTuple".equals(myName))
				sv.result(null);
			else {
				sv.result(block.pushFunction(myName, fn.name(), nargs));
			}
		} else if (defn instanceof StandaloneMethod) {
			if (nargs == 0) {
				StandaloneMethod fn = (StandaloneMethod) defn;
				makeFunctionClosure(false, fn.name(), myName, fn.argCount());
			} else
				sv.result(block.pushFunction(myName, (FunctionName) defn.name(), nargs));
		} else if (defn instanceof ObjectMethod) {
			// TODO: does this need some kind of "object" closure?
			if (nargs == 0) {
				ObjectMethod fn = (ObjectMethod) defn;
				makeFunctionClosure(true, null, myName, fn.argCount());
			} else
				sv.result(block.pushFunction(myName, (FunctionName) defn.name(), nargs));
		} else if (defn instanceof StructDefn) {
			// if the constructor has no args, eval it here
			// otherwise leave it until "leaveExpr" or "leaveFunction"
			StructDefn sd = (StructDefn)defn;
			if (nargs == 0 && sd.argCount() == 0) {
				sv.result(block.structConst(sd.name()));
			} else if (myName.equals("MakeArray") || sd.argCount() == nargs) {
				sv.result(null); // MakeArray does not exist
			} else if (nargs > 0) {
				sv.result(block.pushConstructor(defn.name(), myName));
			} else {
				sv.result(block.curry(false, sd.argCount(), block.pushConstructor(defn.name(), myName)));
			}
		} else if (defn instanceof HandlerImplements) {
			// if the constructor has no args, eval it here
			// otherwise leave it until "leaveExpr" or "leaveFunction"
			HandlerImplements hi = (HandlerImplements)defn;
			if (nargs == 0 && hi.argCount() == 0) {
				List<JSExpr> args = new ArrayList<JSExpr>();
				args.add(state.container(hi.getParent().name()));
				sv.result(block.createObject(defn.name(), args));
			} else if (nargs > 0) {
				sv.result(block.pushConstructor(defn.name(), myName));
			} else {
				sv.result(block.curry(false, hi.argCount(), block.pushConstructor(defn.name(), myName)));
			}
		} else if (defn instanceof VarPattern) {
			sv.result(block.boundVar(((VarPattern)defn).var));
		} else if (defn instanceof TypedPattern) {
			sv.result(block.boundVar(((TypedPattern)defn).var.var));
		} else if (defn instanceof HandlerLambda) {
			sv.result(block.lambda((HandlerLambda)defn));
		} else if (defn instanceof StructField) {
			StructField sf = (StructField)defn;
			sv.result(block.loadField(state.container(sf.container.name()), sf.name));
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
			RequiresContract rc = (RequiresContract)defn;
			sv.result(block.contractByVar(state.container(rc.getParent().name()), rc.referAsVar));
		} else if (defn instanceof ObjectContract) {
			ObjectContract oc = (ObjectContract)defn;
			sv.result(block.member(oc.implementsType().defn().name(), oc.varName().var));
		} else if (defn instanceof TupleMember) {
			makeFunctionClosure(false, ((TupleMember)defn).name(), myName, 0);
		} else if (defn instanceof UnitDataDeclaration) {
			handleUnitTestData((UnitDataDeclaration) defn);
		} else if (defn instanceof IntroduceVar) {
			sv.result(block.fromIntroduction(state.resolveIntroduction((IntroduceVar) defn)));
		} else if (defn instanceof ObjectCtor) {
			ObjectCtor oc = (ObjectCtor) defn;
			int expArgs = oc.argCountIncludingContracts();
			JSExpr fn = block.callStatic(oc.name(), expArgs + 1);
			if (nargs == 0) {
				JSExpr call;
				if (expArgs > 0) {
					call = block.curry(false, expArgs + 1, new JSExpr[] { fn });
				} else {
					call = block.closure(false, new JSExpr[] { fn, state.container(new PackageName("_DisplayUpdater")) });
				}
				sv.result(call);
			} else
				sv.result(fn);
		} else
			throw new NotImplementedException("cannot generate fn for " + defn);
	}

	private void handleUnitTestData(UnitDataDeclaration udd) {
		sv.result(state.resolveMock(block, udd));
	}

	private void makeFunctionClosure(boolean hasState, FunctionName fnName, String func, int expArgs) {
		JSExpr[] args;
		if (hasState)
			args = new JSExpr[] { block.pushFunction(func, fnName, expArgs), new JSThis() };
		else
			args = new JSExpr[] { block.pushFunction(func, fnName, expArgs) };
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
		} else if (defn instanceof LogicHolder && ((LogicHolder)defn).hasState())
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
		case ">=":
			return "FLBuiltin.greaterEqual";
		case ">":
			return "FLBuiltin.greaterThan";
		case "<=":
			return "FLBuiltin.lessEqual";
		case "<":
			return "FLBuiltin.lessThan";
		case "+":
			return "FLBuiltin.plus";
		case "-":
			if (nargs == 2)
				return "FLBuiltin.minus";
			else
				return "FLBuiltin.unaryMinus";
		case "*":
			return "FLBuiltin.mul";
		case "/":
			return "FLBuiltin.div";
		case "%":
			return "FLBuiltin.mod";
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
		case "{}":
		{
			return "MakeHash";
		}
		case ":":
		{
			return "FLBuiltin.hashPair";
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
