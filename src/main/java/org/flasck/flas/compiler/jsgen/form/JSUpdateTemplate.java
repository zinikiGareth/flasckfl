package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.parsedForm.TemplateField;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSUpdateTemplate implements JSExpr {
	private final TemplateField field;
	private final int posn;
	private final String templateName;
	private final JSExpr expr;
	private final JSExpr tc;

	public JSUpdateTemplate(TemplateField field, int posn, String templateName, JSExpr expr, JSExpr tc) {
		this.field = field;
		this.posn = posn;
		this.templateName = templateName;
		this.expr = expr;
		this.tc = tc;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("not for use");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("this._updateTemplate(_cxt, '");
		w.print(field.type().toString().toLowerCase());
		w.print("', '");
		w.print(field.text);
		w.print("', ");
		w.print("this._updateTemplate" + posn);
		w.print(", '");
		w.print(templateName);
		w.print("', ");
		w.print(expr.asVar());
		w.print(", ");
		w.print(tc.asVar());
		w.println(");");
	}

}
