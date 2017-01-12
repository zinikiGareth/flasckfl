package org.flasck.flas.template;

import org.flasck.flas.commonBase.names.FunctionName;

public interface EventHandlerGenerator {

	void handle(boolean giveDistinguishedName, String action, FunctionName handlerFn);

	void done();

}
