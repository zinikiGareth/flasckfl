package org.flasck.flas.generators;

import org.flasck.flas.commonBase.names.NameOfThing;

public class ServiceMethodCodeGenerator<T> implements CodeGenerator<T> {

	@Override
	public void begin(GenerationContext<T> cxt) {
		NameOfThing clzContext = cxt.nameContext();
		cxt.selectClass(clzContext.javaClassName());
		cxt.instanceMethod();
	}

}
