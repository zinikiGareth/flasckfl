package org.flasck.flas.testrunner;

public class JSTestState {
	public final SingleJSTest test;
	public int failed = 0;

	public JSTestState(SingleJSTest ret) {
		this.test = ret;
	}
}
