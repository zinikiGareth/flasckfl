package org.flasck.flas.generators;

import org.flasck.flas.commonBase.names.NameOfThing;

public class PureFunctionCodeGenerator<T> implements CodeGenerator<T> {

	@Override
	public void begin(GenerationContext<T> cxt) {
		NameOfThing clzContext = cxt.nameContext();
		final String inClz = clzContext.javaName() + ".PACKAGEFUNCTIONS";
		if (cxt.selectClass(inClz)) {
			cxt.defaultCtor();
		}
		cxt.staticMethod();
		cxt.trampoline(inClz);
	}
}
