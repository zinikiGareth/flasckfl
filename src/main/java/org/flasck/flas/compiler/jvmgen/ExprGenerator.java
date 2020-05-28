package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
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
	private final boolean isExpectation;

	public ExprGenerator(FunctionState state, NestedVisitor sv, List<IExpr> currentBlock, boolean isExpectation) {
		this.state = state;
		this.sv = sv;
		this.currentBlock = currentBlock;
		this.isExpectation = isExpectation;
		this.meth = state.meth;
		this.fcx = state.fcx;
		sv.push(this);
	}

	@Override
	public void visitCheckTypeExpr(CheckTypeExpr expr) {
		new CheckTypeGenerator(state, sv, currentBlock, isExpectation);
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
	public void visitMakeSend(MakeSend expr) {
		new ApplyExprGenerator(state, sv, currentBlock);
	}
	
	@Override
	public void visitMakeAcor(MakeAcor expr) {
		new ApplyExprGenerator(state, sv, currentBlock);
	}
	
	@Override
	public void visitCurrentContainer(CurrentContainer expr) {
		sv.result(state.stateObj);
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
	public void visitAnonymousVar(AnonymousVar var) {
		if (isExpectation) {
			sv.result(meth.makeNew(J.BOUNDVAR));
		} else 
			sv.result(new JVMCurryArg());
	}
	
	@Override
	public void visitIntroduceVar(IntroduceVar var) {
		IExpr ret = meth.makeNew(J.BOUNDVAR);
		if (var != null) {
			Var v = meth.avar(J.BOUNDVAR, state.nextVar("v"));
			currentBlock.add(meth.assign(v, ret));
			ret = v;
			state.addIntroduction(var, v);
		}
		sv.result(ret);
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
			FunctionDefinition fd = (FunctionDefinition) defn;
			if (nargs == 0) {
				FunctionDefinition fn = (FunctionDefinition) defn;
				makeFunctionClosure(fn.hasState(), fn.name(), fn.argCount());
			} else if ("MakeTuple".equals(myName)) {
				sv.result(null);
			} else {
				if (fd.hasState())
					sv.result(meth.makeNew(J.CALLMETHOD, meth.classConst(fd.name().inContext.javaName()), meth.stringConst(fd.name().name), meth.intConst(fd.argCount())));
				else
					sv.result(meth.makeNew(J.CALLEVAL, meth.classConst(myName)));
			}
		} else if (defn instanceof StandaloneMethod) {
			StandaloneMethod fn = (StandaloneMethod) defn;
			if (nargs == 0) {
				makeFunctionClosure(false, fn.name(), fn.argCount());
			} else if (fn.om.hasState())
				sv.result(meth.makeNew(J.CALLMETHOD, meth.classConst(fn.name().inContext.javaName()), meth.stringConst(fn.name().name), meth.intConst(fn.argCount())));
			else
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
		} else if (defn instanceof HandlerImplements) {
			// if the constructor has no args, eval it here
			// otherwise leave it until "leaveExpr" or "leaveFunction"
			HandlerImplements hi = (HandlerImplements)defn;
			if (nargs == 0 && hi.argCount() == 0) {
				List<IExpr> provided = new ArrayList<>();
				if (hi.getParent() != null) {
					provided.add(state.container);
				}
				IExpr args = meth.arrayOf(J.OBJECT, provided);
				sv.result(meth.callStatic(myName, J.OBJECT, "eval", fcx, args));
			} else if (nargs > 0) {
				sv.result(meth.makeNew(J.CALLEVAL, meth.classConst(myName)));
			} else {
				IExpr call = meth.callInterface(J.FLCURRY, fcx, "curry", meth.intConst(hi.argCount()), meth.as(meth.classConst(myName), J.APPLICABLE), meth.arrayOf(J.OBJECT));
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
		} else if (defn instanceof HandlerLambda) {
			String v = ((TypedPattern)((HandlerLambda)defn).patt).var.var;
			sv.result(meth.getField(v));
		} else if (defn instanceof StructField) {
			if (state.stateObj == null)
				throw new NotImplementedException("stateObj has not been bound");
			StructField sf = (StructField) defn;
			IExpr ret = meth.callInterface(J.OBJECT, state.stateObj, "get", meth.stringConst(sf.name));
			sv.result(ret);
		} else if (defn instanceof TemplateNestedField) {
			if (state.templateObj == null)
				throw new NotImplementedException("templateObj has not been bound");
			TemplateNestedField tnf = (TemplateNestedField) defn;
			StructField sf = tnf.getField();
			IExpr from = state.templateObj.get(tnf.name().var);
			if (sf != null) {
				sv.result(meth.callInterface(J.OBJECT, from, "get", meth.stringConst(sf.name)));
			} else {
				sv.result(from);
			}
		} else if (defn instanceof RequiresContract) {
			RequiresContract rc = (RequiresContract) defn;
			IExpr ret = meth.callInterface(J.OBJECT, meth.as(state.container, J.CONTRACT_RETRIEVER), "require", state.fcx, meth.stringConst(rc.referAsVar));
			sv.result(ret);
		} else if (defn instanceof ObjectContract) {
			ObjectContract rc = (ObjectContract) defn;
			IExpr ret = meth.getField(state.container, rc.varName().var);
			sv.result(ret);
		} else if (defn instanceof TupleMember) {
			makeFunctionClosure(false, ((TupleMember) defn).name(), 0);
		} else if (defn instanceof UnitDataDeclaration) {
			handleUnitTestData((UnitDataDeclaration) defn);
		} else if (defn instanceof IntroduceVar) {
			handleIntroduction(state.resolveIntroduction((IntroduceVar)defn));
		} else if (defn instanceof ObjectCtor) {
			ObjectCtor oc = (ObjectCtor) defn;
			IExpr fn = meth.makeNew(J.CALLSTATIC, meth.classConst(oc.name().container().javaName()), meth.stringConst(oc.name().name), meth.intConst(nargs));
			if (nargs == 0) {
				Var v = makeClosure(false, fn, oc.argCountIncludingContracts());
				sv.result(v);
			} else
				sv.result(fn);
		} else
			throw new NotImplementedException("cannot evaluate " + defn.getClass());
	}

	private void handleUnitTestData(UnitDataDeclaration udd) {
		sv.result(state.resolveMock(udd));
	}
	
	private void handleIntroduction(IExpr intr) {
		sv.result(meth.callVirtual(J.OBJECT, intr, "introduced"));
	}

	private void makeFunctionClosure(boolean hasState, FunctionName name, int expArgs) {
		IExpr fn;
		if (hasState)
			fn = meth.makeNew(J.CALLMETHOD, meth.classConst(name.inContext.javaName()), meth.stringConst(name.name), meth.intConst(expArgs));
		else
			fn = meth.makeNew(J.CALLEVAL, meth.classConst(name.javaClassName()));
		Var v = makeClosure(hasState, fn, expArgs);
		sv.result(v);
	}

	private Var makeClosure(boolean hasState, IExpr fn, int expArgs) {
		ArrayList<IExpr> iargs = new ArrayList<IExpr>();
		if (this.state.container != null)
			iargs.add(this.state.container);
		IExpr args = meth.arrayOf(J.OBJECT, iargs);
		IExpr call;
		if (expArgs > 0) {
			if (hasState)
				call = meth.callInterface(J.FLCURRY, fcx, "ocurry", meth.intConst(expArgs), meth.as(fn, J.APPLICABLE), args);
			else
				call = meth.callInterface(J.FLCURRY, fcx, "curry", meth.intConst(expArgs), meth.as(fn, J.APPLICABLE), args);
		} else {
			if (hasState)
				call = meth.callInterface(J.FLCLOSURE, fcx, "oclosure", meth.as(fn, J.APPLICABLE), args);
			else
				call = meth.callInterface(J.FLCLOSURE, fcx, "closure", meth.as(fn, J.APPLICABLE), args);
		}
		Var v = meth.avar(J.FLCLOSURE, state.nextVar("v"));
		currentBlock.add(meth.assign(v, call));
		return v;
	}

	private String resolveOpName(String op) {
		String inner;
		switch (op) {
		case "&&":
			inner = "And";
			break;
		case "||":
			inner = "Or";
			break;
		case "!":
			inner = "Not";
			break;
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
