package org.flasck.flas.generators;

import org.flasck.flas.commonBase.names.NameOfThing;

public class EventMethodCodeGenerator<T> implements CodeGenerator<T> {

	@Override
	public void begin(GenerationContext<T> cxt) {
		NameOfThing clzContext = cxt.nameContext();
		final String inClz = clzContext.javaPackageName();
		cxt.selectClass(inClz);
		cxt.instanceMethod();
		cxt.trampolineWithSelf(inClz);
	}

}
