package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSMockCard implements IVForm {
	private final CardName name;
	private JSLocal nameAs;

	public JSMockCard(CardName name) {
		this.name = name;
	}

	public void nameAs(JSLocal ret) {
		nameAs = ret;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.mockCard('");
		w.print(nameAs.asVar());
		w.print("', new ");
		w.print(name.jsName());
		w.print("(_cxt))");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr agent = md.makeNew(name.javaName(), jvm.cxt());
		IExpr mc = md.callInterface(J.MOCKCARD, jvm.cxt(), "mockCard", md.as(agent, J.FLCARD));
		jvm.local(this, mc);
	}

	@Override
	public void asivm(IVFWriter iw) {
		iw.print("mockCard[" + name.uniqueName() + "]");
	}
}
