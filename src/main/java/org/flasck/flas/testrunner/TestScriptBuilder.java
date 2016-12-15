package org.flasck.flas.testrunner;

import org.flasck.flas.blockForm.InputPosition;

public interface TestScriptBuilder {
	void error(InputPosition posn, String msg);

	void addAssert(InputPosition evalPos, Object eval, InputPosition pos, Object valueExpr);
	void addCreate(InputPosition at, String bindVar, String cardType);
	void addMatchElement(InputPosition posn, String onCard, String selector, String contents);
	// TODO: other steps as well

	void addTestCase(String message);



}
