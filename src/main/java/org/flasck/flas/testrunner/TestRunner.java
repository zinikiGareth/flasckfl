package org.flasck.flas.testrunner;

import org.flasck.jvm.FLEvalContext;

public interface TestRunner {
	String name();
	void invoke(FLEvalContext cx, Object sendExpr) throws Exception;
	void send(FLEvalContext cx, Object to, String contract, String meth, Object... args);
	void event(Object card, Object event) throws Exception;
//	void match(HTMLMatcher matcher, String selector) throws NotMatched;
//	void expect(String cardVar, String ctr, String method, List<Integer> chkargs);
//	void click(String selector);
}
