package org.flasck.flas.generators;

import org.flasck.flas.commonBase.names.FunctionName;

public class EventConnectorCodeGenerator implements CodeGenerator {

	@Override
	public void begin(GenerationContext cxt) {
		FunctionName clzContext = cxt.funcName();
		cxt.selectClass(clzContext.javaPackageName());
		cxt.instanceMethod(false);
//		cxt.staticMethod(false);
	}

}
