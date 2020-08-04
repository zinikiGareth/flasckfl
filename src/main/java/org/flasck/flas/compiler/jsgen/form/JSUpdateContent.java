package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.parsedForm.TemplateField;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSUpdateContent implements JSExpr {
	private final String templateName;
	private final TemplateField field;
	private final int option;
	private final JSExpr source;
	private final JSExpr expr;

	public JSUpdateContent(String templateName, TemplateField field, int option, JSExpr source, JSExpr expr) {
		this.templateName = templateName;
		this.field = field;
		this.option = option;
		this.source = source;
		this.expr = expr;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("not for use");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("this._updateContent(_cxt, _renderTree, '");
		w.print(templateName);
		w.print("', '");
		w.print(field.text);
		w.print("', ");
		w.print(Integer.toString(option));
		w.print(", ");
		w.print(source.asVar());
		w.print(", ");
		w.print(expr.asVar());
		w.println(");");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		// TODO Auto-generated method stub
		
	}

}
