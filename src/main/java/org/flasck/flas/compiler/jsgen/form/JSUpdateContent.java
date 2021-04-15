package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.jvm.J;
import org.ziniki.splitter.FieldType;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.HaventConsideredThisException;
import org.zinutils.exceptions.NotImplementedException;

public class JSUpdateContent implements JSExpr {
	private final String templateName;
	private final TemplateField field;
	private final int option;
	private final JSExpr source;
	private final String fromField;
	private final JSExpr expr;

	public JSUpdateContent(String templateName, TemplateField field, int option, JSExpr source, String fromField, JSExpr expr) {
		this.templateName = templateName;
		this.field = field;
		this.option = option;
		this.source = source;
		this.fromField = fromField;
		this.expr = expr;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("not for use");
	}

	@Override
	public void write(IndentWriter w) {
		if (field.type() == FieldType.CONTENT)
			w.print("this._updateContent(_cxt, _renderTree, '");
		else if (field.type() == FieldType.IMAGE)
			w.print("this._updateImage(_cxt, _renderTree, '");
		else if (field.type() == FieldType.LINK)
			w.print("this._updateLink(_cxt, _renderTree, '");
		else
			throw new HaventConsideredThisException(field.type().name());
		w.print(templateName);
		w.print("', '");
		w.print(field.text);
		w.print("', ");
		w.print(Integer.toString(option));
		w.print(", ");
		w.print(source.asVar());
		w.print(", ");
		w.print(expr.asVar());
		w.print(", ");
		if (fromField == null)
			w.print("null");
		else
			w.print("\"" + fromField + "\"");
		w.println(");");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		String fn;
		if (field.type() == FieldType.CONTENT)
			fn = "_updateContent";
		else if (field.type() == FieldType.IMAGE)
			fn = "_updateImage";
		else if (field.type() == FieldType.LINK)
			fn = "_updateLink";
		else
			throw new HaventConsideredThisException(field.type().name());
		IExpr me = md.callVirtual("void", jvm.argAsIs(new JSThis()), fn, jvm.cxt(), jvm.argAsIs(new JSVar("_renderTree")), md.stringConst(templateName), md.stringConst(field.text), md.intConst(this.option), jvm.arg(source), jvm.arg(expr), fromField == null ? md.as(md.aNull(), J.STRING) : md.stringConst(fromField));
		jvm.local(this, me);
	}

}
