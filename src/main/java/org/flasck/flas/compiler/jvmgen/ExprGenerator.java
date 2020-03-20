package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.CurrentContainer;
import org.flasck.flas.parsedForm.CurryArgument;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
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
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.Var.AVar;
import org.zinutils.exceptions.NotImplementedException;

public class ExprGenerator extends LeafAdapter implements ResultAware {
	private final FunctionState state;
	private final NestedVisitor sv;
	private final MethodDefiner meth;
	private final IExpr fcx;
	private final List<IExpr> currentBlock;

	public ExprGenerator(FunctionState state, NestedVisitor sv, List<IExpr> currentBlock) {
		this.state = state;
		this.sv = sv;
		this.currentBlock = currentBlock;
		this.meth = state.meth;
		this.fcx = state.fcx;
		sv.push(this);
	}

	@Override
	public void visitApplyExpr(ApplyExpr expr) {
		new ApplyExprGenerator(state, sv, currentBlock);
	}
	
	@Override
	public void visitMessages(Messages msgs) {
		new ApplyExprGenerator(state, sv, currentBlock);
	}
	
	@Override
	public void visitMakeAcor(MakeAcor expr) {
		new ApplyExprGenerator(state, sv, currentBlock);
	}
	
	@Override
	public void visitCurrentContainer(CurrentContainer expr) {
		sv.result(state.container);
	}
	
	@Override
	public void visitNumericLiteral(NumericLiteral expr) {
		Object val = expr.value();
		if (val instanceof Integer)
			sv.result(meth.makeNew(J.NUMBER, meth.box(meth.intConst((int) val)), meth.castTo(meth.aNull(), "java.lang.Double")));
		else
			throw new NotImplementedException();
	}
	
	@Override
	public void visitStringLiteral(StringLiteral expr) {
		sv.result(meth.stringConst(expr.text));
	}
	
	// I think at the moment I am mixing up three completely separate cases here
	// Basically this is just "leaveApplyExpr" with no args.
	// It is OK to call eval directly if we know if will complete quickly, i.e. it's a constructor
	// But if it is a regular var - i.e. a function of 0 args, it could be arbitrarily complex and should be a closure
	// And if it is the "first" token of an ApplyExpr, we need to just push "it" without eval or closure ...
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		RepositoryEntry defn = var.defn();
		if (defn == null)
			throw new RuntimeException("var " + var + " was still not resolved");
		generateFnOrCtor(defn, defn.name().javaClassName(), nargs);
	}
	
	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator, int nargs) {
		RepositoryEntry defn = operator.defn();
		if (defn == null)
			throw new RuntimeException("var " + operator + " was still not resolved");
		generateFnOrCtor(defn, resolveOpName(operator.op), nargs);
	}

	private void generateFnOrCtor(RepositoryEntry defn, String myName, int nargs) {
		if (defn instanceof FunctionDefinition) {
			if (nargs == 0) {
				FunctionDefinition fn = (FunctionDefinition) defn;
				makeFunctionClosure(myName, fn.argCount());
			} else if ("MakeTuple".equals(myName)) {
				sv.result(null);
			} else
				sv.result(meth.makeNew(J.CALLEVAL, meth.classConst(myName)));
		} else if (defn instanceof StandaloneMethod) {
			if (nargs == 0) {
				StandaloneMethod fn = (StandaloneMethod) defn;
				makeFunctionClosure(myName, fn.argCount());
			} else
				sv.result(meth.makeNew(J.CALLEVAL, meth.classConst(myName)));
		} else if (defn instanceof StructDefn) {
			// if the constructor has no args, eval it here
			// otherwise leave it until "leaveExpr" or "leaveFunction"
			StructDefn sd = (StructDefn)defn;
			if (nargs == 0 && sd.argCount() == 0) {
				List<IExpr> provided = new ArrayList<>();
				IExpr args = meth.arrayOf(J.OBJECT, provided);
				sv.result(meth.callStatic(myName, J.OBJECT, "eval", fcx, args));
			} else if (myName.equals(J.NIL) || sd.argCount() == nargs) {
				sv.result(null);
			} else if (nargs > 0) {
				sv.result(meth.classConst(myName));
			} else {
				IExpr call = meth.callInterface(J.FLCURRY, fcx, "curry", meth.intConst(sd.argCount()), meth.as(meth.classConst(myName), J.APPLICABLE), meth.arrayOf(J.OBJECT));
				Var v = meth.avar(J.FLCLOSURE, state.nextVar("v"));
				currentBlock.add(meth.assign(v, call));
				sv.result(v);
			}
		} else if (defn instanceof VarPattern) {
			String v = ((VarPattern)defn).var;
			AVar var = state.boundVar(v);
			if (var == null)
				throw new RuntimeException("Could not find " + v);
			sv.result(var);
		} else if (defn instanceof TypedPattern) {
			String v = ((TypedPattern)defn).var.var;
			AVar var = state.boundVar(v);
			if (var == null)
				throw new RuntimeException("Could not find " + v);
			sv.result(var);
		} else if (defn instanceof StructField) {
			StructField sf = (StructField) defn;
			IExpr ret = meth.callInterface(J.OBJECT, meth.getField("state"), "get", meth.stringConst(sf.name));
			sv.result(ret);
		} else if (defn instanceof TupleMember) {
			makeFunctionClosure(myName, 0);
		} else if (defn instanceof CurryArgument) {
			sv.result(new JVMCurryArg());
		} else if (defn instanceof UnitDataDeclaration) {
			handleUnitTestData((UnitDataDeclaration) defn);
		} else
			throw new NotImplementedException();
	}

	private void handleUnitTestData(UnitDataDeclaration udd) {
		sv.result(state.resolveMock(udd));
	}

	private void makeFunctionClosure(String name, int expArgs) {
		IExpr fn = meth.makeNew(J.CALLEVAL, meth.classConst(name));
		IExpr args = meth.arrayOf(J.OBJECT, new ArrayList<IExpr>());
		IExpr call;
		if (expArgs > 0)
			call = meth.callInterface(J.FLCURRY, fcx, "curry", meth.intConst(expArgs), meth.as(fn, J.APPLICABLE), args);
		else
			call = meth.callInterface(J.FLCLOSURE, fcx, "closure", meth.as(fn, J.APPLICABLE), args);
		Var v = meth.avar(J.FLCLOSURE, state.nextVar("v"));
		currentBlock.add(meth.assign(v, call));
		sv.result(v);
	}

	private String resolveOpName(String op) {
		String inner;
		switch (op) {
		case "==":
			inner = "IsEqual";
			break;
		case "+":
			inner = "Plus";
			break;
		case "-":
			inner = "Minus";
			break;
		case "*":
			inner = "Mul";
			break;
		case "/":
			inner = "Div";
			break;
		case "++":
			inner = "strAppend";
			break;
		case "[]":
			return J.NIL;
		case "()":
			return "MakeTuple";
		default:
			throw new RuntimeException("There is no operator " + op);
		}
		return J.FLEVAL + "$" + inner;
	}

	@Override
	public void result(Object r) {
		sv.result((IExpr) r);
	}
}
