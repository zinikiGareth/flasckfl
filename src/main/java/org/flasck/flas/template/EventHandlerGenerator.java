package org.flasck.flas.template;

public interface EventHandlerGenerator {

	void handle(boolean giveDistinguishedName, String action, String callFn);

	void done();

}
