package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSStoreField implements JSExpr {

	private final JSExpr inObj;
	private final String field;
	private final JSExpr value;

	public JSStoreField(JSExpr inObj, String field, JSExpr value) {
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
		// TODO Auto-generated method stub
		
	}
}
