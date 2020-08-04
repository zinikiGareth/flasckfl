package org.flasck.flas.compiler.jsgen.creators;

import java.util.HashMap;
import java.util.Map;

import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSLocal;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.NotImplementedException;

public class BasicJVMCreationContext implements JVMCreationContext {
	ByteCodeSink bcc;
	NewMethodDefiner md;
	boolean isTest;
	Var runner;
	private Var cxt;
	private JSLocal assignNextTo;
	private final Map<JSLocal, Var> vars = new HashMap<>();
	
	public BasicJVMCreationContext(ByteCodeEnvironment bce, String pkg, String name, boolean isStatic) {
		if (!isStatic) {
			throw new NotImplementedException();
		} else {
			String clz;
			String fn;
			if (name.startsWith("_ut")) {
				clz = pkg + "." + name;
				fn = "dotest";
				isTest = true;
			} else {
				clz = pkg + ".PACKAGEFUNCTIONS";
				fn = name;
				isTest = false;
			}
			if (bce.hasClass(clz))
				bcc = bce.get(clz);
			else
				bcc = bce.newClass(clz);
			GenericAnnotator ann = GenericAnnotator.newMethod(bcc, isStatic, fn);
			if (isTest) {
				PendingVar r1 = ann.argument(J.TESTHELPER, "runner");
				PendingVar c1 = ann.argument(J.FLEVALCONTEXT, "cxt");
				ann.returns(JavaType.void_);
				md = ann.done();
				cxt = c1.getVar();
				runner = r1.getVar();
			} else {
				ann.returns(J.OBJECT);
				md = ann.done();
				cxt = null;
				runner = null;
			}
		}
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
	public void assignTo(JSLocal jsLocal) {
		this.assignNextTo = jsLocal;
	}

	@Override
	public void pushFunction(String fn) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IExpr arg(JSExpr jsExpr) {
		if (!vars.containsKey(jsExpr))
			throw new NotImplementedException("there is no var for " + jsExpr.asVar());
		return vars.get(jsExpr);
	}

	@Override
	public void closure(boolean wantObject, JSExpr[] args) {
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
		doflush(call);
	}

	private void doflush(IExpr e) {
		if (assignNextTo != null) {
			Var v = md.avar(J.OBJECT, "iii"); // should have some var #
			md.assign(v, e);
			vars .put(assignNextTo, v);
			assignNextTo = null;
		}
	}

	@Override
	public void done() {
		if (isTest) {
			md.callInterface("void", runner, "testComplete").flush();
			md.returnVoid().flush();
		}
	}
}
