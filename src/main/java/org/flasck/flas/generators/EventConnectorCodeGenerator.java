package org.flasck.flas.generators;

import org.flasck.flas.commonBase.names.FunctionName;

public class EventConnectorCodeGenerator<T> implements CodeGenerator<T> {

	@Override
	public void begin(GenerationContext<T> cxt) {
		FunctionName clzContext = cxt.funcName();
		cxt.selectClass(clzContext.javaPackageName());
		cxt.instanceMethod();
//		cxt.staticMethod(false);
	}

}
