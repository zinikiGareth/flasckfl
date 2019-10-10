package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
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
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.Var.AVar;
import org.zinutils.exceptions.NotImplementedException;

public class ExprGenerator extends LeafAdapter implements HSIVisitor {
	private final FunctionState state;
	private final NestedVisitor sv;
	private final MethodDefiner meth;
	private final IExpr fcx;
	private final List<IExpr> stack = new ArrayList<IExpr>();
	private final List<IExpr> currentBlock;

	public ExprGenerator(FunctionState state, NestedVisitor sv, List<IExpr> currentBlock) {
		this.state = state;
		this.sv = sv;
		this.currentBlock = currentBlock;
		this.meth = state.meth;
		this.fcx = state.fcx;
	}

	@Override
	public boolean isHsi() {
		return true;
	}
	
	@Override
	public void endInline(FunctionIntro fi) {
		if (stack.size() != 1)
			throw new RuntimeException("I think this is impossible, but obviously not");
		sv.result(meth.returnObject(stack.remove(0)));
	}

	@Override
	public void visitNumericLiteral(NumericLiteral expr) {
		Object val = expr.value();
		if (val instanceof Integer)
			stack.add(meth.makeNew(J.NUMBER, meth.box(meth.intConst((int) val)), meth.castTo(meth.aNull(), "java.lang.Double")));
		else
			throw new NotImplementedException();
	}
	
	@Override
	public void visitStringLiteral(StringLiteral expr) {
		stack.add(meth.stringConst(expr.text));
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
				stack.add(meth.classConst(myName));
				makeClosure(defn, 0, fn.argCount());
			} else
				stack.add(meth.classConst(myName));
		} else if (defn instanceof StructDefn) {
			// if the constructor has no args, eval it here
			// otherwise leave it until "leaveExpr" or "leaveFunction"
			if (nargs == 0 && ((StructDefn)defn).argCount() == 0) {
				List<IExpr> provided = new ArrayList<>();
				IExpr args = meth.arrayOf(J.OBJECT, provided);
				stack.add(meth.callStatic(myName, J.OBJECT, "eval", fcx, args));
			}
		} else if (defn instanceof VarPattern) {
			IExpr in = meth.arrayItem(J.OBJECT, state.fargs, 0);
			AVar var = new Var.AVar(meth, J.OBJECT, "head_0");
			currentBlock.add(meth.assign(var, meth.callStatic(J.FLEVAL, J.OBJECT, "head", fcx, in)));
			stack.add(var);
		} else if (defn instanceof TypedPattern) {
			IExpr in = meth.arrayItem(J.OBJECT, state.fargs, 0);
			AVar var = new Var.AVar(meth, J.OBJECT, "head_0");
			currentBlock.add(meth.assign(var, meth.callStatic(J.FLEVAL, J.OBJECT, "head", fcx, in)));
			stack.add(var);
		} else
			throw new NotImplementedException();
	}

	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		Object fn = expr.fn;
		int expArgs = 0;
		RepositoryEntry defn = null;
		if (fn instanceof UnresolvedVar) {
			defn = ((UnresolvedVar)fn).defn();
			expArgs = ((WithTypeSignature)defn).argCount();
		} else if (fn instanceof UnresolvedOperator) {
			UnresolvedOperator op = (UnresolvedOperator) fn;
			defn = op.defn();
			expArgs = op.argCount();
		}
		if (expr.args.isEmpty()) // then it's a spurious apply
			return;
		makeClosure(defn, expr.args.size(), expArgs);
	}

	private void makeClosure(RepositoryEntry defn, int depth, int expArgs) {
		List<IExpr> provided = new ArrayList<IExpr>();
		int k = stack.size()-depth;
		for (int i=0;i<depth;i++)
			provided.add(stack.remove(k));
		IExpr args = meth.arrayOf(J.OBJECT, provided);
		if (defn.name().uniqueName().equals("Nil")) {
			stack.add(meth.callStatic(J.FLEVAL, J.OBJECT, "makeArray", fcx, args));
		} else if (defn instanceof StructDefn && !provided.isEmpty()) {
			// do the creation immediately
			// Note that we didn't push anything onto the stack earlier ...
			// TODO: I think we need to cover the currying case separately ...
			IExpr ctor = meth.callStatic(defn.name().javaClassName(), J.OBJECT, "eval", fcx, args);
			stack.add(ctor);
		} else {
			IExpr fn = stack.remove(stack.size()-1);
			IExpr call;
			if (depth < expArgs)
				call = meth.callStatic(J.FLCLOSURE, J.FLCLOSURE, "curry", meth.as(fn, "java.lang.Object"), meth.intConst(expArgs), args);
			else
				call = meth.callStatic(J.FLCLOSURE, J.FLCLOSURE, "simple", meth.as(fn, "java.lang.Object"), args);
			Var v = meth.avar(J.FLCLOSURE, state.nextVar("v"));
			currentBlock.add(meth.assign(v, call));
			stack.add(v);
		}
	}

	@Override
	public void leaveAssertExpr(boolean isValue, Expr e) {
		if (stack.size() != 1)
			throw new RuntimeException("I think this should be impossible, but obviously not");
		sv.result(stack.get(0));
	}
	
	private String resolveOpName(String op) {
		String inner;
		switch (op) {
		case "+":
			inner = "Plus";
			break;
		case "*":
			inner = "Mul";
			break;
		case "[]":
			return J.NIL;
		default:
			throw new RuntimeException("There is no operator " + op);
		}
		return J.FLEVAL + "$" + inner;
	}

	@Override
	public void hsiArgs(List<Slot> slots) {
	}

	@Override
	public void switchOn(Slot slot) {
	}

	@Override
	public void withConstructor(String string) {
	}

	@Override
	public void constructorField(Slot parent, String field, Slot slot) {
	}

	@Override
	public void matchNumber(int i) {
	}

	@Override
	public void matchString(String s) {
	}

	@Override
	public void matchDefault() {
	}

	@Override
	public void defaultCase() {
	}

	@Override
	public void errorNoCase() {
	}

	@Override
	public void bind(Slot slot, String var) {
	}

	@Override
	public void endSwitch() {
	}
}
