package org.flasck.flas.compiler.jsgen.form;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.jsgen.JSStyleIf;
import org.flasck.flas.parsedForm.TemplateField;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSUpdateStyle implements JSExpr {
	private final TemplateField field;
	private final String constant;
	private final List<JSStyleIf> vars = new ArrayList<>();

	public JSUpdateStyle(TemplateField field, List<JSStyleIf> styles) {
		this.field = field;
		StringBuilder sb = new StringBuilder();
		for (JSStyleIf si : styles) {
			if (si.cond == null) {
				if (sb.length() > 0)
					sb.append(" ");
				sb.append(si.styles);
			} else
				vars.add(si);
		}
		this.constant = sb.toString();
	}
	
	@Override
	public String asVar() {
		throw new NotImplementedException("not for use");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("this._updateStyle(_cxt, '");
		w.print(field.type().toString().toLowerCase());
		w.print("', '");
		w.print(field.text);
		w.print("', '");
		w.print(constant);
		w.print("'");
		for (JSStyleIf si : vars) {
			w.print(", ");
			w.print(si.cond.asVar());
			w.print(", '");
			w.print(si.styles);
			w.print("'");
		}
		w.println(");");
	}
}
