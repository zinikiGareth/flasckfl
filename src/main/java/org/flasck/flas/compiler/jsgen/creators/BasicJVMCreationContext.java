package org.flasck.flas.compiler.jsgen.creators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSLiteral;
import org.flasck.flas.compiler.jsgen.form.JSLocal;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.NotImplementedException;

public class BasicJVMCreationContext implements JVMCreationContext {
	private final ByteCodeSink bcc;
	private final NewMethodDefiner md;
	private final boolean isTest;
	private final Var runner;
	private final Var cxt;
	private final Var args;
	private final Map<JSLocal, Var> vars = new HashMap<>();
	private final Map<JSExpr, IExpr> stack = new HashMap<>();
	
	public BasicJVMCreationContext(ByteCodeEnvironment bce, NameOfThing fnName, boolean isStatic, int ac) {
		if (ac == -420)
			throw new NotImplementedException();
		if (fnName instanceof UnitTestName) {
			isTest = true;
			bcc = bce.newClass(fnName.javaName());
			GenericAnnotator ann = GenericAnnotator.newMethod(bcc, true, "dotest");
			PendingVar r1 = ann.argument(J.TESTHELPER, "runner");
			PendingVar c1 = ann.argument(J.FLEVALCONTEXT, "cxt");
			ann.returns(JavaType.void_);
			md = ann.done();
			cxt = c1.getVar();
			args = null;
			runner = r1.getVar();
		} else if (!isStatic) {
			throw new NotImplementedException();
		} else {
			isTest = false;
			bcc = bce.newClass(fnName.javaClassName());
			bcc.generateAssociatedSourceFile();
			IFieldInfo fi = bcc.defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs");
			fi.constValue(ac);
			GenericAnnotator ann = GenericAnnotator.newMethod(bcc, true, "eval");
			PendingVar c1 = ann.argument(J.FLEVALCONTEXT, "cxt");
			PendingVar a1 = ann.argument("[" + J.OBJECT, "args");
			ann.returns(J.OBJECT);
			md = ann.done();
			cxt = c1.getVar();
			args = a1.getVar();
			runner = null;
		}
		md.lenientMode(true);
	}

	@Override
	public NewMethodDefiner method() {
		return md;
	}

	@Override
	public IExpr helper() {
		return runner;
	}

	@Override
	public IExpr cxt() {
		return cxt;
	}

	@Override
	public void bind(JSExpr key, Slot slot) {
		if (slot instanceof ArgSlot) {
			ArgSlot as = (ArgSlot) slot;
			int pos = as.argpos();
			
		} else {
			throw new NotImplementedException("ctor slots");
		}
	}

	@Override
	public void assignTo(JSLocal local, JSExpr value) {
		if (!stack.containsKey(value))
			throw new NotImplementedException("there is no value for " + value.getClass() + " " + value);
		Var v = md.avar(J.OBJECT, local.asVar());
		md.assign(v, stack.get(value)).flush();
		vars.put(local, v);
	}

	@Override
	public void pushFunction(JSExpr key, FunctionName fn) {
		String push = null;
		if (fn.inContext == null)
			push = resolveOpName(fn.name);
		if (push == null)
			push = fn.javaClassName();
		System.out.println("pushing fn name " + push);
		stack.put(key, md.makeNew(J.CALLEVAL, md.classConst(push)));
	}

	@Override
	public IExpr arg(JSExpr jsExpr) {
		return md.as(argAsIs(jsExpr), J.OBJECT);
	}

	private IExpr argAsIs(JSExpr jsExpr) {
		if (vars.containsKey(jsExpr))
			return vars.get(jsExpr);
		else if (stack.containsKey(jsExpr))
			return stack.get(jsExpr);
		else if (jsExpr instanceof JSLiteral) {
			JSLiteral l = (JSLiteral) jsExpr;
			try {
				int x = Integer.parseInt(l.asVar());
				return md.makeNew(J.NUMBER, md.box(md.intConst(x)), md.castTo(md.aNull(), "java.lang.Double"));
			} catch (NumberFormatException ex) {
				throw new NotImplementedException("non-integer cases");
			}
		} else if (jsExpr instanceof JSString) {
			JSString l = (JSString) jsExpr;
			return md.stringConst(l.asVar());
		} else
			throw new NotImplementedException("there is no var for " + jsExpr.getClass() + " " + jsExpr.asVar());
	}

	@Override
	public void closure(JSExpr key, boolean wantObject, JSExpr[] args) {
		IExpr fn = null;
		IExpr[] stack = new IExpr[args.length-1];
		for (int i=0;i<args.length;i++) {
			System.out.println("clos arg " + args[i]);
			if (i == 0)
				fn = arg(args[i]);
			else
				stack[i-1] = arg(args[i]);
		}
		IExpr as = md.arrayOf(J.OBJECT, stack);

		IExpr call;
		if (wantObject)
			call = md.callInterface(J.FLCLOSURE, cxt, "oclosure", md.as(fn, J.APPLICABLE), as);
		else
			call = md.callInterface(J.FLCLOSURE, cxt, "closure", md.as(fn, J.APPLICABLE), as);
		this.stack.put(key, call);
	}

	@Override
	public void eval(JSExpr key, String clz, List<JSExpr> args) {
		stack.put(key, md.aNull());
	}

	@Override
	public void returnExpr(JSExpr jsExpr) {
		md.returnObject(arg(jsExpr)).flush();
	}

	@Override
	public void done() {
		if (isTest) {
			md.callInterface("void", runner, "testComplete").flush();
			md.returnVoid().flush();
		}
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
		case ">=":
			inner = "GreaterEqual";
			break;
		case ">":
			inner = "GreaterThan";
			break;
		case "<=":
			inner = "LessEqual";
			break;
		case "<":
			inner = "LessThan";
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
		case "%":
			inner = "Mod";
			break;
		case "++":
			inner = "strAppend";
			break;
		case "[]":
			return J.NIL;
		case "()":
			return "MakeTuple";
		default:
			return null;
		}
		return J.FLEVAL + "$" + inner;
	}
}
