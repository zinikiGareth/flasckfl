package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.parsedForm.TemplateField;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSUpdateStyle implements JSExpr {
	private final TemplateField field;
	private final String styles;

	public JSUpdateStyle(TemplateField field, String styles) {
		this.field = field;
		this.styles = styles;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("not for use");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("this._updateStyle(_cxt, '");
		w.print(field.text);
		w.print("', '");
		w.print(styles);
		w.println("');");
	}

}
