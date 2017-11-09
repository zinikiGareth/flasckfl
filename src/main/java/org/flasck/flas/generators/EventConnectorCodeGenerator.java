package org.flasck.flas.generators;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.jvm.J;

public class EventConnectorCodeGenerator implements CodeGenerator {

	@Override
	public void begin(GenerationContext cxt) {
		FunctionName clzContext = cxt.funcName();
		cxt.selectClass(clzContext.javaNameAsNestedClass());
		cxt.implementsInterface(J.HANDLER);
//		cxt.staticMethod(false);
	}

}
