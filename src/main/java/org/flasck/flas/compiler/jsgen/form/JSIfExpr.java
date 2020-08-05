package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSIfCreator;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSIfExpr implements JSIfCreator {
	private final JSExpr test;
	private final JSBlockCreator trueCase;
	private final JSBlockCreator falseCase;

	public JSIfExpr(JSExpr test, JSBlockCreator trueCase, JSBlockCreator falseCase) {
		this.test = test;
		this.trueCase = trueCase;
		this.falseCase = falseCase;
	}

	public JSBlockCreator trueCase() {
		return trueCase;
	}

	public JSBlockCreator falseCase() {
		return falseCase;
	}

	@Override
	public String asVar() {
		return null;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("if (");
		test.write(w);
		w.print(") ");
		trueCase.write(w);
		JSExpr ec = falseCase.singleton();
		if (ec != null) {
			w.print(" else ");
			if (ec instanceof JSIfExpr)
				ec.write(w);
			else {
				w.println("");
				ec.write(w.indent());
			}
		} else if (falseCase.isEmpty()) {
			w.println("");
		} else {
			w.print(" else ");
			falseCase.write(w);
			w.println("");
		}
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		test.generate(jvm);
		trueCase.generate(jvm);
		falseCase.generate(jvm);
		IExpr ret = md.ifBoolean(jvm.argAs(test, JavaType.boolean_), jvm.blk(trueCase), jvm.blk(falseCase));
		jvm.local(this, ret);
	}
}
