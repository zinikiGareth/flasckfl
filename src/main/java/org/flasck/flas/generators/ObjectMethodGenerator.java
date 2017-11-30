package org.flasck.flas.generators;

import org.flasck.flas.commonBase.names.NameOfThing;

public class ObjectMethodGenerator<T> implements CodeGenerator<T> {

	@Override
	public void begin(GenerationContext<T> cxt) {
		NameOfThing clzContext = cxt.nameContext();
		final String inClz = clzContext.javaName();
		cxt.selectClass(inClz);
		cxt.instanceMethod();
	}

}
