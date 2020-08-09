package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSMockAgent implements JSExpr {
	private final CardName name;

	public JSMockAgent(CardName name) {
		this.name = name;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.mockAgent(new ");
		w.print(name.jsName());
		w.print("(_cxt))");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr agent = md.makeNew(name.javaName(), jvm.cxt());
		IExpr mc = md.callInterface(J.MOCKAGENT, jvm.cxt(), "mockAgent", md.as(agent, J.CONTRACT_HOLDER));
		jvm.local(this, mc);
	}
}
