package org.flasck.flas.compiler.jsgen.creators;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.compiler.jsgen.form.ClearRunner;
import org.flasck.flas.compiler.jsgen.form.IVFWriter;
import org.flasck.flas.compiler.jsgen.form.InitContext;
import org.flasck.flas.compiler.jsgen.form.JSCopyContract;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSInheritFrom;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSMethod extends JSBlock implements JSMethodCreator {
	private final JSStorage jse;
	private final NameOfThing fnName;
	private final NameOfThing clzName;
	private final boolean prototype;
	private boolean isOptional;
	private final String name;
	final List<JSVar> args = new ArrayList<>();
	private int nextVar = 1;
	private boolean wantArgumentList = false;
	private String returnsA = J.OBJECT;
	public List<JSExpr> superArgs = new ArrayList<>();
	private boolean hasHandler;
	private boolean genJS = true;
	private boolean genJVM = true;
	private JSExpr runner = null;

	public JSMethod(JSStorage jse, NameOfThing fnName, NameOfThing pkg, boolean prototype, String name) {
		this.jse = jse;
		this.fnName = fnName;
		this.clzName = pkg;
		this.prototype = prototype;
		this.name = name;
	}
	
	public String getPackage() {
		return clzName.jsName();
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public void noJS() {
		genJS = false;
	}

	@Override
	public void noJVM() {
		genJVM = false;
	}
	
	public void makeOptional() {
		isOptional = true;
	}

	@Override
	public String jsName() {
		if (name == null)
			return clzName.jsName();
		else
			return clzName.jsName() +"." + name;
	}
	
	@Override
	public void argumentList() {
		wantArgumentList = true;
	}

	@Override
	public JSVar argument(String type, String name) {
		JSVar ret = new JSVar(type, name);
		args.add(ret);
		return ret;
	}

	@Override
	public JSVar argument(String name) {
		JSVar ret = new JSVar(name);
		args.add(ret);
		return ret;
	}

	@Override
	public JSVar handlerArg() {
		hasHandler = true;
		return argument("_ih");
	}

	@Override
	public void returnsType(String ty) {
		this.returnsA = ty;
	}

	@Override
	public void helper(JSExpr runner) {
		this.runner = runner;
	}

	@Override
	public void superArg(JSExpr a) {
		superArgs.add(a);
	}
	
	public void inheritFrom(NameOfThing baseClass) {
		stmts.add(new JSInheritFrom(this, baseClass));
	}

	@Override
	public void clear() {
		stmts.add(new ClearRunner());
	}

	@Override
	public void initContext(boolean field) {
		stmts.add(new InitContext(jse, field));
	}

	@Override
	public void copyContract(JSExpr copyInto, String fld, String arg) {
		stmts.add(new JSCopyContract(copyInto, fld, arg));
	}
	
	public void write(IndentWriter w, Set<NameOfThing> names) {
		if (!genJS)
			return;
		w.println("");
		if (fnName != null && fnName instanceof FunctionName) {
			FunctionName fn = (FunctionName)fnName;
			ensureContainingNames(w, fn.container(), names);
			w.print(fn.jsPName());
		} else {
			w.print(clzName.jsName());
			if (name != null) {
				w.print(".");
				if (prototype)
					w.print("prototype.");
				w.print(name);
			}
		}
		w.print(" = function");
		w.print("(");
		boolean isFirst = true;
		for (JSVar v : args) {
			if (isFirst)
				isFirst = false;
			else
				w.print(", ");
			w.print(v.asVar());
		}
		w.print(") ");
		super.write(w);
		w.println("");
		if (name != null) {
			w.println("");
			if (fnName != null && fnName instanceof FunctionName)
				w.print(((FunctionName)fnName).jsPName());
			else {
				w.print(clzName.jsName());
				w.print(".");
				if (prototype)
					w.print("prototype.");
				w.print(name);
			}
			w.print(".nfargs = function() { return ");
			w.print(Integer.toString(args.size() - (hasHandler?2:1))); // -1 for context, -1 for handler if present
			w.println("; }");
		}
	}
	
	static void ensureContainingNames(IndentWriter w, NameOfThing container, Set<NameOfThing> curr) {
		if (container != null && !curr.contains(container)) {
			ensureContainingNames(w, container.container(), curr);
			if (container instanceof FunctionName) {
				String full = ((FunctionName) container).jsPName();
				w.print("if (typeof(");
				w.print(full);
				w.print(") === 'undefined') ");
				w.print(full);
				w.println(" = {};");
				w.println("");
			}
			curr.add(container);
		}
	}

	public void generate(ByteCodeEnvironment bce, boolean isInterface) {
		if (bce != null && genJVM) {
			JVMCreationContext jvm;
			if (fnName instanceof UnitTestName)
				jvm = new BasicJVMCreationContext(bce, (UnitTestName)fnName, args);
			else if (fnName == null && name == null)
				jvm = new BasicJVMCreationContext(bce, clzName, args, superArgs);
			else if (this.prototype)
				jvm = new BasicJVMCreationContext(bce, clzName, name, fnName, wantArgumentList, args, runner, returnsA);
			else
				jvm = new BasicJVMCreationContext(bce, clzName, name, fnName, wantArgumentList, args, runner, returnsA, null);
			if (!isInterface || isOptional) {
				if (isOptional)
					jvm.version(55);
				super.generate(jvm);
				jvm.done(this);
			}
		}
	}
	
	public String asivm() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		IVFWriter iw = new IVFWriter(pw);
		asivm(iw);
		return sw.toString();
	}

	public void asivm(IVFWriter iw) {
		if (this.name == null)
			iw.println("ctor");
		else
			iw.println("method " + this.name);
		super.asivm(iw);
	}

	public String obtainNextVar() {
		return "_v" + nextVar ++;
	}
	
	@Override
	public String toString() {
		return "method " + this.name;
	}
}
