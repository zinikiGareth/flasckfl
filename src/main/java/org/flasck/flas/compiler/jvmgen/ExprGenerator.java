package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.CurryArgument;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.WithTypeSignature;
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

public class ExprGenerator extends LeafAdapter implements HSIVisitor, ResultAware {
	public class XCArg {
		public final int arg;
		public final IExpr expr;

		public XCArg(int arg, IExpr expr) {
			this.arg = arg;
			this.expr = expr;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof XCArg))
				return false;
			XCArg o = (XCArg) obj;
			return o.arg == arg && o.expr == expr;
		}
		
		@Override
		public int hashCode() {
			return arg ^ expr.hashCode();
		}
		
		@Override
		public String toString() {
			return arg + ":" + expr;
		}
	}

	public class JVMCurryArg implements IExpr {
		@Override
		public void spitOutByteCode(MethodDefiner meth) {
			throw new NotImplementedException();
		}

		@Override
		public void flush() {
			throw new NotImplementedException();
		}

		@Override
		public String getType() {
			throw new NotImplementedException();
		}

		@Override
		public int asSource(StringBuilder sb, int ind, boolean b) {
			throw new NotImplementedException();
		}
	}

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
	public void leaveGuard(FunctionCaseDefn c) {
		if (stack.size() != 1)
			throw new RuntimeException("I think this is impossible, but obviously not");
		sv.result(stack.remove(0));
	}

	@Override
	public void leaveCase(FunctionCaseDefn c) {
		if (stack.size() != 1)
			throw new RuntimeException("I think this is impossible, but obviously not");
		sv.result(meth.returnObject(stack.remove(0)));
	}

	@Override
	public void endInline(FunctionIntro fi) {
		if (stack.size() != 1)
			throw new RuntimeException("I think this is impossible, but obviously not");
		sv.result(meth.returnObject(stack.remove(0)));
	}
	
	@Override
	public void visitApplyExpr(ApplyExpr expr) {
		sv.push(new ExprGenerator(state, sv, currentBlock));
	}
	
	@Override
	public void visitMessages(Messages msgs) {
		sv.push(new ExprGenerator(state, sv, currentBlock));
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
				makeClosure(fn, 0, fn.argCount());
			} else
				stack.add(meth.classConst(myName));
		} else if (defn instanceof StandaloneMethod) {
			if (nargs == 0) {
				StandaloneMethod fn = (StandaloneMethod) defn;
				stack.add(meth.classConst(myName));
				makeClosure(fn, 0, fn.argCount());
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
			String v = ((VarPattern)defn).var;
			AVar var = state.boundVar(v);
			if (var == null)
				throw new RuntimeException("Could not find " + v);
			stack.add(var);
		} else if (defn instanceof TypedPattern) {
			IExpr in = meth.arrayItem(J.OBJECT, state.fargs, 0);
			AVar var = new Var.AVar(meth, J.OBJECT, "head_0");
			currentBlock.add(meth.assign(var, meth.callStatic(J.FLEVAL, J.OBJECT, "head", fcx, in)));
			stack.add(var);
		} else if (defn instanceof CurryArgument) {
			stack.add(new JVMCurryArg());
		} else if (defn instanceof UnitDataDeclaration) {
			handleUnitTestData((UnitDataDeclaration) defn);
		} else
			throw new NotImplementedException();
	}

	private void handleUnitTestData(UnitDataDeclaration udd) {
		stack.add(state.resolveMock(udd));
	}

	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		if (!expr.args.isEmpty()) {
			Object fn = expr.fn;
			int expArgs = 0;
			WithTypeSignature defn = null;
			if (fn instanceof UnresolvedVar) {
				defn = (WithTypeSignature) ((UnresolvedVar)fn).defn();
				expArgs = ((WithTypeSignature)defn).argCount();
			} else if (fn instanceof UnresolvedOperator) {
				UnresolvedOperator op = (UnresolvedOperator) fn;
				defn = (WithTypeSignature) op.defn();
				expArgs = ((WithTypeSignature)defn).argCount();
			} else if (fn instanceof MakeSend) {
				defn = (MakeSend) fn;
				expArgs = defn.argCount();
			} else
				throw new NotImplementedException("Cannot handle " + fn.getClass());
			makeClosure(defn, expr.args.size(), expArgs);
		}
		if (stack.size() != 1)
			throw new NotImplementedException();
		sv.result(stack.remove(0));
	}

	@Override
	public void leaveMessages(Messages msgs) {
		List<IExpr> provided = new ArrayList<IExpr>();
		int k = stack.size();
		for (int i=0;i<k;i++)
			provided.add(stack.remove(0));
		IExpr args = meth.arrayOf(J.OBJECT, provided);
		IExpr call = meth.callStatic(J.FLEVAL, J.OBJECT, "makeArray", fcx, args);
		Var v = meth.avar(J.FLCLOSURE, state.nextVar("v"));
		currentBlock.add(meth.assign(v, call));
		sv.result(v);
	}
	
	private void makeClosure(WithTypeSignature defn, int depth, int expArgs) {
		List<IExpr> provided = new ArrayList<IExpr>();
		int k = stack.size()-depth;
		for (int i=0;i<depth;i++)
			provided.add(stack.remove(k));
		IExpr args = meth.arrayOf(J.OBJECT, provided);
		if (defn instanceof StructDefn && defn.name().uniqueName().equals("Nil")) {
			IExpr call = meth.callStatic(J.FLEVAL, J.OBJECT, "makeArray", fcx, args);
			Var v = meth.avar(J.FLCLOSURE, state.nextVar("v"));
			currentBlock.add(meth.assign(v, call));
			stack.add(v);
		} else if (defn instanceof StructDefn && !provided.isEmpty()) {
			// do the creation immediately
			// Note that we didn't push anything onto the stack earlier ...
			// TODO: I think we need to cover the currying case separately ...
			IExpr ctor = meth.callStatic(defn.name().javaClassName(), J.OBJECT, "eval", fcx, args);
			stack.add(ctor);
		} else {
			IExpr fn = stack.remove(stack.size()-1);
			List<XCArg> xcs = checkExtendedCurry(provided);
			IExpr call;
			if (xcs != null) {
				call = meth.callStatic(J.FLCLOSURE, J.FLCURRY, "xcurry", meth.as(fn, "java.lang.Object"), meth.intConst(expArgs), meth.arrayOf(J.OBJECT, asjvm(xcs)));
			} else if (depth < expArgs)
				call = meth.callStatic(J.FLCLOSURE, J.FLCURRY, "curry", meth.as(fn, "java.lang.Object"), meth.intConst(expArgs), args);
			else
				call = meth.callStatic(J.FLCLOSURE, J.FLCLOSURE, "simple", meth.as(fn, "java.lang.Object"), args);
			Var v = meth.avar(J.FLCLOSURE, state.nextVar("v"));
			currentBlock.add(meth.assign(v, call));
			stack.add(v);
		}
	}

	@Override
	public void visitMakeSend(MakeSend expr) {
		IExpr obj = stack.remove(stack.size()-1);
		IExpr mksend = meth.callInterface(J.OBJECT, fcx, "mksend", meth.classConst(expr.sendMeth.inContext.javaClassName()), meth.stringConst(expr.sendMeth.name), obj, meth.intConst(expr.nargs));
		stack.add(mksend);
	}
	
	private List<XCArg> checkExtendedCurry(List<IExpr> provided) {
		List<XCArg> ret = new ArrayList<>();
		boolean needed = false;
		int i=0;
		for (IExpr e : provided) {
			if ((e instanceof JVMCurryArg))
				needed = true;
			else
				ret.add(new XCArg(i, e));
			i++;
		}
		if (!needed)
			return null;
		else
			return ret;
	}

	private List<IExpr> asjvm(List<XCArg> xcs) {
		List<IExpr> ret = new ArrayList<>();
		for (XCArg a : xcs) {
			ret.add(meth.box(meth.intConst(a.arg)));
			ret.add(a.expr);
		}
		return ret;
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
			inner = "Concat";
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
		throw new NotImplementedException();
	}

	@Override
	public void endSwitch() {
	}

	@Override
	public void result(Object r) {
		stack.add((IExpr) r);
	}
}
