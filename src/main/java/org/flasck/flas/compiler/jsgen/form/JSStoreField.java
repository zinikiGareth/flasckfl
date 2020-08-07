package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSStoreField implements JSExpr {
	private final boolean jsOnly;
	private final JSExpr inObj;
	private final String field;
	private final JSExpr value;

	public JSStoreField(boolean jsOnly, JSExpr inObj, String field, JSExpr value) {
		this.jsOnly = jsOnly;
		this.inObj = inObj;
		this.field = field;
		this.value = value;
	}

	@Override
	public void write(IndentWriter w) {
		if (inObj == null)
			w.print("this");
		else
			w.print(inObj.asVar());
		w.print(".state.set('");
		w.print(field);
		w.print("', ");
		w.print(value.asVar());
		w.println(");");
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		if (!jsOnly) {
			if (!jvm.hasLocal(value))
				value.generate(jvm);
			IExpr arg = jvm.arg(value);
			IExpr item;
			if (inObj == null)
				item = md.myThis();
			else {
				if (!jvm.hasLocal(inObj))
					inObj.generate(jvm);
				item = jvm.argAsIs(inObj);
			}
			IExpr svar = md.getField(item, "state");
			IExpr doset = md.callInterface("void", svar, "set", md.stringConst(field), arg);
			jvm.local(this, doset);
		} else {
			jvm.local(this, null);
		}
	}
}
