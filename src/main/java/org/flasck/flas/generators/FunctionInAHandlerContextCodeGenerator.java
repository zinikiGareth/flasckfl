package org.flasck.flas.generators;

import org.flasck.flas.commonBase.names.NameOfThing;

public class FunctionInAHandlerContextCodeGenerator implements CodeGenerator {

	@Override
	public void begin(GenerationContext cxt) {
		NameOfThing clzContext = cxt.nameContext();
		final String inClz = clzContext.javaClassName();
		cxt.selectClass(inClz);
		cxt.instanceMethod(false);
		cxt.trampolineWithSelf(inClz);
	}

}
