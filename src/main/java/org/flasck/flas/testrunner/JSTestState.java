package org.flasck.flas.testrunner;

import netscape.javascript.JSObject;

public class JSTestState {
	public final JSObject jsobj;
	public int failed = 0;

	public JSTestState(Object ret) {
		if (ret instanceof JSObject)
			this.jsobj = (JSObject) ret;
		else
			this.jsobj = null;
	}
}
