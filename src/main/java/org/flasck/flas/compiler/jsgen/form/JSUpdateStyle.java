package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.parsedForm.TemplateField;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSUpdateStyle implements JSExpr {
	private final TemplateField field;
	private final JSExpr expr;
	private final String styles;

	public JSUpdateStyle(TemplateField field, JSExpr expr, String styles) {
		this.field = field;
		this.expr = expr;
		this.styles = styles;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("not for use");
	}

	@Override
	public void write(IndentWriter w) {
		IndentWriter iw = w;
		if (expr != null) {
			w.print("if (_cxt.isTruthy(");
			w.print(expr.asVar());
			w.println(")) {");
			iw = w.indent();
		}
		iw.print("this._updateStyle(_cxt, '");
		iw.print(field.text);
		iw.print("', '");
		iw.print(styles);
		iw.println("');");
		if (expr != null) {
			w.println("}");
		}
	}
}
