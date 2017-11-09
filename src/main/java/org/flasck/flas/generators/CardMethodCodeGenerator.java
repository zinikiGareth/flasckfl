package org.flasck.flas.generators;

import org.flasck.flas.commonBase.names.NameOfThing;

public class CardMethodCodeGenerator implements CodeGenerator {

	@Override
	public void begin(GenerationContext cxt) {
		NameOfThing clzContext = cxt.nameContext();
		cxt.selectClass(clzContext.javaPackageName());
		cxt.instanceMethod(false);
	}

}
