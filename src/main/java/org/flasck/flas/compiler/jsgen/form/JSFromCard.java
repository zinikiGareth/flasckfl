package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSFromCard implements JSExpr {
	private final NameOfThing cardName;

	public JSFromCard(NameOfThing cardName) {
		this.cardName = cardName;
	}
	
	@Override
	public String asVar() {
		return "this._card";
	}

	@Override
	public void write(IndentWriter w) {
		// shouldn't happen
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		String n = cardName instanceof CSName || cardName instanceof HandlerName ? cardName.container().javaName() : cardName.javaName();
		jvm.local(this, md.castTo(jvm.clazz().getField(md, "_card"), n));
	}

}
