package org.flasck.flas.template;

import org.flasck.flas.commonBase.names.SolidName;

public interface EventHandlerGenerator {

	void handle(boolean giveDistinguishedName, String action, SolidName callFn);

	void done();

}
