package org.flasck.flas.generators;

import org.flasck.flas.commonBase.names.NameOfThing;

public class PureFunctionCodeGenerator implements CodeGenerator {

	@Override
	public void begin(GenerationContext cxt) {
		NameOfThing clzContext = cxt.nameContext();
		final String inClz = clzContext.javaName() + ".PACKAGEFUNCTIONS";
		cxt.selectClass(inClz);
		cxt.staticMethod(false);
		cxt.trampoline(inClz);
	}
}
