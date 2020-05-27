package org.flasck.flas.compiler.jsgen.creators;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.form.ClearRunner;
import org.flasck.flas.compiler.jsgen.form.InitContext;
import org.flasck.flas.compiler.jsgen.form.JSBlockComplete;
import org.flasck.flas.compiler.jsgen.form.JSCopyContract;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSInheritFrom;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSMethod extends JSBlock implements JSMethodCreator {
	private final JSStorage jse;
	private final String pkg;
	private final boolean prototype;
	private final String name;
	final List<JSVar> args = new ArrayList<>();
	private int nextVar = 1;

	public JSMethod(JSStorage jse, String pkg, boolean prototype, String name) {
		this.jse = jse;
		this.pkg = pkg;
		this.prototype = prototype;
		this.name = name;
	}
	
	public String getPackage() {
		return pkg;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public String jsName() {
		if (name == null)
			return pkg;
		else
			return pkg +"." + name;
	}
	
	@Override
	public JSExpr argument(String name) {
		JSVar ret = new JSVar(name);
		args.add(ret);
		return ret;
	}

	public void inheritFrom(NameOfThing baseClass) {
		stmts.add(new JSInheritFrom(baseClass));
	}

	@Override
	public void clear() {
		stmts.add(new ClearRunner());
	}

	@Override
	public void initContext(PackageName packageName) {
		stmts.add(new InitContext(packageName, jse));
	}

	@Override
	public void copyContract(JSExpr copyInto, String fld, String arg) {
		stmts.add(new JSCopyContract(copyInto, fld, arg));
	}
	
	@Override
	public void testComplete() {
		stmts.add(new JSBlockComplete());
	}

	@Override
	public void write(IndentWriter w) {
		w.println("");
		w.print(pkg);
		if (name != null) {
			w.print(".");
			if (prototype)
				w.print("prototype.");
			w.print(name);
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
			w.print(pkg);
			w.print(".");
			if (prototype)
				w.print("prototype.");
			w.print(name);
			w.print(".nfargs = function() { return ");
			w.print(Integer.toString(args.size() - 1)); // -1 for context
			w.println("; }");
		}
	}

	public String obtainNextVar() {
		return "v" + nextVar ++;
	}
}
