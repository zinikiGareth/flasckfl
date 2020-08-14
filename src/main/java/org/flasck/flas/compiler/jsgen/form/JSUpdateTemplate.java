package org.flasck.flas.compiler.jsgen.form;


import java.util.List;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSUpdateTemplate implements JSExpr {
	private final TemplateField field;
	private final int posn;
	private final JSExpr onObj;
	private final String templateName;
	private final JSExpr expr;
	private final JSExpr tc;

	public JSUpdateTemplate(TemplateField field, int posn, boolean isOtherObject, String templateName, JSExpr expr, JSExpr tc) {
		this.field = field;
		this.posn = posn;
		this.onObj = isOtherObject ? expr : new JSThis();
		this.templateName = templateName;
		this.expr = isOtherObject ? null : expr;
		this.tc = tc;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("not for use");
	}

	@Override
	public void write(IndentWriter w) {
		IndentWriter iw = w;
		if (expr == null) {
			// we need to be sure that onObj is defined
			w.print("if (");
			w.print(onObj.asVar());
			w.println(") {");
			iw = w.indent();
		}
		iw.print(onObj.asVar());
		iw.print("._updateTemplate(_cxt, _renderTree, '");
		iw.print(field.type().toString().toLowerCase());
		iw.print("', '");
		iw.print(field.text);
		iw.print("', ");
		iw.print(onObj.asVar());
		iw.print("._updateTemplate" + posn);
		iw.print(", '");
		iw.print(templateName);
		iw.print("'");
		if (expr != null) { // this is only necessary if onObj is true
			iw.print(", ");
			iw.print(expr.asVar());
			iw.print(", ");
			iw.print(tc.asVar());
		} else {
			// something is currently needed by the library to make it fire a foreign template ...
			iw.print(", true");
		}
			
		iw.println(");");
		if (expr == null) {
			w.println("}");
		}
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr ci = md.callInterface("void", jvm.argAs(onObj, new JavaType(J.TEMPLATE_HOLDER)), "_updateTemplate",
				jvm.cxt(), jvm.argAsIs(new JSVar("_renderTree")), 
				md.stringConst(field.type().toString().toLowerCase()), md.stringConst(field.text),
				md.intConst(posn),
				md.stringConst(templateName),
				expr == null ? md.makeNew(J.OBJECT) : jvm.arg(expr),
				jvm.argAs(tc, new JavaType(List.class.getName())));
		if (expr == null) {
			ci = md.ifNotNull(jvm.arg(onObj), ci, null);
		}
		jvm.local(this, ci);
	}

}
