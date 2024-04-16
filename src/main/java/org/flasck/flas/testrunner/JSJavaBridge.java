package org.flasck.flas.testrunner;

import org.zinutils.sync.LockingCounter;

public interface JSJavaBridge {

	void sendJson(String json);

	void log(String s);

	void debugmsg(String s);

	void error(String s);

	LockingCounter getTestCounter();

}
