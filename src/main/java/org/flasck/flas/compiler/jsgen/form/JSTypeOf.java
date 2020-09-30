package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.tc3.NamedType;
import org.flasck.jvm.J;
import org.flasck.jvm.builtin.TypeOf;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSTypeOf implements JSExpr {
	private final NamedType type;
	private final JSExpr expr;

	public JSTypeOf(NamedType defn) {
		this.type = defn;
		this.expr = null;
	}

	public JSTypeOf(JSExpr expr) {
		this.type = null;
		this.expr = expr;
	}

	@Override
	public String asVar() {
		if (type != null) {
			String tn = type.name().jsName();
			if ("Number".equals(tn))
				tn = "'number'";
			else if ("Type".equals(tn))
				tn = "'TypeOf'";
			return "new TypeOf(" + tn + ")";
		} else {
			return "TypeOf.eval(_cxt, " + expr.asVar() + ")";
		}
	}

	@Override
	public void write(IndentWriter w) {
		throw new NotImplementedException();
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		if (type != null) {
			String tn = type.name().javaName();
			if ("org.flasck.jvm.builtin.Number".equals(tn))
				tn = Double.class.getName();
			else if ("org.flasck.jvm.builtin.Type".equals(tn))
				tn = TypeOf.class.getName();
			jvm.local(this, md.makeNew(J.TYPEOF, md.classConst(tn)));
		} else {
			IExpr call = md.callInterface(J.FLCLOSURE, jvm.cxt(), "closure", md.as(md.makeNew(J.CALLEVAL, md.classConst(J.TYPEOF)), J.APPLICABLE), md.arrayOf(J.OBJECT, jvm.arg(expr)));
			jvm.local(this, call);
		}
	}

}
