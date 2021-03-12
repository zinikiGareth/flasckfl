package org.flasck.flas.testrunner;

import org.ziniki.ziwsh.intf.JsonSender;
import org.zinutils.sync.LockingCounter;

public interface JSJavaBridge {

	void sendJson(String json);

	void transport(JsonSender toZiniki);

	Object module(Object runner, String s);

	void log(String s);

	void debugmsg(String s);

	void error(String s);

	LockingCounter getTestCounter();

}
