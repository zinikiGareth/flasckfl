package org.flasck.flas.compiler.jsgen.creators;

import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.TemplateField;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSUpdateContent implements JSExpr {
	private final TemplateField field;
	private final JSExpr expr;

	public JSUpdateContent(TemplateField field, JSExpr expr) {
		this.field = field;
		this.expr = expr;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("not for use");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("this._updateContent(_cxt, div, '");
		w.print(field.text);
		w.print("', ");
		w.print(expr.asVar());
		w.println(");");
	}

}
