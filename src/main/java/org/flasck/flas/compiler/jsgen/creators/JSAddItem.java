package org.flasck.flas.compiler.jsgen.creators;

import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
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
		w.print("this._addItem(_cxt, _renderTree, parent, currNode, ");
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

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr ai = md.callVirtual("void", md.myThis(), "_addItemWithName", jvm.cxt(), jvm.argAsIs(new JSVar("_renderTree")), jvm.argAsIs(new JSVar("parent")), jvm.argAsIs(new JSVar("currNode")), md.stringConst(templateName), md.intConst(posn), jvm.arg(expr), jvm.argAsIs(tc));
		jvm.local(this, ai);
	}

}
