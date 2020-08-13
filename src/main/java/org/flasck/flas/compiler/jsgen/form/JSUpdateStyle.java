package org.flasck.flas.compiler.jsgen.form;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.jsgen.JSStyleIf;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSUpdateStyle implements JSExpr {
	private final String templateName;
	private final TemplateField field;
	private final int option;
	private final JSExpr source;
	private final JSExpr constant;
	private final List<JSStyleIf> vars;

	public JSUpdateStyle(String templateName, TemplateField field, int option, JSExpr source, JSExpr c, List<JSStyleIf> styles) {
		this.templateName = templateName;
		this.field = field;
		this.option = option;
		this.source = source;
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
		w.print(templateName);
		w.print("', ");
		if (field == null)
			w.print("null, null, null, ");
		else {
			w.print("'");
			w.print(field.type().toString().toLowerCase());
			w.print("', '");
			w.print(field.text);
			w.print("', ");
			w.print(Integer.toString(option));
			w.print(", ");
		}
		w.print(source.asVar());
		w.print(", ");
		w.print(constant.asVar());
		for (JSStyleIf si : vars) {
			w.print(", ");
			w.print(si.cond.asVar());
			w.print(", ");
			w.print(si.style.asVar());
		}
		w.println(");");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr ty;
		IExpr tx;
		if (field == null) {
			ty = md.as(md.aNull(), J.STRING);
			tx = md.as(md.aNull(), J.STRING);
		} else {
			ty = md.stringConst(field.type().toString().toLowerCase());
			tx = md.stringConst(field.text);
		}
		IExpr ce;
		if (constant == null)
			ce = md.as(md.aNull(), J.STRING);
		else {
			if (!jvm.hasLocal(constant))
				constant.generate(jvm);
			ce = jvm.argAs(constant, JavaType.string);
		}
		List<IExpr> arr = new ArrayList<>();
		for (JSStyleIf si : vars) {
			arr.add(jvm.arg(si.cond));
			arr.add(jvm.arg(si.style));
		}
		IExpr me = md.callVirtual("void", jvm.argAsIs(new JSThis()), "_updateStyles", jvm.cxt(), jvm.argAsIs(new JSVar("_renderTree")), md.stringConst(templateName), ty, tx, md.intConst(this.option), jvm.arg(source), ce, md.arrayOf(J.OBJECT, arr));
		jvm.local(this, me);
	}
}
