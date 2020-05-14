package org.flasck.flas.compiler.jsgen.form;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.jsgen.JSStyleIf;
import org.flasck.flas.parsedForm.TemplateField;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSUpdateStyle implements JSExpr {
	private final TemplateField field;
	private final JSExpr constant;
	private final List<JSStyleIf> vars;

	public JSUpdateStyle(TemplateField field, JSExpr c, List<JSStyleIf> styles) {
		this.field = field;
		this.constant = c;
		this.vars = new ArrayList<>(styles);
	}
	
	@Override
	public String asVar() {
		throw new NotImplementedException("not for use");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("this._updateStyle(_cxt, _renderTree, '");
		w.print(field.type().toString().toLowerCase());
		w.print("', '");
		w.print(field.text);
		w.print("', ");
		w.print(constant.asVar());
		for (JSStyleIf si : vars) {
			w.print(", ");
			w.print(si.cond.asVar());
			w.print(", ");
			w.print(si.style.asVar());
		}
		w.println(");");
	}
}
