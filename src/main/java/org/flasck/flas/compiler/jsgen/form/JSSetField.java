package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSSetField implements IVForm {
	private final JSExpr on;
	private final String field;
	private final JSExpr value;

	public JSSetField(String field, JSExpr value) {
		this(new JSThis(), field, value);
	}

	public JSSetField(JSExpr on, String field, JSExpr value) {
		this.on = on;
		this.field = field;
		this.value = value;
	}

	@Override
	public String asVar() {
		return on.asVar() + "." + field;
	}

	@Override
	public void write(IndentWriter w) {
		w.print(on.asVar());
		w.print(".");
		w.print(field);
		w.print(" = ");
		value.write(w);
		w.println(";");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		if ("_contracts".equals(field)) {
			// I believe that this is handled through reflection in Java
			jvm.local(this, null);
			return;
		}
		else if ("_template".equals(field)) {
			// I believe that this is a constructor arg in Java
			jvm.local(this, null);
			return;
		}
		if (!jvm.hasLocal(value))
			value.generate(jvm);
		NewMethodDefiner ctor = jvm.method();
		IExpr ret = ctor.assign(ctor.getField(field), jvm.argAsIs(value));
		jvm.local(this, ret);
	}
	
	@Override
	public void asivm(IVFWriter iw) {
		iw.print(on.asVar() + "[" + field + "] <- ");
		iw.write(value);
		iw.println("");
	}

	@Override
	public String toString() {
		return "JSSetField[" + field + " <- " + value + "]";
	}
}
