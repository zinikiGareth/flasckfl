package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.parsedForm.TemplateField;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSUpdatePunnet implements JSExpr {
	private final TemplateField field;
	private final JSExpr expr;
	private final int ucidx;

	public JSUpdatePunnet(TemplateField field, JSExpr expr, int ucidx) {
		this.field = field;
		this.expr = expr;
		this.ucidx = ucidx;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("not for use");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("this._updatePunnet(_cxt, _renderTree, '");
		w.print(field.text);
		w.print("', ");
		w.print(expr.asVar());
		w.print(", ");
		w.print("this._updatePunnet");
		w.print(Integer.toString(ucidx));
		w.println(");");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr uc = md.callVirtual("void", jvm.argAsIs(new JSThis()), "_updatePunnet", jvm.cxt(), jvm.argAsIs(new JSVar("_renderTree")), md.stringConst(field.text), jvm.arg(expr), md.intConst(ucidx));
		jvm.local(this, uc);
	}
}
