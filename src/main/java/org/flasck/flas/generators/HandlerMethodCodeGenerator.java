package org.flasck.flas.generators;

import org.flasck.flas.commonBase.names.NameOfThing;

public class HandlerMethodCodeGenerator implements CodeGenerator {

	@Override
	public void begin(GenerationContext cxt) {
		NameOfThing clzContext = cxt.nameContext();
		cxt.selectClass(clzContext.javaClassName());
		cxt.instanceMethod(true);
	}

}
