package org.flasck.flas.testrunner;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.testrunner.MatchStep.WhatToMatch;

public interface TestScriptBuilder {
	void error(InputPosition posn, String msg);

	void addAssert(InputPosition evalPos, Object eval, InputPosition pos, Object valueExpr);
	void addCreate(InputPosition at, String bindVar, String cardType);
	void addMatch(InputPosition posn, WhatToMatch what, String onCard, String selector, String contents);
	// TODO: send

	void addTestCase(String message);



}
