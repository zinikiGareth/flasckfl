package org.flasck.flas.testrunner;

import org.zinutils.sync.LockingCounter;

public interface JSJavaBridge {

	void sendJson(String json);

	Object module(Object runner, String s);

	void log(String s);

	void debugmsg(String s);

	void error(String s);

	LockingCounter getTestCounter();

}
