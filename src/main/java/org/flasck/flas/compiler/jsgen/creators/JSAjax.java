package org.flasck.flas.compiler.jsgen.creators;

import java.net.URI;

import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSAjax implements JSExpr {
	private final JSExpr runner;
	private final StringLiteral url;

	public JSAjax(JSExpr runner, StringLiteral url) {
		this.runner = runner;
		this.url = url;
	}

	@Override
	public String asVar() {
		return runner.asVar() + ".newAjax(_cxt, new URL(" + new JSString(url.text).asVar() + "))";
	}

	@Override
	public void write(IndentWriter w) {
		throw new NotImplementedException("should be assigned directly");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		IExpr r = jvm.argAsIs(runner);
		IExpr uri = jvm.method().makeNew(URI.class.getName(), jvm.method().stringConst(url.text));
		jvm.local(this, jvm.method().callInterface(J.AJAXMOCK, r, "newAjax", jvm.cxt(), uri));
	}

}
