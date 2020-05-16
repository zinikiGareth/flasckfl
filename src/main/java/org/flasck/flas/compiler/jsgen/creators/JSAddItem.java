package org.flasck.flas.compiler.jsgen.creators;

import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSAddItem implements JSExpr {
	private final int posn;
	private final String templateName;
	private final JSExpr expr;
	private final JSExpr tc;

	public JSAddItem(int posn, String templateName, JSExpr expr, JSExpr tc) {
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
		w.print("this._addItem(_cxt, _renderTree, parent, ");
		w.print("document.getElementById('");
		w.print(templateName);
		w.print("'), ");
		w.print("this._updateTemplate" + posn);
		w.print(", ");
		w.print(expr.asVar());
		w.print(", ");
		w.print(tc.asVar());
		w.println(");");
	}

}
