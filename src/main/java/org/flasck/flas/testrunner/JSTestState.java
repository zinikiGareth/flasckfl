package org.flasck.flas.testrunner;

import org.flasck.flas.testrunner.CommonTestRunner.CommonState;

public class JSTestState extends CommonState {
	public final SingleJSTest test;

	public JSTestState(SingleJSTest ret) {
		this.test = ret;
	}
}
