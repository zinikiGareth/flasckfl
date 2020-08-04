package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.parsedForm.TemplateField;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSUpdateContainer implements JSExpr {
	private final TemplateField field;
	private final JSExpr expr;
	private final int ucidx;

	public JSUpdateContainer(TemplateField field, JSExpr expr, int ucidx) {
		this.field = field;
		this.expr = expr;
		this.ucidx = ucidx;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("not for use");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("this._updateContainer(_cxt, _renderTree, '");
		w.print(field.text);
		w.print("', ");
		w.print(expr.asVar());
		w.print(", ");
		w.print("this._updateContainer");
		w.print(Integer.toString(ucidx));
		w.println(");");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		// TODO Auto-generated method stub
		
	}
}
