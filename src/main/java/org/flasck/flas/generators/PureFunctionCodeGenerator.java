package org.flasck.flas.generators;

import org.flasck.flas.commonBase.names.NameOfThing;

public class PureFunctionCodeGenerator implements CodeGenerator {

	@Override
	public void begin(GenerationContext cxt) {
		NameOfThing clzContext = cxt.nameContext();
		cxt.selectClass(clzContext.javaName() + ".PACKAGEFUNCTIONS");
		cxt.staticMethod(false);
	}
}
