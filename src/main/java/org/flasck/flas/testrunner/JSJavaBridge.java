package org.flasck.flas.testrunner;

import org.ziniki.ziwsh.intf.JsonSender;
import org.zinutils.sync.LockingCounter;

public interface JSJavaBridge {

	void sendJson(String json);

	void transport(JsonSender toZiniki);

	Object module(String s);

	void log(String s);

	void error(String s);

	LockingCounter getTestCounter();

}
